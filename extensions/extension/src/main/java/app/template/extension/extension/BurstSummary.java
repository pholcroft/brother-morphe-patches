package app.template.extension.extension;

import java.util.Locale;

/** Compact card copy derived only from one stored burst. */
public final class BurstSummary {
    public static String card(ActivityBurst burst){
        if(burst==null)return "Actual time: Not recorded";
        String actual=String.format(Locale.ROOT,"Burst #%d\nActual time: %.2f s",burst.ref.sequence,burst.elapsedNanos/1000000000.0);
        long live=0,avoided=0,expected=0;int hits=0,matched=0;
        for(RequestRecord r:burst.requests()){
            long duration=r.endNanos-r.startNanos;
            if(!r.cached){live=safeAdd(live,duration);continue;}
            hits++;if(r.details==null||r.details.sampleNanos<0)continue;matched++;
            expected=safeAdd(expected,r.details.sampleNanos);avoided=safeAdd(avoided,Math.max(0,r.details.sampleNanos-duration));
        }
        String total=burst.coverage.requestsComplete&&hits==matched?String.format(Locale.ROOT,"~%.2f s of requests",safeAdd(live,expected)/1000000000.0):"estimate unavailable";
        String saved=(hits==0||matched>0)?String.format(Locale.ROOT," (~%.2f s avoided%s)",avoided/1000000000.0,matched<hits?", partial":""):"";
        return actual+"\nWithout cache: "+total+saved;
    }
    private static long safeAdd(long a,long b){return b>Long.MAX_VALUE-a?Long.MAX_VALUE:a+b;}
    private BurstSummary(){}
}
