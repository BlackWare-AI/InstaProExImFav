# InstaPro Patcher

Telefonda çevrimdışı çalışan sade APK patcher. AMOLED siyah arayüz: kaynak APK seçimi, görünen ad, isteğe bağlı paket eki (varsayılan `.gpt`), **Modla ve kaydet**. Kullanıcı APK’si ve favorileri ağa gönderilmez; uygulama INTERNET izni istemez.

**Durum: 0.1.2 teşhis adayı.** Android 11 / API 30 / x86_64 emülatörde patcher açılışı ve form testleri geçti. Kullanıcının telefonundaki 0.1.1 açılış çökmesi burada yeniden üretilemedi; telefon kaydı olmadan nedeninin giderildiği söylenmiyor. Tam hedef InstaPro APK’sinin üretimi ve açılışı hâlâ test edilmedi. [Test raporu](docs/TEST_REPORT_TR.md).

## Açılış hatasının kaydını alma

0.1.2, aynı imzayla önceki patcher üzerine kurulan bir **teşhis sürümüdür**. Patcher’ı kaldırmayın; imza anahtarı özel alanda kalır. Java arayüz başlatma hatası yakalanırsa sade hata ekranı açılır. Başka bir yakalanmamış Java hatasında Android’in normal crash handler’ı korunur ve kayıt yerelde saklanır; sonraki açılışta hata ekranı gösterilir. **Hata kaydını kopyala** ile model, Android/API, ABI, patcher sürümü ve stack trace alınabilir. Kayıt otomatik gönderilmez; APK/JSON/anahtar içeriği toplanmaz. Native veya Application başlamadan oluşan hataların bu yöntemle kaydedileceği garanti edilmez.

Normal formda paket eki isteğe bağlıdır: `gpt`/`.gpt` aynı sonucu verir, boş ek orijinal paketi korur. Kalıcı etiketler, paket önizlemesi, siyah zemin ve doğrulanmış kaynak/düğme durumları korunur. Android test kaynakları `android-tests/` altında, komutlar `docs/ANDROID_TEST_TR.md` içindedir.

## Kapsam

Yalnız şu orijinal profil kabul edilir:

- InstaPro 15.85 / manifest versionName `439.0.0.38.89`, minSdk 28.
- Boyut `180564084` bayt.
- SHA-256 `eeb7b8598b3cb2718bf3cf4c293e854e910e159e103ba9ccc0225dc4168e064e`.

Diğer sürümler ve zaten patch uygulanmış APK’ler reddedilir. `gpt` ve `.gpt` eşdeğerdir. Paket eki boş bırakılırsa `com.instagram.android`, authority/izin/process kimlikleri ve resources.arsc korunur; klon oluşturulmaz. Ek girilirse manifest ve resources.arsc paket adı değiştirilir; authority, özel izin referansları, process/taskAffinity ayrılır. Java/JNI sınıf adları korunur. Ek girildiğinde yeni veri alanı oluşur; orijinal uygulamanın oturumu veya özel verisi devralınmaz. Boş ekle aynı paket korunabilir; ancak yeniden imzalanan APK, farklı imzalı mevcut kurulumun üzerine yüklenemez. Patcher mevcut uygulamayı kaldırmaz veya onun verilerine dokunmaz.

Mevcut Backup ekranının normal iki satırı **Import favorites / Export favorites** olur. Satır layout’u ve dört gelişmiş işlem korunur. Yalnız iki `initPreference` metodu managed koda geçirilir. Orijinal native `onPreferenceClick`, lifecycle, constructor ve native kütüphaneler korunur. Bu sınırın gerçek Android davranışı ayrıca sınanmalıdır.

