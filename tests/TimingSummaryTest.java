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
        String summary=AutoCache.timingSummary(ip);check(summary.contains("Latest activity")&&!AutoCache.timingValue(ip,"cached").isEmpty()&&AutoCache.timingRequestCount(ip,"cached").equals("2"),"store baseline and mixed cache burst");
        String csv=Diagnostics.csv();check(csv.contains("latest_span_ms")&&csv.contains("latest_matched_hits"),"export latest estimate components");check(csv.contains("baseline_span_ms")&&csv.contains("cached_span_ms")&&csv.contains(","+AutoCache.timingValue(ip,"baseline")+",1,"+AutoCache.timingValue(ip,"cached")+",2"),"include recorded timings and counts in export");
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
        String estimate="192.0.2.13";
        CommunicationTiming.request(estimate);CommunicationTiming.liveSample(estimate,20);CommunicationTiming.response(estimate);CommunicationTiming.request(estimate);CommunicationTiming.cacheHit(estimate,"1.2.3",40);CommunicationTiming.request(estimate);CommunicationTiming.cacheHit(estimate,"1.2.4",-1);CommunicationTiming.finishAll();
        check(AutoCache.timingSummary(estimate).contains("0.04 s")&&AutoCache.timingSummary(estimate).contains("1 of 2"),"show estimate and matched coverage only");
        check(!AutoCache.timingSummary(estimate).contains("Estimated total request time"),"do not invent total for unmatched hits");
        String complete="192.0.2.14";
        CommunicationTiming.request(complete);CommunicationTiming.liveSample(complete,20);CommunicationTiming.response(complete);
        CommunicationTiming.request(complete);CommunicationTiming.cacheHit(complete,"1.2.3",40);CommunicationTiming.finishAll();
        check(AutoCache.timingSummary(complete).contains("Estimated total request time without cache: 0.06 s"),"sum live and matched expected durations");
        String card=AutoCache.timingCardSummary(complete);
        check(card.contains("Actual time:")&&card.contains("Without cache: ~0.06 s of requests (~0.04 s avoided)"),"show latest burst and estimates together");
        check(!card.contains("First activity")&&!card.contains("Later cached"),"do not label bursts as previews");
        check(AutoCache.timingCardSummary(estimate).contains("partial"),"incomplete savings estimate is labelled");
        check(AutoCache.timingCardSummary(estimate).contains("Without cache: estimate unavailable"),"do not guess full estimate when incomplete");
        AutoCache.saveTiming(complete,123,1,0,0,0,120,1);
        card=AutoCache.timingCardSummary(complete);
        check(card.contains("0.12 s")&&card.contains("~0.00 s avoided"),"latest live-only burst replaces cached burst");
        check(AutoCache.timingValue(complete,"latest").equals("123"),"store latest live burst");
        AutoCache.clearTimings(complete);check(AutoCache.timingValue(complete,"latest").isEmpty(),"reset clears latest measurement");
        CommunicationTiming.startMeasurement(estimate);check(CommunicationTiming.isMeasuring(estimate),"measurement starts");
        CommunicationTiming.request(estimate);CommunicationTiming.response(estimate);CommunicationTiming.stopMeasurement(estimate);
        check(!CommunicationTiming.isMeasuring(estimate)&&AutoCache.timingSummary(estimate).contains("Uncached measurement"),"measurement stops and is retained");
        String auto="192.0.2.11";CommunicationTiming.request(auto);CommunicationTiming.response(auto);Thread.sleep(3400);check(!AutoCache.timingValue(auto,"baseline").isEmpty(),"timing saves automatically after quiet period");
        AutoCache.clearAll();check(AutoCache.timingSummary(ip).contains("Not recorded"),"clearing removes timing summaries");
        System.out.println("Communication timing summary checks passed");
    }
}

