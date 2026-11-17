import app.template.extension.extension.OidDictionary;
public final class OidDictionaryTest {
 public static void main(String[] args){
  OidDictionary.Entry sys=OidDictionary.lookup("1.3.6.1.2.1.1.1.0");
  if(sys==null||!sys.name.equals("System description")||!sys.symbol.equals("sysDescr"))throw new AssertionError("identify sysDescr instance");
  OidDictionary.Entry lang=OidDictionary.lookup("1.3.6.1.2.1.43.15.1.1.2.1.10");
  if(lang==null||!lang.symbol.equals("prtInterpreterLangFamily"))throw new AssertionError("table instances use same definition");
  if(OidDictionary.lookup("1.3.6.1.2.1.43.15.1.1.20.1.10")!=null)throw new AssertionError("prefix matching must respect numeric boundaries");
  if(!"PostScript".equals(lang.valueMeaning("integer:6")))throw new AssertionError("decode language value 6");
  if(lang.valueMeaning("integer:987")!=null||lang.valueMeaning("octets:Ng==")!=null)throw new AssertionError("unverified values must remain uninterpreted");
  OidDictionary.Entry papers=OidDictionary.lookup("1.3.6.1.4.1.2435.2.3.9.4.2.1.5.1.2.71.12.1.2.17");
  if(papers==null||!papers.name.contains("paper"))throw new AssertionError("identify Brother indexed paper table");
  OidDictionary.Entry pdl=OidDictionary.lookup("1.3.6.1.4.1.2435.2.3.9.4.2.1.5.1.2.70.1.1.2.1");
  if(pdl==null||pdl.valueMeaning("integer:17")==null||pdl.valueMeaning("integer:3")!=null)throw new AssertionError("interpret only supported Brother PDL value");
  OidDictionary.Entry path=OidDictionary.lookup("1.3.6.1.2.1.43.13.4.1.9.1.2");
  if(path==null||!path.valueMeaning("integer:3").contains("long-edge")||path.valueMeaning("integer:88")!=null)throw new AssertionError("decode standard paper path types");
  if(OidDictionary.lookup("1.3.6.1.4.1.2435.999")!=null)throw new AssertionError("unknown vendor OIDs must not be guessed");
 }
}
