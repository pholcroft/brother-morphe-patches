package app.template.extension.extension;

import java.util.Locale;

/** Compact request list text. Full identifiers and values remain available in details. */
public final class RequestRow {
 public static String format(RequestRecord request,long offsetNanos,long durationNanos,boolean estimated){
  OidDictionary.Entry entry=OidDictionary.lookup(request.oid);String name=entry==null?"Unknown request":entry.name;
  String value;
  if(request.outcome==RequestRecord.Outcome.TIMEOUT)value="Timed out";
  else if(request.outcome==RequestRecord.Outcome.CANCELLED)value="Cancelled";
  else if(request.outcome==RequestRecord.Outcome.ERROR&&request.response()==null)value="Request failed";
  else value=SnmpValueFormatter.format(request.response(),request.oid);
  value=value.replaceAll("[\\r\\n\\t]+"," ").trim();if(value.isEmpty())value="Empty response";
  if(value.length()>120)value=value.substring(0,117)+"...";
  String offset=offsetNanos<0?"time unavailable":String.format(Locale.ROOT,"%.3f s",offsetNanos/1e9);
  String duration=durationNanos<0?"duration unavailable":String.format(Locale.ROOT,"%s%.0f ms",estimated?"~":"",durationNanos/1e6);
  return offset+" · "+duration+" · "+name+"\n"+value;
 }
 private RequestRow(){}
}
