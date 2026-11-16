package app.template.extension.extension;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Approximate per-printer SNMP activity bursts; never claims tap-to-preview time. */
public final class CommunicationTiming {
    private static final long IDLE_MS=3000;
    private static final ScheduledExecutorService WORK=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"printer-timing");t.setDaemon(true);return t;});
    private static final Map<String,Burst> BURSTS=new HashMap<>();
    private static final Map<String,List<ReplayEngine.Spec>> RECORDED=new HashMap<>();
    private static final int MAX_REPLAY_REQUESTS=4096;
    private static final class Burst {long first;long last;int live,cached,matched,liveMeasured;long estimated,liveTime;boolean incomplete;final List<ReplayEngine.Spec> requests=new ArrayList<>();Burst(long now){first=last=now;}}
    private static long now(){return System.nanoTime()/1000000L;}
    private static void schedule(String ip,Burst b,long delay){WORK.schedule(()->finishIfIdle(ip,b),delay,TimeUnit.MILLISECONDS);}
    public static synchronized void request(String ip){
        if(ip==null)return;long n=now();Burst b=BURSTS.get(ip);
        if(b==null){if(BURSTS.size()>=32)return;b=new Burst(n);BURSTS.put(ip,b);schedule(ip,b,IDLE_MS);}
        b.last=n;b.live++;
    }
    public static synchronized void cacheHit(String ip){cacheHit(ip,"",-1);}
    public static synchronized void cacheHit(String ip,String oid,long estimate){Burst b=BURSTS.get(ip);if(b!=null){if(b.live>0)b.live--;b.cached++;b.last=now();if(estimate>=0){b.estimated+=estimate;b.matched++;}}}
    public static synchronized void liveSample(String ip,long ms){Burst b=BURSTS.get(ip);if(b!=null){b.liveTime+=ms;b.liveMeasured++;}}
    public static synchronized void capture(String ip,ReplayEngine.Spec spec){Burst b=BURSTS.get(ip);if(b==null)return;if(spec==null||b.requests.size()>=MAX_REPLAY_REQUESTS)b.incomplete=true;else b.requests.add(spec);}
    private static List<ReplayEngine.Spec> copyRequests(Burst b){if(b==null||b.incomplete||b.requests.size()!=b.live+b.cached)return null;return new ArrayList<>(b.requests);}
    public static synchronized List<ReplayEngine.Spec> replaySequence(String ip){Burst b=BURSTS.get(ip);if(b!=null)return copyRequests(b);List<ReplayEngine.Spec> done=RECORDED.get(ip);return done==null?null:new ArrayList<>(done);}
    public static synchronized void response(String ip){Burst b=BURSTS.get(ip);if(b!=null)b.last=now();}
    private static void finishIfIdle(String ip,Burst b){
        synchronized(CommunicationTiming.class){
            if(BURSTS.get(ip)!=b)return;long remaining=IDLE_MS-(now()-b.last);
            if(remaining>0){schedule(ip,b,remaining);return;}BURSTS.remove(ip);List<ReplayEngine.Spec> requests=copyRequests(b);if(RECORDED.size()>=32&&!RECORDED.containsKey(ip))RECORDED.clear();if(requests==null)RECORDED.remove(ip);else RECORDED.put(ip,requests);
        }
        AutoCache.saveTiming(ip,Math.max(0,b.last-b.first),b.live,b.cached,b.estimated,b.matched,b.liveTime,b.liveMeasured);
    }
    public static void finishAll(){
        Map<String,Burst> copy;
        synchronized(CommunicationTiming.class){copy=new HashMap<>(BURSTS);BURSTS.clear();for(Map.Entry<String,Burst> e:copy.entrySet()){List<ReplayEngine.Spec> requests=copyRequests(e.getValue());if(RECORDED.size()>=32&&!RECORDED.containsKey(e.getKey()))RECORDED.clear();if(requests==null)RECORDED.remove(e.getKey());else RECORDED.put(e.getKey(),requests);}}
        for(Map.Entry<String,Burst> e:copy.entrySet()){Burst b=e.getValue();AutoCache.saveTiming(e.getKey(),Math.max(0,b.last-b.first),b.live,b.cached,b.estimated,b.matched,b.liveTime,b.liveMeasured);}
    }
    public static synchronized void discard(String ip){BURSTS.remove(ip);RECORDED.remove(ip);}
    public static synchronized void discardAll(){BURSTS.clear();RECORDED.clear();}
    private CommunicationTiming(){}
}

