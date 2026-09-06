# Prayer Time Reminder App — Technical Build Specification

> Dokumen ini ditulis untuk dikonsumsi oleh Agentic AI (contoh: Claude Code) sebagai spesifikasi implementasi. Ikuti urutan milestone di Section 9 secara sekuensial. Jangan menambahkan teknologi di luar yang disebutkan di Section 2 tanpa alasan kuat.

---

## 1. Project Overview

**Nama kerja:** Prayer Time Reminder (adzan reminder app)

**Tujuan:** Aplikasi Android yang mengingatkan waktu sholat (Subuh, Dzuhur, Ashar, Maghrib, Isya) + Imsak + Dhuha berdasarkan lokasi pengguna, dengan pemutaran audio adzan otomatis pada waktu yang tepat.

**Target device:** Hp Android low-end/lawas. Prioritas: RAM rendah (1–2GB), storage terbatas, kadang tanpa Google Play Services aktif penuh.

**Constraint utama:** APK sekecil mungkin, kalkulasi waktu sholat harus **offline** (tidak bergantung API eksternal setelah setup awal), baterai efisien.

---

## 2. Tech Stack (WAJIB, jangan diganti tanpa approval)

| Komponen | Pilihan | Alasan |
|---|---|---|
| Bahasa | Kotlin | Standar resmi, ringan |
| UI Toolkit | **XML Views + View Binding** | ❌ JANGAN pakai Jetpack Compose — overhead render lebih tinggi, tidak ideal untuk RAM rendah |
| Build system | Gradle (Kotlin DSL, `build.gradle.kts`) | Standar modern |
| minSdkVersion | 21 (Lollipop) | Cover hp jadul |
| targetSdkVersion | Terbaru stabil saat build (cek Play Store requirement terkini) | Kepatuhan Play Store |
| Kalkulasi waktu sholat | [`com.batoulapps.adhan:adhan:2.3.1`](https://github.com/batoulapps/adhan-kotlin) (cek versi terbaru) | Library offline, sudah teruji, support banyak metode kalkulasi |
| Lokasi | `android.location.LocationManager` (built-in) | ❌ JANGAN pakai FusedLocationProviderClient / Play Services — tambahan dependency berat & tidak selalu tersedia di hp jadul |
| Audio adzan | `android.media.MediaPlayer` (built-in) | Native, tidak perlu library tambahan |
| Scheduling | `android.app.AlarmManager` | `setExactAndAllowWhileIdle()` untuk presisi meski Doze Mode aktif |
| Storage setting | `SharedPreferences` | ❌ JANGAN pakai Room DB — tidak ada kebutuhan query relasional |
| Notifikasi | `androidx.core.app.NotificationCompat` | Standar AndroidX minimal |

**Dependency yang HARUS DIHINDARI:** Firebase (semua modul), Play Services Location, Retrofit/OkHttp (tidak ada API call), Room, Dagger/Hilt (overkill untuk skala app ini), Jetpack Compose.

---

## 2.1 UI/UX Design System

Referensi gaya visual: layout hangat, kartu (card) besar dengan sudut membulat, ilustrasi lembut, warna aksen oranye di atas latar krem/off-white, bottom navigation bar sederhana. Terapkan bahasa visual ini ke konteks jadwal sholat (bukan konten anak-anak).

### Warna

| Token | Nilai (contoh) | Pemakaian |
|---|---|---|
| `background` | `#FDF6E9` (krem hangat) | Latar utama semua screen |
| `surfaceCard` | `#FFFFFF` / `#FFF3E0` | Kartu waktu sholat, kartu rekomendasi |
| `accentPrimary` | `#F97F31` (oranye) | Tombol play/pause, highlight, progress bar, FAB |
| `textPrimary` | `#1F1B16` (hampir hitam, bukan pure black) | Heading & teks utama |
| `textSecondary` | `#8A8078` (abu hangat) | Caption, durasi, subtitle |
| `chipBackground` | Variasi pastel (kuning muda, biru muda, ungu muda) per kategori | Kategori/waktu sholat berbeda bisa dapat warna chip berbeda agar mudah dibedakan sekilas |

### Tipografi

- **Heading (judul screen, nama waktu sholat, judul kartu):** font **Sora**, weight Bold/SemiBold. Contoh skala: H1 24sp (judul screen), H2 18sp (judul kartu/section)
- **Body/paragraf (deskripsi, label, teks kecil):** font **Poppins**, weight Regular/Medium. Contoh skala: Body 14–16sp, Caption 12sp

**Implementasi teknis (penting untuk konsistensi dengan Section 2):**
- Bundle file font **statis** (bukan variable font) di `res/font/`: minimal `sora_semibold.ttf`, `sora_bold.ttf`, `poppins_regular.ttf`, `poppins_medium.ttf` — cukup 2 weight per font untuk jaga ukuran APK tetap kecil
- ❌ **JANGAN** pakai Google Fonts "Downloadable Fonts" provider (`app:fontProviderAuthority`) — itu bergantung ke Google Play Services yang sudah diputuskan dihindari di Section 2 untuk device jadul
- Definisikan di `res/font/` lalu buat `font_family` XML, atau set langsung via `android:fontFamily="@font/sora_semibold"` per style di `styles.xml` — jangan hardcode font di tiap layout satu-satu
- Buat `TextAppearance` styles terpisah: `TextAppearance.Heading1`, `TextAppearance.Heading2`, `TextAppearance.BodyRegular`, `TextAppearance.Caption` — supaya konsisten dan mudah diubah terpusat

### Pola Layout (adaptasi 3 screen referensi ke konteks app ini)

1. **Home Screen** (pengganti "Welcome back" + search + categories):
   - Header: sapaan singkat + lokasi user saat ini + ikon notifikasi/pengaturan
   - Kartu besar "waktu sholat berikutnya" dengan hitung mundur (pengganti search bar besar di referensi) — ini elemen fokus utama, pakai `accentPrimary`
   - List 7 waktu (Imsak, Subuh, Dhuha, Dzuhur, Ashar, Maghrib, Isya) sebagai baris/kartu kecil rounded, waktu yang sudah lewat hari ini ditampilkan pudar/muted
   - Bottom navigation: Home, Jadwal Bulanan, Pengaturan (3 item cukup, tidak perlu search terpisah kalau app tidak butuh pencarian teks)

2. **Jadwal Bulanan Screen** (pengganti "Animals Story" list):
   - Header rounded besar dengan judul bulan + jumlah hari
   - List kartu per hari atau per kategori (bisa toggle tampilan mingguan/bulanan), tiap kartu pakai ilustrasi kecil/ikon + label durasi seperti pola referensi

3. **Detail/Now Playing Screen** (pengganti "Story Player" — dipakai saat adzan sedang berkumandang atau saat user cek detail 1 waktu sholat):
   - Ilustrasi/ikon besar di atas (mis. ikon masjid/bulan sesuai waktu sholat)
   - Judul waktu sholat (Sora Bold) + label kecil di bawahnya (Poppins)
   - Progress bar horizontal oranye (pengganti progress bar audio di referensi) — bisa dipakai untuk countdown ke waktu berikutnya
   - Kontrol player (play/pause/stop adzan, skip) pakai tombol bulat besar oranye di tengah, sama seperti pola referensi

### Catatan Konsistensi dengan Constraint Lightweight

Menambahkan design system ini **tidak mengubah keputusan di Section 2** — tetap native XML Views, tanpa Compose. Kartu rounded, warna custom, dan ilustrasi vector cukup dicapai dengan `shape drawable` (`<shape>` XML) + `CardView`/`MaterialCardView` standar AndroidX, bukan library desain tambahan. Ilustrasi sebaiknya dalam format **vector (SVG → convert ke VectorDrawable)**, bukan PNG/JPG resolusi tinggi, untuk jaga ukuran APK.

---

## 3. Functional Requirements

| ID | Requirement |
|---|---|
| FR1 | User bisa set lokasi via GPS (one-time fetch, bukan tracking realtime) ATAU pilih kota manual dari daftar/koordinat preset |
| FR2 | App menghitung 7 waktu: Imsak, Subuh, Terbit (sunrise, internal saja), Dhuha, Dzuhur, Ashar, Maghrib, Isya — offline, berbasis lokasi + tanggal |
| FR3 | User bisa pilih metode kalkulasi (default: mendekati Kemenag RI — pakai parameter custom `CalculationParameters` dari Adhan lib, fallback ke metode "MuslimWorldLeague" atau "Karachi" bila metode Kemenag tidak persis tersedia) |
| FR4 | App menjadwalkan alarm untuk setiap waktu sholat (H-24 jam ke depan), auto reschedule tiap tengah malam / setelah reboot |
| FR5 | Saat alarm terpicu: mainkan audio adzan (full untuk 5 waktu sholat, notifikasi ringan untuk Imsak & Dhuha — tanpa adzan penuh, cukup reminder singkat) + tampilkan notifikasi |
| FR6 | User bisa toggle on/off adzan per waktu sholat individual, atur volume, dan pilih file adzan (minimal 1 default, opsional lebih) |
| FR7 | Semua setting persist lewat `SharedPreferences`, survive reboot |
| FR8 | Tampilkan jadwal 7 waktu hari ini di layar utama (MainActivity) |

**Out of scope (jangan diimplementasi di v1):** Multi-bahasa, backup ke cloud, widget home screen, wear OS, dark mode custom (ikut sistem saja), qibla direction (fitur terpisah, bukan bagian dari spec ini).

---

## 4. Non-Functional Requirements

- Target ukuran APK: **< 8 MB** setelah R8/ProGuard shrink
- Idle RAM footprint: serendah mungkin — tidak boleh ada background service yang jalan terus-menerus (pakai AlarmManager, bukan polling loop)
- Harus tetap berfungsi **tanpa koneksi internet** setelah lokasi awal disetel
- Tidak boleh drain baterai — hindari wake lock berkepanjangan, hindari foreground service permanen

---

## 5. Architecture

Pakai arsitektur sederhana (jangan overengineer dengan MVVM penuh + DI framework). Struktur:

```
UI Layer (Activity/Fragment)
   ↓
Repository Layer (PrayerTimeRepository, LocationRepository, SettingsRepository)
   ↓
Scheduler (AlarmScheduler) + Receiver (AlarmReceiver, BootReceiver)
```

Tidak perlu ViewModel/LiveData kompleks — `SharedPreferences` + fungsi kalkulasi langsung sudah cukup untuk skala app ini.

---

## 6. Data Models

```kotlin
data class PrayerTimes(
    val imsak: LocalDateTime,
    val fajr: LocalDateTime,
    val sunrise: LocalDateTime,  // internal use only, untuk hitung dhuha
    val dhuha: LocalDateTime,
    val dhuhr: LocalDateTime,
    val asr: LocalDateTime,
    val maghrib: LocalDateTime,
    val isha: LocalDateTime
)

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String,   // e.g. "Asia/Jakarta"
    val cityLabel: String     // display only
)

enum class PrayerName { IMSAK, FAJR, DHUHA, DHUHR, ASR, MAGHRIB, ISHA }
```

**Formula turunan (tidak ada di Adhan lib secara langsung):**
- `imsak = fajr - 10 menit` (default, boleh dibuat configurable 5–15 menit)
- `dhuha = sunrise + 15 menit` (default, boleh dibuat configurable 10–25 menit)

---

## 7. Core Components — Implementation Notes

### 7.1 LocationRepository
- Ambil lokasi via `LocationManager.getLastKnownLocation()` dulu (instant, tanpa GPS lock baru), fallback ke `requestLocationUpdates` sekali saja dengan timeout ~10 detik
- Simpan hasil ke `SharedPreferences`, tidak perlu fetch ulang tiap buka app
- Sediakan opsi manual: dropdown/list kota-kota besar Indonesia dengan koordinat preset, untuk device tanpa GPS chip yang jalan baik

### 7.2 PrayerTimeRepository
- Wrap `Adhan` library: input `Coordinates(lat, lng)` + `CalculationParameters` + tanggal → output `PrayerTimes` (dari lib) → mapping ke `PrayerTimes` model lokal + hitung imsak & dhuha manual

### 7.3 AlarmScheduler
- Setiap kali dipanggil (saat boot, saat setting berubah, atau tengah malam): hitung `PrayerTimes` untuk hari ini, jadwalkan `AlarmManager.setExactAndAllowWhileIdle()` untuk tiap waktu yang belum lewat
- Gunakan `PendingIntent` unik per `PrayerName` (requestCode berbeda) supaya tidak saling overwrite
- Jadwalkan juga 1 alarm "midnight refresh" untuk reschedule otomatis besok

### 7.4 BootReceiver
- Listen `Intent.ACTION_BOOT_COMPLETED`
- Panggil `AlarmScheduler.rescheduleAll()`

### 7.5 AlarmReceiver
- Terima broadcast dari `AlarmManager`
- Baca `PrayerName` dari intent extra
- Jika Subuh/Dzuhur/Ashar/Maghrib/Isya → putar full adzan via `MediaPlayer` + tampilkan notifikasi full-screen-ish (heads-up)
- Jika Imsak/Dhuha → tampilkan notifikasi ringan saja (opsional short chime, bukan adzan penuh)
- Pastikan `MediaPlayer` di-`release()` setelah selesai untuk hindari memory leak

### 7.6 Permissions (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

> Catatan: `POST_NOTIFICATIONS` wajib di-request runtime untuk Android 13+ (API 33). `SCHEDULE_EXACT_ALARM` di Android 12+ butuh pengecekan `AlarmManager.canScheduleExactAlarms()` dan arahkan user ke setting jika ditolak.

---

## 8. Suggested Package Structure

```
com.example.prayerreminder/
├── data/
│   ├── PrayerTimes.kt
│   ├── UserLocation.kt
│   └── SettingsRepository.kt
├── location/
│   └── LocationRepository.kt
├── prayer/
│   └── PrayerTimeRepository.kt
├── alarm/
│   ├── AlarmScheduler.kt
│   ├── AlarmReceiver.kt
│   └── BootReceiver.kt
├── ui/
│   ├── MainActivity.kt
│   └── SettingsActivity.kt
└── PrayerReminderApp.kt (Application class, init default settings)
```

---

## 9. Build Order (ikuti sekuensial, jangan lompat)

1. **Setup project** — Gradle Kotlin DSL, minSdk 21, tambahkan dependency Adhan-kotlin, AndroidX core minimal
2. **Validasi kalkulasi** — hardcode 1 koordinat (misal Jakarta), test `PrayerTimeRepository` menghasilkan waktu yang masuk akal (bandingkan manual dengan jadwal sholat online sekali saja untuk validasi, bukan untuk dependency runtime)
3. **Data layer** — implementasi `SettingsRepository` (SharedPreferences wrapper), `LocationRepository`
4. **UI dasar** — `MainActivity` menampilkan 7 waktu hari ini (statis dulu, belum ada alarm), ikuti Design System di Section 2.1 (warna, font Sora/Poppins, rounded card)
5. **AlarmScheduler + Receivers** — implementasi scheduling, test dengan `adb shell dumpsys alarm` untuk verifikasi alarm ter-set benar
6. **Audio + Notifikasi** — `AlarmReceiver` memutar adzan + notifikasi, test manual trigger (jangan tunggu waktu asli, set alarm 1 menit ke depan untuk testing)
7. **BootReceiver** — test dengan restart device/emulator, pastikan alarm ter-reschedule
8. **SettingsActivity** — UI untuk ubah lokasi, metode kalkulasi, toggle per-waktu, volume
9. **Optimisasi** — aktifkan R8/ProGuard, kompres file audio adzan (64kbps mono), ganti semua icon ke vector drawable
10. **Test di physical device jadul** — bukan emulator, minimal 1 device Android 8–9 dengan RAM ≤2GB

---

## 10. Testing Checklist

- [ ] Alarm tetap berbunyi saat device dalam kondisi Doze Mode (screen off lama)
- [ ] Alarm ter-reschedule otomatis setelah reboot
- [ ] Tidak ada crash saat permission lokasi/notifikasi ditolak user
- [ ] APK size setelah build release < 8MB
- [ ] App tidak muncul di "battery drain apps" setelah 24 jam pemakaian normal
- [ ] Kalkulasi waktu benar untuk minimal 3 lokasi berbeda (WIB, WITA, WIT)
- [ ] Berfungsi normal dalam mode Airplane/tanpa internet

---

## 11. Known Pitfalls (WAJIB diperhatikan agent saat implementasi)

1. **Android 12+ (`SCHEDULE_EXACT_ALARM`)** — permission ini bisa ditolak default di beberapa device; cek `canScheduleExactAlarms()` dan arahkan user ke `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` jika perlu.
2. **OEM agresif (MIUI/Xiaomi, ColorOS/Oppo, FuntouchOS/Vivo)** — sering mematikan background app meski pakai AlarmManager. Tidak bisa di-fix murni lewat kode; sediakan dialog edukasi user untuk whitelist app secara manual (arahkan ke setting battery optimization).
3. **Timezone** — jangan pakai timezone device, pakai timezone hasil dari koordinat lokasi (via `android.icu.util.TimeZone` + geocoding kasar, atau biarkan user pilih timezone manual di setting kota).
4. **Metode kalkulasi Kemenag RI** tidak 1:1 tersedia di library Adhan (yang berbasis library internasional) — dokumentasikan dengan jelas di UI bahwa hasil adalah "pendekatan", beri opsi manual offset menit jika user ingin menyesuaikan.
5. **MediaPlayer leak** — selalu `release()` di `onCompletion` listener, jangan biarkan instance menggantung.
6. **Doze Mode + `setExactAndAllowWhileIdle`** — ini API yang tepat, JANGAN pakai `WorkManager` untuk ini (WorkManager tidak presisi untuk exact-time trigger).

---

## 12. Definition of Done (v1)

App dianggap selesai v1 jika:
- Bisa dipasang & jalan di device Android 8+ dengan RAM 2GB tanpa lag berarti
- Menampilkan & memberi notifikasi + audio adzan akurat untuk lokasi yang dipilih user, tanpa internet
- Alarm reliable setelah reboot dan setelah screen mati lama (Doze test)
- APK release < 8MB
