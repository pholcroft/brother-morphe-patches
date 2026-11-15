package app.template.extension.extension;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/** Learns responses from normal Brother traffic, stored in app-private preferences. */
public final class AutoCache {
    private static final String SNMP="com.brother.sdk.common.socket.devicemanagement.snmp.";
    private static final String WEST=SNMP+"westhawk.";
    private static final String MAC_OID="1.3.6.1.2.1.2.2.1.6.1";
    private static volatile SharedPreferences prefs;

    public static void init(Context context) {
        if(context!=null) prefs=context.getApplicationContext().getSharedPreferences("morphe_printer_cache_v1",Context.MODE_PRIVATE);
    }
    private static Object field(Object o,String name)throws ReflectiveOperationException{
        Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);
    }
    private static String address(Object socket)throws ReflectiveOperationException {
        if(socket==null || !socket.getClass().getName().equals("com.brother.sdk.network.BrDatagramSocket"))return null;
        InetAddress addr=(InetAddress)field(socket,"mInetAddress");
        if(addr==null || addr.isAnyLocalAddress() || addr.isMulticastAddress() || addr.isLoopbackAddress())return null;
        String ip=addr.getHostAddress();
        return ip!=null && ip.matches("[0-9]+(\\.[0-9]+){3}") && !ip.equals("255.255.255.255") ? ip:null;
    }
    private static String oid(Object request)throws ReflectiveOperationException {
        if(request==null || ((Byte)field(request,"msg_type"))!=(byte)0xa0)return null;
        Vector<?> bindings=(Vector<?>)field(request,"reqVarbinds");if(bindings.size()!=1)return null;
        Object obj=bindings.get(0).getClass().getMethod("getOid").invoke(bindings.get(0));
        String oid=(String)obj.getClass().getMethod("getValue").invoke(obj);
        return oid!=null && oid.matches("[0-9]+(\\.[0-9]+)+") ? oid:null;
    }
    private static String key(String ip,String oid){return "entry|"+ip+"|"+oid;}
    private static void diagnosticResult(String source,Object result){
        if(!Diagnostics.isRecording())return;
        try{
            String state=result==null?"No result":String.valueOf(result.getClass().getField("state").get(result));
            String type="",value="";
            if(result!=null && state.equals("SuccessRequest")){
                Object values=result.getClass().getField("results").get(result);
                if(values!=null && Array.getLength(values)==1){
                    Object v=Array.get(values,0).getClass().getMethod("getValue").invoke(Array.get(values,0));
                    if(v!=null && v.getClass().getName().equals(WEST+"AsnOctets")){byte[] bytes=(byte[])v.getClass().getMethod("getBytes").invoke(v);if(bytes.length<=8192){type="octets_base64";value=Base64.encodeToString(bytes,Base64.NO_WRAP);}else type="too_large";}
                    else if(v!=null && v.getClass().getName().equals(WEST+"AsnInteger")){type="integer";value=String.valueOf(v.getClass().getMethod("getValue").invoke(v));}
                    else if(v!=null)type="unsupported";
                }
            }
            Diagnostics.finish(source,state,type,value);
        }catch(Exception|LinkageError ignored){Diagnostics.finish(source,"unreadable","","");}
    }
    public static boolean enabled(){SharedPreferences p=prefs;return p!=null && p.getBoolean("enabled",true);}
    public static void setEnabled(boolean value){SharedPreferences p=prefs;if(p!=null)p.edit().putBoolean("enabled",value).apply();}
    public static boolean liveCheckEnabled(){SharedPreferences p=prefs;return p!=null && p.getBoolean("live_check",false);}
    public static void setLiveCheckEnabled(boolean value){SharedPreferences p=prefs;if(p!=null)p.edit().putBoolean("live_check",value).apply();}
    public static String readString(String key,String fallback){SharedPreferences p=prefs;return p==null?fallback:p.getString(key,fallback);}
    public static String printerName(String ip){
        SharedPreferences p=prefs;if(p==null||ip==null)return "Saved printer";
        for(String id:new String[]{"1.3.6.1.2.1.25.3.2.1.3.1","1.3.6.1.2.1.1.1.0"}){
            try{String saved=p.getString(key(ip,id),null);if(saved==null||!saved.startsWith("octets:"))continue;
                String name=new String(Base64.decode(saved.substring(7),Base64.DEFAULT),StandardCharsets.UTF_8).replaceAll("[\\p{Cntrl}\\s]+"," ").trim();
                if(!name.isEmpty())return name.length()>80?name.substring(0,77)+"…":name;
            }catch(Exception ignored){}
        }
        return "Saved printer";
    }
    public static void writeString(String key,String value){SharedPreferences p=prefs;if(p!=null)p.edit().putString(key,value).apply();}
    public static Set<String> printers(){
        Set<String> result=new HashSet<>();SharedPreferences p=prefs;if(p==null)return result;
        for(String k:p.getAll().keySet())if(k.startsWith("entry|")){int end=k.indexOf('|',6);if(end>6)result.add(k.substring(6,end));}
        return result;
    }
    public static void clearPrinter(String ip){
        SharedPreferences p=prefs;if(p==null||ip==null)return;SharedPreferences.Editor e=p.edit();
        for(String k:p.getAll().keySet())if(k.startsWith("entry|"+ip+"|")||k.equals("mac|"+ip)||k.equals("status|"+ip))e.remove(k);e.apply();
    }
    public static void clearAll(){for(String ip:printers())clearPrinter(ip);}
    public static Object lookup(Object socket,Object request){
        try{
            Diagnostics.abandon();
            String ip=address(socket),id=oid(request);if(ip==null||id==null)return null;
            Diagnostics.begin(ip,id);
            Readiness.maybeCheck(ip);if(!enabled())return null;
            String saved=prefs.getString(key(ip,id),null);if(saved==null)return null;
            String[] parts=saved.split(":",2);ClassLoader loader=request.getClass().getClassLoader();
            Class<?> resultClass=loader.loadClass(SNMP+"SnmpResult");Object result=resultClass.getConstructor().newInstance();
            if(parts[0].equals("missing")){
                Class<?> state=loader.loadClass(SNMP+"SnmpResult$SnmpState");resultClass.getField("state").set(result,state.getField("ErrorRequestNoSuchName").get(null));diagnosticResult("cache",result);return result;
            }
            if(parts.length!=2)return null;Class<?> asn=loader.loadClass(WEST+"AsnObject"),vb=loader.loadClass(WEST+"Varbind");Object value;
            if(parts[0].equals("octets"))value=loader.loadClass(WEST+"AsnOctets").getConstructor(byte[].class).newInstance((Object)Base64.decode(parts[1],Base64.DEFAULT));
            else if(parts[0].equals("integer"))value=loader.loadClass(WEST+"AsnInteger").getConstructor(int.class).newInstance(Integer.parseInt(parts[1]));
            else return null;
            Object bind=vb.getConstructor(String.class,asn).newInstance(id,value);Object array=Array.newInstance(vb,1);Array.set(array,0,bind);resultClass.getField("results").set(result,array);diagnosticResult("cache",result);return result;
        }catch(Exception|LinkageError ignored){return null;}
    }
    public static void record(Object socket,Object request,Object result){
        try{
            diagnosticResult("printer",result);
            if(!enabled()||result==null)return;String ip=address(socket),id=oid(request);if(ip==null||id==null)return;
            String state=String.valueOf(result.getClass().getField("state").get(result));String encoded;
            if(state.equals("ErrorRequestNoSuchName"))encoded="missing:";
            else if(state.equals("SuccessRequest")){
                Object values=result.getClass().getField("results").get(result);if(values==null||Array.getLength(values)!=1)return;
                Object binding=Array.get(values,0);Object returnedOid=binding.getClass().getMethod("getOid").invoke(binding);
                if(!id.equals(returnedOid.getClass().getMethod("getValue").invoke(returnedOid)))return;
                Object value=binding.getClass().getMethod("getValue").invoke(binding);if(value==null)return;
                if(value.getClass().getName().equals(WEST+"AsnOctets")){
                    byte[] bytes=(byte[])value.getClass().getMethod("getBytes").invoke(value);if(bytes.length>8192)return;
                    encoded="octets:"+Base64.encodeToString(bytes,Base64.NO_WRAP);
                    if(id.equals(MAC_OID)&&bytes.length==6){StringBuilder mac=new StringBuilder();for(byte b:bytes){if(mac.length()>0)mac.append(':');mac.append(String.format(java.util.Locale.ROOT,"%02x",b&255));}prefs.edit().putString("mac|"+ip,mac.toString()).apply();}
                }else if(value.getClass().getName().equals(WEST+"AsnInteger"))encoded="integer:"+value.getClass().getMethod("getValue").invoke(value);
                else return;
            }else return;
            prefs.edit().putString(key(ip,id),encoded).apply();
        }catch(Exception|LinkageError ignored){}
    }
    private AutoCache(){}
}
