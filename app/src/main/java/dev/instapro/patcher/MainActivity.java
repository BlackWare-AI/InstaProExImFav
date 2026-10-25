package dev.instapro.patcher;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.text.InputType;
import android.widget.*;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.*;
public final class MainActivity extends Activity {
 static final int SELECT=1,SAVE=2,EXPORT_KEY=3,IMPORT_KEY=4;
 static Session session;TextView status;EditText label,suffix;boolean savePicker,resumed;
 static final class Session {
  final Context app;final File dir,source,output;final ExecutorService worker=Executors.newSingleThreadExecutor();
  volatile boolean busy,ready;volatile String message="Orijinal InstaPro 15.85 APK dosyasını seçin.";volatile Future<?> task;volatile Thread running;volatile boolean cancelRequested;WeakReference<MainActivity> view=new WeakReference<>(null);
  Session(Context c){app=c.getApplicationContext();dir=new File(app.getCacheDir(),"patch-work");dir.mkdirs();source=new File(dir,"source.apk");output=new File(dir,"release.apk");source.delete();output.delete();new File(dir,"unsigned.tmp.apk").delete();}
  void update(String text){message=text;new Handler(Looper.getMainLooper()).post(()->{MainActivity a=view.get();if(a!=null&&!a.isDestroyed())a.render();});}
  void run(Work work){if(busy)return;busy=true;ready=false;cancelRequested=false;task=worker.submit(()->{running=Thread.currentThread();if(cancelRequested)running.interrupt();try{work.run();}catch(Exception e){output.delete();update("İşlem tamamlanamadı: "+e.getClass().getSimpleName()+"\n"+String.valueOf(e.getMessage()));}finally{running=null;Thread.interrupted();busy=false;update(message);}});}
 }
 interface Work {void run()throws Exception;}
 @Override public void onCreate(Bundle state){super.onCreate(state);if(session==null)session=new Session(this);session.view=new WeakReference<>(this);getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(22),dp(22),dp(22),dp(22));root.setBackgroundColor(Color.BLACK);
  TextView title=new TextView(this);title.setText("InstaPro Patcher");title.setTextSize(25);title.setTextColor(Color.WHITE);root.addView(title);
  TextView hint=new TextView(this);hint.setText("Favorileri içe ve dışa aktarma ekle.\nPaket ekini değiştirmek ayrı uygulama ve veri alanı oluşturur.");hint.setTextSize(14);hint.setTextColor(0xffaaaaaa);hint.setPadding(0,dp(16),0,dp(20));root.addView(hint);
  button(root,"Kaynak APK seç",()->picker(SELECT,"*/*",null));label=field(root,"Görünen uygulama adı",state==null?"InstaPro GPT":state.getString("label","InstaPro GPT"));suffix=field(root,"Paket eki",state==null?".gpt":state.getString("suffix",".gpt"));
  button(root,"Modla ve kaydet",()->build());button(root,"İmza anahtarı",()->new AlertDialog.Builder(this).setTitle("İmza anahtarı").setMessage("Güncellemeler aynı anahtarı gerektirir. Patcher kaldırılmadan önce parolalı yedek alın. Eski denemenin anahtarı burada bulunmaz.").setPositiveButton("Yedeği kaydet",(d,w)->picker(EXPORT_KEY,"application/octet-stream","InstaPro-signing.p12")).setNeutralButton("Yedeği yükle",(d,w)->new AlertDialog.Builder(this).setMessage("Mevcut imza anahtarı değişecek. Önce mevcut anahtarı yedekleyin; farklı anahtarlı APK mevcut kurulumun üzerine yüklenemez.").setPositiveButton("Devam",(a,b)->picker(IMPORT_KEY,"*/*",null)).setNegativeButton("Vazgeç",null).show()).setNegativeButton("Kapat",null).show());
  button(root,"İptal",()->{if(session.busy&&session.task!=null){session.cancelRequested=true;if(session.running!=null)session.running.interrupt();session.update("İptal isteniyor…");}});
  status=new TextView(this);status.setTextColor(0xffcccccc);status.setTextSize(14);root.addView(status);ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
  root.setOnApplyWindowInsetsListener((v,in)->{root.setPadding(dp(22)+in.getSystemWindowInsetLeft(),dp(22)+in.getSystemWindowInsetTop(),dp(22)+in.getSystemWindowInsetRight(),dp(22)+in.getSystemWindowInsetBottom());return in;});root.requestApplyInsets();savePicker=state!=null&&state.getBoolean("savePicker");render();
 }
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 EditText field(LinearLayout root,String name,String value){EditText e=new EditText(this);e.setHint(name);e.setText(value);e.setTextColor(Color.WHITE);e.setHintTextColor(0xff888888);e.setSingleLine();root.addView(e,new LinearLayout.LayoutParams(-1,dp(60)));return e;}
 void button(LinearLayout root,String name,Runnable action){Button b=new Button(this);b.setText(name);b.setAllCaps(false);b.setTextColor(Color.WHITE);GradientDrawable bg=new GradientDrawable();bg.setColor(Color.BLACK);bg.setStroke(dp(1),0xff555555);bg.setCornerRadius(dp(8));b.setBackground(bg);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.topMargin=dp(12);lp.bottomMargin=dp(8);root.addView(b,lp);b.setOnClickListener(v->{if(!session.busy||name.equals("İptal"))action.run();});}
 void render(){status.setText(session.message);if(session.ready&&!session.busy&&!savePicker&&resumed&&!isFinishing()){savePicker=true;picker(SAVE,"application/vnd.android.package-archive","InstaPro-release.apk");}}
 void picker(int request,String mime,String name){Intent i=new Intent(name==null?Intent.ACTION_OPEN_DOCUMENT:Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(mime);if(name!=null)i.putExtra(Intent.EXTRA_TITLE,name);try{startActivityForResult(i,request);}catch(Exception e){savePicker=false;session.update("Dosya seçici açılamadı: "+e.getMessage());}}
 void build(){String name=label.getText().toString().trim(),tail=suffix.getText().toString().trim();try{PatchEngine.checkSettings(name,tail);if(!session.source.isFile())throw new IOException("Önce kaynak APK seçin");if(session.dir.getUsableSpace()<650_000_000L)throw new IOException("En az 650 MB boş alan gerekli");}catch(Exception e){session.update(e.getMessage());return;}
  session.run(()->{byte[] payload;try(InputStream in=getApplicationContext().getAssets().open("favorites.dex");ByteArrayOutputStream out=new ByteArrayOutputStream()){PatchEngine.copy(in,out,2_000_000);payload=out.toByteArray();}SigningKeys keys=new SigningKeys(getApplicationContext().getFilesDir());char[] keyPass=keys.password();try{PatchEngine.build(session.source,session.output,payload,name,tail,keys.load(),keyPass,session::update,3);}finally{Arrays.fill(keyPass,'\0');}session.ready=true;session.update("APK imzası ve release manifesti doğrulandı. Android açılışı bu kontrolde test edilmez.");});
 }
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==SAVE){savePicker=false;session.ready=false;}if(result!=RESULT_OK||data==null||data.getData()==null){session.update("Dosya seçimi iptal edildi.");if(req==SAVE)session.output.delete();return;}Uri uri=data.getData();Context app=getApplicationContext();
  if(req==SELECT)session.run(()->{session.source.delete();if(session.dir.getUsableSpace()<850_000_000L)throw new IOException("En az 850 MB boş alan gerekli");session.update("Kaynak APK kopyalanıyor ve doğrulanıyor…");try{try(InputStream in=app.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(session.source)){if(in==null)throw new IOException("Kaynak açılamadı");PatchEngine.copy(in,out,200_000_000L);}if(session.source.length()!=180564084L||!PatchEngine.sha(session.source).equals(PatchEngine.SOURCE_SHA))throw new IOException("APK desteklenen orijinal 15.85 dosyası değil");}catch(Exception e){session.source.delete();throw e;}session.update("Orijinal InstaPro 15.85 doğrulandı. Adı ve paket ekini seçip modlayın.");});
  if(req==SAVE)session.run(()->{boolean ok=false;try{try(InputStream in=new FileInputStream(session.output);OutputStream out=app.getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Hedef açılamadı");PatchEngine.copy(in,out,250_000_000L);out.flush();}ok=true;session.update("İmzalı release APK kaydedildi. debug=false. Cihazda açılış testi gereklidir.");}finally{session.output.delete();if(!ok)try{DocumentsContract.deleteDocument(app.getContentResolver(),uri);}catch(Exception ignored){}}});
  if(req==EXPORT_KEY||req==IMPORT_KEY){EditText pass=new EditText(this);pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);pass.setHint("En az 12 karakter");new AlertDialog.Builder(this).setTitle("Anahtar yedeği parolası").setView(pass).setPositiveButton("Devam",(d,w)->{char[] chars=pass.getText().toString().toCharArray();pass.setText("");if(chars.length<12){Arrays.fill(chars,'\0');session.update("Parola en az 12 karakter olmalı.");return;}session.run(()->{try{SigningKeys keys=new SigningKeys(app.getFilesDir());if(req==EXPORT_KEY){boolean ok=false;try{try(OutputStream out=app.getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Hedef açılamadı");keys.exportTo(out,chars);}ok=true;}finally{if(!ok)try{DocumentsContract.deleteDocument(app.getContentResolver(),uri);}catch(Exception ignored){}}}else{InputStream in=app.getContentResolver().openInputStream(uri);if(in==null)throw new IOException("Yedek açılamadı");keys.importFrom(in,chars);}session.update("İmza anahtarı yedeği "+(req==EXPORT_KEY?"kaydedildi.":"yüklendi."));}finally{Arrays.fill(chars,'\0');}});}).setNegativeButton("Vazgeç",null).show();}
 }
 @Override protected void onResume(){super.onResume();resumed=true;render();}
 @Override protected void onPause(){resumed=false;super.onPause();}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("label",label.getText().toString());out.putString("suffix",suffix.getText().toString());out.putBoolean("savePicker",savePicker);}
 @Override protected void onDestroy(){if(session.view.get()==this)session.view.clear();super.onDestroy();}
}
