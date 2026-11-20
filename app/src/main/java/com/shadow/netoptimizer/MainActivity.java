package com.shadow.netoptimizer;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    TextView status, type, speed, ping, note;
    Button refresh, test, dns, wifi;
    LinearLayout root;
    final String TEST_URL = "https://speed.cloudflare.com/__down?bytes=5000000";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        updateNetwork();
    }

    void buildUi() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 34, 28, 28);
        root.setBackgroundColor(0xFF101820);
        scroll.addView(root);

        TextView title = text("SHADOW NET OPTIMIZER", 25, 0xFF62F5B2);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView subtitle = text("Wi-Fi + Mobile Data diagnostics", 14, 0xFFB9C7D1);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        status = text("Checking connection…", 20, 0xFFFFFFFF);
        type = text("Network: —", 16, 0xFFB9C7D1);
        speed = text("Download speed: Not tested", 18, 0xFFFFFFFF);
        ping = text("Latency: Not tested", 18, 0xFFFFFFFF);
        note = text("This app measures and guides. It cannot override your carrier/router speed limit.", 14, 0xFFFFD27A);
        root.addView(gap()); root.addView(status); root.addView(type);
        root.addView(gap()); root.addView(speed); root.addView(ping);
        root.addView(gap()); root.addView(note); root.addView(gap());

        refresh = button("CHECK CONNECTION");
        test = button("RUN SPEED TEST (5 MB)");
        dns = button("OPEN PRIVATE DNS SETTINGS");
        wifi = button("OPEN WI-FI / NETWORK SETTINGS");
        root.addView(refresh); root.addView(test); root.addView(dns); root.addView(wifi);

        refresh.setOnClickListener(v -> updateNetwork());
        test.setOnClickListener(v -> runTest());
        dns.setOnClickListener(v -> openSettings("dns"));
        wifi.setOnClickListener(v -> openSettings("wifi"));
        setContentView(scroll);
    }

    TextView text(String s, int size, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size);
        t.setTextColor(color); t.setPadding(0, 10, 0, 10); return t;
    }
    View gap() { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, 18)); return v; }
    Button button(String s) {
        Button b = new Button(this); b.setText(s); b.setTextColor(0xFF07130F);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF62F5B2));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 8, 0, 8); b.setLayoutParams(p); return b;
    }

    void updateNetwork() {
        try {
            ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n = cm.getActiveNetwork();
            NetworkCapabilities c = n == null ? null : cm.getNetworkCapabilities(n);
            if (c == null) {
                status.setText("No active internet network detected");
                type.setText("Network: Offline / connecting");
                return;
            }
            boolean internet = c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            boolean validated = c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            String kind = c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ? "Wi-Fi" :
                c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ? "Mobile Data" :
                c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ? "Ethernet" : "Other";
            type.setText("Network: " + kind);
            status.setText(internet && validated ? "Internet connection active" :
                internet ? "Network connected; internet not yet verified" : "No internet capability");
        } catch (Exception e) {
            status.setText("Could not read network status");
        }
    }

    void runTest() {
        test.setEnabled(false);
        speed.setText("Download speed: Testing… (5 MB)");
        ping.setText("Latency: Checking…");
        new Thread(() -> {
            long latency = -1, start = 0, end = 0;
            double mbps = 0;
            try {
                long p0 = System.currentTimeMillis();
                HttpURLConnection pc = (HttpURLConnection)new URL("https://www.google.com/generate_204").openConnection();
                pc.setConnectTimeout(7000); pc.setReadTimeout(7000); pc.setInstanceFollowRedirects(false);
                pc.getResponseCode(); latency = System.currentTimeMillis() - p0; pc.disconnect();

                start = System.nanoTime();
                HttpURLConnection dc = (HttpURLConnection)new URL(TEST_URL).openConnection();
                dc.setConnectTimeout(10000); dc.setReadTimeout(15000);
                byte[] buf = new byte[8192]; int count, total = 0;
                java.io.InputStream in = dc.getInputStream();
                while ((count = in.read(buf)) != -1) total += count;
                in.close(); dc.disconnect();
                end = System.nanoTime();
                double seconds = (end - start) / 1_000_000_000.0;
                if (seconds > 0) mbps = (total * 8.0) / seconds / 1_000_000.0;
                final double result = mbps; final long lat = latency; final int bytes = total;
                runOnUiThread(() -> {
                    speed.setText(String.format(java.util.Locale.US, "Download speed: %.2f Mbps", result));
                    ping.setText(lat >= 0 ? "Latency to test server: " + lat + " ms" : "Latency: Test failed");
                    note.setText("Measured " + bytes/1024 + " KB. Results vary by signal, congestion and test server.");
                    test.setEnabled(true);
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    speed.setText("Speed test failed — check your connection");
                    ping.setText("Latency: Unavailable");
                    note.setText("Speed test needs internet access. Try again later.");
                    test.setEnabled(true);
                });
            }
        }).start();
    }

    void openSettings(String which) {
        try {
            Intent i;
            if (which.equals("dns") && android.os.Build.VERSION.SDK_INT >= 28) {
                i = new Intent("android.settings.PRIVATE_DNS_SETTINGS");
            } else if (which.equals("wifi")) {
                i = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            } else {
                i = new Intent(Settings.ACTION_SETTINGS);
            }
            startActivity(i);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }
}