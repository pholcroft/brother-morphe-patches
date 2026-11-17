package app.template.extension.extension;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.Collections;

public final class HistoryUiTest {
    private static boolean contains(View view,String text){
        if(view instanceof TextView && ((TextView)view).getText().toString().contains(text))return true;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(contains(group.getChildAt(i),text))return true;}
        return false;
    }
    private static View find(View view,String text){
        if(view instanceof TextView&&((TextView)view).getText().toString().contains(text))return view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),text);if(found!=null)return found;}}return null;
    }
    public static void run(Instrumentation instrumentation,Context context)throws Exception{
        ActivityStore store=new ActivityStore(context);String ip="192.0.2.88",mac="02:01:02:03:04:08";
        PrinterRecord printer=store.resolveVerified(ip,mac,"Synthetic printer");long sequence=store.reserveSequences(1);
        RequestRecord.Details details=new RequestRecord.Details("192.0.2.88",161,0,1,1000,1,"context","encoded","success",75000000L);
        RequestRecord request=new RequestRecord("row",0,"1.3.6.1.2.1.1.1.0",0,1000000L,true,"octets:QnJvdGhlcg==".getBytes(java.nio.charset.StandardCharsets.UTF_8),details);
        RequestRecord language=new RequestRecord("language",1,"1.3.6.1.2.1.43.15.1.1.2.1.1",1000001L,2000000L,true,"integer:6".getBytes(java.nio.charset.StandardCharsets.UTF_8),details);
        ActivityBurst burst=new ActivityBurst(new ActivityBurst.Ref(printer.id,"ui-test-burst",sequence),1,2000000L,java.util.Arrays.asList(request,language),ip,new ActivityBurst.Coverage(true,true,2,2));
        if(!store.publish(burst,store.registerPrinter(printer.id)))throw new AssertionError("could not create history fixture");store.close();
        CacheSettingsActivity settings=(CacheSettingsActivity)instrumentation.startActivitySync(new Intent(context,CacheSettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        java.lang.reflect.Method cardMethod=CacheSettingsActivity.class.getDeclaredMethod("printerCard",String.class);cardMethod.setAccessible(true);
        View[] card={null};instrumentation.runOnMainSync(()->{try{card[0]=(View)cardMethod.invoke(settings,ip);}catch(Exception e){throw new RuntimeException(e);}});
        Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(ActivityHistoryActivity.class.getName(),null,false);
        instrumentation.runOnMainSync(()->card[0].performClick());
        android.app.Activity opened=instrumentation.waitForMonitorWithTimeout(monitor,3000);instrumentation.removeMonitor(monitor);
        if(opened==null)throw new AssertionError("tapping the printer card must open activity history");
        if(!ip.equals(opened.getIntent().getStringExtra("printer_ip")))throw new AssertionError("activity history must open for the selected printer");
        instrumentation.runOnMainSync(()->{opened.finish();settings.finish();});
        Intent intent=new Intent(context,ActivityHistoryActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("printer_ip",ip);intent.putExtra("printer_mac",mac);
        ActivityHistoryActivity activity=(ActivityHistoryActivity)instrumentation.startActivitySync(intent);
        boolean[] visible={false};
        for(int i=0;i<100&&!visible[0];i++){
            instrumentation.runOnMainSync(()->visible[0]=contains(activity.getWindow().getDecorView(),"Burst #"+sequence));
            if(!visible[0])Thread.sleep(50);
        }
        boolean[] exportVisible={false},timeVisible={false};String time=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM,java.text.DateFormat.MEDIUM).format(new java.util.Date(burst.startedWallMillis));
        instrumentation.runOnMainSync(()->{exportVisible[0]=contains(activity.getWindow().getDecorView(),"Export activity history");timeVisible[0]=contains(activity.getWindow().getDecorView(),time);});
        View[] selected={null};instrumentation.runOnMainSync(()->selected[0]=find(activity.getWindow().getDecorView(),"Burst #"+sequence));
        if(selected[0]==null)throw new AssertionError("burst button missing");instrumentation.runOnMainSync(()->selected[0].performClick());instrumentation.waitForIdleSync();
        boolean[] screen={false};instrumentation.runOnMainSync(()->screen[0]=contains(activity.getWindow().getDecorView(),"Request activity")&&contains(activity.getWindow().getDecorView(),"System description")&&contains(activity.getWindow().getDecorView(),"Brother")&&contains(activity.getWindow().getDecorView(),"0.000 s")&&contains(activity.getWindow().getDecorView(),"~75 ms")&&contains(activity.getWindow().getDecorView(),"0.075 s")&&contains(activity.getWindow().getDecorView(),"6 (PostScript)"));
        if(!screen[0])throw new AssertionError("clicking a burst should show the full request screen with names, values and timings");
        android.os.Bundle saved=new android.os.Bundle();instrumentation.runOnMainSync(()->instrumentation.callActivityOnSaveInstanceState(activity,saved));
        if(!"ui-test-burst".equals(saved.getString("burst_id"))||saved.getLong("burst_sequence")!=sequence)throw new AssertionError("screen state must retain the exact selected burst");
        instrumentation.runOnMainSync(activity::onBackPressed);instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(activity::finish);
        if(!visible[0]||!exportVisible[0]||!timeVisible[0])throw new AssertionError("history screen must show burst, export action and seconds");
    }
}
