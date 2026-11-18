package app.template.extension.extension;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
public final class DiagnosticsUiTest {
 private static View find(View v,String text){if(v instanceof TextView&&((TextView)v).getText().toString().equals(text))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View x=find(g.getChildAt(i),text);if(x!=null)return x;}}return null;}
 private static View description(View v,String label){if(label.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View x=description(g.getChildAt(i),label);if(x!=null)return x;}}return null;}
 private static boolean has(Instrumentation i,DiagnosticsActivity a,String text){boolean[] found={false};i.runOnMainSync(()->found[0]=find(a.getWindow().getDecorView(),text)!=null);return found[0];}
 public static void run(Instrumentation i,Context c)throws Exception{
  Diagnostics.stop();Diagnostics.clear();DiagnosticsActivity a=(DiagnosticsActivity)i.startActivitySync(new Intent(c,DiagnosticsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  if(!has(i,a,"Diagnostic recording")||!has(i,a,"Stopped")||!has(i,a,"0 requests recorded"))throw new AssertionError("diagnostics screen must show simple stopped state");
  View[] menu={null},button={null};i.runOnMainSync(()->{menu[0]=description(a.getWindow().getDecorView(),"Diagnostic options");button[0]=find(a.getWindow().getDecorView(),"Start recording");});
  if(menu[0]==null||button[0]==null||has(i,a,"Refresh list"))throw new AssertionError("diagnostic actions must use the compact layout");
  i.runOnMainSync(()->button[0].performClick());if(!has(i,a,"Recording")||!has(i,a,"Stop recording"))throw new AssertionError("recording controls must start recording");
  Diagnostics.begin("192.0.2.1","1.3.6.1.2.1.1.1.0",7);Diagnostics.finish("printer","ok","integer","7");
  boolean updated=false;for(int n=0;n<40&&!updated;n++){Thread.sleep(100);updated=has(i,a,"1 request recorded");}
  if(!updated||!has(i,a,"System description")||!has(i,a,"Request / response")||!has(i,a,"7"))throw new AssertionError("recording view must display the request and response in columns");
  i.runOnMainSync(()->find(a.getWindow().getDecorView(),"Stop recording").performClick());if(Diagnostics.isRecording()||!has(i,a,"Stopped"))throw new AssertionError("recording must stop");
  android.accessibilityservice.AccessibilityServiceInfo ai=i.getUiAutomation().getServiceInfo();ai.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;i.getUiAutomation().setServiceInfo(ai);
  i.runOnMainSync(()->menu[0].performClick());i.waitForIdleSync();String[] actions={"Export CSV","Clear recording","About diagnostic recording","How to share"};boolean[] present=new boolean[actions.length];
  for(int attempt=0;attempt<20;attempt++){for(android.view.accessibility.AccessibilityWindowInfo window:i.getUiAutomation().getWindows()){android.view.accessibility.AccessibilityNodeInfo root=window.getRoot();if(root!=null)for(int j=0;j<actions.length;j++)if(!root.findAccessibilityNodeInfosByText(actions[j]).isEmpty())present[j]=true;}if(present[0]&&present[1]&&present[2]&&present[3])break;Thread.sleep(100);}
  for(boolean shown:present)if(!shown)throw new AssertionError("diagnostic menu must contain export, clear, about and sharing actions");i.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
  i.runOnMainSync(a::finish);Diagnostics.clear();
 }
}
