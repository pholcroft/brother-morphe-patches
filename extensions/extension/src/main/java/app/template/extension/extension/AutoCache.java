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
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

/** Learns responses from normal Brother traffic, stored in app-private preferences. */
public final class AutoCache {
    private static final String SNMP="com.brother.sdk.common.socket.devicemanagement.snmp.";
    private static final String WEST=SNMP+"westhawk.";
    private static final String MAC_OID="1.3.6.1.2.1.2.2.1.6.1";
    private static volatile SharedPreferences prefs;
    private static final ConcurrentHashMap<String,Long> VERIFIED=new ConcurrentHashMap<>();
    private static final LinkedHashMap<String,LinkedHashMap<String,String>> PENDING_LIVE=new LinkedHashMap<>();
    private static final long VERIFY_TTL_MS=30000;

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
    private static synchronized void resetVerification(){VERIFIED.clear();PENDING_LIVE.clear();}
    private static long monotonicMillis(){return System.nanoTime()/1000000L;}
    private static boolean isVerified(String ip){Long t=VERIFIED.get(ip);return t!=null && monotonicMillis()-t<VERIFY_TTL_MS;}
    private static String macAddress(byte[] bytes){
        if(bytes==null||bytes.length!=6||(bytes[0]&1)!=0)return null;
        boolean zero=true;for(byte b:bytes)if(b!=0)zero=false;if(zero)return null;
        StringBuilder s=new StringBuilder();for(byte b:bytes){if(s.length()>0)s.append(':');s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));}return s.toString();
    }
    private static void archiveAddress(SharedPreferences p,String ip,String mac){
        SharedPreferences.Editor edit=p.edit();String prefix="entry|"+ip+"|";
        for(Map.Entry<String,?> e:p.getAll().entrySet())if(e.getKey().startsWith(prefix)){
            if(mac!=null && e.getValue() instanceof String)edit.putString("identity|"+mac+"|"+e.getKey().substring(prefix.length()),(String)e.getValue());
            edit.remove(e.getKey());
        }
        edit.remove("mac|"+ip).remove("status|"+ip).apply();VERIFIED.remove(ip);
    }
    private static synchronized void acceptIdentity(String ip,String mac){
        SharedPreferences p=prefs;if(p==null)return;
        String previous=p.getString("mac|"+ip,null);
        if(previous==null||!previous.equals(mac))archiveAddress(p,ip,previous);
        String last=p.getString("last|"+mac,null);boolean moved=false;
        for(Map.Entry<String,?> entry:p.getAll().entrySet()){
            String k=entry.getKey();if(!k.startsWith("mac|")||!mac.equals(entry.getValue()))continue;
            String old=k.substring(4);if(!old.equals(ip)){moved=true;archiveAddress(p,old,mac);}
        }
        SharedPreferences.Editor edit=p.edit();String prefix="identity|"+mac+"|";
        for(Map.Entry<String,?> entry:p.getAll().entrySet())if(entry.getKey().startsWith(prefix)&&entry.getValue() instanceof String){
            String dest=key(ip,entry.getKey().substring(prefix.length()));
            if(!p.getAll().containsKey(dest))edit.putString(dest,(String)entry.getValue());
            edit.remove(entry.getKey());
        }
        LinkedHashMap<String,String> pending=PENDING_LIVE.remove(ip);
        if(pending!=null)for(Map.Entry<String,String> entry:pending.entrySet())edit.putString(key(ip,entry.getKey()),entry.getValue());
        edit.putString("mac|"+ip,mac).putString("last|"+mac,ip);
        if(moved||(last!=null&&!last.equals(ip)))edit.putBoolean("dynamic|"+mac,true);
        edit.apply();VERIFIED.put(ip,monotonicMillis());
    }
    public static boolean hasChangedAddress(String ip){SharedPreferences p=prefs;if(p==null)return false;String mac=p.getString("mac|"+ip,null);return mac!=null&&p.getBoolean("dynamic|"+mac,false);}
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
    public static synchronized void setEnabled(boolean value){SharedPreferences p=prefs;if(p!=null)p.edit().putBoolean("enabled",value).apply();if(!value)resetVerification();}
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
    public static synchronized void clearPrinter(String ip){
        SharedPreferences p=prefs;if(p==null||ip==null)return;SharedPreferences.Editor e=p.edit();
        String mac=p.getString("mac|"+ip,null);
        for(String k:p.getAll().keySet())if(k.startsWith("entry|"+ip+"|")||k.equals("mac|"+ip)||k.equals("status|"+ip)||(mac!=null&&(k.startsWith("identity|"+mac+"|")||k.equals("last|"+mac)||k.equals("dynamic|"+mac))))e.remove(k);e.apply();VERIFIED.remove(ip);PENDING_LIVE.remove(ip);
    }
    public static synchronized void clearAll(){SharedPreferences p=prefs;if(p==null)return;SharedPreferences.Editor e=p.edit();for(String k:p.getAll().keySet())if(k.startsWith("entry|")||k.startsWith("mac|")||k.startsWith("status|")||k.startsWith("identity|")||k.startsWith("last|")||k.startsWith("dynamic|"))e.remove(k);e.apply();VERIFIED.clear();PENDING_LIVE.clear();}
    private static void stageLive(String ip,String oid,String value){
        LinkedHashMap<String,String> pending=PENDING_LIVE.get(ip);
        if(pending==null){if(PENDING_LIVE.size()>=8)PENDING_LIVE.remove(PENDING_LIVE.keySet().iterator().next());pending=new LinkedHashMap<>();PENDING_LIVE.put(ip,pending);}
        if(!pending.containsKey(oid)&&pending.size()>=128)pending.remove(pending.keySet().iterator().next());pending.put(oid,value);
    }
    public static synchronized Object lookup(Object socket,Object request){
        try{
            Diagnostics.abandon();
            String ip=address(socket),id=oid(request);if(ip==null||id==null)return null;
            Diagnostics.begin(ip,id);
            Readiness.maybeCheck(ip);if(!enabled())return null;
            if(id.equals(MAC_OID)){VERIFIED.remove(ip);return null;}
            if(!isVerified(ip))return null;
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
    public static synchronized void record(Object socket,Object request,Object result){
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
                    if(id.equals(MAC_OID)){String mac=macAddress(bytes);if(mac==null){VERIFIED.remove(ip);PENDING_LIVE.remove(ip);return;}acceptIdentity(ip,mac);}
                }else if(value.getClass().getName().equals(WEST+"AsnInteger"))encoded="integer:"+value.getClass().getMethod("getValue").invoke(value);
                else return;
            }else return;
            if(isVerified(ip))prefs.edit().putString(key(ip,id),encoded).apply();else if(!id.equals(MAC_OID))stageLive(ip,id,encoded);
        }catch(Exception|LinkageError ignored){}
    }
    private AutoCache(){}
}
