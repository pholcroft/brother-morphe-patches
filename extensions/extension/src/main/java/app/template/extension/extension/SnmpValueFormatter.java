package app.template.extension.extension;

import android.util.Base64;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Present observed values without pretending binary data is text. */
public final class SnmpValueFormatter {
 public static String format(byte[] response,String oid){
  String raw=format(response);OidDictionary.Entry entry=OidDictionary.lookup(oid);if(entry==null||response==null)return raw;
  String encoded=new String(response,StandardCharsets.UTF_8),meaning=entry.valueMeaning(encoded);
  return meaning==null?raw:raw+" ("+meaning+")";
 }
 public static String format(byte[] response){
  if(response==null)return "Not recorded";String encoded=new String(response,StandardCharsets.UTF_8);
  if(encoded.startsWith("integer:"))return encoded.substring(8);
  if(encoded.startsWith("missing:"))return "No such object";
  if(encoded.startsWith("octets:")){String base64=encoded.substring(7);try{
   byte[] data=Base64.decode(base64,Base64.DEFAULT);String value=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data)).toString();
   for(int i=0;i<value.length();i++)if(Character.isISOControl(value.charAt(i))&&value.charAt(i)!='\n'&&value.charAt(i)!='\r'&&value.charAt(i)!='\t')return "Binary response (Base64): "+base64;
   return value;
  }catch(Exception e){return "Binary response (Base64): "+base64;}}
  return encoded;
 }
 private SnmpValueFormatter(){}
}
