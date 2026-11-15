import app.template.extension.extension.Readiness;
import java.io.*;
public class ReadinessTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static byte[] ipp(int state,boolean accepts,String reason)throws Exception{
  ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);
  d.write(new byte[]{1,1,0,0,0,0,0,1,4});attribute(d,0x23,"printer-state",new byte[]{0,0,0,(byte)state});attribute(d,0x22,"printer-is-accepting-jobs",new byte[]{(byte)(accepts?1:0)});attribute(d,0x44,"printer-state-reasons",reason.getBytes("UTF-8"));d.writeByte(3);return b.toByteArray();
 }
 static void attribute(DataOutputStream d,int tag,String name,byte[] v)throws Exception{byte[] n=name.getBytes("UTF-8");d.writeByte(tag);d.writeShort(n.length);d.write(n);d.writeShort(v.length);d.write(v);}
 public static void main(String[] args)throws Exception{
  check(Readiness.parse(ipp(3,true,"none")).equals("Ready"),"idle");
  check(Readiness.parse(ipp(4,true,"none")).equals("Busy"),"busy");
  check(Readiness.parse(ipp(3,false,"none")).equals("Not accepting jobs"),"not accepting");
  check(Readiness.parse(ipp(3,true,"media-empty-error")).equals("Attention required"),"paper");
  check(Readiness.parse(ipp(5,true,"none")).equals("Stopped"),"stopped");
  check(Readiness.parse(new byte[]{1,1,0}).equals("Status unavailable"),"truncated");
  System.out.println("IPP readiness parser checks passed");
 }
}
