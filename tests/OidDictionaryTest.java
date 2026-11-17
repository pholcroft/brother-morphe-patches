import app.template.extension.extension.OidDictionary;
public final class OidDictionaryTest {
 public static void main(String[] args){
  OidDictionary.Entry sys=OidDictionary.lookup("1.3.6.1.2.1.1.1.0");
  if(sys==null||!sys.name.equals("System description")||!sys.symbol.equals("sysDescr"))throw new AssertionError("identify sysDescr instance");
  OidDictionary.Entry lang=OidDictionary.lookup("1.3.6.1.2.1.43.15.1.1.2.1.10");
  if(lang==null||!lang.symbol.equals("prtInterpreterLangFamily"))throw new AssertionError("table instances use same definition");
  if(OidDictionary.lookup("1.3.6.1.2.1.43.15.1.1.20.1.10")!=null)throw new AssertionError("prefix matching must respect numeric boundaries");
  if(OidDictionary.lookup("1.3.6.1.4.1.2435.999")!=null)throw new AssertionError("unknown vendor OIDs must not be guessed");
 }
}
