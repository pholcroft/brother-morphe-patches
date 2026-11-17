import app.template.extension.extension.*;
import java.nio.charset.StandardCharsets;
public final class RequestRowTest {
 static RequestRecord item(boolean cached,long sample,String value,RequestRecord.Outcome outcome){RequestRecord.Details details=new RequestRecord.Details("192.0.2.1",161,1,1,1000,1,"ctx","encoded","ok",sample);return new RequestRecord("t",0,"1.3.6.1.2.1.1.1.0",0,100000000L,cached,value==null?null:value.getBytes(StandardCharsets.UTF_8),details,outcome);}
 public static void main(String[] args){
  String live=RequestRow.format(item(false,-1,"octets:QnJvdGhlcg==",RequestRecord.Outcome.SUCCESS));
  if(!live.contains("System description")||!live.contains("Brother")||!live.contains("100 ms")||live.contains("1.3.6.1"))throw new AssertionError(live);
  String hit=RequestRow.format(item(true,75000000L,"integer:42",RequestRecord.Outcome.SUCCESS));
  if(!hit.contains("42")||!hit.contains("~75 ms")||hit.contains("Cache")||hit.contains("100 ms"))throw new AssertionError(hit);
  String noSample=RequestRow.format(item(true,-1,"missing:",RequestRecord.Outcome.SUCCESS));if(!noSample.contains("No such object")||noSample.contains("ms"))throw new AssertionError(noSample);
  String failure=RequestRow.format(item(false,-1,null,RequestRecord.Outcome.TIMEOUT));if(!failure.contains("Timed out"))throw new AssertionError(failure);
 }
}
