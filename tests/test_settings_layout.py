"""Exercise settings integration with Brother's AndroidX-style ViewGroup.

Run from the repository root with a JDK in JAVA_HOME. Android classes are
small test doubles; the AndroidX layout deliberately does not extend LinearLayout.
"""
import os
import pathlib
import subprocess
import tempfile

root = pathlib.Path(__file__).resolve().parents[1]
source = root / 'extensions/extension/src/main/java/app/template/extension/extension/SettingsIntegration.java'
files = {
'android/content/Context.java': '''package android.content; public class Context { public Context getApplicationContext(){return this;} public String getPackageName(){return "example";} public android.content.res.Resources getResources(){return new android.content.res.Resources();} public void startActivity(Intent i){} }''',
'android/content/Intent.java': '''package android.content; public class Intent { public Intent(Context c,Class<?> k){} }''',
'android/content/res/Resources.java': '''package android.content.res; public class Resources { public int getIdentifier(String n,String t,String p){return 1;} public android.util.DisplayMetrics getDisplayMetrics(){return new android.util.DisplayMetrics();} }''',
'android/util/DisplayMetrics.java': '''package android.util; public class DisplayMetrics { public float density=1; }''',
'android/app/Activity.java': '''package android.app; public class Activity extends android.content.Context { public android.view.View screen; public android.view.View findViewById(int id){return screen;} }''',
'android/view/View.java': '''package android.view; public class View { Object tag; public void setTag(Object t){tag=t;} public View findViewWithTag(Object t){return t.equals(tag)?this:null;} public interface OnClickListener {void onClick(View v);} public void setOnClickListener(OnClickListener l){} }''',
'android/view/ViewGroup.java': '''package android.view; import java.util.*; public class ViewGroup extends View { public final ArrayList<View> children=new ArrayList<>(); public int getChildCount(){return children.size();} public View getChildAt(int i){return children.get(i);} public void addView(View v){children.add(v);} public void addView(View v,int i){children.add(i,v);} public void removeView(View v){children.remove(v);} public View findViewWithTag(Object t){for(View v:children){View x=v.findViewWithTag(t);if(x!=null)return x;}return super.findViewWithTag(t);} }''',
'android/widget/LinearLayout.java': '''package android.widget; public class LinearLayout extends android.view.ViewGroup { public static final int VERTICAL=1; public LinearLayout(android.content.Context c){} public void setOrientation(int o){} public void setPadding(int a,int b,int c,int d){} }''',
'android/widget/TextView.java': '''package android.widget; public class TextView extends android.view.View { public TextView(android.content.Context c){} public void setText(String s){} public void setTextSize(int n){} public void setTextColor(int c){} public void setTypeface(Object f,int style){} }''',
'android/widget/Button.java': '''package android.widget; public class Button extends TextView { public Button(android.content.Context c){super(c);} }''',
'android/widget/Switch.java': '''package android.widget; public class Switch extends TextView { public Switch(android.content.Context c){super(c);} public void setChecked(boolean b){} public interface Listener {void onCheckedChanged(Switch b,boolean c);} public void setOnCheckedChangeListener(Listener l){} }''',
'app/template/extension/extension/AutoCache.java': '''package app.template.extension.extension; public class AutoCache { public static void init(android.content.Context c){} public static boolean enabled(){return true;} public static boolean liveCheckEnabled(){return false;} public static void setEnabled(boolean b){} public static void setLiveCheckEnabled(boolean b){} }''',
'app/template/extension/extension/CacheSettingsActivity.java': '''package app.template.extension.extension; public class CacheSettingsActivity {}''',
'SettingsLayoutTest.java': '''import android.app.Activity; import android.view.ViewGroup; import app.template.extension.extension.SettingsIntegration; public class SettingsLayoutTest { public static void main(String[] args) { Activity a=new Activity(); ViewGroup scroll=new ViewGroup(); ViewGroup androidXLinearLayoutCompat=new ViewGroup(); scroll.addView(androidXLinearLayoutCompat); a.screen=scroll; SettingsIntegration.attach(a); if(androidXLinearLayoutCompat.getChildCount()!=1) throw new AssertionError("controls missing from AndroidX layout"); SettingsIntegration.attach(a); if(androidXLinearLayoutCompat.getChildCount()!=1) throw new AssertionError("duplicate controls on resume"); System.out.println("Settings layout checks passed"); } }''',
}
with tempfile.TemporaryDirectory() as directory:
    base=pathlib.Path(directory)
    for name, content in files.items():
        p=base/name; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(content,encoding='utf-8')
    javac=pathlib.Path(os.environ['JAVA_HOME'])/'bin/javac.exe'
    java=pathlib.Path(os.environ['JAVA_HOME'])/'bin/java.exe'
    subprocess.run([str(javac),'-d',str(base/'classes'),str(source),*[str(p) for p in base.rglob('*.java')]],check=True)
    subprocess.run([str(java),'-cp',str(base/'classes'),'SettingsLayoutTest'],check=True)
