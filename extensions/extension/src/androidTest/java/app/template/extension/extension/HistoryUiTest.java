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
    public static void run(Instrumentation instrumentation,Context context)throws Exception{
        ActivityStore store=new ActivityStore(context);String ip="192.0.2.88",mac="02:01:02:03:04:08";
        PrinterRecord printer=store.resolveVerified(ip,mac,"Synthetic printer");long sequence=store.reserveSequences(1);
        ActivityBurst burst=new ActivityBurst(new ActivityBurst.Ref(printer.id,"ui-test-burst",sequence),1,1000,Collections.<RequestRecord>emptyList());
        if(!store.publish(burst,store.registerPrinter(printer.id)))throw new AssertionError("could not create history fixture");store.close();
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
        RequestRecord.Details details=new RequestRecord.Details("192.0.2.88",161,0,1,1000,1,"context","encoded","success",75000000L);
        RequestRecord request=new RequestRecord("row",0,"1.3.6.1.2.1.1.1.0",0,1000000L,true,"octets:QnJvdGhlcg==".getBytes(java.nio.charset.StandardCharsets.UTF_8),details);
        ActivityBurst example=new ActivityBurst(new ActivityBurst.Ref(printer.id,"row-example",sequence),1,1000000L,Collections.singletonList(request));
        android.app.AlertDialog[] dialog={null};java.lang.reflect.Method method=ActivityHistoryActivity.class.getDeclaredMethod("showRequests",ActivityBurst.class);method.setAccessible(true);
        instrumentation.runOnMainSync(()->{try{dialog[0]=(android.app.AlertDialog)method.invoke(activity,example);}catch(Exception e){throw new RuntimeException(e);}});instrumentation.waitForIdleSync();
        boolean[] rowVisible={false};instrumentation.runOnMainSync(()->rowVisible[0]=contains(dialog[0].getWindow().getDecorView(),"System description")&&contains(dialog[0].getWindow().getDecorView(),"Brother")&&contains(dialog[0].getWindow().getDecorView(),"~75 ms"));
        instrumentation.runOnMainSync(()->dialog[0].dismiss());if(!rowVisible[0])throw new AssertionError("request list must show name, response and previous printer time");
        instrumentation.runOnMainSync(activity::finish);
        if(!visible[0]||!exportVisible[0]||!timeVisible[0])throw new AssertionError("history screen must show burst, export action and seconds");
    }
}
