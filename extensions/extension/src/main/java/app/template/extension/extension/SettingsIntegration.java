package app.template.extension.extension;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/** Adds cache controls to Brother's existing service settings. */
public final class SettingsIntegration {
    public static void attach(Activity activity) {
        AutoCache.init(activity.getApplicationContext());
        int id = activity.getResources().getIdentifier("scrollView_settings", "id", activity.getPackageName());
        View view = activity.findViewById(id);
        if (!(view instanceof ViewGroup)) return;
        ViewGroup scroll = (ViewGroup) view;
        if (scroll.getChildCount() != 1 || !(scroll.getChildAt(0) instanceof ViewGroup)) return;
        ViewGroup parent = (ViewGroup) scroll.getChildAt(0);
        View previous = parent.findViewWithTag("morphe-fast-preview-controls");
        if (previous != null) parent.removeView(previous);
        LinearLayout section = new LinearLayout(activity);
        section.setTag("morphe-fast-preview-controls");
        section.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * activity.getResources().getDisplayMetrics().density);
        section.setPadding(pad, pad, pad, pad);
        TextView heading = new TextView(activity);
        heading.setText("Faster print previews");
        heading.setTextSize(19);
        section.addView(heading);
        TextView attribution = new TextView(activity);
        attribution.setText("Unofficial community patch · Not affiliated with Brother");
        attribution.setTextSize(12);
        attribution.setTextColor(0xff666666);
        attribution.setTypeface(null, 2);
        section.addView(attribution);
        Switch cache = new Switch(activity);
        cache.setText("Speed up previews with saved printer responses");
        cache.setChecked(AutoCache.enabled());
        cache.setOnCheckedChangeListener((button, checked) -> AutoCache.setEnabled(checked));
        section.addView(cache);
        Switch ready = new Switch(activity);
        ready.setText("Check printer readiness in the background");
        ready.setChecked(AutoCache.liveCheckEnabled());
        ready.setOnCheckedChangeListener((button, checked) -> AutoCache.setLiveCheckEnabled(checked));
        section.addView(ready);
        Button manage = new Button(activity);
        manage.setText("Manage saved printer data");
        manage.setOnClickListener(button -> activity.startActivity(new Intent(activity, CacheSettingsActivity.class)));
        section.addView(manage);
        parent.addView(section, 0);
    }
    private SettingsIntegration() {}
}
