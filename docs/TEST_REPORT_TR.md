# Test durumu — 0.1.2 teşhis adayı, 25 Ekim 2026

Kullanıcı 0.1.1 patcher’ın açılır açılmaz kapandığını bildirdi. **Telefonundaki çökme nedeni hâlâ bilinmiyor.** Aynı yayımlanmış 0.1.1 APK’si Android 11 emülatöründe kuruldu ve açıldı; süreç hayattaydı, MainActivity ekrandaydı, crash buffer boştu. Bu, kullanıcıdaki arızayı geçersiz kılmaz. Telefon modeli/Android sürümü ve cihazdan alınmış hata kaydı bu rapor hazırlanırken mevcut değil.

0.1.2 bir teşhis adaydır: Java arayüz başlatma hatasını sade ekranla gösterebilir; diğer yakalanmamış Java hatalarını yerelde kaydedip Android’in normal hata handler’ına devreder. Sonraki açılışta hata ekranından kayıt kopyalanabilir. Native sinyal veya Application öncesindeki hata kaydı garanti edilmez. **Telefon arızası çözüldü iddiası yok.**

## Android üzerinde gerçekten yapılanlar

Ortam: Android SDK built for x86_64, Android 11 / API 30, x86_64,x86. Google AOSP default system image revision 11, emulator 37.3.5.0, Linux üzerinde TCG yazılım CPU ve SwiftShader. Fiziksel ARM64 telefon testi değildir.

- Yayımlanmış 0.1.1 (SHA-256 `7704163273d62031d76babae2fd7c262e9ea1a1849385c0a04753884c77cf96e`) kuruldu; soğuk açılış, PID, resumed Activity ve crash buffer kaydı alındı. Çökme yeniden üretilemedi.
- 0.1.1 formunda Android instrumentation ile `gpt`, `.gpt`, boş/boşluk eki önizlemeleri, düğme durumu ve ScrollView fillViewport geçti.
- İmzalı 0.1.2 release adayı üç kez soğuk açıldı. Her açılışta `am start` sonucuna ek olarak gecikmeli PID ve resumed MainActivity kontrolleri geçti. Son varsayılan form/önizleme UI XML’i ve ekran görüntüsü alındı. Kendi crash buffer kaydı boştu; kurulu manifestte DEBUGGABLE yoktu.
- 0.1.2 Android instrumentation testi; form/ek önizlemeleri, viewport ve `debuggable=false` kontrollerini geçti.
- **Açıkça sentetik bir test hatası** kaydedildi; recovery ekranı ve panoya stack trace kopyalama kontrolü geçti. Bu test hatası, kullanıcının gerçek çökme kaydı veya nedeni değildir. Test raporu dosyası test bitiminde temizlendi.

Son imzalı APK hash’i `RELEASE_ARTIFACT.json`, Android test çıktıları ve ekran görüntüsü `docs/validation/` altında. GitHub CI ayrıca aynı imzalı artifact’i Android 11 emülatöründe açar. Yayımlama host/build ve Android başlangıç işlerine bağlıdır.

## Masaüstünde yapılanlar (Android testlerinden ayrı)

Linux x86_64 / OpenJDK 21.0.12.1:

| Kontrol | Sonuç |
|---|---|
| Kanıt kimlikleri | FILES.json içindeki 14 dosyanın boyut/SHA-256 kontrolü geçti |
| Native şema | ELF PT_LOAD üzerinden VA→offset: favorites.db, üç CREATE TABLE ve Favorites seed tekrar çözüldü |
| Envanter | 43.772 ZIP girdisi, 297.878.696 açılmış bayt; tam APK mevcut değil |
| Ek regresyonu | `gpt`/`.gpt` eşdeğer; boş ek orijinal paket; bozuk girişler reddedildi |
| Orijinal paket | Manifestte authority/izin/process/taskAffinity kimlikleri korundu; bu yolda resources.arsc değiştirilmeden kopyalanır |
| AXML | Bağımsız apksig parser ile 734 component android:name resource-ID, yeni hedef Activity, Unicode label ve debug=false kontrolleri geçti |
| ARSC/DEX | ARSC’de clone için yalnız paket alanı; DEX’te yalnız iki initPreference gövdesi. Native onPreferenceClick/lifecycle ve GOLD sınıfı korundu; stub dışarıda |
| Anahtar | PKCS12 kalıcılık, parolalı export/import ve yanlış parolada eski anahtarı koruma geçti |
| SQLite | **Sentetik** 16.932 hesap / 8 klasör / 17.615 üyelik; roundtrip, tekrar merge, replace, yedek/yazma hatasında rollback, Unicode ve bozuk JSON kontrolleri geçti |
| Release | D8 --release; açık debuggable=false; bilinçli olarak minify/shrink kapalı; imza ve ZIP hizalama/bütünlük kontrolü geçti. 0.1.2 versionCode 3, önceki patcher ile aynı sertifika |

Host testleri Android runtime yerine sayılmadı. Sentetik veri kullanıcının bulunmayan gerçek yedeği değildir.

## Hâlâ yapılmayanlar

- Kullanıcının telefonundaki patcher arızasının gerçek stack trace/logcat ile teşhisi ve o cihazda doğrulaması.
- Tam orijinal InstaPro APK’siyle uçtan uca telefon patch üretimi; hedef APK imzası/kurulumu/orijinalle yan yana çalışma ve açılışı.
- Hedefte baseline → Activity → kanca aşamalarının Android testleri ve önceki hedef açılış çökmesinin teşhisi.
- Native mod ayarları, login/session, Android SQLite backend ile native helper eşzamanlılığı; favori ekranının import sonrası yenilenmesi.
- Gerçek kullanıcı JSON’uyla 16.932/8/17.615 doğrulaması; SAF import/export, kalıcılık ve hedef update testi; INSFAV/Plus parser incelemesi.
- Fiziksel cihazda rotasyon/process death/düşük disk/180 MB I/O ve anahtar yedeği testleri.

Tam APK arşivde bulunmadığı için bu oturumda hedef InstaPro APK’si üretilmedi. Önceki hedef çökmesiyle şu an bildirilen patcher çökmesi ayrı olaylardır. Kabul kriterleri 2–8 tamamlandı denilemez.
