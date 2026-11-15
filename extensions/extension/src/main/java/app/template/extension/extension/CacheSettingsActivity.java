package app.template.extension.extension;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;

/** Local controls accessible from the print service's settings entry. */
public final class CacheSettingsActivity extends Activity {
    private LinearLayout content;
    @Override public void onCreate(Bundle state){super.onCreate(state);AutoCache.init(getApplicationContext());render();}
    @Override public void onResume(){super.onResume();if(content!=null)render();}
    private TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setPadding(0,12,0,12);return t;}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setOnClickListener(v->action.run());return b;}
    private void render(){
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(20*getResources().getDisplayMetrics().density);content.setPadding(pad,pad,pad,pad);
        getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        scroll.setClipToPadding(false);
        scroll.setOnApplyWindowInsetsListener((view,insets)->{view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();});
        scroll.addView(content,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));setContentView(scroll);
        content.addView(text("Faster print previews",23));
        TextView attribution=text("Unofficial community patch · Not affiliated with Brother",12);attribution.setTextColor(0xff666666);attribution.setTypeface(null,2);content.addView(attribution);
        content.addView(text("Save printer responses on this device to make future previews faster. The first preview learns them; saved information may become out of date.",15));
        Switch enabled=new Switch(this);enabled.setText("Speed up previews with saved printer responses");enabled.setChecked(AutoCache.enabled());enabled.setOnCheckedChangeListener((v,checked)->AutoCache.setEnabled(checked));content.addView(enabled);
        Switch live=new Switch(this);live.setText("Check printer reachability and readiness in background");live.setChecked(AutoCache.liveCheckEnabled());live.setOnCheckedChangeListener((v,checked)->{AutoCache.setLiveCheckEnabled(checked);if(checked)for(String ip:AutoCache.printers())Readiness.refresh(ip);render();});content.addView(live);
        content.addView(text("The live check reports the printer's IPP state and whether it accepts jobs. It does not block preview or guarantee a successful print.",14));
        ArrayList<String> printers=new ArrayList<>(AutoCache.printers());Collections.sort(printers);
        if(printers.isEmpty())content.addView(text("No saved printers yet. Open a print preview to start saving responses.",16));
        for(String ip:printers){
            String mac=AutoCache.readString("mac|"+ip,"");String status=AutoCache.liveCheckEnabled()?AutoCache.readString("status|"+ip,"Not checked yet"):"Live check off";
            content.addView(text("Printer: "+ip+(mac.isEmpty()?"":"\nHardware address: "+mac)+"\n"+status,16));
            if(AutoCache.liveCheckEnabled())content.addView(button("Refresh readiness",()->{Readiness.refresh(ip);new Handler(Looper.getMainLooper()).postDelayed(()->{if(!isFinishing())render();},4500);}));
            content.addView(button("Clear this printer",()->new AlertDialog.Builder(this).setTitle("Clear this printer?").setMessage("Its responses will be learned again the next time you print.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{AutoCache.clearPrinter(ip);render();}).show()));
        }
        if(!printers.isEmpty())content.addView(button("Clear all printers",()->new AlertDialog.Builder(this).setTitle("Clear all printers?").setMessage("All saved responses will be learned again.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{AutoCache.clearAll();render();}).show()));
    }
}
