package app.template.extension.extension;

import java.util.List;
import java.util.Map;

/** Keeps a replay and its result alive across settings-screen recreation, within this process. */
public final class ReplayController {
 public interface Listener{void changed();}
 public interface Factory{ReplayEngine.Transport open(String ip,ReplayEngine.Spec first)throws Exception;}
 public static final class Outcome{public final ReplayEngine.Result result;public final String error;Outcome(ReplayEngine.Result r,String e){result=r;error=e;}}
 private static boolean running,cancelled;private static ReplayEngine.Transport active;private static Outcome outcome;private static Listener listener;
 public static synchronized boolean isRunning(){return running;}
 public static synchronized void attach(Listener l){listener=l;}
 public static synchronized void detach(Listener l){if(listener==l)listener=null;}
 public static synchronized Outcome peekOutcome(){return outcome;}
 public static synchronized Outcome takeOutcome(){Outcome o=outcome;outcome=null;return o;}
 public static synchronized void clearOutcome(Outcome value){if(outcome==value)outcome=null;}
 private static void notifyListener(){Listener l;synchronized(ReplayController.class){l=listener;}if(l!=null)l.changed();}
 private static synchronized boolean cancelled(){return cancelled;}
 public static void cancel(){ReplayEngine.Transport t;synchronized(ReplayController.class){if(!running)return;cancelled=true;t=active;}if(t!=null)new Thread(t::close,"cancel-printer-replay").start();}
 public static boolean start(String ip,List<ReplayEngine.Spec> specs,Map<String,String> cache,String mac){return start(ip,specs,cache,mac,(address,first)->new ReplayTransport(address,first));}
 public static boolean start(String ip,List<ReplayEngine.Spec> specs,Map<String,String> cache,String mac,Factory factory){
  synchronized(ReplayController.class){if(running)return false;running=true;cancelled=false;outcome=null;}
  new Thread(()->{ReplayEngine.Result result=null;String error=null;ReplayEngine.Transport transport=null;try{transport=factory.open(ip,specs.get(0));synchronized(ReplayController.class){active=transport;}if(cancelled()){transport.close();return;}final ReplayEngine.Transport delegate=transport;ReplayEngine.Transport guarded=new ReplayEngine.Transport(){public String probe(ReplayEngine.Spec s)throws Exception{if(cancelled())throw new InterruptedException();return delegate.probe(s);}public String request(ReplayEngine.Spec s)throws Exception{if(cancelled())throw new InterruptedException();return delegate.request(s);}public void close(){delegate.close();}};result=ReplayEngine.run(specs,cache,mac,guarded);}catch(ReplayEngine.ProbeException e){error=e.getMessage();}catch(Exception|LinkageError e){error="The replay could not be completed. Cached data was not changed.";}finally{synchronized(ReplayController.class){active=null;running=false;if(!cancelled)outcome=new Outcome(result,error);}notifyListener();}},"printer-replay").start();notifyListener();return true;
 }
 private ReplayController(){}
}

