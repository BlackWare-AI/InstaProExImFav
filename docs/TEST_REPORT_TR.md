# Test ve teslim durumu — 25 Ekim 2026

Bu teslim **deneysel release adayıdır**. Patcher uygulaması geliştirildi ve imzalı release APK üretildi. Tam hedef APK henüz sağlanmadığından bu oturumda imzalı InstaPro çıktı APK’si üretilemedi. Önceki açılış çökmesinin nedeni **bilinmiyor**; logcat/native crash kaydı bulunmuyor.

## Yapılan kontroller

Ortam: Linux x86_64, OpenJDK 21.0.12.1. Android cihaz modeli/Android sürümü/ABI: **test edilmedi**. Bağlı Android cihazı veya uygun ARM64 Android runtime’ı kullanılmadı; ADB bu ortamda kurulu değil. Bu Linux ortamı bir Android açılış testi değildir.

| Kontrol | Sonuç ve sınırı |
|---|---|
| START_HERE ve teknik devir | Tamamı okundu; önceki raporlardaki debugtrue/çalışıyor iddiaları temel alınmadı |
| Orijinal kanıt kimlikleri | FILES.json içindeki tüm 14 kanıtın boyut/SHA-256 kontrolü geçti |
| Native şema çözümü | ELF PT_LOAD ile VA→dosya offseti çevrildi; favorites.db, üç CREATE TABLE ve Favorites seed yeniden çözüldü |
| Envanter | 43.772 ZIP girdisi; 297.878.696 bayt açılmış içerik. Tam APK arşivde yok |
| AXML bağımsız parser | apksig parser ile 734 component android:name resource-ID kontrolü, yeni Activity ve debug=false geçti |
| AXML roundtrip/baseline | Unicode/emoji label ve `.gpt`; yeni Activity olmadan baseline manifest kontrolü geçti |
| resources.arsc | Yalnız sabit boyutlu paket adı alanı değişti; diğer paketlenmiş kaynak baytları korundu |
| DEX karşılaştırması | Özgün classes20 üzerinde yalnız iki native initPreference gövdesi değişti; diğer metot gövdeleri/flags korundu; GOLD stub dışarıda |
| Teşhis aşamaları | Payload/Activity için sıfır kanca ve ardından tek kanca DEX kontrolleri geçti |
| Tekrar patch/bozuk ek | Zaten patch uygulanmış DEX reddedildi; geçersiz paket ekleri reddedildi. Telefon girişinde tam APK hash’i de sabit profile bağlı |
| Anahtar | Host’ta kalıcı kimlik, parolalı PKCS12 export/import ve yanlış parolada eski anahtarı koruma geçti. Android provider davranışı test edilmedi |
| Favori SQLite | **Sentetik** 16.932 kullanıcı / 8 klasör / 17.615 üyelik: replace, tekrar merge ve JSON→SQLite→JSON→yeni SQLite alan/ilişki karşılaştırması geçti |
| Veri koruma | Sentetik testte yedek callback hatası ve ortada yazma hatasında rollback; Unicode, boş klasör, case dedup; bozuk/ilgisiz JSON ve iç string’de trailing garbage reddi geçti |
| Release derleme | Patcher D8 `--release`, min API 28; obfuscation/shrinking bilinçli kapalı; açık debuggable=false |
| Release imza | apksig API 28–36 aralığı doğrulaması geçti; seçilen v3 şeması doğrulandı. v2/v3 imzalama etkin; verifier’ın v2=false raporu tek başına geçersizlik değildir |

Sentetik workload, kullanıcının bulunmayan gerçek JSON yedeği değildir. Önceki oturumun gerçek yedek testleri burada tekrar yapılmış sayılmadı.

Yereldeki komut çıktıları `docs/validation/` altında. APK hash/boyut ve sertifika kimliği `docs/RELEASE_ARTIFACT.json` içinde kaydedilir. İmzadan sonra APK baytları değiştirilmez.

## Henüz yapılmayanlar

- Tam orijinal APK’nin doğrulanması, orijinalin Android’de kurulum/soğuk açılışı.
- Telefonda patcher açılışı, gerçek APK seçimi, 180 MB I/O ve tüm hattın çalışması.
- Üretilmiş InstaPro APK’sinin imzası/kurulumu, orijinalle yan yana çalışma ve açılış.
- Paket/ad baseline → Activity → ilk kanca → iki kanca aşamalarının gerçek cihaz testleri.
- Önceki çökmenin logcat/Java stack/native sinyal/tombstone ile teşhisi veya giderildiğinin doğrulanması.
- Native mod ayarları, login/session, favorite Activity yenilenmesi.
- Android SAF seçici, gerçek SQLite backend ile native helper eşzamanlılığı.
- Gerçek kullanıcı yedeğiyle 16.932 / 8 / 17.615 doğrulaması; INSFAV/Plus parser incelemesi.
- Telefon yeniden başlatması, aynı paket+anahtarla update sonrası veri koruma.
- Android’de rotasyon/process death/iptal/düşük disk ve yarım hedef temizleme testleri.
- Android’de anahtar export/import ve uygulama kaldırma/yeniden kurma sonrası aynı imzayla güncelleme.

Bu eksikler nedeniyle kabul kriterleri 2–8 tamamlandı veya açılış çökmesi çözüldü denilemez. Tam APK ve uygun cihaz/crash kayıtları geldiğinde `ANDROID_TEST_TR.md` sırası izlenmelidir. Patcher dağıtım imzasının kalıcı özel anahtarı GitHub’da bulunmaz; CI’de kalıcı imza secrets henüz kurulmadı.
