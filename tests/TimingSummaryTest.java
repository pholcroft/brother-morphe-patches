import android.content.Context;
import app.template.extension.extension.AutoCache;
import app.template.extension.extension.Diagnostics;
import app.template.extension.extension.CommunicationTiming;

public class TimingSummaryTest {
    static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
    public static void main(String[] args)throws Exception{
        AutoCache.init(new Context());String ip="192.0.2.10";
        check(AutoCache.timingSummary(ip).contains("Not recorded"),"missing times must not be guessed");
        CommunicationTiming.request(ip);CommunicationTiming.response(ip);CommunicationTiming.finishAll();
        check(!AutoCache.timingValue(ip,"baseline").isEmpty(),"record original all-live burst without diagnostics");
        Diagnostics.clear();Diagnostics.start();Diagnostics.begin(ip,"1.2.3");
        CommunicationTiming.request(ip);CommunicationTiming.cacheHit(ip);Diagnostics.finish("cache","SuccessRequest","integer","1");
        CommunicationTiming.request(ip);CommunicationTiming.response(ip);CommunicationTiming.finishAll();Diagnostics.stop();
        String summary=AutoCache.timingSummary(ip);check(summary.contains("First activity")&&summary.contains("Later cached activity")&&!AutoCache.timingValue(ip,"cached").isEmpty()&&AutoCache.timingRequestCount(ip,"cached").equals("2"),"store baseline and mixed cache burst");
        String csv=Diagnostics.csv();check(csv.contains("baseline_span_ms")&&csv.contains("cached_span_ms")&&csv.contains(","+AutoCache.timingValue(ip,"baseline")+",1,"+AutoCache.timingValue(ip,"cached")+",2"),"include recorded timings and counts in export");
        AutoCache.clearTimings(ip);check(AutoCache.timingValue(ip,"baseline").isEmpty()&&AutoCache.timingValue(ip,"cached").isEmpty(),"reset timings separately");
        String mixed="192.0.2.12";
        CommunicationTiming.request(mixed);CommunicationTiming.response(mixed);
        CommunicationTiming.request(mixed);CommunicationTiming.cacheHit(mixed);CommunicationTiming.finishAll();
        check(AutoCache.timingRequestCount(mixed,"baseline").equals("2"),"first mixed burst must be recorded as the first measurement");
        check(AutoCache.timingValue(mixed,"cached").isEmpty(),"first burst must not also be presented as the later measurement");
        check(AutoCache.timingSummary(mixed).contains("1 cached"),"first measurement reports cache hits honestly");
        CommunicationTiming.request(mixed);CommunicationTiming.cacheHit(mixed);CommunicationTiming.finishAll();
        check(AutoCache.timingRequestCount(mixed,"cached").equals("1"),"later cache burst recorded separately");
        check(AutoCache.timingRequestCount(mixed,"baseline").equals("2"),"first measurement stays unchanged");
        String auto="192.0.2.11";CommunicationTiming.request(auto);CommunicationTiming.response(auto);Thread.sleep(3400);check(!AutoCache.timingValue(auto,"baseline").isEmpty(),"timing saves automatically after quiet period");
        AutoCache.clearAll();check(AutoCache.timingSummary(ip).contains("Not recorded"),"clearing removes timing summaries");
        System.out.println("Communication timing summary checks passed");
    }
}

