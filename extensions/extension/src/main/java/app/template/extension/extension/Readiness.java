package app.template.extension.extension;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Optional, non-blocking IPP status check; it does not guarantee a physical print. */
public final class Readiness {
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
    private static final ConcurrentHashMap<String,Long> LAST=new ConcurrentHashMap<>();
    public static void maybeCheck(String ip) {
        if(!AutoCache.liveCheckEnabled() || ip==null)return;
        long now=System.currentTimeMillis();Long last=LAST.putIfAbsent(ip,now);
        if(last!=null){if(now-last<30000 || !LAST.replace(ip,last,now))return;}
        WORK.execute(()-> AutoCache.writeString("status|"+ip,check(ip)+" · "+java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(new java.util.Date())));
    }
    public static void refresh(String ip){LAST.remove(ip);maybeCheck(ip);}
    private static void attr(DataOutputStream out,int tag,String name,String value)throws IOException{
        byte[] n=name.getBytes(StandardCharsets.UTF_8),v=value.getBytes(StandardCharsets.UTF_8);out.writeByte(tag);out.writeShort(n.length);out.write(n);out.writeShort(v.length);out.write(v);
    }
    private static byte[] request(String ip)throws IOException {
        ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);
        d.write(new byte[]{1,1,0,11,0,0,0,1,1});
        attr(d,0x47,"attributes-charset","utf-8");attr(d,0x48,"attributes-natural-language","en");
        attr(d,0x45,"printer-uri","ipp://"+ip+":631/ipp/print");
        attr(d,0x44,"requested-attributes","printer-state");attr(d,0x44,"","printer-state-reasons");attr(d,0x44,"","printer-is-accepting-jobs");d.writeByte(3);return b.toByteArray();
    }
    private static String check(String ip){
        HttpURLConnection connection=null;
        try{
            connection=(HttpURLConnection)new URL("http://"+ip+":631/ipp/print").openConnection();connection.setConnectTimeout(2000);connection.setReadTimeout(2000);connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/ipp");
            try(OutputStream out=connection.getOutputStream()){out.write(request(ip));}
            if(connection.getResponseCode()!=200)return "Status unavailable";
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=connection.getInputStream()){byte[] buffer=new byte[1024];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>65536)return "Status unavailable";bytes.write(buffer,0,n);}}
            return parse(bytes.toByteArray());
        }catch(Exception ignored){return "Not reachable or status unavailable";}finally{if(connection!=null)connection.disconnect();}
    }
    public static String parse(byte[] response){
        try{
            DataInputStream d=new DataInputStream(new ByteArrayInputStream(response));byte[] header=new byte[8];d.readFully(header);
            if(header[2]!=0 || header[3]!=0)return "Status unavailable";
            int state=-1;Boolean accepting=null;boolean hasReasons=false,attention=false;String name="";
            while(d.available()>0){int tag=d.readUnsignedByte();if(tag==3)break;if(tag<16)continue;
                int nl=d.readUnsignedShort();if(nl>1024||nl>d.available())return "Status unavailable";byte[] nb=new byte[nl];d.readFully(nb);if(nl>0)name=new String(nb,StandardCharsets.UTF_8);
                int len=d.readUnsignedShort();if(len>d.available())return "Status unavailable";byte[] value=new byte[len];d.readFully(value);
                if(name.equals("printer-state")&&tag==0x23&&len==4)state=((value[0]&255)<<24)|((value[1]&255)<<16)|((value[2]&255)<<8)|(value[3]&255);
                else if(name.equals("printer-is-accepting-jobs")&&tag==0x22&&len==1)accepting=value[0]!=0;
                else if(name.equals("printer-state-reasons")&&tag==0x44){hasReasons=true;String reason=new String(value,StandardCharsets.UTF_8).toLowerCase(java.util.Locale.ROOT);if(reason.endsWith("-error")||reason.equals("paused")||reason.equals("shutdown")||reason.equals("offline"))attention=true;}
            }
            if(state<0||accepting==null||!hasReasons)return "Status unavailable";
            if(!accepting)return "Not accepting jobs";if(state==5)return "Stopped";if(attention)return "Attention required";
            if(state==3)return "Ready";if(state==4)return "Busy";return "Status unavailable";
        }catch(Exception ignored){return "Status unavailable";}
    }
    private Readiness(){}
}
