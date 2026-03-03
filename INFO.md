## LibreTube – Proje Özeti ve Hızlı Başlangıç Rehberi

Bu dosyanın amacı, yeni bir agent ya da geliştiricinin projeyi **çok hızlı** kavrayabilmesini sağlamaktır.  
Temel sorular: “Bu ne projesi?”, “Nereden başlasam?”, “Önemli dosyalar nerede?”, “Hangi katman ne iş yapıyor?”

---

## 1. Stack ve Platform

- **Platform**: Native Android uygulaması
- **Diller**:  
  - Uygulama: **Kotlin**  
  - Gradle build script: **Kotlin DSL** (`*.gradle.kts`)
- **Ana Android teknolojileri**:
  - AndroidX: Activity, Fragment, Navigation, Lifecycle, Room, WorkManager, Media3
  - Ağ: Retrofit + OkHttp + Kotlinx Serialization
  - Medya: Media3 / ExoPlayer tabanlı oynatıcı
  - Yerel veri: Room (SQLite üstü)
  - Arka plan işler: WorkManager + foreground servisler
  - Video/metaveri kaynağı: **Piped / NewPipe extractor** tabanlı

---

## 2. Modüller ve Üst Düzey Dizinler

Root (proje kökü):

- **`app`**: Ana Android uygulama modülü. Tüm UI, servisler, veri katmanı, yardımcılar burada.
- **`baselineprofile`**: Performans için baseline profile / macrobenchmark modülü (`:app` hedefleniyor).
- **`fastlane`, `assets`, `gradle`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`**:  
  Build ve yayınlama ile ilgili yapılandırmalar.

`app/src/main/java/com/github/libretube` altındaki ana paketler:

- **`api`**: Retrofit arayüzleri, Retrofit/OkHttp kurulumları, Piped API erişimi.
- **`repo`**: Repository katmanı; Piped API + Room DB + diğer kaynakları birleştirir.
- **`db`**: Room `AppDatabase`, `dao`’lar ve entity (`obj`) tanımları.
- **`ui`**: Aktiviteler, fragment’ler, view model’ler, adapter’lar, dialog’lar, bottom sheet’ler.
- **`services`**: Oynatıcı servisleri, indirme servisleri, temizleme servisleri (Android `Service`).
- **`workers`**: WorkManager worker’ları (özellikle bildirimler için).
- **`helpers`**: Ağ, tema, oynatıcı, bildirim, yönlendirme, kısayol vb. için yardımcı sınıflar.
- **`util`**: Çeşitli yardımcı sınıflar, hata yakalama, güncelleme kontrolü, queue yönetimi vs.
- **`extensions`**: Ortak Kotlin extension fonksiyonları.
- **`constants`**: `PreferenceKeys`, `IntentData` gibi global sabitler.
- **`compat`, `receivers`, `json`, `parcelable`, `enums`, `obj`**:  
  Uyumluluk sınıfları, broadcast receiver’lar, JSON modeller, parcelable wrapper’lar, enum’lar vb.

---

## 3. Giriş Noktaları (Entry Points)

### 3.1 Uygulama Başlangıcı

- **Manifest**: `app/src/main/AndroidManifest.xml`
  - `<application android:name=".LibreTubeApp" ... />`
- **`LibreTubeApp`**: `app/src/main/java/com/github/libretube/LibreTubeApp.kt`
  - Bildirim kanallarını oluşturur.
  - `PreferenceHelper`’ı başlatır ve migration yapar.
  - Image loader’ı başlatır (`ImageHelper`).
  - Arka plan bildirim işçisini (`NotificationHelper.enqueueWork`) kuyruğa alır.
  - Proxy URL gibi ağ ayarlarını çeker (`ProxyHelper`).
  - Global `ExceptionHandler` kurar.
  - Dinamik kısayolları oluşturur (`ShortcutHelper`).
  - NewPipe extractor’ı başlatır (`NewPipeExtractorInstance`).

### 3.2 Ana Activity ve Navigasyon

- **Launcher**:
  - Manifest’te `.Default` alias, `.ui.activities.MainActivity`’ye işaret eder.
- **`MainActivity`**: `app/src/main/java/com/github/libretube/ui/activities/MainActivity.kt`
  - `AbstractPlayerHostActivity`’den türemiştir; mini player’ı host eder.
  - İlk açılışta:
    - Ağ var mı kontrol eder (`NetworkHelper`), yoksa `NoInternetActivity`.
    - Karşılama akışını (`WelcomeActivity`) tercih/flag’lere göre yönlendirir (`PreferenceHelper`).
  - **Bottom navigation + Navigation Component**:
    - `NavHostFragment` üzerinden `R.navigation.nav` graph’ını yükler.
    - Başlangıç destination’ını tercih / ayarlara göre seçer (`NavBarHelper`).
  - **Arama**:
    - `SearchViewModel` ile entegre arama çubuğu ve öneriler.
    - Yapıştırılan YouTube/Piped URL’lerini `IntentHelper` ile çözümler ve ilgili ekrana yönlendirir.
  - **Mini player & PiP**:
    - Oyuncu fragment’ine/servisine köprü, MotionLayout ile mini/full player animasyonları.

### 3.3 Router ve Dış Bağlantılar

- **`RouterActivity`**: `ui/activities/RouterActivity.kt`
  - Manifest’te birçok `<intent-filter>`:
    - `SEND text/plain` – paylaşılan linkler.
    - `VIEW` – `youtube.com`, `youtu.be`, Piped instance URL’leri.
  - Gelen intent’leri `NavigationHelper` / `IntentHelper` / `LinkHandler` ile uygun ekrana yönlendirir.

### 3.4 Servisler ve Worker’lar

- **Oynatıcı servisleri**:
  - `services/OnlinePlayerService.kt`
  - `services/OfflinePlayerService.kt`
  - Ortak taban: `services/AbstractPlayerService.kt`
  - Media3 `MediaSessionService` ile entegre; foreground service + bildirim.
- **İndirme servisleri**:
  - `services/DownloadService.kt`
  - `services/PlaylistDownloadEnqueueService.kt`
- **WorkManager**:
  - `workers/NotificationWorker.kt`
  - Kuyruklama: `helpers/NotificationHelper.kt`

---

## 4. Mimari ve Katmanlar

Genel yaklaşım: **MVVM + Repository** yapısı, DI framework yok, **manual DI + singleton helper** yaklaşımı.

- **UI (View)**:
  - `ui/activities`, `ui/fragments`
  - ViewBinding (Gradle’da `viewBinding { enable = true }`).
  - Navigation Component ile yönlendirme.
- **ViewModel’ler**:
  - Konum: `ui/models`
  - Örnekler: `HomeViewModel`, `SearchViewModel`, `PlayerViewModel`, `SubscriptionsViewModel`…
  - Genellikle repository’ler ile çalışır ve LiveData / Flow üzerinden UI’ya veri sağlar.
- **Repository katmanı**:
  - Konum: `repo`
  - Tipik sorumluluklar:
    - Piped API’den veri çekmek (Retrofit).
    - Room DB’den cache / offline veri almak.
    - Gerekirse ikisini birleştirmek.
  - Örnekler: `FeedRepository`, `SubscriptionsRepository`, `PlaylistRepository`, `AccountSubscriptionsRepository`…
- **Veri kaynakları**:
  - Ağ: `api` paketi (Retrofit interface’leri, `RetrofitInstance`, `PipedApi`, `ExternalApi`, `PipedAuthApi`…)
  - Yerel DB: `db` paketi (`AppDatabase`, `dao` altındaki DAO’lar, `obj` altındaki entity’ler).
- **Yardımcı / servis benzeri katman**:
  - `helpers` paketi:
    - Oynatıcı: `PlayerHelper`, `AudioHelper`, `DashHelper`, `NewPipeDownloaderImpl`
    - Ağ: `NetworkHelper`, `ProxyHelper`, `DownloadHelper`
    - UI / tema: `ThemeHelper`, `NavBarHelper`, `WindowHelper`, `BrightnessHelper`
    - Ayarlar: `PreferenceHelper`, `BackupHelper`, `LocaleHelper`, `ShortcutHelper`
    - Bildirim: `NotificationHelper`, `NowPlayingNotification`

---

## 5. Önemli Dosyalar ve İlk Bakılacak Yerler

Yeni bir agent için **ilk okunması önerilen dosyalar**:

1. **Genel yapı ve lifecycle**
   - `AndroidManifest.xml` – Aktiviteler, servisler, receiver’lar, deep link’ler, `LibreTubeApp`.
   - `LibreTubeApp.kt` – Global başlangıç, helper init, bildirim kanalları.
2. **Ana ekran ve navigasyon**
   - `ui/activities/MainActivity.kt` – Bottom nav, arama, mini player köprüsü.
   - `ui/base/BaseActivity.kt` + `AbstractPlayerHostActivity` – Ortak aktivite davranışları.
   - `res/navigation/nav.xml` – Navigasyon graph’ı, ana destinasyonlar.
3. **Ağ katmanı**
   - `api/RetrofitInstance.kt` – Piped taban URL’leri, Retrofit/OkHttp config.
   - `api/PipedApi.kt` ve ilgili diğer API interface’leri.
   - `api/PipedMediaServiceRepository.kt` – Hangi Piped instance’ın kullanılacağını belirler.
4. **Veri ve repository’ler**
   - `db/AppDatabase.kt` + `db/dao/*` – DB şeması, var olan DAO’lar.
   - Örnek repository’ler:
     - `repo/FeedRepository.kt`
     - `repo/SubscriptionsRepository.kt`
     - `repo/PlaylistRepository.kt`
5. **Oynatıcı ve indirmeler**
   - `services/OnlinePlayerService.kt`, `OfflinePlayerService.kt`, `DownloadService.kt`
   - `util/PlayingQueue.kt`
   - `helpers/PlayerHelper.kt`, `AudioHelper.kt`, `NowPlayingNotification.kt`
6. **Ayarlar ve tercih yönetimi**
   - `helpers/PreferenceHelper.kt`
   - `constants/PreferenceKeys.kt`
   - `ui/preferences/MainSettings.kt`, `GeneralSettings.kt` vb.
7. **Çekirdek yardımcılar**
   - `helpers/NavigationHelper.kt`, `IntentHelper.kt`, `NetworkHelper.kt`, `NewPipeExtractorInstance.kt`
   - `util/UpdateChecker.kt`, `util/ExceptionHandler.kt`

---

## 6. Konvansiyonlar ve Yapılandırma

### 6.1 İsimlendirme

- ViewModel: `*ViewModel` (ör: `HomeViewModel`, `SearchViewModel`)
- Repository: `*Repository` (ör: `FeedRepository`, `LocalPlaylistsRepository`)
- Helper: `*Helper` (ör: `NetworkHelper`, `PreferenceHelper`, `DownloadHelper`)
- Service: `*Service` (ör: `OnlinePlayerService`, `DownloadService`)
- Worker: `*Worker` (ör: `NotificationWorker`)
- Preference/Intent key’leri:
  - `constants/PreferenceKeys.kt`
  - `constants/IntentData.kt`

### 6.2 Paket Düzeni

- `ui` altı tür bazlı:
  - `activities`, `fragments`, `models` (ViewModel), `adapters`, `views`, `dialogs`, `sheets`, `preferences`, `base`
- “Feature-first” yerine “tip-first” (activity/fragment/adapters) yaklaşımı baskın; ViewModel’ler feature’a göre gruplanmış.

### 6.3 Yapılandırma ve Gizli Bilgiler

- **Keystore / imzalama**:
  - `app/build.gradle.kts` içindeki `keystore.properties` okuması:
    - `storeFile`, `storePassword`, `keyAlias`, `keyPassword`
  - Bu dosya repo’da yok, lokal olmalı; imzalama için kullanılır.
- **BuildConfig**:
  - `buildFeatures { buildConfig = true }`
  - Kullanım örnekleri:
    - `BuildConfig.DEBUG` – debug davranışları, loglar, debug ekranları.
    - `BuildConfig.VERSION_CODE` – güncelleme kontrolleri ve kullanıcı bilgisi dialog’larında.
- **Diğer ayarlar**:
  - Hedef/min SDK: `minSdk = 26`, `targetSdk = 36`
  - `android.nonTransitiveRClass=true`, `android.nonFinalResIds=true`
  - Lokal instance/Piped sunucuları, `PreferenceKeys` aracılığıyla ayarlanıyor.

---

## 7. Veri Katmanı: Room + Preferences

### 7.1 Room

- **`AppDatabase`**: `db/AppDatabase.kt`
  - Tüm entity’ler (watch history, download’lar, playlist’ler, subscription’lar, vb.) burada tanımlı.
  - Versiyon: `22`, birçok `@AutoMigration` deklarasyonu var.
  - Versiyon değişiminde **otomatik migration** sadece belirli geçişler için tanımlı; yeni değişiklikler eklerken dikkat.
- **DAO’lar**: `db/dao/*`
  - Örnekler:
    - `WatchHistoryDao`, `DownloadDao`, `LocalPlaylistsDao`, `LocalSubscriptionDao`…
- **Helper’lar**:
  - `DatabaseHelper.kt` / `DatabaseHolder.kt`
  - UI tarafı çoğunlukla bu helper üzerinden DB’ye erişiyor (örn. `DatabaseHelper.addToSearchHistory`).

### 7.2 SharedPreferences / Ayarlar

- **`PreferenceHelper`**:
  - Tüm SharedPreferences işlemleri buradan geçiyor.
  - `LibreTubeApp` içinde `PreferenceHelper.initialize` ile başlatılıyor.
  - Migrasyon/versiyonlama iç lojik içeriyor.
- **`PreferenceKeys`**:
  - Tüm key’ler burada toplu şekilde listelenmiş.
  - Özellik bayraklarını veya belirli bir ayarı ararken ilk bakılacak yer.

---

## 8. Navigasyon ve Deep Link’ler

- **Navigation Component**:
  - Graph: `res/navigation/nav.xml`
  - `MainActivity` üzerinden `NavHostFragment` ve bottom nav bağlanıyor.
  - SafeArgs kullanımı var; `*Directions` sınıfları otomatik üretiliyor.
- **Deep link & dış link routing**:
  - `RouterActivity` intent filter’larıyle YouTube/Piped link’lerini yakalar.
  - `NavigationHelper`, `IntentHelper`, `LinkHandler` bunları yorumlayıp uygun ekrana yönlendirir.

---

## 9. Medya Oynatıcı ve İndirme Akışı (Yüksek Seviye)

### 9.1 Oynatıcı

- Servisler:
  - Online: `OnlinePlayerService`
  - Offline: `OfflinePlayerService`
  - Ortak: `AbstractPlayerService`
- Kuyruk:
  - `util/PlayingQueue.kt` – geçerli oynatma kuyruğu yönetimi.
- NewPipe:
  - `helpers/NewPipeExtractorInstance.kt` + `util/NewPipeDownloaderImpl.kt` – YouTube/Piped stream bilgisi için NewPipe entegrasyonu.
- Bildirim:
  - `helpers/NowPlayingNotification.kt` + `NotificationHelper`
  - Media3 session ile entegre: kilit ekranı/bildirim kontrolleri.

### 9.2 İndirmeler

- Servisler:
  - `DownloadService`, `PlaylistDownloadEnqueueService`
- Yardımcı:
  - `DownloadHelper`
- DB:
  - İndirme durumları için `db/obj` altındaki entity’ler (örn. `Download`, `DownloadItem`).
- UI:
  - İndirmeleri gösteren fragment’ler (örn. `ui/fragments/DownloadsFragment`).

---

## 10. Dikkat Edilmesi Gerekenler ve “Gotcha”lar

- **DI framework yok, manual DI**:
  - Dagger/Hilt yok; `object` singleton’lar ve statik helper’lar üzerinden dependency kullanılıyor.
  - `LibreTubeApp.instance`, `PreferenceHelper`, `DatabaseHelper` gibi global erişimler yaygın.
  - Yeni kod yazarken gizli bağımlılıklara dikkat et; test edilebilirliği zorlaştırabilir.
- **Global state**:
  - Oynatıcı kuyruğu, bazı helper’lar ve `LibreTubeApp` üzerinden global state tutuluyor.
  - Threading / lifecycle ile ilgili yan etkiler olabileceğini akılda tut.
- **Manifest karmaşıklığı**:
  - Birden fazla `activity-alias`, çok sayıda `intent-filter`, özellikle `RouterActivity` üzerinde.
  - Launcher ikonları / tarzları ve deep link davranışını değiştirirken tüm alias’ları senkron tutmak önemli.
- **Room migration’ları**:
  - `AppDatabase` versiyon 22, birçok `@AutoMigration` tanımı var.
  - Entity değiştirilirken yeni migration gerekip gerekmediği mutlaka kontrol edilmeli.
- **Build toolchain**:
  - Navigation SafeArgs, Room KSP, Kotlinx Serialization, Baseline Profile plugin gibi kod üretimi yapan epey araç var.
  - Yeni navigation action, entity veya serializable model eklerken Gradle build/sync gerekebilir.
- **Edge-to-edge UI & MotionLayout**:
  - `MainActivity` ve player container, sistem inset’leri (status bar, navigation bar) ile hassas çalışıyor.
  - Layout değiştirirken ilgili helper’ların (`onSystemInsets` vb.) kullanımına dikkat edilmeli.

---

## 11. Yeni Bir Özellik Eklerken Önerilen Yol

**Örnek akış (yüksek seviye)**:

1. **İş gereksinimini haritalandır**  
   - Hangi ekran(lar) etkilenecek?  
   - Sadece UI mi, yoksa Piped API/DB değişiyor mu?
2. **İlgili ViewModel ve Repository’yi bul**  
   - Navigation graph’tan ilgili fragment’i bul → ilgili `*ViewModel` → ilgili `*Repository`.
3. **Veri katmanı değişikliklerini tasarla**  
   - Ağ isteği gerekiyorsa: `api` altına yeni endpoint ekle, gerekiyorsa model ekle.  
   - Kalıcı veri gerekiyorsa: `db/obj` + `db/dao` + `AppDatabase` + migration ihtiyacını değerlendir.
4. **UI tarafını güncelle**  
   - Fragment / Activity, ilgili ViewModel’den veri alır, adapter’lar/görünümler ile gösterir.  
   - Gerekirse yeni navigation action veya deep link ekle (graph + SafeArgs).
5. **Helper / util ihtiyacı varsa ekle**  
   - Örneğin yeni bir indirme tipi için `DownloadHelper` içinde genişletme yap.
6. **Ayar / flag gerekiyorsa**  
   - Yeni key’i `PreferenceKeys` içine ekle, `PreferenceHelper` üzerinden kullan.

Bu rehber, yeni bir agent’in projeye başlarken hızlıca genel resmi görmesini ve ilgili katmanlara atlayabilmesini amaçlar.  
Detaylı bir akışı anlamak için, yukarıda listelenen “İlk bakılacak dosyalar” sırasını izlemek en verimli yaklaşımdır.

