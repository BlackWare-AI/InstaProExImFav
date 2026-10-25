package dev.instapro.patcher;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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
 static Session session;
 TextView status,sourceStatus,packagePreview;
 EditText label,suffix;
 Button chooseButton,buildButton,keyButton,cancelButton;
 ProgressBar progress;
 boolean savePicker,resumed,startupFailed;
 static final class Session {
  final Context app;final File dir,source,output;final ExecutorService worker=Executors.newSingleThreadExecutor();
  volatile boolean busy,ready,sourceVerified;volatile String message="Orijinal InstaPro 15.85 APK dosyasını seçin.";volatile Future<?> task;volatile Thread running;volatile boolean cancelRequested;WeakReference<MainActivity> view=new WeakReference<>(null);
  Session(Context c){app=c.getApplicationContext();dir=new File(app.getCacheDir(),"patch-work");dir.mkdirs();source=new File(dir,"source.apk");output=new File(dir,"release.apk");source.delete();output.delete();new File(dir,"unsigned.tmp.apk").delete();}
  void update(String text){message=text;new Handler(Looper.getMainLooper()).post(()->{MainActivity a=view.get();if(a!=null&&!a.isDestroyed())a.render();});}
  void run(Work work){if(busy)return;busy=true;ready=false;cancelRequested=false;update(message);task=worker.submit(()->{running=Thread.currentThread();if(cancelRequested)running.interrupt();try{work.run();}catch(Exception e){output.delete();update("İşlem tamamlanamadı: "+e.getClass().getSimpleName()+"\n"+String.valueOf(e.getMessage()));}finally{running=null;Thread.interrupted();busy=false;update(message);}});}
 }
 interface Work {void run()throws Exception;}
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  try{initialize(state);}
  catch(RuntimeException|LinkageError error){
   startupFailed=true;
   StartupActivity.showFailure(this,CrashReporter.record(this,error));
  }
 }
 void initialize(Bundle state){
  if(session==null)session=new Session(this);
  session.view=new WeakReference<>(this);
  getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
  getWindow().setStatusBarColor(Color.BLACK);
  getWindow().setNavigationBarColor(Color.BLACK);

  LinearLayout root=new LinearLayout(this);
  root.setOrientation(LinearLayout.VERTICAL);
  root.setBackgroundColor(Color.BLACK);
  root.setPadding(dp(22),dp(22),dp(22),dp(22));
  TextView title=text(root,"InstaPro Patcher",25,Color.WHITE);
  title.setTypeface(null,Typeface.BOLD);
  TextView hint=text(root,"Favori içe/dışa aktarımını APK’ye ekleyin.",14,0xffaaaaaa);
  hint.setPadding(0,dp(10),0,dp(18));

  chooseButton=button(root,"Kaynak APK seç",()->picker(SELECT,"*/*",null));
  sourceStatus=text(root,"",13,0xffaaaaaa);
  sourceStatus.setPadding(0,0,0,dp(10));
  label=field(root,"Görünen uygulama adı",state==null?"InstaPro GPT":state.getString("label","InstaPro GPT"),"InstaPro");
  suffix=field(root,"Paket eki · isteğe bağlı",state==null?".gpt":state.getString("suffix",".gpt"),"Boş bırak: orijinal paket");
  suffix.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
  packagePreview=text(root,"",13,0xffaaaaaa);
  packagePreview.setPadding(0,dp(8),0,dp(12));
  suffix.addTextChangedListener(new TextWatcher(){
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence s,int start,int before,int count){suffix.setError(null);updatePackagePreview();}
   public void afterTextChanged(Editable value){}
  });
  buildButton=button(root,"Modla ve kaydet",this::build);
  progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
  progress.setIndeterminate(true);
  progress.setIndeterminateTintList(ColorStateList.valueOf(0xffbbbbbb));
  root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));
  status=text(root,"",14,0xffcccccc);
  status.setPadding(0,dp(12),0,dp(12));
  status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
  cancelButton=button(root,"İptal",()->{
   if(session.busy){session.cancelRequested=true;if(session.running!=null)session.running.interrupt();session.update("İptal isteniyor…");}
  });
  keyButton=button(root,"İmza anahtarı",()->new AlertDialog.Builder(this)
   .setTitle("İmza anahtarı")
   .setMessage("Güncellemeler aynı anahtarı gerektirir. Patcher kaldırılmadan önce parolalı yedek alın.")
   .setPositiveButton("Yedeği kaydet",(d,w)->picker(EXPORT_KEY,"application/octet-stream","InstaPro-signing.p12"))
   .setNeutralButton("Yedeği yükle",(d,w)->new AlertDialog.Builder(this)
    .setMessage("Mevcut imza anahtarı değişecek. Önce mevcut anahtarı yedekleyin; farklı anahtarlı APK mevcut kurulumun üzerine yüklenemez.")
    .setPositiveButton("Devam",(a,c)->picker(IMPORT_KEY,"*/*",null))
    .setNegativeButton("Vazgeç",null).show())
   .setNegativeButton("Kapat",null).show());

  ScrollView scroll=new ScrollView(this);
  scroll.setBackgroundColor(Color.BLACK);
  scroll.setFillViewport(true);
  scroll.addView(root,new ScrollView.LayoutParams(-1,-2));
  setContentView(scroll);
  root.setOnApplyWindowInsetsListener((v,in)->{
   root.setPadding(dp(22)+in.getSystemWindowInsetLeft(),dp(22)+in.getSystemWindowInsetTop(),dp(22)+in.getSystemWindowInsetRight(),dp(22)+in.getSystemWindowInsetBottom());return in;
  });
  root.requestApplyInsets();
  savePicker=state!=null&&state.getBoolean("savePicker");
  updatePackagePreview();
  render();
 }
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 TextView text(LinearLayout root,String value,int size,int color){
  TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(color);root.addView(view);return view;
 }
 GradientDrawable border(int color){
  GradientDrawable bg=new GradientDrawable();bg.setColor(Color.BLACK);bg.setStroke(dp(1),color);bg.setCornerRadius(dp(8));return bg;
 }
 EditText field(LinearLayout root,String name,String value,String hint){
  TextView caption=text(root,name,13,0xffbbbbbb);
  caption.setPadding(0,dp(10),0,dp(7));
  EditText input=new EditText(this);
  input.setId(View.generateViewId());caption.setLabelFor(input.getId());
  input.setHint(hint);input.setText(value);input.setTextColor(Color.WHITE);input.setHintTextColor(0xff777777);
  input.setTextSize(16);input.setSingleLine();input.setImeOptions(EditorInfo.IME_ACTION_DONE);
  input.setBackground(border(0xff444444));input.setPadding(dp(14),0,dp(14),0);
  input.setHighlightColor(0xff555555);
  if(Build.VERSION.SDK_INT>=29){GradientDrawable cursor=new GradientDrawable();cursor.setColor(Color.WHITE);cursor.setSize(dp(2),dp(24));input.setTextCursorDrawable(cursor);}
  root.addView(input,new LinearLayout.LayoutParams(-1,dp(50)));
  return input;
 }
 Button button(LinearLayout root,String name,Runnable action){
  Button b=new Button(this);b.setText(name);b.setTextSize(15);b.setAllCaps(false);b.setTextColor(Color.WHITE);
  b.setBackground(border(0xff555555));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(50));lp.topMargin=dp(8);lp.bottomMargin=dp(8);root.addView(b,lp);
  b.setOnClickListener(v->action.run());return b;
 }
 void updatePackagePreview(){
  try{
   String pkg=PatchEngine.packageName(suffix.getText().toString());
   packagePreview.setText("Paket: "+pkg+"\n"+(pkg.equals(PatchEngine.ORIGINAL_PACKAGE)
    ?"Orijinal paket korunur. Farklı imzalı mevcut kurulumun üzerine yüklenemez."
    :"Ayrı uygulama olarak kurulur. Eki boş bırakırsanız orijinal paket korunur."));
  }catch(IOException e){packagePreview.setText("gpt veya .gpt yazın; orijinal paket için boş bırakın.");}
 }
 void enabled(View view,boolean enabled){view.setEnabled(enabled);view.setAlpha(enabled?1f:0.45f);}
 void render(){
  if(startupFailed||status==null)return;
  status.setText(session.message);
  sourceStatus.setText(session.sourceVerified?"✓ Orijinal InstaPro 15.85 doğrulandı":"Henüz doğrulanmış APK seçilmedi.");
  enabled(chooseButton,!session.busy);enabled(keyButton,!session.busy);
  enabled(buildButton,!session.busy&&session.sourceVerified);
  enabled(label,!session.busy);enabled(suffix,!session.busy);
  progress.setVisibility(session.busy?View.VISIBLE:View.GONE);
  cancelButton.setVisibility(session.busy?View.VISIBLE:View.GONE);
  if(session.ready&&!session.busy&&!savePicker&&resumed&&!isFinishing()){
   savePicker=true;picker(SAVE,"application/vnd.android.package-archive","InstaPro-release.apk");
  }
 }
 void picker(int request,String mime,String name){Intent i=new Intent(name==null?Intent.ACTION_OPEN_DOCUMENT:Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(mime);if(name!=null)i.putExtra(Intent.EXTRA_TITLE,name);try{startActivityForResult(i,request);}catch(Exception e){savePicker=false;session.update("Dosya seçici açılamadı: "+e.getMessage());}}
 void build(){
  String name=label.getText().toString().trim(),tail=suffix.getText().toString();
  label.setError(null);suffix.setError(null);
  try{PatchEngine.checkSettings(name,tail);}
  catch(IOException e){
   if(name.isEmpty()||name.length()>80||name.chars().anyMatch(c->Character.isISOControl(c)))label.setError(e.getMessage());
   else suffix.setError(e.getMessage());
   session.update(e.getMessage());return;
  }
  if(!session.sourceVerified||!session.source.isFile()){session.update("Önce kaynak APK seçin.");return;}
  if(session.dir.getUsableSpace()<650_000_000L){session.update("En az 650 MB boş alan gerekli.");return;}
  Context app=getApplicationContext();
  session.run(()->{
   byte[] payload;
   try(InputStream in=app.getAssets().open("favorites.dex");ByteArrayOutputStream out=new ByteArrayOutputStream()){
    PatchEngine.copy(in,out,2_000_000);payload=out.toByteArray();
   }
   SigningKeys keys=new SigningKeys(app.getFilesDir());char[] keyPass=keys.password();
   try{PatchEngine.build(session.source,session.output,payload,name,tail,keys.load(),keyPass,session::update,3);}
   finally{Arrays.fill(keyPass,'\0');}
   session.ready=true;session.update("İmzalı APK hazır. Kaydetme konumunu seçin.");
  });
 }
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==SAVE){savePicker=false;session.ready=false;}if(result!=RESULT_OK||data==null||data.getData()==null){session.update("Dosya seçimi iptal edildi.");if(req==SAVE)session.output.delete();return;}Uri uri=data.getData();Context app=getApplicationContext();
  if(req==SELECT)session.run(()->{session.sourceVerified=false;session.source.delete();if(session.dir.getUsableSpace()<850_000_000L)throw new IOException("En az 850 MB boş alan gerekli");session.update("Kaynak APK kopyalanıyor ve doğrulanıyor…");try{try(InputStream in=app.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(session.source)){if(in==null)throw new IOException("Kaynak açılamadı");PatchEngine.copy(in,out,200_000_000L);}if(session.source.length()!=180564084L||!PatchEngine.sha(session.source).equals(PatchEngine.SOURCE_SHA))throw new IOException("APK desteklenen orijinal 15.85 dosyası değil");}catch(Exception e){session.source.delete();throw e;}session.sourceVerified=true;session.update("APK hazır. Adı ve isteğe bağlı paket ekini seçip modlayın.");});
  if(req==SAVE)session.run(()->{boolean ok=false;try{try(InputStream in=new FileInputStream(session.output);OutputStream out=app.getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Hedef açılamadı");PatchEngine.copy(in,out,250_000_000L);out.flush();}ok=true;session.update("İmzalı APK kaydedildi.");}finally{session.output.delete();if(!ok)try{DocumentsContract.deleteDocument(app.getContentResolver(),uri);}catch(Exception ignored){}}});
  if(req==EXPORT_KEY||req==IMPORT_KEY){EditText pass=new EditText(this);pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);pass.setHint("En az 12 karakter");new AlertDialog.Builder(this).setTitle("Anahtar yedeği parolası").setView(pass).setPositiveButton("Devam",(d,w)->{char[] chars=pass.getText().toString().toCharArray();pass.setText("");if(chars.length<12){Arrays.fill(chars,'\0');session.update("Parola en az 12 karakter olmalı.");return;}session.run(()->{try{SigningKeys keys=new SigningKeys(app.getFilesDir());if(req==EXPORT_KEY){boolean ok=false;try{try(OutputStream out=app.getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Hedef açılamadı");keys.exportTo(out,chars);}ok=true;}finally{if(!ok)try{DocumentsContract.deleteDocument(app.getContentResolver(),uri);}catch(Exception ignored){}}}else{InputStream in=app.getContentResolver().openInputStream(uri);if(in==null)throw new IOException("Yedek açılamadı");keys.importFrom(in,chars);}session.update("İmza anahtarı yedeği "+(req==EXPORT_KEY?"kaydedildi.":"yüklendi."));}finally{Arrays.fill(chars,'\0');}});}).setNegativeButton("Vazgeç",null).show();}
 }
 @Override protected void onResume(){super.onResume();resumed=true;render();}
 @Override protected void onPause(){resumed=false;super.onPause();}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(startupFailed||label==null||suffix==null)return;out.putString("label",label.getText().toString());out.putString("suffix",suffix.getText().toString());out.putBoolean("savePicker",savePicker);}
 @Override protected void onDestroy(){if(session!=null&&session.view.get()==this)session.view.clear();super.onDestroy();}
}
