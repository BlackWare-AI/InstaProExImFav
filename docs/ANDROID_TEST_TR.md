# Android açılışı ve çökme teşhisi

Tam APK bu geliştirme oturumuna henüz ulaşmadı. ZIP’teki kısmi kanıtlar tam uygulama değildir. `file-limit-tools` parçaları `join_apk.py` ile birleştirilir; araç tam dosyanın SHA-256 değerini doğrular. Beklenen hash README’dedir.

Gerçek ARM64 Android cihazı ve yetkilendirilmiş ADB bağlantısı gerekir. Patcher ve hedef `debuggable=false` kalır. X86 ortamındaki ARM64 yükleme başarısızlığını ürün çökmesi diye raporlamayın.

1. Cihaz modeli, Android sürümü/API, ABI, APK hash’i ve kurulu imza kimliğini kaydedin. Orijinal APK’nin aynı cihazdaki soğuk açılışını doğrulayın. Login/session ve mod ayarlarını kontrol edin.
2. `tools/desktop_patch.py` ile aşağıdaki aşamaları sırayla üretin. Aynı paket ve anahtar kullanılır; uygulama verisini silmeyin. Araç masaüstünde telefonla aynı motoru çalıştırır, Android testi yapmaz. Anahtar `$HOME/.instapro-target-signing` altında kalır. Telefonun anahtarıyla eşleştiği varsayılmaz.

```bash
python tools/desktop_patch.py original.apk baseline.apk 'InstaPro GPT' .gpt baseline
python tools/desktop_patch.py original.apk activity.apk 'InstaPro GPT' .gpt activity
python tools/desktop_patch.py original.apk hook1.apk 'InstaPro GPT' .gpt hook1
python tools/desktop_patch.py original.apk favorites.apk 'InstaPro GPT' .gpt favorites
```

Baseline yalnız manifest/kaynak paketi değiştirir. Activity aşaması payload ve Activity ekler; hook eklemez. hook1 ilk `initPreference` metodunu, favorites her iki metodu değiştirir. Payload ayrı DEX olarak eklenmez; classes20 içine birleştirilir.

3. Her aşamada kurulum/soğuk açılış, bir süre sonra PID, resumed Activity ve yeni crash kaydını inceleyin. `am start`/monkey başarı kodu yeterli değildir:

```bash
python tools/capture_startup.py baseline.apk com.instagram.android.gpt \
  --serial CIHAZ_SERI_NO --out crash-logs/baseline --install
```

Araç logları temizlemez. Zaman pencereli logcat/crash-buffer, dumpsys, PID örnekleri ve cihaz/hash kaydı toplar. Bilinçli olarak otomatik "geçti" kararı vermez (exit 2). Ham loglar başka uygulama verisi içerebilir; yerelde inceleyip yalnız ilgili Java stack trace / native SIGABRT-SIGSEGV / linker / tombstone bölümünü paylaşın. Loglar Git’e alınmaz. Native tombstone erişimi cihaz politikasına bağlıdır; root gerektiren erişim varsayılmaz.

4. Önce başarısız aşamayı ve ilk fatal kaydı belirleyin. Debug flag, imza kontrolü veya JNI’yi kanıt olmadan neden ilan etmeyin. Mevcut bilinen manifest resource-map hatası kurulum reddini açıklıyordu; sonraki açılış çökmesini çözdüğü kanıtlanmadı.
5. Telefon patcher’ında orijinal APK seçerek aynı akışı gerçekten çalıştırın. Üretilen APK’nin orijinalle yan yana kurulmasını, görünen ad/paket/imzayı, `dumpsys package` DEBUGGABLE bayrağının bulunmamasını ve Backup ekranını kontrol edin.
6. Gerçek JSON ile 16.932 farklı kullanıcı / 8 klasör / 17.615 üyelik; import sonrası ekran yenilenmesi; export→import; tekrar merge; yanlış JSON; iptal ve yazma hatasında veri koruma; otomatik yedek export; yeniden başlatma ve aynı paket+anahtarla update sonrası kalıcılığı sınayın.
7. Patcher’da ekran döndürme, process death, düşük disk, dosya seçici iptali, 180.564.084 bayt kaynakla I/O, anahtar export/import ve iptal/yarım hedef temizliğini gerçek cihazda sınayın. Ani process death sonrasında otomatik yeniden patch yapılmaz; geçici dosyalar bir sonraki başlangıçta temizlenir. DB transaction çökmede rollback’e tabidir; bunun Android backend/native helper ile birlikte doğrulanması gerekir.

Sonuç tablosunda her satır için cihaz/Android/ABI, aşama, hash, uygulanan senaryo ve log kanıtını belirtin. Derleme, imza, AXML/DEX analizi ve masaüstü SQLite ayrı başlıklardır.
