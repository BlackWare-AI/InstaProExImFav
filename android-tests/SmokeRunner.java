package dev.instapro.patcher.smoke;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
/** Executes against the installed, signed, debuggable=false patcher. No source APK required. */
public final class SmokeRunner extends Instrumentation {
 public void onCreate(Bundle args){super.onCreate(args);start();}
 static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 static List<View> views(View root){List<View> out=new ArrayList<>();out.add(root);if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++)out.addAll(views(group.getChildAt(i)));}return out;}
 static String preview(Activity a){for(View v:views(a.getWindow().getDecorView()))if(v instanceof TextView){String s=((TextView)v).getText().toString();if(s.startsWith("Paket: "))return s;}throw new AssertionError("No package preview");}
 public void onStart(){
  Bundle result=new Bundle();Activity activity=null;
  try{
   Intent intent=new Intent(Intent.ACTION_MAIN);intent.setClassName("dev.instapro.patcher","dev.instapro.patcher.MainActivity");intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   activity=startActivitySync(intent);waitForIdleSync();final Activity a=activity;
   runOnMainSync(()->{
    List<EditText> fields=new ArrayList<>();boolean viewport=false,mod=false,cancel=false;
    for(View v:views(a.getWindow().getDecorView())){
     if(v instanceof EditText)fields.add((EditText)v);
     if(v instanceof ScrollView){check(((ScrollView)v).isFillViewport(),"ScrollView must fill the screen");viewport=true;}
     if(v instanceof Button){Button b=(Button)v;if(b.getText().toString().equals("Modla ve kaydet")){check(!b.isEnabled(),"Build enabled without verified APK");mod=true;}if(b.getText().toString().equals("İptal")){check(b.getVisibility()==View.GONE,"Idle cancel visible");cancel=true;}}
    }
    check(fields.size()==2&&viewport&&mod&&cancel,"Form missing controls");
    EditText suffix=fields.get(1);
    for(String s:new String[]{"gpt",".gpt"}){suffix.setText(s);check(preview(a).startsWith("Paket: com.instagram.android.gpt\n"),"Preview rejected "+s);}
    for(String s:new String[]{"","  "}){suffix.setText(s);check(preview(a).startsWith("Paket: com.instagram.android\n"),"Original-package preview failed");}
    suffix.setText("gpt");
   });
   waitForIdleSync();
   int flags=getTargetContext().getApplicationInfo().flags;
   check((flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)==0,"Target is debuggable");
   // Exercise the recovery screen with an explicitly synthetic report, not a claimed real crash.
   Class<?> reporter=Class.forName("dev.instapro.patcher.CrashReporter",true,getTargetContext().getClassLoader());
   java.lang.reflect.Method record=reporter.getDeclaredMethod("record",android.content.Context.class,Throwable.class);record.setAccessible(true);
   java.lang.reflect.Method clear=reporter.getDeclaredMethod("clear",android.content.Context.class);clear.setAccessible(true);
   String marker="SMOKE_TEST_SYNTHETIC_FAILURE";
   record.invoke(null,getTargetContext(),new IllegalStateException(marker));
   Intent recoveryIntent=new Intent(Intent.ACTION_MAIN).setClassName("dev.instapro.patcher","dev.instapro.patcher.StartupActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   final Activity recovery=startActivitySync(recoveryIntent);waitForIdleSync();
   runOnMainSync(()->{
    boolean found=false;
    for(View v:views(recovery.getWindow().getDecorView()))if(v instanceof Button&&((Button)v).getText().toString().equals("Hata kaydını kopyala")){v.performClick();found=true;}
    check(found,"Recovery copy button missing");
    ClipboardManager clipboard=(ClipboardManager)recovery.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
    check(clipboard.getPrimaryClip()!=null&&clipboard.getPrimaryClip().getItemAt(0).getText().toString().contains(marker),"Error report copy failed");
   });
   clear.invoke(null,getTargetContext());
   runOnMainSync(()->recovery.finish());waitForIdleSync();
   result.putString("stream","PASS local report persistence, recovery screen and copy (synthetic fault); release Activity startup; form controls; optional gpt/.gpt/blank suffix previews; viewport; debuggable=false. APK patch generation and InstaPro launch are NOT tested.\n");
   finish(Activity.RESULT_OK,result);
  }catch(Throwable error){result.putString("stream",android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}
 }
}
