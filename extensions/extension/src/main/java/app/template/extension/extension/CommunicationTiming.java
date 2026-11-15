package app.template.extension.extension;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Approximate per-printer SNMP activity bursts; never claims tap-to-preview time. */
public final class CommunicationTiming {
    private static final long IDLE_MS=3000;
    private static final ScheduledExecutorService WORK=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"printer-timing");t.setDaemon(true);return t;});
    private static final Map<String,Burst> BURSTS=new HashMap<>();
    private static final class Burst {final long first;long last;int live,cached;Burst(long now){first=last=now;}}
    private static long now(){return System.nanoTime()/1000000L;}
    private static void schedule(String ip,Burst b,long delay){WORK.schedule(()->finishIfIdle(ip,b),delay,TimeUnit.MILLISECONDS);}
    public static synchronized void request(String ip){
        if(ip==null)return;long n=now();Burst b=BURSTS.get(ip);
        if(b==null){if(BURSTS.size()>=32)return;b=new Burst(n);BURSTS.put(ip,b);schedule(ip,b,IDLE_MS);}
        b.last=n;b.live++;
    }
    public static synchronized void cacheHit(String ip){Burst b=BURSTS.get(ip);if(b!=null){if(b.live>0)b.live--;b.cached++;b.last=now();}}
    public static synchronized void response(String ip){Burst b=BURSTS.get(ip);if(b!=null)b.last=now();}
    private static void finishIfIdle(String ip,Burst b){
        synchronized(CommunicationTiming.class){
            if(BURSTS.get(ip)!=b)return;long remaining=IDLE_MS-(now()-b.last);
            if(remaining>0){schedule(ip,b,remaining);return;}BURSTS.remove(ip);
        }
        AutoCache.saveTiming(ip,Math.max(0,b.last-b.first),b.live,b.cached);
    }
    public static void finishAll(){
        Map<String,Burst> copy;
        synchronized(CommunicationTiming.class){copy=new HashMap<>(BURSTS);BURSTS.clear();}
        for(Map.Entry<String,Burst> e:copy.entrySet()){Burst b=e.getValue();AutoCache.saveTiming(e.getKey(),Math.max(0,b.last-b.first),b.live,b.cached);}
    }
    public static synchronized void discard(String ip){BURSTS.remove(ip);}
    public static synchronized void discardAll(){BURSTS.clear();}
    private CommunicationTiming(){}
}
