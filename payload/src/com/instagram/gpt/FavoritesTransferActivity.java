package com.instagram.gpt;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public final class FavoritesTransferActivity extends Activity {
    private static final int IMPORT=7141, EXPORT=7142;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private TextView status;
    private boolean busy;
    private ProgressDialog progress;
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private boolean alive() { return !isFinishing()&&!isDestroyed(); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(state!=null) { automaticExport=state.getBoolean("automaticExport",false); String path=state.getString("selectedBackup"); if(path!=null) selectedBackup=new File(path); }
        getWindow().setStatusBarColor(Color.BLACK); getWindow().setNavigationBarColor(Color.BLACK);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.BLACK); root.setPadding(dp(24),dp(24),dp(24),dp(24));
        TextView title=new TextView(this); title.setText("Favorites"); title.setTextColor(Color.WHITE); title.setTextSize(22); title.setTypeface(null,Typeface.BOLD); root.addView(title);
        TextView info=new TextView(this); info.setText("Hesapları ve klasörleri içe / dışa aktar"); info.setTextColor(0xffaaaaaa); info.setTextSize(14); info.setPadding(0,dp(12),0,dp(24)); root.addView(info);
        button(root,"Import favorites",()->pickImport()); button(root,"Export favorites",()->pickExport());
        button(root,"Otomatik yedeği dışa aktar",()->exportAutomaticBackup());
        status=new TextView(this); status.setTextColor(0xffcccccc); status.setTextSize(14); status.setPadding(0,dp(24),0,dp(20)); root.addView(status);
        button(root,"Geri",()->finish());
        root.setOnApplyWindowInsetsListener((v,insets)->{ root.setPadding(dp(24)+insets.getSystemWindowInsetLeft(),dp(24)+insets.getSystemWindowInsetTop(),dp(24)+insets.getSystemWindowInsetRight(),dp(24)+insets.getSystemWindowInsetBottom()); return insets; });
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.addView(root); setContentView(scroll); root.requestApplyInsets();
        if(state==null) {
            String mode=getIntent().getStringExtra("mode");
            if("import".equals(mode)) pickImport(); else if("export".equals(mode)) pickExport();
            else status.setText("Bir işlem seçin.");
        } else status.setText("Dosya seçimini tamamlayın veya işlemi yeniden başlatın.");
    }
    private void button(LinearLayout parent,String label,Runnable action) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTextSize(16);
        GradientDrawable bg=new GradientDrawable(); bg.setColor(Color.BLACK); bg.setStroke(dp(1),0xff444444); bg.setCornerRadius(dp(12)); b.setBackground(bg);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54)); lp.bottomMargin=dp(12); parent.addView(b,lp);
        b.setOnClickListener(v->{ if(!busy) action.run(); });
    }
    private void pickImport() {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*");
        try { startActivityForResult(i,IMPORT); } catch(Exception e) { error(e); }
    }
    private boolean automaticExport;
    private void pickExport() { automaticExport=false; createExport("InstaPro_Favorites_"); }
    private void exportAutomaticBackup() {
        File dir=new File(getFilesDir(),"gpt-favorites-backups");
        File[] files=dir.listFiles((d,name)->name.endsWith(".json"));
        if(files==null||files.length==0) { status.setText("Henüz otomatik yedek yok. İlk içe aktarma öncesinde oluşturulur."); return; }
        Arrays.sort(files,(a,b)->Long.compare(b.lastModified(),a.lastModified()));
        String[] names=new String[files.length]; for(int i=0;i<files.length;i++) names[i]=files[i].getName();
        new AlertDialog.Builder(this).setTitle("Otomatik yedek seç").setItems(names,(d,index)->{ selectedBackup=files[index]; automaticExport=true; createExport("InstaPro_BeforeImport_"); }).setNegativeButton("Vazgeç",null).show();
    }
    private File selectedBackup;
    private void createExport(String prefix) {
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE,prefix+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.ROOT).format(new Date())+".json");
        try { startActivityForResult(i,EXPORT); } catch(Exception e) { error(e); }
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null) { status.setText("İşlem iptal edildi; kayıtlar değişmedi."); return; }
        Uri uri=data.getData();
        if(request==IMPORT) run("Favoriler okunuyor…",()->{
            String text;
            try(InputStream stream=getContentResolver().openInputStream(uri)) { text=read(stream); }
            final FavoritesDocument doc=FavoritesDocument.parse(text);
            runOnUiThread(()->{
                if(!alive()) return;
                done();
                if(doc.pages.isEmpty()) { status.setText("Dosyada hesap bulunamadı; kayıtlar değişmedi."); return; }
                new AlertDialog.Builder(this).setTitle("Import favorites")
                    .setMessage(doc.pages.size()+" farklı hesap\n"+doc.categories.size()+" klasör\n"+doc.memberships()+" klasör üyeliği\n\nBirleştir: mevcut kayıtları korur.\nDeğiştir: yalnız bu bu uygulamandaki favori listesini değiştirir.\nHer işlemden önce otomatik yedek alınır.")
                    .setPositiveButton("Birleştir",(d,w)->apply(doc,false))
                    .setNeutralButton("Değiştir",(d,w)->new AlertDialog.Builder(this).setTitle("Liste değiştirilsin mi?")
                        .setMessage("bu uygulamanın mevcut favorileri, bu dosyadaki listeyle değiştirilecek.")
                        .setPositiveButton("Değiştir",(a,b)->apply(doc,true)).setNegativeButton("Vazgeç",null).show())
                    .setNegativeButton("Vazgeç",null).show();
            });
        });
        if(request==EXPORT) {
            final boolean backup=automaticExport; final File source=selectedBackup;
            run("Favoriler dışa aktarılıyor…",()->{
                String text;
                if(backup) { try(InputStream in=new FileInputStream(source)) { text=read(in); } }
                else try(AndroidFavoritesBackend backend=new AndroidFavoritesBackend(this)) { text=new FavoritesStore(backend).snapshot(); }
                // Validate the exact serialized content before overwriting the chosen destination.
                FavoritesDocument doc=FavoritesDocument.parse(text);
                try(OutputStream stream=getContentResolver().openOutputStream(uri,"wt")) {
                    if(stream==null) throw new IOException("Çıktı dosyası açılamadı.");
                    stream.write(text.getBytes(StandardCharsets.UTF_8)); stream.flush();
                }
                final String message=doc.pages.size()+" hesap, "+doc.categories.size()+" klasör dışa aktarıldı.";
                success(message);
            });
        }
    }
    private void apply(FavoritesDocument doc,boolean replace) {
        run("Favoriler kaydediliyor…",()->{
            String result;
            try(AndroidFavoritesBackend backend=new AndroidFavoritesBackend(this)) {
                result=new FavoritesStore(backend).apply(doc,replace,json->{
                    File dir=new File(getFilesDir(),"gpt-favorites-backups");
                    if(!dir.isDirectory()&&!dir.mkdirs()) throw new IOException("Otomatik yedek klasörü oluşturulamadı.");
                    File target=new File(dir,"before-import-"+System.currentTimeMillis()+".json");
                    File temp=new File(target.getPath()+".tmp");
                    try {
                        try(FileOutputStream stream=new FileOutputStream(temp)) { stream.write(json.getBytes(StandardCharsets.UTF_8)); stream.flush(); stream.getFD().sync(); }
                        if(!temp.renameTo(target)) throw new IOException("Otomatik yedek kaydedilemedi.");
                    } finally { temp.delete(); }
                });
            }
            success(result);
        });
    }
    interface Work { void execute() throws Exception; }
    private void run(String title,Work work) {
        if(busy) return; busy=true;
        progress=ProgressDialog.show(this,"Favorites",title,true,false);
        worker.execute(()->{ try { work.execute(); } catch(Exception e) { runOnUiThread(()->error(e)); } });
    }
    private void success(String message) { runOnUiThread(()->{ if(!alive()) return; done(); status.setText(message); new AlertDialog.Builder(this).setTitle("Tamamlandı").setMessage(message).setPositiveButton("Tamam",null).show(); }); }
    private void done() { busy=false; if(progress!=null) { progress.dismiss(); progress=null; } }
    private void error(Exception e) { if(!alive()) return; done(); status.setText("İşlem tamamlanamadı.\n"+e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage())); }
    private static String read(InputStream stream) throws IOException {
        if(stream==null) throw new IOException("Dosya açılamadı.");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); byte[] buffer=new byte[8192]; int n;
        while((n=stream.read(buffer))!=-1) { if(bytes.size()+n>32*1024*1024) throw new IOException("Dosya 32 MB sınırını aşıyor."); bytes.write(buffer,0,n); }
        return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
    }
    @Override public void onBackPressed() { if(!busy) super.onBackPressed(); else Toast.makeText(this,"İşlemin bitmesini bekleyin.",Toast.LENGTH_SHORT).show(); }
    @Override protected void onSaveInstanceState(Bundle state) { super.onSaveInstanceState(state); state.putBoolean("automaticExport",automaticExport); if(selectedBackup!=null) state.putString("selectedBackup",selectedBackup.getPath()); }
    @Override protected void onDestroy() { if(progress!=null) { progress.dismiss(); progress=null; } worker.shutdown(); super.onDestroy(); }
}
