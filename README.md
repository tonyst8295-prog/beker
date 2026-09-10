# Cetak Nota - Aplikasi Bridge WebView + Bluetooth Thermal Printer

Aplikasi Android minimal yang membungkus halaman PHP sistem stok kamu di
dalam WebView, ditambah jembatan JavaScript untuk mencetak nota langsung ke
printer thermal 80mm lewat Bluetooth - tanpa perlu app print service
berbayar seperti RawBT.

Library yang dipakai: [ESCPOS-ThermalPrinter-Android](https://github.com/DantSu/ESCPOS-ThermalPrinter-Android)
(open source, lisensi MIT, gratis dipakai termasuk untuk keperluan komersial).

## Cara build

`SERVER_URL` di `MainActivity.java` sudah diarahkan ke `https://sbcngoro.net/`.
Kalau halaman nota-nya ada di path tertentu (bukan halaman utama domain), buka
file itu dan tambahkan path-nya, misal:
```java
private static final String SERVER_URL = "https://sbcngoro.net/pos/cetak_nota.php?id=123";
```

Ada dua cara dapat file `.apk`-nya - pilih salah satu:

### Opsi A - Android Studio (kalau sudah/mau install di komputer)
1. Buka Android Studio &rarr; **Open** &rarr; pilih folder `cetak-nota-android` ini.
2. Tunggu Gradle sync selesai (otomatis download library dari JitPack, butuh internet).
3. Jalankan ke HP Android (lewat kabel USB + mode developer aktif), atau
   **Build > Build Bundle(s) / APK(s) > Build APK(s)** untuk dapat file `.apk`
   yang bisa dikirim & di-install manual ke HP lain (sideload, tidak perlu Play Store).

### Opsi B - GitHub Actions (tanpa install apapun di komputer)
Project ini sudah dilengkapi `.github/workflows/build-apk.yml` yang otomatis
meng-compile APK di server GitHub (gratis) setiap kali di-push:
1. Buat repository baru di GitHub (boleh private), upload/push seluruh isi folder ini.
2. Buka tab **Actions** di repo tersebut - build akan berjalan otomatis (~2-3 menit).
3. Setelah selesai (tanda centang hijau), klik run yang selesai itu &rarr; bagian
   **Artifacts** di bawah &rarr; download `cetak-nota-debug-apk` (file zip berisi .apk).
4. Kirim file `.apk` itu ke HP dan install manual (sideload).

> Catatan: APK dari opsi ini adalah **debug build** (belum ditandatangani untuk
> rilis) - cukup untuk dipakai sendiri di toko, tapi kalau nanti mau publish ke
> Play Store perlu proses signing release APK terpisah.

## Cara pakai di HP toko

1. Pairing printer thermal lewat **Settings > Bluetooth** Android seperti biasa
   (sekali saja, seperti pairing headset).
2. Install & buka aplikasi ini.
3. Kalau printernya lebih dari satu yang pernah di-pairing, buka menu titik-tiga
   di pojok kanan atas &rarr; **Pilih Printer** &rarr; pilih yang benar.
4. Tekan tombol cetak di halaman web (di halaman contoh: "Cetak Nota Contoh").
5. Kalau berhasil akan muncul notifikasi "Nota berhasil dicetak." dan kertas keluar.

## Menyambungkan ke halaman PHP asli

Di halaman PHP kamu (misal `cetak_nota.php`), tombol cetak jangan panggil
`window.print()` langsung, tapi cek dulu ada jembatan Android atau tidak:

```js
function cetak() {
    var teks = susunTeksNota(); // fungsi kamu sendiri, pakai data dari mutasi_stok/penjualan

    if (window.Android) {
        window.Android.cetakNota(teks);   // dicetak lewat printer Bluetooth
    } else {
        window.print();                   // fallback kalau dibuka di browser biasa (mis. dites di PC)
    }
}
```

Format teks nota pakai tag perataan yang dikenali library-nya:

| Tag   | Arti          |
|-------|---------------|
| `[L]` | rata kiri     |
| `[C]` | rata tengah   |
| `[R]` | rata kanan    |

Beberapa tag di baris yang sama = kolom terpisah, contoh:
`"[L]Kabel USB-C[R]30.000\n"` &rarr; nama barang rata kiri, harga rata kanan
di baris yang sama. Lihat contoh lengkapnya di `app/src/main/assets/test_nota.html`.

## Kalau hasil cetak kepotong / font terlalu kecil-besar

Buka `WebAppInterface.java`, sesuaikan tiga konstanta di atas:

```java
private static final int PRINTER_DPI = 203;
private static final float PRINTER_WIDTH_MM = 80f;
private static final int PRINTER_CHARS_PER_LINE = 48;
```

`PRINTER_CHARS_PER_LINE` yang paling sering perlu disesuaikan - coba naik/turunkan
beberapa angka (mis. 42-48) sampai hasilnya pas dengan printer kamu.

## Struktur file

```
cetak-nota-android/
├── settings.gradle, build.gradle, gradle.properties   - konfigurasi project
└── app/
    ├── build.gradle                                    - dependency library ESC/POS
    └── src/main/
        ├── AndroidManifest.xml                         - izin Bluetooth
        ├── assets/test_nota.html                        - halaman contoh untuk uji coba
        ├── java/com/pos/printerbridge/
        │   ├── MainActivity.java                        - WebView + menu pilih printer
        │   └── WebAppInterface.java                      - jembatan JS -> Bluetooth printer
        └── res/                                          - layout & tema
```

## Catatan

- Tidak wajib publish ke Play Store - cukup bagikan file `.apk` hasil build dan
  install manual (sideload) di HP toko manapun, gratis tanpa batas jumlah HP/toko.
- Kalau printernya ternyata tipe Bluetooth Low Energy (BLE) bukan Classic/SPP,
  atau tipe USB/WiFi, library ini juga punya dukungan untuk itu (`UsbConnection`,
  `TcpConnection`) - beri tahu saya kalau butuh contoh untuk tipe itu.