Import; önizleme, Birleştir/Listeyi değiştir, değiştirmede ek onay ve transaction içinde yazmadan önce otomatik yedek içerir. Export Android dosya seçicisiyle JSON üretir. İç yedekler aktarım ekranından dışarı alınabilir. İlişkiler ve boş klasörler korunur. Replace, yalnız içe alınan kategorileri bırakır; fazladan `Favorites` eklemez. Tarihsiz eski kayıtlar ilk görülme sırasına göre `import-zamanı - sıra` milisaniyesi alır; export/roundtrip bu tarihi korur. Profil görsellerinin URL metni taşınır, görsel dosyası indirilmez. INSFAV/Plus parser uyumluluğu doğrulanmadı.

İmzalı aday APK ve hash dosyası `artifacts/` altında da bulunur; özel imza anahtarı içermez. GitHub Actions yalnız bu geliştirme dalının push olayında hash’i denetleyip ön sürüm ekini yayımlar.

## Release ve imza

Patcher ve hedef APK açıkça **`android:debuggable=false`** kullanır. D8 `--release`, min API 28. Obfuscation/resource shrinking bu ilk adayda kapalıdır; runtime kod aktarımını cihazda doğrulamadan minify açılmaz. Bu bir debug varyantı değildir.

Hedef imzası ilk işlemde telefonda RSA-3072 ile oluşturulur, uygulamanın özel alanında rastgele yerel parolayla PKCS12 olarak kalıcı saklanır. Parolalı taşınabilir anahtar export/import vardır; en az 12 karakter gerekir. Uygulamayı kaldırmadan önce anahtarı yedekleyin. Yanlış parola mevcut anahtarı değiştirmez. Önceki denemenin özel anahtarı arşivde bulunmadığından bu sürüm o denemenin üzerine güncellenemez.

Patcher’ın dağıtım anahtarı ile telefonda hedef APK için oluşturulan anahtar ayrıdır. Hiçbir özel anahtar kaynaklara veya APK’ye gömülmez. Dağıtım anahtarı GitHub’a yüklenmedi. Sonraki patcher güncellemesinin aynı anahtarla imzalanması gerekir; bu çalışma ortamındaki dağıtım anahtarı ayrıca güvenli yedeklenmelidir.

## Derleme (Linux, Python 3, JDK 21)

```bash
python tools/build.py
python tools/test.py --host-only
```

Araç bağımlılıkları sabit sürümlü ve SHA-256 kilitlidir; ilk masaüstü derleme indirme yapar. Telefon işlemleri çevrimdışıdır. Android SDK platformu, D8, dexlib2 ve apksig kullanılır; telefonda masaüstü Java/Python süreçleri çalıştırılmaz. Framework derlemesinde API 35 kullanılır; bu, kaynak InstaPro’nun targetSdk 36 bilgisini değiştirmez.

İmza için mevcut kalıcı kimliği belirtin:

```bash
export PATCHER_RELEASE_KEYSTORE=/guvenli/identity.p12
export PATCHER_RELEASE_PASSWORD_FILE=/guvenli/password
export PATCHER_RELEASE_ALIAS=patcher-release
python tools/sign_release.py
```

Yalnız ilk yerel dağıtım kimliğini oluşturmak için `python tools/sign_release.py --init-local-identity` kullanılır. Varsayılan konum `/workspace/shared/instapro-release/`; dosyaları güvenli saklayın. Anahtar yokken olağan imza komutu sessizce başka kimlik üretmez. CI kaynakları release olarak derler, host testlerini çalıştırır ve depodaki imzalı APK’yi Android emülatöründe üç kez soğuk açar. Yayımlama bu iki işin geçmesini gerektirir. Dağıtım imzası yerelde kalıcı anahtarla uygulanır; özel anahtar CI’ye veya repoya eklenmez.

Orijinal kanıtlar yerelde mevcutsa ek statik test:

```bash
python tools/test.py /path/original-evidence
python tools/analyze_evidence.py /path/original-evidence
```

Kanıt binary’leri ve kullanıcı verisi depoya konulmadı. Test workload’u sentetiktir. [Android test/çökme prosedürü](docs/ANDROID_TEST_TR.md).
