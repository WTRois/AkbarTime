<div align="center">

  <img src="app/src/main/res/drawable/akbartime.webp" alt="AkbarTime Logo" width="160" />

  # AkbarTime

  **Aplikasi Pengingat Waktu Sholat & Adzan Otomatis — Ringan, Bebas Iklan, & Berdesain Neobrutalism**

  [![Android](https://img.shields.io/badge/Android-5.0%2B%20(API%2021%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
  [![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-blue?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
  [![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
  [![License: MIT](https://img.shields.io/badge/License-MIT-F58220?style=for-the-badge)](LICENSE)
  [![Ads Free](https://img.shields.io/badge/Ads-100%25%20Free-success?style=for-the-badge)](README.md)
  [![APK Size](https://img.shields.io/badge/APK%20Size-~5.2%20MB-informational?style=for-the-badge)](README.md)

</div>

---

## 📌 Tentang AkbarTime

**AkbarTime** adalah aplikasi waktu sholat dan pengingat adzan untuk Android yang dibangun dengan prinsip **privacy-first**, efisiensi daya, dan keandalan tinggi. Banyak aplikasi serupa dipenuhi iklan invasif, konsumsi memori berat, serta sering gagal mengumandangkan adzan karena pembatasan optimasi baterai di background. 

AkbarTime dirancang untuk mengatasi persoalan tersebut:
- **100% Bebas Iklan & Pelacak**: Tanpa banner, tanpa intersitial video, dan tidak mengumpulkan data pribadi.
- **Tepat Waktu & Andal**: Menggunakan arsitektur alarm modern yang tahan terhadap mode Doze Android dan restart perangkat.
- **Visual Neobrutalism**: Tampilan antarmuka berkarakter kuat dengan border kontras, bayangan tegas, dan tipografi modern (*Sora* & *Poppins*).

---

## ✨ Fitur Utama

| Fitur | Deskripsi |
|---|---|
| 🕋 **Kalkulasi Offline Akurat** | Menghitung waktu Subuh, Dzuhur, Ashar, Maghrib, dan Isya menggunakan algoritma astronomi Batoul Apps (`Adhan`) tanpa memerlukan kuota internet. |
| 🔊 **Adzan Tepat Waktu (Foreground Service)** | Pemutaran audio adzan menggunakan `ForegroundService` tipe `mediaPlayback` dengan manajemen `AudioFocus` otomatis agar suara tidak terpotong. |
| ⏰ **Resistant Terhadap Doze & Reboot** | Dijadwalkan melalui `AlarmManager.setExactAndAllowWhileIdle()`. Dilengkapi `BootReceiver` untuk mendaftar ulang alarm secara otomatis setelah HP menyala ulang. |
| 🧭 **Kompas Kiblat Interaktif** | Sensor fusion (*accelerometer* & *magnetometer*) dengan peredam getaran (*low-pass filter*) untuk memandu arah Ka'bah secara stabil dan akurat. |
| 🔕 **Mode Senyap Otomatis (Auto-Silent)** | Otomatis mengalihkan HP ke mode hening saat sholat berlangsung (15 menit pasca-adzan) dan mengembalikan volume ke kondisi semula. |
| ⏱️ **Koreksi Waktu Manual** | Penyesuaian menit (+/-) per waktu sholat guna menyesuaikan jadwal dengan standar masjid setempat. |
| 🔔 **Pengingat Persiapan (Pre-Reminder)** | Notifikasi pengingat 10 menit sebelum adzan berkumandang untuk persiapan wudhu dan sholat. |
| 📅 **Kalender Hijriah & Masehi** | Menampilkan tanggal kalender Hijriah dan Masehi secara otomatis di halaman utama. |

---

## 🏗️ Arsitektur & Teknologi

AkbarTime dibangun dengan kode Kotlin yang modular, bersih, dan mematuhi kaidah arsitektur Android modern:

```
app/src/main/java/com/atf/akbartime/
├── alarm/            # Scheduler, Foreground Service Adzan, & Broadcast Receivers
├── data/             # Repositori SharedPreferences & Model Data
├── location/         # Manajemen koordinat GPS (LocationManager & Fallback)
├── prayer/           # Integrasi kalkulasi astronomi Adhan
└── ui/               # MainActivity, SettingsActivity, QiblaActivity, & SplashActivity
```

### Tech Stack
- **Language**: Kotlin 2.x
- **UI Toolkit**: Android XML Views, ViewBinding, Material Components
- **Typography**: Sora & Poppins (Custom Font Families)
- **Calculation Core**: [Batoul Apps - Adhan Java](https://github.com/batoulapps/adhan-java)
- **Core Desugaring**: Dukungan Java 8+ API time desugaring untuk kompatibilitas mundur hingga Android 5.0 (API 21)
- **Code Shrinker**: R8 / ProGuard teroptimasi dengan ukuran rilis APK hanya **~5.2 MB**

---

## 🔒 Izin Sistem (Permissions)

AkbarTime hanya meminta izin yang mutlak dibutuhkan untuk fungsionalitas inti aplikasi:

| Izin | Alasan Penggunaan |
|---|---|
| `ACCESS_FINE_LOCATION` / `COARSE` | Menentukan lintang & bujur perangkat guna menghitung waktu sholat dan sudut kiblat yang presisi. |
| `POST_NOTIFICATIONS` | Menampilkan pemberitahuan waktu sholat dan countdown di Android 13+ (API 33+). |
| `SCHEDULE_EXACT_ALARM` | Memastikan alarm adzan berbunyi tepat di detik pertama waktu sholat tiba. |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Menjalankan pemutaran audio adzan di background tanpa dihentikan paksa oleh sistem. |
| `RECEIVE_BOOT_COMPLETED` | Menjadwalkan ulang jadwal alarm adzan setelah perangkat dihidupkan ulang. |
| `WAKE_LOCK` | Mencegah CPU sleep saat memproses transisi trigger alarm waktu sholat. |

---

## 🚀 Memulai (Build & Development)

### Prasyarat
- **Android Studio**: Ladybug / Meerkat atau yang lebih baru
- **JDK**: Versi 17 atau 21
- **Android SDK**: Platform API 35

### Langkah-langkah
1. **Clone repositori:**
   ```bash
   git clone https://github.com/WTRois/AkbarTime.git
   cd AkbarTime
   ```

2. **Buka di Android Studio** dan tunggu proses Gradle Sync selesai.

3. **Build APK Debug:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Build APK Release (Optimized & Shrunk):**
   ```bash
   ./gradlew assembleRelease
   ```
   *Output APK akan berada di `app/build/outputs/apk/release/`.*

---

## 📄 Lisensi

Proyek ini dilisensikan di bawah **MIT License**. Lihat file [LICENSE](LICENSE) untuk informasi hak cipta selengkapnya.

```
Copyright (c) 2026 Muhammad Rois Akbar
```

---

<div align="center">
  <sub>Dibuat dengan dedikasi untuk kemudahan ibadah harian kaum muslimin.</sub>
</div>
