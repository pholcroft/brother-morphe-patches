package app.template.extension.extension;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
        scroll.addView(content,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));setContentView(scroll);
        content.addView(text("Printer response cache",23));
        content.addView(text("The first preview learns printer responses. Later previews reuse them. Stored values are private to this app and may be stale.",15));
        Switch enabled=new Switch(this);enabled.setText("Use and learn cached responses");enabled.setChecked(AutoCache.enabled());enabled.setOnCheckedChangeListener((v,checked)->AutoCache.setEnabled(checked));content.addView(enabled);
        Switch live=new Switch(this);live.setText("Check printer reachability and readiness in background");live.setChecked(AutoCache.liveCheckEnabled());live.setOnCheckedChangeListener((v,checked)->{AutoCache.setLiveCheckEnabled(checked);if(checked)for(String ip:AutoCache.printers())Readiness.refresh(ip);render();});content.addView(live);
        content.addView(text("The live check reports the printer's IPP state and whether it accepts jobs. It does not block preview or guarantee a successful print.",14));
        content.addView(button("Open Brother settings",()->{Intent intent=new Intent();intent.setClassName(getPackageName(),"com.brother.printservice.settings.SettingsActivity");startActivity(intent);}));
        ArrayList<String> printers=new ArrayList<>(AutoCache.printers());Collections.sort(printers);
        if(printers.isEmpty())content.addView(text("No printers learned yet. Open a print preview once to learn one.",16));
        for(String ip:printers){
            String mac=AutoCache.readString("mac|"+ip,"");String status=AutoCache.liveCheckEnabled()?AutoCache.readString("status|"+ip,"Not checked yet"):"Live check off";
            content.addView(text("Printer: "+ip+(mac.isEmpty()?"":"\nHardware address: "+mac)+"\n"+status,16));
            if(AutoCache.liveCheckEnabled())content.addView(button("Refresh readiness",()->{Readiness.refresh(ip);new Handler(Looper.getMainLooper()).postDelayed(()->{if(!isFinishing())render();},4500);}));
            content.addView(button("Clear this printer",()->new AlertDialog.Builder(this).setTitle("Clear this printer?").setMessage("Its responses will be learned again the next time you print.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{AutoCache.clearPrinter(ip);render();}).show()));
        }
        if(!printers.isEmpty())content.addView(button("Clear all printers",()->new AlertDialog.Builder(this).setTitle("Clear all printers?").setMessage("All saved responses will be learned again.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{AutoCache.clearAll();render();}).show()));
    }
}
