## 0.1.1 düzeltmesi

- `gpt` ve `.gpt` kabul edilir; baştaki nokta zorunlu değil.
- Paket eki isteğe bağlı. Boş bırakıldığında `com.instagram.android`, authority/izin/process kimlikleri ve kaynak tablosu korunur.
- Aynı paket seçildiğinde farklı imzalı mevcut kurulumun üzerine güncelleme yapılamayacağı formda belirtilir. Patcher hiçbir uygulamayı kaldırmaz.
- Kalıcı alan etiketleri ve canlı paket önizlemesi; APK doğrulama/işlem durumu; tam siyah pencere/kaydırma zemini. Modla ve kaydet, kaynak APK doğrulanınca etkinleşir; İptal yalnız işlem sırasında görünür.

İmzalı **release** patcher, versionCode 2; önceki patcher ile aynı sertifika. Önceki patcher kaldırılmadan güncelleme için hazırlanmıştır. Patcher ve hedef çıktı için `android:debuggable=false` kuralı korunur.

Ek normalizasyonu, orijinal manifest kimliklerini koruma, AXML/DEX/ARSC, anahtar roundtrip ve sentetik favori SQLite testleri geçti. Bu kontroller Android testi değildir. Bu yeni adayın UI/güncelleme kurulumu/telefonda uçtan uca modlama ve hedef açılışı geliştirme ortamında test edilemedi. Kullanıcının önceki sürüm ekran görüntüsü `gpt` doğrulama engelini gösteriyor; bu hata düzeltildi, başka bir runtime arızasının çözüldüğü ileri sürülmüyor.

Tam orijinal APK ve uygun Android cihazı hâlâ bu ortama sağlanmadı; hedef InstaPro APK’si bu oturumda üretilmedi. Önceki hedef açılış çökmesinin nedeni bilinmiyor. Patcher yalnız sabitlenmiş orijinal 15.85 profilini kabul eder. Kaynaklar ve tüm test sınırları PR #1 / docs/TEST_REPORT_TR.md içinde.

APK SHA-256: `7704163273d62031d76babae2fd7c262e9ea1a1849385c0a04753884c77cf96e`
