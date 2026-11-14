# MoneyFlow - Aplikasi Pengelolaan Keuangan Pribadi Android

[![Build Debug APK](https://github.com/ecotech982/MoneyFlow-2/actions/workflows/build-apk.yml/badge.svg)](https://github.com/ecotech982/MoneyFlow-2/actions/workflows/build-apk.yml)

Aplikasi manajemen keuangan pribadi modern untuk Android yang dibangun menggunakan **Kotlin**, **Jetpack Compose (Material 3)**, dan **Room Database** (penyimpanan lokal cepat dan aman). Dilengkapi dengan pencatatan pemasukan dan pengeluaran manual, analitik anggaran (Aturan 50/30/20), multi-dompet/rekening bank, riwayat transaksi, serta ekspor laporan Excel/CSV.

---

## 📥 Unduh APK Langsung

Setelah repository di-push ke GitHub, GitHub Actions akan otomatis mem-build APK debug terbaru:

- **Download APK Terbaru:** [app-debug.apk](https://github.com/ecotech982/MoneyFlow-2/releases/latest/download/app-debug.apk)
- **Halaman Releases:** [GitHub Releases `ecotech982/MoneyFlow-2`](https://github.com/ecotech982/MoneyFlow-2/releases)
- **File APK Lokal di Proyek:** Folder `APK DOWNLOAD/app-debug.apk`

---

## ✨ Fitur Utama

1. **📝 Pencatatan Pemasukan & Pengeluaran Manual:**
   - Input nominal, kategori transaksi, catatan, dompet/rekening, dan tanggal transaksi secara cepat.
   - Rencana alokasi metode 50/30/20 otomatis saat mencatat pemasukan.

2. **👛 Multi-Dompet & Rekening (Wallet):**
   - Rekap saldo otomatis per akun: Tunai, E-Wallet, Rekening 1 (BCA), Rekening 2 (BRI), Rekening 3 (Danamon), dan Lainnya.

3. **📜 Riwayat Transaksi:**
   - Pencarian transaksi, filter tipe (Semua, Pemasukan, Pengeluaran), dan filter periode (Semua Waktu, Harian, Mingguan, Bulanan).
   - Detail transaksi lengkap dengan fitur edit dan hapus.

4. **📊 Dashboard & Analitik Interaktif:**
   - Aturan Alokasi Keuangan 50/30/20 (Kebutuhan 50%, Keinginan 30%, Tabungan 20%).
   - Grafik tren pengeluaran mingguan, arus kas masuk vs keluar, diagram persentase kategori, serta target anggaran bulanan/tahunan.

5. **📄 Laporan & Ekspor Data:**
   - Ekspor transaksi ke format Excel (.xlsx) dan CSV (.csv) untuk pembukuan atau pencetakan.

---

## 🛠️ Arsitektur & Teknologi

- **Bahasa:** Kotlin
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Penyimpanan Data:** Room Database (SQLite) + SharedPreferences
- **Arsitektur:** MVVM (Model-View-ViewModel) + StateFlow & Coroutines
- **CI/CD:** GitHub Actions (`.github/workflows/build-apk.yml`)
