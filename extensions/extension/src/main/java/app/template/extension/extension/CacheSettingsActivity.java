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
    private void clearPrinter(String ip){new AlertDialog.Builder(this).setTitle("Clear saved printer data?").setMessage("Saved responses for this printer will be learned again the next time you print.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{AutoCache.clearPrinter(ip);render();}).show();}
    private void clearAll(){new AlertDialog.Builder(this).setTitle("Clear all saved printers?").setMessage("Saved responses for all printers will be learned again the next time you print.").setNegativeButton("Cancel",null).setPositiveButton("Clear all",(d,w)->{AutoCache.clearAll();render();}).show();}
    private void printerMenu(View anchor,String ip){
        PopupMenu menu=new PopupMenu(this,anchor);
        menu.getMenu().add("Refresh status").setEnabled(AutoCache.liveCheckEnabled());
        menu.getMenu().add("Clear saved data");
        menu.setOnMenuItemClickListener(item->{
            if(item.getTitle().equals("Refresh status")){Readiness.refresh(ip);new Handler(Looper.getMainLooper()).postDelayed(()->{if(!isFinishing())render();},4500);return true;}
            if(item.getTitle().equals("Clear saved data")){clearPrinter(ip);return true;}
            return false;
        });
        menu.show();
    }
    private View printerCard(String ip){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(16),dp(14),dp(8),dp(14));card.setBackground(rounded(Color.WHITE,0xffd1d5d8,10));
        LinearLayout details=new LinearLayout(this);details.setOrientation(LinearLayout.VERTICAL);
        TextView name=text("Saved printer",17,TEXT);name.setTypeface(null,Typeface.BOLD);details.addView(name);
        TextView address=text(ip,14,MUTED);address.setPadding(0,dp(5),0,0);details.addView(address);
        card.addView(details,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        String status=AutoCache.liveCheckEnabled()?AutoCache.readString("status|"+ip,"Not checked yet"):"Check off";
        boolean ready=status.startsWith("Ready ·") || status.equals("Ready");
        TextView badge=text(status,12,ready?0xff08623d:MUTED);badge.setMaxWidth(dp(135));badge.setPadding(dp(8),dp(5),dp(8),dp(5));badge.setBackground(rounded(ready?0xffd8f4e6:0xffeeeeee,0,14));card.addView(badge);
        TextView menu=text("⋮",24,TEXT);menu.setGravity(Gravity.CENTER);menu.setContentDescription("Printer options for "+ip);menu.setOnClickListener(v->printerMenu(v,ip));card.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48)));
        return card;
    }
    private void render(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(NAVY);
        getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i.consumeSystemWindowInsets();});
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setBackgroundColor(NAVY);
        TextView back=text("‹",32,Color.WHITE);back.setGravity(Gravity.CENTER);back.setContentDescription("Back");back.setOnClickListener(v->finish());header.addView(back,new LinearLayout.LayoutParams(dp(56),dp(56)));
        TextView title=text("Saved printer data",20,Color.WHITE);title.setTypeface(null,Typeface.BOLD);header.addView(title);root.addView(header);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.WHITE);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(24),dp(16),dp(20));scroll.addView(content,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        ArrayList<String> printers=new ArrayList<>(AutoCache.printers());Collections.sort(printers);
        if(printers.isEmpty()){TextView empty=text("No saved printers yet. Open a print preview to start saving responses.",15,MUTED);empty.setPadding(0,0,0,dp(16));content.addView(empty);}
        for(String ip:printers){LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);params.bottomMargin=dp(12);content.addView(printerCard(ip),params);}
        View separator=new View(this);separator.setBackgroundColor(0xffdadde0);LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(1));line.topMargin=dp(20);content.addView(separator,line);
        LinearLayout diagnostics=new LinearLayout(this);diagnostics.setGravity(Gravity.CENTER_VERTICAL);diagnostics.setPadding(0,dp(18),0,dp(18));diagnostics.setContentDescription("Diagnostics: record requests and export CSV");diagnostics.setOnClickListener(v->startActivity(new Intent(this,DiagnosticsActivity.class)));
        LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);TextView dtitle=text("Diagnostics",17,TEXT);dtitle.setTypeface(null,Typeface.BOLD);labels.addView(dtitle);TextView sub=text("Record requests and export CSV",14,MUTED);sub.setPadding(0,dp(3),0,0);labels.addView(sub);diagnostics.addView(labels,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));TextView arrow=text("›",26,TEXT);diagnostics.addView(arrow);content.addView(diagnostics);
        View bottomLine=new View(this);bottomLine.setBackgroundColor(0xffdadde0);content.addView(bottomLine,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(1)));
        root.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        if(!printers.isEmpty()){TextView all=text("Clear all saved printers",15,TEAL);all.setTypeface(null,Typeface.BOLD);all.setGravity(Gravity.CENTER_VERTICAL);all.setPadding(dp(24),0,dp(16),0);all.setBackgroundColor(Color.WHITE);all.setOnClickListener(v->clearAll());root.addView(all,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(56)));}
        setContentView(root);
    }
}
