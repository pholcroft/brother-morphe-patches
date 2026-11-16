package app.template.extension.extension;



import android.app.Activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.net.Uri;

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
import android.widget.FrameLayout;

import android.widget.PopupMenu;

import android.widget.ScrollView;

import android.widget.TextView;

import java.util.ArrayList;

import java.util.Collections;



/** Manage saved printer data and open diagnostics. */

public final class CacheSettingsActivity extends Activity {

    private static final int NAVY=0xff182a43, TEAL=0xff00796c, TEXT=0xff212121, MUTED=0xff666666;

    private LinearLayout root;
    private ProgressDialog replayProgress;private AlertDialog replayResult;private boolean replayVisible;
    private final Handler replayHandler=new Handler(Looper.getMainLooper());
    private final ReplayController.Listener replayListener=()->replayHandler.post(this::showReplayState);

    private int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}

    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}

    private GradientDrawable rounded(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}

    @Override public void onCreate(Bundle state){super.onCreate(state);AutoCache.init(getApplicationContext());render();}

    @Override public void onResume(){super.onResume();replayVisible=true;ReplayController.attach(replayListener);if(root!=null)render();showReplayState();}
    @Override public void onPause(){replayVisible=false;ReplayController.detach(replayListener);if(replayProgress!=null){replayProgress.dismiss();replayProgress=null;}if(replayResult!=null){replayResult.dismiss();replayResult=null;}super.onPause();}

    private void clearPrinter(String ip){new AlertDialog.Builder(this).setTitle("Clear saved printer data?").setMessage("Saved responses and comparison times will be removed. With the cache on, new replies are learned immediately and may be reused during the same preview. Turn the cache off in settings to use only live replies.\n\nExisting diagnostic recordings are kept separately.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{AutoCache.clearPrinter(ip);render();}).show();}

    private void clearAll(){new AlertDialog.Builder(this).setTitle("Clear all saved printers?").setMessage("Saved responses and comparison times will be removed. With the cache on, new replies are learned immediately and may be reused during the same preview. Turn the cache off in settings to use only live replies.\n\nExisting diagnostic recordings are kept separately.").setNegativeButton("Cancel",null).setPositiveButton("Clear all",(d,w)->{AutoCache.clearAll();render();}).show();}

    private void openFixExplanation(){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/pholcroft/brother-morphe-patches#how-it-works-and-limitations")));}catch(ActivityNotFoundException e){new AlertDialog.Builder(this).setTitle("Unable to open link").setMessage("No app is available to open the GitHub page.").setPositiveButton("OK",null).show();}}
    private void replay(String ip){
        if(ReplayController.isRunning()){showReplayState();return;}
        java.util.List<ReplayEngine.Spec> specs=CommunicationTiming.replaySequence(ip);
        if(specs==null||specs.isEmpty()){new AlertDialog.Builder(this).setTitle("No complete activity recorded").setMessage("Open a print preview and wait for the options to load, then try again. The latest activity is kept while the print service is running.").setPositiveButton("OK",null).show();return;}
        String mac=AutoCache.readString("mac|"+ip,"");if(mac.isEmpty()){new AlertDialog.Builder(this).setTitle("Printer identity unavailable").setMessage("The printer's hardware address has not been saved. Open a print preview and try again. No requests were replayed.").setPositiveButton("OK",null).show();return;}
        ReplayController.start(ip,specs,AutoCache.replaySnapshot(ip,specs),mac);showReplayState();
    }
    private void showReplayState(){
        if(!replayVisible||isFinishing()||isDestroyed())return;
        if(ReplayController.isRunning()){
            if(replayProgress==null){replayProgress=new ProgressDialog(this);replayProgress.setMessage("Checking printer and replaying requests...");replayProgress.setCancelable(false);replayProgress.setButton(DialogInterface.BUTTON_NEGATIVE,"Cancel",(d,w)->{});replayProgress.show();replayProgress.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v->{ReplayController.cancel();replayProgress.setMessage("Cancelling...");replayProgress.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(false);});}return;
        }
        if(replayProgress!=null){replayProgress.dismiss();replayProgress=null;}
        ReplayController.Outcome outcome=ReplayController.peekOutcome();if(outcome==null||replayResult!=null)return;
        AlertDialog.Builder dialog=new AlertDialog.Builder(this).setCancelable(false);
        if(outcome.error!=null)dialog.setTitle("Unable to replay").setMessage(outcome.error);
        else{ReplayEngine.Result result=outcome.result;String summary=String.format(java.util.Locale.ROOT,"Replay time: %.2f s\nRequests replayed: %d\nChanged objects: %d",result.elapsedMs/1000.0,result.completed,result.changes.size());int routine=0;for(ReplayEngine.Change c:result.changes)if(ReplayEngine.isRoutine(c.oid))routine++;if(routine>0)summary+="\nIncludes "+routine+" normally changing uptime value(s)";if(result.uncached>0||result.unsupported>0)summary+="\nNot compared: "+(result.uncached+result.unsupported)+" requests without comparable cached values";summary+="\n\nSingle-object SNMP requests were replayed in the order observed by the service. The identity check is excluded from the time. Cached data was not changed.";dialog.setTitle("Uncached replay result").setMessage(summary);if(!result.changes.isEmpty())dialog.setNeutralButton("View differences",(d,w)->{ReplayController.clearOutcome(outcome);replayResult=null;ReplayDiffActivity.setChanges(result.changes);startActivity(new Intent(this,ReplayDiffActivity.class));});}
        dialog.setPositiveButton("Close",(d,w)->{ReplayController.clearOutcome(outcome);replayResult=null;});replayResult=dialog.create();replayResult.show();
    }
    private void printerMenu(View anchor,String ip){

        PopupMenu menu=new PopupMenu(this,anchor);

        menu.getMenu().add("Printer details");



        menu.getMenu().add("Measure without cache");


        menu.getMenu().add("Clear saved data");

        menu.setOnMenuItemClickListener(item->{

            if(item.getTitle().equals("Printer details")){String mac=AutoCache.readString("mac|"+ip,"");new AlertDialog.Builder(this).setTitle("Printer details").setMessage(AutoCache.printerName(ip)+"\nAddress: "+ip+"\nHardware address: "+(mac.isEmpty()?"Not recorded":mac)+(AutoCache.hasChangedAddress(ip)?"\nThis printer has been seen at another address.":"")+"\n\nThe mod checks the hardware address when the service requests it and can reuse saved responses if the printer moves to a new IP address. If the address cannot be checked, normal printer requests are used.").setPositiveButton("Close",null).show();return true;}

            if(item.getTitle().equals("Measure without cache")){replay(ip);return true;}

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
        TextView menu=text("⋮",24,TEXT);menu.setGravity(Gravity.CENTER);menu.setContentDescription("Printer options for "+ip);menu.setOnClickListener(v->printerMenu(v,ip));header.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48)));card.addView(header);
        TextView timings=text(AutoCache.timingCardSummary(ip),12,MUTED);timings.setPadding(0,dp(8),dp(8),0);card.addView(timings);timings.postDelayed(()->timings.setText(AutoCache.timingCardSummary(ip)),3500);
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

        LinearLayout footer=new LinearLayout(this);footer.setOrientation(LinearLayout.HORIZONTAL);footer.setGravity(Gravity.CENTER_VERTICAL);footer.setBackgroundColor(Color.WHITE);
        if(!printers.isEmpty()){TextView all=text("Clear all saved printers",15,TEAL);all.setTypeface(null,Typeface.BOLD);all.setGravity(Gravity.CENTER_VERTICAL);all.setPadding(dp(24),0,dp(8),0);all.setOnClickListener(v->clearAll());footer.addView(all,new LinearLayout.LayoutParams(0,dp(56),1));}else footer.addView(new View(this),new LinearLayout.LayoutParams(0,dp(56),1));
        FrameLayout infoTouch=new FrameLayout(this);infoTouch.setContentDescription("What is this fix?");infoTouch.setOnClickListener(v->openFixExplanation());
        TextView info=text("i",12,MUTED);info.setTypeface(null,Typeface.BOLD);info.setGravity(Gravity.CENTER);info.setBackground(rounded(Color.WHITE,0xff999999,20));FrameLayout.LayoutParams iconParams=new FrameLayout.LayoutParams(dp(18),dp(18),Gravity.CENTER);infoTouch.addView(info,iconParams);footer.addView(infoTouch,new LinearLayout.LayoutParams(dp(48),dp(56)));root.addView(footer);


        setContentView(root);

    }

}


