import app.template.extension.extension.SnmpValueFormatter;
public final class SnmpValueFormatterTest {
 public static void main(String[] args){
  if(!"Brother".equals(SnmpValueFormatter.format("octets:QnJvdGhlcg==".getBytes(java.nio.charset.StandardCharsets.UTF_8))))throw new AssertionError("decode readable text");
  if(!"42".equals(SnmpValueFormatter.format("integer:42".getBytes(java.nio.charset.StandardCharsets.UTF_8))))throw new AssertionError("show integer");
  if(!SnmpValueFormatter.format("octets:AAE=".getBytes(java.nio.charset.StandardCharsets.UTF_8)).contains("Base64"))throw new AssertionError("binary should remain encoded");
  if(!SnmpValueFormatter.format(null).equals("Not recorded"))throw new AssertionError("missing response");
 }
}
