import app.template.extension.extension.*;
import java.util.Arrays;

public class BurstSummaryTest {
    public static void main(String[] args){
        RequestRecord.Details live=new RequestRecord.Details("192.0.2.1",161,1,1,1000,2,"id","encoded","success",-1);
        RequestRecord.Details hit=new RequestRecord.Details("192.0.2.1",161,1,2,1000,2,"id","encoded","success",1000000000L);
        RequestRecord a=new RequestRecord("a",0,"1.2.3",0,500000000L,false,null,live);
        RequestRecord b=new RequestRecord("b",1,"1.2.4",500000000L,600000000L,true,null,hit);
        ActivityBurst burst=new ActivityBurst(new ActivityBurst.Ref("p","b",7),1,600000000L,Arrays.asList(a,b),"192.0.2.1",new ActivityBurst.Coverage(true,false,1,1));
        String text=BurstSummary.card(burst);
        if(!text.contains("Actual time: 0.60 s")||!text.contains("~1.50 s of requests")||!text.contains("~0.90 s avoided"))throw new AssertionError("summary must use this burst's own measurements");
        RequestRecord.Details unknown=new RequestRecord.Details("192.0.2.1",161,1,2,1000,2,"id","encoded","success",-1);
        ActivityBurst partial=new ActivityBurst(burst.ref,1,600000000L,Arrays.asList(a,new RequestRecord("b",1,"1.2.4",500000000L,600000000L,true,null,unknown)),"192.0.2.1",new ActivityBurst.Coverage(true,false,0,1));
        if(!BurstSummary.card(partial).contains("estimate unavailable"))throw new AssertionError("missing samples must not yield a total estimate");
    }
}
