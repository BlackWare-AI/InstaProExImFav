package dev.instapro.patcher;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

/** Minimal launcher stays independent of patch/sign libraries and the main form. */
public final class StartupActivity extends Activity {
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  String report=CrashReporter.read(this);
  if(report!=null){showFailure(this,report);return;}
  openMain(this);
 }
 static void openMain(Activity activity){
  CrashReporter.clear(activity);
  try{activity.startActivity(new Intent().setClassName(activity,"dev.instapro.patcher.MainActivity"));activity.finish();}
  catch(RuntimeException|LinkageError error){showFailure(activity,CrashReporter.record(activity,error));}
 }
 static void showFailure(Activity activity,String report){
  activity.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
  activity.getWindow().setStatusBarColor(Color.BLACK);activity.getWindow().setNavigationBarColor(Color.BLACK);
  int padding=Math.round(20*activity.getResources().getDisplayMetrics().density);
  LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(padding,padding,padding,padding);root.setBackgroundColor(Color.BLACK);
  TextView title=new TextView(activity);title.setText("Açılış hatası kaydedildi");title.setTextSize(21);title.setTextColor(Color.WHITE);root.addView(title);
  TextView info=new TextView(activity);info.setText("Nedeni belirlemek için aşağıdaki hata kaydını kopyalayıp paylaşın. APK ve anahtarlarınız değiştirilmedi.");info.setTextColor(0xffcccccc);info.setPadding(0,padding,0,padding);root.addView(info);
  Button copy=new Button(activity);copy.setText("Hata kaydını kopyala");copy.setAllCaps(false);root.addView(copy);
  copy.setOnClickListener(v->{ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("InstaPro Patcher error",report));Toast.makeText(activity,"Hata kaydı kopyalandı",Toast.LENGTH_SHORT).show();});
  Button retry=new Button(activity);retry.setText("Yeniden dene");retry.setAllCaps(false);root.addView(retry);retry.setOnClickListener(v->openMain(activity));
  TextView detail=new TextView(activity);detail.setText(report);detail.setTextSize(12);detail.setTextColor(0xffbbbbbb);detail.setTextIsSelectable(true);root.addView(detail);
  ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.BLACK);scroll.addView(root);activity.setContentView(scroll);
  root.setOnApplyWindowInsetsListener((v,in)->{root.setPadding(padding+in.getSystemWindowInsetLeft(),padding+in.getSystemWindowInsetTop(),padding+in.getSystemWindowInsetRight(),padding+in.getSystemWindowInsetBottom());return in;});root.requestApplyInsets();
 }
}
