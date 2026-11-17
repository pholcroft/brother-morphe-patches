package app.template.extension.extension;

import java.util.Locale;

/** Compact request list text. Full identifiers and values remain available in details. */
public final class RequestRow {
 public static String format(RequestRecord request){
  OidDictionary.Entry entry=OidDictionary.lookup(request.oid);String name=entry==null?"Unknown request":entry.name;
  String value;
  if(request.outcome==RequestRecord.Outcome.TIMEOUT)value="Timed out";
  else if(request.outcome==RequestRecord.Outcome.CANCELLED)value="Cancelled";
  else if(request.outcome==RequestRecord.Outcome.ERROR&&request.response()==null)value="Request failed";
  else value=SnmpValueFormatter.format(request.response());
  value=value.replaceAll("[\\r\\n\\t]+"," ").trim();if(value.isEmpty())value="Empty response";
  if(value.length()>120)value=value.substring(0,117)+"...";
  String timing="";
  if(request.cached){if(request.details!=null&&request.details.sampleNanos>=0)timing=String.format(Locale.ROOT," · Previous printer time: ~%.0f ms",request.details.sampleNanos/1e6);}
  else timing=String.format(Locale.ROOT," · Printer: %.0f ms",(request.endNanos-request.startNanos)/1e6);
  return name+"\n"+value+timing;
 }
 private RequestRow(){}
}
