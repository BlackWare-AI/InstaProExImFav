İmzalı **release** patcher adayıdır; `android:debuggable=false`. Yaklaşık 1,21 MB. Android üzerinde test edilmedi.

Kaynak/inceleme: PR #1. Build workflow ve Türkçe test/teşhis belgeleri kaynaklarda bulunur. APK imzası ve release manifesti, AXML/DEX/ARSC kontrolleri ve sentetik favori SQLite testleri geçti. Bu kontroller Android açılış testi değildir.

**Eksikler:** Tam orijinal InstaPro APK’si henüz sağlanmadığından telefonda uçtan uca patch üretimi yapılmadı ve bu oturumda hedef InstaPro APK’si üretilmedi. Önceki açılış çökmesinin nedeni bilinmiyor; logcat/native kayıt gerekiyor. Gerçek kullanıcı JSON’u da bu oturumda test edilmedi. Uygun ARM64 cihazda orijinal → paket/ad baseline → Activity → hook aşamalarıyla test gereklidir.

Patcher yalnız SHA-256 ile sabitlenmiş orijinal 15.85 profilini kabul eder. Her iki APK’de debug=false kuralı vardır. Telefonda oluşacak hedef imzası kalıcıdır; patcher kaldırılmadan önce parolalı anahtar yedeği alınmalıdır. Önceki denemenin imza anahtarı bulunmaz. Patcher’ın dağıtım özel anahtarı GitHub’a eklenmedi; CI imza secrets henüz kurulmadı.

APK SHA-256: `745738d68dfee50bfbbbdedad4abb0d0d85287fa25f78d17714baab89396c7e7`
