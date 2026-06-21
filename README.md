# Road Damage Detector - Sensor & Camera Logger 🚗📉📸

**Road Damage Detector** adalah aplikasi Android canggih untuk memantau, mencatat, dan menganalisis kondisi permukaan jalan. Aplikasi ini mengubah smartphone Anda menjadi alat telemetri presisi tinggi yang menggabungkan data **Accelerometer**, **Gyroscope**, **GPS**, dan **Kamera (Computer Vision Context)** untuk mendeteksi anomali jalan seperti lubang atau polisi tidur secara otomatis.

## 🌟 Fitur Utama

- **Dashboard Modern & Intuitif**:
  - Antarmuka berbasis Material Design 3 dengan Jetpack Compose.
  - Ringkasan statistik *real-time*: Total Perjalanan, Total Jarak, Status GPS, Status Sensor, dan antrean *Upload Tertunda*.
- **Real-time On-Device Inference (1D-CNN)**:
  - Deteksi anomali jalan (Lubang / *Pothole* dan Polisi Tidur / *Speed Bump*) secara lokal (*Edge AI*) menggunakan model 1D-CNN.
  - Model inferensi dieksekusi secara *real-time* via **ONNX Runtime Android** dengan latensi sangat rendah.
  - Integrasi sensor fusion dinamis di perangkat sebelum data diumpankan ke model.
- **High-Frequency Sensor Logging**:
  - Merekam data **Accelerometer** (X, Y, Z) dan **Gyroscope** (X, Y, Z) secara bersamaan.
  - Menghitung **G-Force* / Magnitudo Total secara otomatis.
  - Frekuensi *sampling* (Hz) dan *Sensitivity Threshold* (G-Force) yang dapat disesuaikan.
- **GPS & Geospatial Mapping**: 
  - Sinkronisasi getaran dengan koordinat Latitude, Longitude, Altitude, Kecepatan, Akurasi, dan Bearing.
  - Visualisasi rute perjalanan menggunakan peta *offline-ready* (**Osmdroid**).
- **Real-time Sensor Visualization**:
  - Grafik *Live* 3-sumbu (X, Y, Z) saat sesi perekaman aktif.
- **Data Export & Manajemen Riwayat**:
  - Menyimpan data dalam format **CSV** yang siap diolah (kompatibel dengan Python/Pandas/MATLAB).
  - Fitur *Backward Compatibility* untuk membaca format CSV versi lama dengan mulus.
  - Ekspor perjalanan lengkap (File CSV, Metadata JSON, dan file Foto Anomali) ke folder *Downloads* perangkat Anda.
- **Background Service & Battery Optimization**:
  - Berjalan tangguh di latar belakang (*Foreground Service*) dengan manajemen *WakeLock* dinamis untuk efisiensi baterai.
- **Auto-Sync & Cloud Upload**:
  - Mengunggah data perjalanan (CSV) beserta foto-foto anomali (Multipart JPEG) ke server menggunakan **WorkManager** saat terhubung ke jaringan Wi-Fi (*Unmetered*).

## 🛠️ Tech Stack & Library

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3)
- **Architecture**: MVVM + Clean Architecture Concepts
- **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
- **Machine Learning Inference**: [ONNX Runtime Android](https://onnxruntime.ai/) (Eksekusi lokal model 1D-CNN)
- **Camera**: [CameraX](https://developer.android.com/training/camerax) (ImageCapture headless)
- **Maps**: [Osmdroid](https://github.com/osmdroid/osmdroid)
- **Asynchronous**: Coroutines & StateFlow
- **Local Storage**:
  - [Room Database](https://developer.android.com/training/data-storage/room) (Metadata Trip & Event Kamera)
  - DataStore Preferences (Pengaturan Pengguna)
  - File System (Penyimpanan file CSV & JPEG)
- **Background Processing**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) & Foreground Services
- **Networking**: [Retrofit](https://square.github.io/retrofit/) + OkHttp (Multipart Uploads)

## 📂 Struktur Project

```text
com.pemalang.roaddamage
├── data           # Akses Data (Room DAO, DataStore, Retrofit API)
├── di             # Hilt Dependency Injection Modules
├── domain         # Use Case Layer (Sensor fusion processor, OnnxModelRunner, dan logika keputusan)
├── model          # Data Classes (Trip, SensorReading, CameraEvent)
├── recording      # Service & Repository utama (Lifecycle perekaman)
├── sensors        # Handler Hardware (Accelerometer, Gyroscope, GPS)
├── ui             # Komponen UI Jetpack Compose
│   ├── components # Widget Reusable (Grafik, Dialog, Navbar)
│   ├── navigation # Setup Navigasi Aplikasi
│   ├── screens    # Layar Utama (Dashboard, ActiveSession, TripDetail, dll)
│   └── theme      # Konfigurasi Warna & Tipografi
├── util           # Utility (Kalkulasi Jarak)
└── work           # Background Worker (Auto-Upload Multipart ke Server)
```

## 🚀 Cara Menjalankan

1. **Clone Repository**
   ```bash
   git clone <repository-url>
   ```
2. **Buka di Android Studio**
   - Disarankan menggunakan Android Studio Koala atau versi terbaru.
   - Tunggu proses *Gradle Sync* selesai.
3. **Build & Run**
   - **Wajib menggunakan perangkat Android fisik**. Emulator tidak dapat mensimulasikan sensor akselerometer, giroskop, dan sistem kamera fisik secara akurat.
   - Izinkan akses Kamera, Lokasi (Presisi Tinggi), dan Notifikasi saat pertama kali membuka aplikasi.

## 📝 Requirements

- **Minimum SDK**: Android 7.0 (API Level 24)
- **Target SDK**: Android 34 (API Level 34)
- **Hardware**: Smartphone dengan sensor Akselerometer, Giroskop, GPS, dan Kamera Belakang.

## 📄 Lisensi

Project ini dibuat untuk tujuan penelitian dan pengembangan sistem deteksi kerusakan jalan (Project Skripsi).