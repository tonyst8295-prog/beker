package com.pos.printerbridge;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections;

public class MainActivity extends AppCompatActivity {

    // ---------------------------------------------------------------
    // Diarahkan ke domain kamu. Kalau halaman nota-nya ada di path
    // tertentu (bukan root), tambahkan path-nya, misal:
    //   "https://sbcngoro.net/pos/cetak_nota.php?id_transaksi=123"
    // ---------------------------------------------------------------
    private static final String SERVER_URL = "https://sbcngoro.net/";

    private static final String PREF_NAME = "cetak_nota_prefs";
    private static final String PREF_KEY_MAC = "printer_mac";
    static final String PREF_KEY_LEBAR_KERTAS = "lebar_kertas_mm";
    private static final int REQ_BLUETOOTH_PERMISSION = 101;

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar kustom - logo SBC Group + tulisan "SBC" sejajar di kiri.
        // Tidak pakai toolbar.setLogo()/setTitle() bawaan karena hasilnya tidak
        // konsisten di sebagian HP (logo malah ke tengah, tulisan hilang) -
        // jadi ditempel manual pakai View sendiri supaya posisinya pasti rata kiri.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // Matikan judul otomatis (nama app "sbc" dari strings.xml) supaya
            // tidak dobel dengan logo+"SBC" custom yang ditempel di bawah ini.
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        View brandView = getLayoutInflater().inflate(R.layout.toolbar_brand, toolbar, false);
        Toolbar.LayoutParams brandParams = new Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL);
        toolbar.addView(brandView, brandParams);

        webView = findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        // PENTING: tanpa WebChromeClient ini, window.alert()/confirm() dari
        // JavaScript di halaman PHP tidak akan menampilkan apa-apa sama sekali
        // (WebView diam-diam mengabaikannya kalau tidak ditangani manual).
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setPositiveButton("OK", (dialog, which) -> result.confirm())
                        .setCancelable(false)
                        .show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setPositiveButton("OK", (dialog, which) -> result.confirm())
                        .setNegativeButton("Batal", (dialog, which) -> result.cancel())
                        .setCancelable(false)
                        .show();
                return true;
            }
        });
        // "Android" di sini adalah nama objek yang dipanggil dari JavaScript:
        // window.Android.cetakNota(teks)
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        webView.loadUrl(SERVER_URL);

        mintaIzinBluetooth();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Pilih Printer");
        menu.add("Ukuran Kertas");
        menu.add("Muat Ulang Halaman");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String judul = String.valueOf(item.getTitle());
        if ("Pilih Printer".equals(judul)) {
            pilihPrinter();
            return true;
        } else if ("Ukuran Kertas".equals(judul)) {
            pilihUkuranKertas();
            return true;
        } else if ("Muat Ulang Halaman".equals(judul)) {
            webView.reload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Android 12+ (API 31) mewajibkan izin Bluetooth diminta saat runtime, tidak cukup di manifest. */
    private void mintaIzinBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean sudahConnect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
            boolean sudahScan = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED;

            if (!sudahConnect || !sudahScan) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                }, REQ_BLUETOOTH_PERMISSION);
            }
        }
        // Android 11 ke bawah: BLUETOOTH & BLUETOOTH_ADMIN adalah izin normal,
        // otomatis aktif dari manifest saja tanpa perlu dialog izin.
    }

    /** Menu untuk memilih printer Bluetooth mana yang dipakai (kalau ada lebih dari satu di-pairing). */
    @SuppressLint("MissingPermission") // sudah dicek lewat mintaIzinBluetooth() di atas
    private void pilihPrinter() {
        BluetoothConnection[] daftar = new BluetoothPrintersConnections().getList();

        if (daftar == null || daftar.length == 0) {
            Toast.makeText(this,
                    "Belum ada printer Bluetooth yang di-pairing. Pairing dulu lewat Settings Bluetooth HP.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String[] namaDevice = new String[daftar.length];
        for (int i = 0; i < daftar.length; i++) {
            namaDevice[i] = daftar[i].getDevice().getName() + "  (" + daftar[i].getDevice().getAddress() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Pilih Printer")
                .setItems(namaDevice, (dialog, index) -> {
                    String mac = daftar[index].getDevice().getAddress();
                    getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                            .edit()
                            .putString(PREF_KEY_MAC, mac)
                            .apply();
                    Toast.makeText(this, "Printer dipilih: " + namaDevice[index], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    /** Menu untuk memilih ukuran kertas printer: 58mm atau 80mm. */
    private void pilihUkuranKertas() {
        String[] pilihan = {"58mm", "80mm"};
        int ukuranSekarang = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getInt(PREF_KEY_LEBAR_KERTAS, 80);
        int indexSekarang = (ukuranSekarang == 58) ? 0 : 1;

        new AlertDialog.Builder(this)
                .setTitle("Pilih Ukuran Kertas")
                .setSingleChoiceItems(pilihan, indexSekarang, (dialog, index) -> {
                    int mm = (index == 0) ? 58 : 80;
                    getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                            .edit()
                            .putInt(PREF_KEY_LEBAR_KERTAS, mm)
                            .apply();
                    Toast.makeText(this, "Ukuran kertas: " + mm + "mm", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .show();
    }

    /**
     * Dipanggil dari WebAppInterface (thread cetak, bukan main thread).
     * Cari koneksi ke printer yang sudah dipilih lewat menu "Pilih Printer";
     * kalau belum pernah pilih, jatuh ke printer pertama yang di-pairing.
     */
    @SuppressLint("MissingPermission")
    BluetoothConnection getPrinterConnection() {
        String mac = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString(PREF_KEY_MAC, null);
        BluetoothConnection[] daftar = new BluetoothPrintersConnections().getList();

        if (daftar != null && mac != null) {
            for (BluetoothConnection koneksi : daftar) {
                if (mac.equals(koneksi.getDevice().getAddress())) {
                    return koneksi;
                }
            }
        }
        return BluetoothPrintersConnections.selectFirstPaired();
    }

    /** Dipanggil dari WebAppInterface untuk tahu ukuran kertas yang dipilih (default 80mm). */
    int getLebarKertasMm() {
        return getSharedPreferences(PREF_NAME, MODE_PRIVATE).getInt(PREF_KEY_LEBAR_KERTAS, 80);
    }

    /** Tampilkan pesan (Toast) dari thread manapun - dipanggil WebAppInterface setelah selesai cetak. */
    void tampilkanPesan(String pesan) {
        runOnUiThread(() -> Toast.makeText(this, pesan, Toast.LENGTH_LONG).show());
    }
}
