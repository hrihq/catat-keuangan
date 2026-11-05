# Catat Keuangan

Aplikasi pencatatan keuangan pribadi untuk Android dengan desain Material You bergaya PixelOS (warna pastel Monet, sudut membulat besar, dan tipografi modern).

## Fitur & Desain (Sesuai PDF)
- **Ringkasan Saldo**: Total saldo, pemasukan, pengeluaran, serta progres anggaran bulanan.
- **Transaksi Terbaru**: Daftar transaksi dengan ikon kategori dinamis dan indikator status pemasukan/pengeluaran.
- **Catat Transaksi**: Form bottom sheet untuk mencatat pemasukan dan pengeluaran beserta kategori (Makanan, Transport, Tagihan, Belanja, Hiburan, Kesehatan) dan tanggal.
- **Statistik**: Visualisasi alokasi pengeluaran per kategori dan insight hemat bulanan.
- **Material You UI**: Palet warna autentik Monet (Biru `#0B57D0`, Kuning `#FFE08A`, Biru Muda `#C2DAFF`, Teal `#C4EED0`).

## Build & CI/CD
Aplikasi ini di-build otomatis menggunakan GitHub Actions di `.github/workflows/build.yml`. Setiap push ke `main` atau tag release akan menghasilkan APK siap instal di Artifacts/Releases.
