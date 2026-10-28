# MoneyFlow - Aplikasi Pengelolaan Keuangan Pribadi Android

[![Build Debug APK](https://github.com/ecotech982/MoneyFlow-2/actions/workflows/build-apk.yml/badge.svg)](https://github.com/ecotech982/MoneyFlow-2/actions/workflows/build-apk.yml)

Aplikasi manajemen keuangan pribadi modern untuk Android yang dibangun menggunakan **Kotlin**, **Jetpack Compose (Material 3)**, dan **Room Database** (penyimpanan lokal cepat dan aman). Dilengkapi dengan analitik anggaran (Aturan 50/30/20), multi-dompet/rekening bank, ekspor laporan PDF/CSV, scan struk OCR otomatis, Voice Over pencatatan suara, serta Chat Asisten Keuangan cerdas berbasis OpenRouter AI.

---

## 📥 Unduh APK Langsung

Setelah repository di-push ke GitHub, GitHub Actions akan otomatis mem-build APK debug terbaru:

- **Download APK Terbaru:** [app-debug.apk](https://github.com/ecotech982/MoneyFlow-2/releases/latest/download/app-debug.apk)
- **Halaman Releases:** [GitHub Releases `ecotech982/MoneyFlow-2`](https://github.com/ecotech982/MoneyFlow-2/releases)
- **File APK Lokal di Proyek:** Folder `APK DOWNLOAD/app-debug.apk`

---

## ✨ Fitur Utama

1. **🤖 Pindai Struk Otomatis (Kamera & Galeri/File via OCR + AI):**
   - Ambil foto langsung dari kamera atau pilih gambar struk/nota/bukti transfer dari berkas galeri.
   - Menggunakan ML Kit Text Recognition (OCR) berkecepatan tinggi yang terintegrasi dengan OpenRouter AI.
   - Otomatis mengekstrak: Tipe (Pemasukan/Pengeluaran), Kategori, Dompet/Rekening, Nominal (Rp), Catatan/Nama Toko, dan Tanggal Transaksi, lalu langsung masuk ke riwayat.

2. **🎙️ Voice Over Pencatatan Keuangan (Speech-to-Text + AI):**
   - Cukup bicara kalimat transaksi (contoh: *"Makan siang 25000 pakai tunai catatan nasi padang"* atau *"Terima gaji 5 juta ke rekening BCA"*).
   - AI mengenali maksud, nominal, kategori, dan dompet, serta otomatis menyimpannya ke database.

3. **💬 AI Financial Assistant di Kolom Chat ("Tanya AI"):**
   - Tab khusus Asisten AI di bilah navigasi utama.
   - Terkoneksi secara dinamis dengan seluruh data database aplikasi: total saldo, pengeluaran & pemasukan, status aturan 50/30/20, saldo per dompet/rekening, kategori pengeluaran terbesar, serta utang/piutang.
   - Menjawab konsultasi dan memberikan saran keuangan berbasis data real pengguna secara ramah dan solutif.

4. **⚙️ Kustomisasi API OpenRouter:**
   - Dikonfigurasi di menu Pengaturan / Profil:
     - **Base URL:** `https://openrouter.ai/api/v1`
     - **Model AI:** Pilihan bebas dari `https://openrouter.ai/models` (contoh: `google/gemini-2.0-flash-001`, `openai/gpt-4o-mini`, `meta-llama/llama-3.3-70b-instruct:free`, `deepseek/deepseek-chat`).
     - **API Key:** Kunci OpenRouter pribadi dengan status koneksi langsung. Tersedia juga parser lokal otomatis bila belum memasukkan kunci.

5. **📊 Dashboard & Analitik Interaktif:**
   - Aturan Alokasi Keuangan 50/30/20 (Kebutuhan 50%, Keinginan 30%, Tabungan 20%).
   - Rekap Saldo Multi-Akun (Tunai, E-Wallet, BCA, BRI, Danamon, Lainnya).
   - Portofolio Keuangan: Hutang Saya, Piutang Orang, dan Aset Emas.

6. **📄 Laporan & Ekspor Data:**
   - Ekspor transaksi ke format PDF dan CSV (.csv) untuk pembukuan atau pencetakan.

---

## 🛠️ Arsitektur & Teknologi

- **Bahasa:** Kotlin
- **UI Framework:** Jetpack Compose (Material Design 3)
- **OCR:** Google ML Kit Text Recognition
- **Voice Recognition:** Android SpeechRecognizer
- **AI Integrasi:** OpenRouter API (`https://openrouter.ai/api/v1`)
- **Penyimpanan Data:** Room Database (SQLite) + SharedPreferences
- **Arsitektur:** MVVM (Model-View-ViewModel) + StateFlow & Coroutines
- **CI/CD:** GitHub Actions (`.github/workflows/build-apk.yml`)

---

## 🚀 Cara Push ke GitHub (`ecotech982/MoneyFlow-2`)

1. Pada Google AI Studio, buka menu **Settings** / **Export** di pojok kanan atas.
2. Pilih opsi **Push to GitHub** atau hubungkan akun GitHub Anda.
3. Arahkan ke repository: `ecotech982/MoneyFlow-2` pada branch `main`.
4. Setiap push akan otomatis memicu GitHub Actions untuk mem-build dan merilis file `app-debug.apk`.
