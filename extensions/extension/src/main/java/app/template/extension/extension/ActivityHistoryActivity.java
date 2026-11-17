package app.template.extension.extension;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Activity history for one verified printer. */
public final class ActivityHistoryActivity extends Activity {
    private static final int EXPORT=76;
    private final Handler main=new Handler(Looper.getMainLooper());
    private LinearLayout content;
    private boolean viewingRequests;private ActivityBurst.Ref selectedRef;
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String value,int size,int color){TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(color);return view;}
    @Override public void onCreate(Bundle state){super.onCreate(state);requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);setTitle("Activity history");
        if(state!=null&&state.containsKey("burst_id")){
            selectedRef=new ActivityBurst.Ref(state.getString("printer_id"),state.getString("burst_id"),state.getLong("burst_sequence"));viewingRequests=true;
            HistoryRuntime.loadBurst(selectedRef,(burst,error)->main.post(()->{if(isFinishing()||isDestroyed())return;if(burst==null){viewingRequests=false;selectedRef=null;showHistoryScreen();}else showRequests(burst);}));
        }else showHistoryScreen();
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);if(viewingRequests&&selectedRef!=null){state.putString("printer_id",selectedRef.printerId);state.putString("burst_id",selectedRef.burstId);state.putLong("burst_sequence",selectedRef.sequence);}}
    @Override public void onBackPressed(){if(viewingRequests){viewingRequests=false;selectedRef=null;showHistoryScreen();}else super.onBackPressed();}
    private void showHistoryScreen(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.WHITE);getWindow().setStatusBarColor(0xff182a43);
        getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() & ~android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i.consumeSystemWindowInsets();});
        LinearLayout header=new LinearLayout(this);header.setGravity(android.view.Gravity.CENTER_VERTICAL);header.setBackgroundColor(0xff182a43);root.addView(header);
        TextView back=text("\u2039",32,Color.WHITE);back.setGravity(android.view.Gravity.CENTER);back.setContentDescription("Back");back.setOnClickListener(v->onBackPressed());header.addView(back,new LinearLayout.LayoutParams(dp(56),dp(56)));
        TextView title=text("Activity history",20,Color.WHITE);title.setTypeface(null,Typeface.BOLD);header.addView(title,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(20),dp(20),dp(20));content.setBackgroundColor(Color.WHITE);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));setContentView(root);
        content.addView(text("Loading activity...",16,0xff666666));loadHistory();
    }
    private void loadHistory(){HistoryRuntime.loadHistory(getIntent().getStringExtra("printer_ip"),getIntent().getStringExtra("printer_mac"),(printer,bursts,error)->main.post(()->{if(isFinishing()||isDestroyed()||viewingRequests)return;showHistory(printer,bursts,error);}));}
    private void clearHistory(){new AlertDialog.Builder(this).setTitle("Delete activity history?").setMessage("All recorded bursts and replay results for this printer will be deleted. Saved printer responses will be kept.").setNegativeButton("Cancel",null).setPositiveButton("Delete all",(d,w)->{
        HistoryRuntime.clearHistory(getIntent().getStringExtra("printer_ip"),getIntent().getStringExtra("printer_mac"),error->main.post(()->{if(isFinishing()||isDestroyed())return;if(error!=null)new AlertDialog.Builder(this).setTitle("Unable to delete history").setMessage(error).setPositiveButton("OK",null).show();loadHistory();}));
    }).show();}
    private void showHistory(PrinterRecord printer,List<ActivityBurst> bursts,String error){
        content.removeAllViews();if(error!=null){content.addView(text(error,16,0xff666666));return;}
        if(printer!=null){TextView heading=text(printer.name==null?"Saved printer":printer.name,20,0xff212121);heading.setTypeface(null,Typeface.BOLD);content.addView(heading);}
        if(bursts==null||bursts.isEmpty()){content.addView(text("No saved activity yet.",16,0xff666666));return;}
        Button export=new Button(this);export.setAllCaps(false);export.setText("Export activity history");export.setOnClickListener(v->exportHistory());content.addView(export,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        Button clear=new Button(this);clear.setAllCaps(false);clear.setText("Delete all activity");clear.setOnClickListener(v->clearHistory());content.addView(clear,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        for(ActivityBurst burst:bursts){Button button=new Button(this);String when=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.MEDIUM).format(new Date(burst.startedWallMillis));
            button.setAllCaps(false);button.setText(String.format(Locale.ROOT,"Burst #%d · %.2f s · %d requests\n%s%s",burst.ref.sequence,burst.elapsedNanos/1000000000.0,burst.requests().size(),when,burst.coverage.requestsComplete?"":" · Incomplete"));
            button.setOnClickListener(v->showRequests(burst));content.addView(button,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));}
    }
    private void exportHistory(){Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/json");String timestamp=new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",Locale.ROOT).format(new Date());intent.putExtra(Intent.EXTRA_TITLE,"printer-activity-"+timestamp+".json");startActivityForResult(intent,EXPORT);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=EXPORT||result!=RESULT_OK||data==null||data.getData()==null)return;
        HistoryRuntime.exportHistory(getApplicationContext(),data.getData(),getIntent().getStringExtra("printer_ip"),getIntent().getStringExtra("printer_mac"),error->main.post(()->{if(isFinishing()||isDestroyed())return;android.widget.Toast.makeText(this,error==null?"Activity saved. Review it before sharing.":error,android.widget.Toast.LENGTH_LONG).show();}));
    }
    private void showRequests(ActivityBurst burst){
        viewingRequests=true;selectedRef=burst.ref;
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.WHITE);getWindow().setStatusBarColor(0xff182a43);
        getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() & ~android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i.consumeSystemWindowInsets();});
        LinearLayout header=new LinearLayout(this);header.setGravity(android.view.Gravity.CENTER_VERTICAL);header.setBackgroundColor(0xff182a43);root.addView(header);
        TextView back=text("\u2039",32,Color.WHITE);back.setGravity(android.view.Gravity.CENTER);back.setContentDescription("Back");back.setOnClickListener(v->onBackPressed());header.addView(back,new LinearLayout.LayoutParams(dp(56),dp(64)));
        LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);TextView title=text("Request activity",20,Color.WHITE);title.setTypeface(null,Typeface.BOLD);labels.addView(title);labels.addView(text("Burst #"+burst.ref.sequence+" · Estimated uncached",13,Color.WHITE));header.addView(labels,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        TextView menu=text("\u22ee",26,Color.WHITE);menu.setGravity(android.view.Gravity.CENTER);menu.setContentDescription("Activity options");menu.setOnClickListener(v->{android.widget.PopupMenu popup=new android.widget.PopupMenu(this,menu);android.view.MenuItem measure=popup.getMenu().add("Measure without cache");measure.setEnabled(burst.coverage.requestsComplete);popup.setOnMenuItemClickListener(item->{startReplay(burst.ref);return true;});popup.show();});header.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(64)));
        LinearLayout columnHeader=new LinearLayout(this);columnHeader.setGravity(android.view.Gravity.CENTER_VERTICAL);columnHeader.setPadding(dp(16),dp(10),dp(16),dp(10));columnHeader.setBackgroundColor(0xffd9efec);TextView timeHeader=text("Time",14,0xff212121);timeHeader.setTypeface(null,Typeface.BOLD);columnHeader.addView(timeHeader,new LinearLayout.LayoutParams(dp(100),ViewGroup.LayoutParams.WRAP_CONTENT));TextView requestHeader=text("Request / response",14,0xff212121);requestHeader.setTypeface(null,Typeface.BOLD);columnHeader.addView(requestHeader,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));root.addView(columnHeader);
        List<RequestRecord> requests=burst.requests();long[] durations=RequestTimeline.durations(requests),offsets=RequestTimeline.offsets(durations);
        android.widget.ListView list=new android.widget.ListView(this);list.setBackgroundColor(Color.WHITE);list.setDivider(new android.graphics.drawable.ColorDrawable(0xffd9dfe2));list.setDividerHeight(dp(1));
        android.widget.BaseAdapter adapter=new android.widget.BaseAdapter(){public int getCount(){return requests.size();}public Object getItem(int position){return requests.get(position);}public long getItemId(int position){return position;}
            @Override public android.view.View getView(int position,android.view.View convert,ViewGroup parent){
                LinearLayout row;if(convert instanceof LinearLayout)row=(LinearLayout)convert;else{row=new LinearLayout(ActivityHistoryActivity.this);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(dp(16),dp(11),dp(16),dp(11));LinearLayout times=new LinearLayout(ActivityHistoryActivity.this);times.setOrientation(LinearLayout.VERTICAL);times.addView(text("",14,0xff212121));times.addView(text("",13,0xff777d86));row.addView(times,new LinearLayout.LayoutParams(dp(100),ViewGroup.LayoutParams.WRAP_CONTENT));LinearLayout values=new LinearLayout(ActivityHistoryActivity.this);values.setOrientation(LinearLayout.VERTICAL);TextView name=text("",15,0xff212121);name.setTypeface(null,Typeface.BOLD);values.addView(name);TextView value=text("",14,0xff343c47);value.setMaxLines(3);value.setEllipsize(android.text.TextUtils.TruncateAt.END);values.addView(value);row.addView(values,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));}
                RequestRecord request=requests.get(position);String text=RequestRow.format(request,offsets[position],durations[position],request.cached);int newline=text.indexOf('\n');String heading=newline<0?text:text.substring(0,newline),value=newline<0?"":text.substring(newline+1);String[] parts=heading.split(" · ",3);LinearLayout times=(LinearLayout)row.getChildAt(0),values=(LinearLayout)row.getChildAt(1);((TextView)times.getChildAt(0)).setText(parts.length>0?parts[0]:"");((TextView)times.getChildAt(1)).setText(parts.length>1?parts[1]:"");((TextView)values.getChildAt(0)).setText(parts.length>2?parts[2]:heading);((TextView)values.getChildAt(1)).setText(value);return row;
            }};
        list.setAdapter(adapter);list.setOnItemClickListener((parent,view,position,id)->showRequest(requests.get(position)));root.addView(list,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));setContentView(root);
    }
    private void showRequest(RequestRecord request){OidDictionary.Entry entry=OidDictionary.lookup(request.oid);String name=entry==null?"Unknown request":entry.name;
        String description=entry==null?"This identifier has not been described in the dictionary yet.":entry.description+"\n\nMIB name: "+entry.symbol;
        String value=SnmpValueFormatter.format(request.response(),request.oid);
        String details=String.format(Locale.ROOT,"%s\n\nOID: %s\nSource: %s\nTime from burst start: %.3f s\nDuration: %.3f s\nOutcome: %s\n\nRecorded response: %s",description,request.oid,request.cached?"Cache":"Printer",request.startNanos/1e9,(request.endNanos-request.startNanos)/1e9,request.outcome.name(),value);
        new AlertDialog.Builder(this).setTitle(name).setMessage(details).setPositiveButton("Close",null).show();
    }
    private void startReplay(ActivityBurst.Ref ref){
        HistoryRuntime.startReplay(ref,error->main.post(()->{if(isFinishing()||isDestroyed())return;if(error!=null)new AlertDialog.Builder(this).setTitle("Unable to replay").setMessage(error).setPositiveButton("OK",null).show();else finish();}));
    }
}
