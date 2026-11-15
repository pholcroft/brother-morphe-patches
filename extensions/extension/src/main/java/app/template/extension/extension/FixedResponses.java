package app.template.extension.extension;

import android.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

/** Aggressive speed experiment: fixed snapshot, unknown GETs appear unavailable. */
public final class FixedResponses {
    private static final String SNMP="com.brother.sdk.common.socket.devicemanagement.snmp.";
    private static final String WEST=SNMP+"westhawk.";
    private static String loadedSnapshot;
    private static Map<String,Entry> entries;
    private static final class Entry {
        final int tag; final byte[] value;
        Entry(int tag, byte[] value) { this.tag=tag; this.value=value; }
    }
    private static synchronized Map<String,Entry> snapshot(String encoded) throws Exception {
        if (encoded.equals(loadedSnapshot) && entries!=null) return entries;
        byte[] compressed=Base64.decode(encoded,Base64.DEFAULT);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(GZIPInputStream in=new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            byte[] buffer=new byte[2048]; int n;
            while((n=in.read(buffer))!=-1) { if(out.size()+n>65536) throw new IllegalArgumentException(); out.write(buffer,0,n); }
        }
        Map<String,Entry> parsed=new HashMap<>();
        for(String row:new String(out.toByteArray(),StandardCharsets.UTF_8).split(";")) {
            String[] fields=row.split(",",-1);
            if(fields.length!=3 || !fields[0].matches("[0-9]+(\\.[0-9]+)+") || !fields[2].matches("(?:[0-9a-f]{2})*")) throw new IllegalArgumentException();
            int tag=Integer.parseInt(fields[1]);
            if(tag!=0 && tag!=2 && tag!=4) throw new IllegalArgumentException();
            byte[] bytes=new byte[fields[2].length()/2];
            for(int i=0;i<bytes.length;i++) bytes[i]=(byte)Integer.parseInt(fields[2].substring(i*2,i*2+2),16);
            if((tag==0 && bytes.length!=0)||(tag==2 && (bytes.length==0 || bytes.length>4)) || parsed.put(fields[0],new Entry(tag,bytes))!=null) throw new IllegalArgumentException();
        }
        if(parsed.isEmpty()) throw new IllegalArgumentException();
        loadedSnapshot=encoded; entries=parsed; return entries;
    }
    private static Object field(Object object,String name) throws ReflectiveOperationException {
        Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
    public static Object lookup(Object socket,Object request,String target,String encoded) {
        try {
            if(socket==null || request==null || target==null || target.isEmpty() || encoded==null || encoded.isEmpty() ||
                !socket.getClass().getName().equals("com.brother.sdk.network.BrDatagramSocket")) return null;
            InetAddress address=(InetAddress)field(socket,"mInetAddress");
            if(address==null || !target.equals(address.getHostAddress()) || ((Byte)field(request,"msg_type"))!=(byte)0xa0) return null;
            Vector<?> bindings=(Vector<?>)field(request,"reqVarbinds");
            if(bindings.size()!=1) return null;
            Object oidObject=bindings.get(0).getClass().getMethod("getOid").invoke(bindings.get(0));
            String oid=(String)oidObject.getClass().getMethod("getValue").invoke(oidObject);
            Entry item=snapshot(encoded).get(oid);
            ClassLoader loader=request.getClass().getClassLoader();
            Class<?> resultClass=loader.loadClass(SNMP+"SnmpResult");
            Object result=resultClass.getConstructor().newInstance();
            if(item==null || item.tag==0) {
                Class<?> stateClass=loader.loadClass(SNMP+"SnmpResult$SnmpState");
                resultClass.getField("state").set(result,stateClass.getField("ErrorRequestNoSuchName").get(null));
                return result;
            }
            Class<?> asnObject=loader.loadClass(WEST+"AsnObject");
            Class<?> varbind=loader.loadClass(WEST+"Varbind");
            Object value=item.tag==4
                ? loader.loadClass(WEST+"AsnOctets").getConstructor(byte[].class).newInstance((Object)item.value)
                : loader.loadClass(WEST+"AsnInteger").getConstructor(int.class).newInstance(new BigInteger(item.value).intValue());
            Object binding=varbind.getConstructor(String.class,asnObject).newInstance(oid,value);
            Object array=Array.newInstance(varbind,1);Array.set(array,0,binding);
            resultClass.getField("results").set(result,array);return result;
        } catch(Exception | LinkageError ignored) { return null; }
    }
    private FixedResponses() {}
}
