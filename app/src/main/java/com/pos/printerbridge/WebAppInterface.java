package com.pos.printerbridge;

import android.webkit.JavascriptInterface;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;

/**
 * Jembatan antara halaman web (PHP) dan printer Bluetooth.
 * Dipanggil dari JavaScript di halaman web:
 *
 *   window.Android.cetakNota(teks)
 *
 * "teks" pakai tag alignment dari library ESCPOS-ThermalPrinter-Android:
 *   [L] rata kiri   [C] rata tengah   [R] rata kanan
 * Lihat contoh formatnya di assets/test_nota.html
 */
public class WebAppInterface {

    private final MainActivity activity;

    // DPI umum untuk hampir semua printer thermal (58mm maupun 80mm).
    // Kalau hasil cetaknya masih kurang pas, coba sesuaikan angka ini.
    private static final int PRINTER_DPI = 203;

    // Karakter per baris untuk font normal - ukuran kertas dipilih lewat
    // menu "Ukuran Kertas" di aplikasi (disimpan, tidak perlu pilih ulang tiap cetak).
    private static final int CHARS_PER_LINE_58MM = 32;
    private static final int CHARS_PER_LINE_80MM = 48;

    WebAppInterface(MainActivity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void cetakNota(String teksNota) {
        // PENTING: koneksi & tulis ke Bluetooth tidak boleh dijalankan di main
        // thread (bisa macet/ANR) - makanya dibungkus Thread terpisah di sini.
        new Thread(() -> {
            try {
                BluetoothConnection koneksi = activity.getPrinterConnection();
                if (koneksi == null) {
                    activity.tampilkanPesan("Printer belum di-pairing. Buka menu \"Pilih Printer\" dulu.");
                    return;
                }

                int lebarMm = activity.getLebarKertasMm();
                int charsPerLine = (lebarMm == 58) ? CHARS_PER_LINE_58MM : CHARS_PER_LINE_80MM;

                EscPosPrinter printer = new EscPosPrinter(
                        koneksi, PRINTER_DPI, (float) lebarMm, charsPerLine
                );
                printer.printFormattedTextAndCut(teksNota);
                printer.disconnectPrinter();

                activity.tampilkanPesan("Nota berhasil dicetak.");
            } catch (Exception e) {
                activity.tampilkanPesan("Gagal mencetak: " + e.getMessage());
            }
        }).start();
    }
}
