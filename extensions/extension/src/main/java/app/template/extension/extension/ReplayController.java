package app.template.extension.extension;

import java.util.List;
import java.util.Map;

/** Keeps a replay and its result alive across settings-screen recreation, within this process. */
public final class ReplayController {
 public interface Listener{void changed();}
 public interface Factory{ReplayEngine.Transport open(String ip,ReplayEngine.Spec first)throws Exception;}
 public interface Completion{void complete(ReplayEngine.Result result);}
 public static final class Outcome{public final ReplayEngine.Result result;public final String error;public final long sequence;Outcome(ReplayEngine.Result r,String e,long sequence){result=r;error=e;this.sequence=sequence;}}
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
  return startInternal(ip,specs,cache,null,mac,factory,-1,null);
 }
 public static boolean startRecorded(String ip,List<ReplayEngine.Spec> specs,List<String> responses,String mac,long sequence,Completion completion){return startInternal(ip,specs,null,responses,mac,(address,first)->new ReplayTransport(address,first),sequence,completion);}
 public static boolean startRecorded(String ip,List<ReplayEngine.Spec> specs,List<String> responses,String mac,long sequence,Factory factory,Completion completion){return startInternal(ip,specs,null,responses,mac,factory,sequence,completion);}
 private static boolean startInternal(String ip,List<ReplayEngine.Spec> specs,Map<String,String> cache,List<String> responses,String mac,Factory factory,long sequence,Completion completion){
  synchronized(ReplayController.class){if(running)return false;running=true;cancelled=false;outcome=null;}
  new Thread(()->{ReplayEngine.Result result=null;String error=null;ReplayEngine.Transport transport=null;try{transport=factory.open(ip,specs.get(0));synchronized(ReplayController.class){active=transport;}if(cancelled()){transport.close();return;}final ReplayEngine.Transport delegate=transport;ReplayEngine.Transport guarded=new ReplayEngine.Transport(){public String probe(ReplayEngine.Spec s)throws Exception{if(cancelled())throw new InterruptedException();return delegate.probe(s);}public String request(ReplayEngine.Spec s)throws Exception{if(cancelled())throw new InterruptedException();return delegate.request(s);}public void close(){delegate.close();}};result=responses==null?ReplayEngine.run(specs,cache,mac,guarded):ReplayEngine.runRecorded(specs,responses,mac,guarded);}catch(ReplayEngine.ProbeException e){error=e.getMessage();}catch(Exception|LinkageError e){error="The replay could not be completed. Cached data was not changed.";}finally{boolean save;synchronized(ReplayController.class){active=null;running=false;save=!cancelled&&result!=null;if(!cancelled)outcome=new Outcome(result,error,sequence);}if(save&&completion!=null)try{completion.complete(result);}catch(RuntimeException ignored){}notifyListener();}},"printer-replay").start();notifyListener();return true;
 }
 private ReplayController(){}
}

