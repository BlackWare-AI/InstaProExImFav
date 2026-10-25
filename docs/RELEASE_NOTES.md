## Açılış kaydı için teşhis sürümü

**Telefonunuzdaki çökme nedeni henüz bulunmadı; çözüldüğü iddia edilmiyor.** Yayımlanmış 0.1.1 APK’si burada Android 11/API30/x86_64 emülatöründe açıldı; kullanıcıdaki kapanma yeniden üretilemedi.

Bu sürümü önceki patcher’ı **kaldırmadan üzerine güncelleyin**. Aynı sertifika, versionCode 3; release ve debuggable=false. Java arayüz başlatma hatası oluşursa sade hata ekranı gösterilir. Başka bir yakalanmamış Java hatasında kayıt yerelde saklanır ve Android’in normal crash handler’ı korunur; tekrar açıldığında hata ekranı gösterilir. **Hata kaydını kopyala** ile sürüm, cihaz/Android/ABI ve stack trace alınabilir. Hiçbir kayıt otomatik gönderilmez, kaynak APK/JSON/anahtar içerikleri toplanmaz. Native veya Application başlamadan oluşan hataların kaydı garanti edilmez.

Son imzalı dosya Android 11 yazılım emülatöründe üç soğuk açılışı, gecikmeli PID/resumed MainActivity ve kurulu debug=false kontrollerini geçti. Formda gpt/.gpt/boş ek önizlemesi sınandı. Açıkça sentetik hata ile kayıt/recovery ekranı/panoya kopyalama testi geçti; bu sentetik kayıt kullanıcının gerçek çökmesi değildir. Ekran görüntüsü ve test kayıtları kaynaklardaki docs/validation içinde.

Host/build ve Android başlangıç işleri geçmeden CI yayımlama yapmaz. Telefonda uçtan uca APK patch üretimi, gerçek kullanıcı JSON’u ve hedef InstaPro açılışı test edilmedi; tam kaynak APK hâlâ mevcut değil. Önceki hedef InstaPro açılış çökmesi bu patcher sorunundan ayrı ve hâlâ teşhis edilmemiştir.

APK SHA-256: `411688df3bce2620fef0ee264e1ba17aa8526e5b16246fb6d2f61fab736a2add`
