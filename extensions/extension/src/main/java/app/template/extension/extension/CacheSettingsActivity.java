package app.template.extension.extension;



import android.app.Activity;

import android.app.AlertDialog;

import android.content.Intent;

import android.graphics.Color;

import android.graphics.Typeface;

import android.graphics.drawable.GradientDrawable;

import android.os.Bundle;

import android.os.Handler;

import android.os.Looper;

import android.view.Gravity;

import android.view.View;

import android.view.ViewGroup;

import android.widget.LinearLayout;

import android.widget.PopupMenu;

import android.widget.ScrollView;

import android.widget.TextView;

import java.util.ArrayList;

import java.util.Collections;



/** Manage saved printer data and open diagnostics. */

public final class CacheSettingsActivity extends Activity {

    private static final int NAVY=0xff182a43, TEAL=0xff00796c, TEXT=0xff212121, MUTED=0xff666666;

    private LinearLayout root;

    private int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}

    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}

    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}

    @Override public void onCreate(Bundle state){super.onCreate(state);AutoCache.init(getApplicationContext());render();}

    @Override public void onResume(){super.onResume();if(root!=null)render();}

    private void clearPrinter(String ip){new AlertDialog.Builder(this).setTitle("Clear saved printer data?").setMessage("Saved responses and comparison times will be removed. With the cache on, new replies are learned immediately and may be reused during the same preview. Turn the cache off in settings to use only live replies.\n\nExisting diagnostic recordings are kept separately.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{AutoCache.clearPrinter(ip);render();}).show();}

    private void clearAll(){new AlertDialog.Builder(this).setTitle("Clear all saved printers?").setMessage("Saved responses and comparison times will be removed. With the cache on, new replies are learned immediately and may be reused during the same preview. Turn the cache off in settings to use only live replies.\n\nExisting diagnostic recordings are kept separately.").setNegativeButton("Cancel",null).setPositiveButton("Clear all",(d,w)->{AutoCache.clearAll();render();}).show();}

    private void printerMenu(View anchor,String ip){

        PopupMenu menu=new PopupMenu(this,anchor);

        menu.getMenu().add("Printer details");

        menu.getMenu().add("Timing details and help");


        menu.getMenu().add(CommunicationTiming.isMeasuring(ip)?"Stop uncached measurement":"Measure without cache");

        menu.getMenu().add("Refresh status").setEnabled(AutoCache.liveCheckEnabled());

        menu.getMenu().add("Clear saved data");

        menu.setOnMenuItemClickListener(item->{

            if(item.getTitle().equals("Printer details")){String mac=AutoCache.readString("mac|"+ip,"");new AlertDialog.Builder(this).setTitle("Printer details").setMessage(AutoCache.printerName(ip)+"\nAddress: "+ip+"\nHardware address: "+(mac.isEmpty()?"Not recorded":mac)+(AutoCache.hasChangedAddress(ip)?"\nThis printer has been seen at another address.":"")+"\n\nThe mod checks the hardware address when the service requests it and can reuse saved responses if the printer moves to a new IP address. If the address cannot be checked, normal printer requests are used.").setPositiveButton("Close",null).show();return true;}

            if(item.getTitle().equals("Timing details and help")){new AlertDialog.Builder(this).setTitle("Timing details and help").setMessage(AutoCache.timingSummary(ip)+"\n\nLatest activity is the most recent burst of observed SNMP activity, not a preview time. A 3-second quiet period separates activity bursts; a single preview can contain more than one burst, and background requests may be included. Previous timings stay until you clear them.\n\nEstimated avoided adds previous live durations for matching cache hits. Estimated uncached adds that figure to the observed live request durations when all requests can be accounted for. Both are sums of request times, not elapsed preview time: requests can overlap, printer response times vary, and the estimate ignores cache overhead. Partial means some cache hits could not be estimated.\n\nUse Measure without cache in the printer menu for an optional live measurement.").setPositiveButton("Close",null).show();return true;}
            if(item.getTitle().equals("Measure without cache")){new AlertDialog.Builder(this).setTitle("Measure without cache?").setMessage("Requests for this printer will go to the printer until you stop the measurement. Open your print preview, wait for the options to load, then return here and choose Stop uncached measurement. Other printer activity can also be included. Cached responses remain saved.").setNegativeButton("Cancel",null).setPositiveButton("Start",(d,w)->{CommunicationTiming.startMeasurement(ip);render();}).show();return true;}

            if(item.getTitle().equals("Stop uncached measurement")){boolean observed=CommunicationTiming.stopMeasurement(ip);render();if(observed)new AlertDialog.Builder(this).setTitle("Uncached test result").setMessage(AutoCache.uncachedMeasurementSummary(ip)+"\n\nTime from the first to the last observed request or response for this printer.").setPositiveButton("OK",null).show();else new AlertDialog.Builder(this).setTitle("No requests recorded").setMessage("No printer requests were observed during this measurement. Any previous uncached result has not changed. Try starting the measurement again before opening print preview.").setPositiveButton("OK",null).show();return true;}


            if(item.getTitle().equals("Refresh status")){Readiness.refresh(ip);new Handler(Looper.getMainLooper()).postDelayed(()->{if(!isFinishing())render();},4500);return true;}

            if(item.getTitle().equals("Clear saved data")){clearPrinter(ip);return true;}

            return false;

        });

        menu.show();

    }

    private View printerCard(String ip){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(14),dp(8),dp(14));card.setBackground(rounded(Color.WHITE,0xffd1d5d8,10));
        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout details=new LinearLayout(this);details.setOrientation(LinearLayout.VERTICAL);
        TextView name=text(AutoCache.printerName(ip),17,TEXT);name.setTypeface(null,Typeface.BOLD);details.addView(name);
        TextView address=text(ip+(AutoCache.hasChangedAddress(ip)?" · Address changed":""),14,MUTED);address.setPadding(0,dp(5),0,0);details.addView(address);
        header.addView(details,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        String status=AutoCache.liveCheckEnabled()?AutoCache.readString("status|"+ip,"Not checked yet"):"Check off";
        boolean ready=status.startsWith("Ready ·") || status.equals("Ready");
        TextView badge=text(status,12,ready?0xff08623d:MUTED);badge.setMaxWidth(dp(135));badge.setPadding(dp(8),dp(5),dp(8),dp(5));badge.setBackground(rounded(ready?0xffd8f4e6:0xffeeeeee,0,14));header.addView(badge);
        TextView menu=text("⋮",24,TEXT);menu.setGravity(Gravity.CENTER);menu.setContentDescription("Printer options for "+ip);menu.setOnClickListener(v->printerMenu(v,ip));header.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48)));card.addView(header);
        TextView timings=text((CommunicationTiming.isMeasuring(ip)?"Uncached measurement running · Stop from the menu\n":"")+AutoCache.timingCardSummary(ip),12,MUTED);timings.setPadding(0,dp(8),dp(8),0);card.addView(timings);timings.postDelayed(()->timings.setText((CommunicationTiming.isMeasuring(ip)?"Uncached measurement running · Stop from the menu\n":"")+AutoCache.timingCardSummary(ip)),3500);
        return card;
    }
    private void render(){

        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(NAVY);

        getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i.consumeSystemWindowInsets();});

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setBackgroundColor(NAVY);

        TextView back=text("\u2039",32,Color.WHITE);back.setGravity(Gravity.CENTER);back.setContentDescription("Back");back.setOnClickListener(v->finish());header.addView(back,new LinearLayout.LayoutParams(dp(56),dp(56)));

        TextView title=text("Saved printer data",20,Color.WHITE);title.setTypeface(null,Typeface.BOLD);header.addView(title);root.addView(header);

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.WHITE);

        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(24),dp(16),dp(20));scroll.addView(content,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        ArrayList<String> printers=new ArrayList<>(AutoCache.printers());Collections.sort(printers);

        if(printers.isEmpty()){TextView empty=text("No saved printers yet. Open a print preview to start saving responses.",15,MUTED);empty.setPadding(0,0,0,dp(16));content.addView(empty);}

        for(String ip:printers){LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);params.bottomMargin=dp(12);content.addView(printerCard(ip),params);}


        View separator=new View(this);separator.setBackgroundColor(0xffdadde0);LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(1));line.topMargin=dp(20);content.addView(separator,line);

        LinearLayout diagnostics=new LinearLayout(this);diagnostics.setGravity(Gravity.CENTER_VERTICAL);diagnostics.setPadding(0,dp(18),0,dp(18));diagnostics.setContentDescription("Diagnostics: record requests and export CSV");diagnostics.setOnClickListener(v->startActivity(new Intent(this,DiagnosticsActivity.class)));

        LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);TextView dtitle=text("Diagnostics",17,TEXT);dtitle.setTypeface(null,Typeface.BOLD);labels.addView(dtitle);TextView sub=text("Record requests and export CSV",14,MUTED);sub.setPadding(0,dp(3),0,0);labels.addView(sub);diagnostics.addView(labels,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));TextView arrow=text("\u203a",26,TEXT);diagnostics.addView(arrow);content.addView(diagnostics);

        View bottomLine=new View(this);bottomLine.setBackgroundColor(0xffdadde0);content.addView(bottomLine,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(1)));

        root.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));

        if(!printers.isEmpty()){TextView all=text("Clear all saved printers",15,TEAL);all.setTypeface(null,Typeface.BOLD);all.setGravity(Gravity.CENTER_VERTICAL);all.setPadding(dp(24),0,dp(16),0);all.setBackgroundColor(Color.WHITE);all.setOnClickListener(v->clearAll());root.addView(all,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(56)));}

        setContentView(root);

    }

}


