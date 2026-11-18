package app.template.extension.extension;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Local recording controls for diagnostic requests. */
public final class DiagnosticsActivity extends Activity {
    private static final int EXPORT=42,NAVY=0xff182a43,TEXT=0xff212121,MUTED=0xff666666,TEAL=0xff00796c;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView status,count,dropped;private Button toggle;private LinearLayout events;
    private int lastCount=-1,lastDropped=-1;private boolean lastRecording,active;
    private final Runnable refresh=new Runnable(){public void run(){if(!active)return;update();handler.postDelayed(this,750);}};
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);return v;}
    @Override public void onCreate(Bundle state){super.onCreate(state);requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);render();}
    @Override protected void onResume(){super.onResume();active=true;update();handler.removeCallbacks(refresh);handler.postDelayed(refresh,750);}
    @Override protected void onPause(){active=false;handler.removeCallbacks(refresh);super.onPause();}
    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i.consumeSystemWindowInsets();});
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setBackgroundColor(NAVY);root.addView(header);
        TextView back=text("\u2039",32,Color.WHITE);back.setGravity(Gravity.CENTER);back.setContentDescription("Back");back.setOnClickListener(v->finish());header.addView(back,new LinearLayout.LayoutParams(dp(56),dp(56)));
        TextView title=text("Diagnostic recording",20,Color.WHITE);title.setTypeface(null,Typeface.BOLD);header.addView(title,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        TextView menu=text("\u22ee",26,Color.WHITE);menu.setGravity(Gravity.CENTER);menu.setContentDescription("Diagnostic options");menu.setOnClickListener(v->showMenu(menu));header.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(56)));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(22),dp(20),dp(24));scroll.addView(content);
        status=text("",21,TEXT);status.setTypeface(null,Typeface.BOLD);content.addView(status);
        count=text("",15,MUTED);count.setPadding(0,dp(5),0,0);content.addView(count);
        dropped=text("",13,MUTED);content.addView(dropped);
        toggle=new Button(this);toggle.setAllCaps(false);toggle.setOnClickListener(v->{if(Diagnostics.isRecording())Diagnostics.stop();else Diagnostics.start();lastCount=-1;update();});LinearLayout.LayoutParams buttonParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);buttonParams.topMargin=dp(14);content.addView(toggle,buttonParams);
        View divider=new View(this);divider.setBackgroundColor(0xffd9dfe2);LinearLayout.LayoutParams dividerParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(1));dividerParams.topMargin=dp(20);content.addView(divider,dividerParams);
        TextView recent=text("Recent requests",18,TEXT);recent.setTypeface(null,Typeface.BOLD);recent.setPadding(0,dp(20),0,dp(8));content.addView(recent);
        events=new LinearLayout(this);events.setOrientation(LinearLayout.VERTICAL);content.addView(events);setContentView(root);lastCount=-1;update();
    }
    private void update(){
        if(status==null)return;int n=Diagnostics.count(),discarded=Diagnostics.dropped();boolean recording=Diagnostics.isRecording();
        if(n==lastCount&&discarded==lastDropped&&recording==lastRecording)return;lastCount=n;lastDropped=discarded;lastRecording=recording;
        status.setText(recording?"Recording":"Stopped");status.setTextColor(recording?TEAL:TEXT);count.setText(n+(n==1?" request recorded":" requests recorded"));toggle.setText(recording?"Stop recording":"Start recording");
        dropped.setText(discarded>0?discarded+" older requests discarded":"");dropped.setVisibility(discarded>0?View.VISIBLE:View.GONE);
        events.removeAllViews();java.util.List<Diagnostics.RecentEvent> recent=Diagnostics.recentEvents();
        if(recent.isEmpty()){TextView empty=text("Your recorded requests will appear here.",14,MUTED);empty.setPadding(0,dp(8),0,0);events.addView(empty);return;}
        for(Diagnostics.RecentEvent event:recent){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(0,dp(10),0,dp(10));OidDictionary.Entry entry=OidDictionary.lookup(event.oid);TextView name=text(entry==null?"Unknown request":entry.name,15,TEXT);name.setTypeface(null,Typeface.BOLD);row.addView(name);
            String encoded=event.type.equals("integer")?"integer:"+event.value:event.type.equals("octets_base64")?"octets:"+event.value:null;
            String value=encoded==null?(event.state==null?"":event.state):SnmpValueFormatter.format(encoded.getBytes(StandardCharsets.UTF_8),event.oid);TextView response=text(value,14,0xff343c47);response.setMaxLines(3);response.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(response);
            String source="cache".equalsIgnoreCase(event.source)?"Cache":"Printer";row.addView(text(source+" · "+event.millis+" ms",12,MUTED));events.addView(row);View line=new View(this);line.setBackgroundColor(0xffd9dfe2);events.addView(line,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(1)));}
    }
    private void showMenu(View anchor){PopupMenu popup=new PopupMenu(this,anchor);android.view.MenuItem export=popup.getMenu().add("Export CSV"),clear=popup.getMenu().add("Clear recording"),about=popup.getMenu().add("About diagnostic recording"),share=popup.getMenu().add("How to share");export.setEnabled(Diagnostics.count()>0);clear.setEnabled(Diagnostics.count()>0);popup.setOnMenuItemClickListener(item->{if(item==export)export();else if(item==clear)clear();else if(item==about)about();else if(item==share)share();return true;});popup.show();}
    private void export(){new AlertDialog.Builder(this).setTitle("Export diagnostic recording?").setMessage("The CSV can contain printer addresses, identifiers and responses. Review it before sharing.").setNegativeButton("Cancel",null).setPositiveButton("Export",(d,w)->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/csv");String timestamp=new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",java.util.Locale.ROOT).format(new java.util.Date());i.putExtra(Intent.EXTRA_TITLE,"printer-diagnostics-"+timestamp+".csv");startActivityForResult(i,EXPORT);}).show();}
    private void clear(){new AlertDialog.Builder(this).setTitle("Clear diagnostic recording?").setMessage("This does not clear saved printer responses.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{Diagnostics.clear();lastCount=-1;update();}).show();}
    private void about(){new AlertDialog.Builder(this).setTitle("About diagnostic recording").setMessage("Records supported SNMP requests and responses seen by the cache hook. It does not include discovery, other protocols, every failure or print-job data. Durations are request hook times, not print preview time.\n\nRecording stays in memory until the app process closes. Up to 2,000 recent events are kept. Nothing is sent automatically.").setPositiveButton("Close",null).show();}
    private void share(){new AlertDialog.Builder(this).setTitle("Share diagnostic results").setMessage("Stop recording and export the CSV. Review it first: printer addresses, identifiers and response values may be included. Base64 is encoding, not encryption. You can attach the file to a GitHub issue yourself. Nothing is sent automatically.").setNegativeButton("Close",null).setPositiveButton("Open issues",(d,w)->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/pholcroft/brother-morphe-patches/issues")))).show();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=EXPORT||result!=RESULT_OK||data==null||data.getData()==null)return;
        try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new java.io.IOException("No output stream");out.write(Diagnostics.csv().getBytes(StandardCharsets.UTF_8));Toast.makeText(this,"CSV saved. Review it before sharing.",Toast.LENGTH_LONG).show();}catch(Exception error){Toast.makeText(this,"Could not save CSV: "+error.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}}
}
