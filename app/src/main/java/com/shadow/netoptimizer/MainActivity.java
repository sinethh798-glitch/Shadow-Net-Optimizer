package com.shadow.netoptimizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG=0xFF090F16, PANEL=0xFF121D27, PANEL2=0xFF172631;
    private static final int GREEN=0xFF4BF0B2, CYAN=0xFF4BCDF5, WHITE=0xFFEFF7FA, MUTED=0xFF9EB2BE;
    private TextView networkValue, networkStatus, speedValue, pingValue, goalText, testNote;
    private ProgressBar goalBar;
    private Button testButton;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildDashboard();
        refreshNetwork();
    }

    private void buildDashboard() {
        ScrollView scroll=new ScrollView(this);
        LinearLayout page=new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20),dp(24),dp(20),dp(28));
        page.setBackgroundColor(BG);
        scroll.addView(page);

        LinearLayout header=new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo=label("S",22,BG,true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(round(GREEN,50));
        header.addView(logo,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout brand=new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(12),0,0,0);
        brand.addView(label("SHADOW NET",18,WHITE,true));
        brand.addView(label("NETWORK CONTROL CENTER",10,MUTED,true));
        header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView ver=label("V2.0",11,GREEN,true);
        ver.setPadding(dp(10),dp(7),dp(10),dp(7));
        ver.setBackground(round(PANEL2,30));
        header.addView(ver);
        page.addView(header);
        space(page,22);

        LinearLayout net=card(page,PANEL);
        net.addView(label("CURRENT CONNECTION",11,MUTED,true));
        LinearLayout netRow=new LinearLayout(this);
        netRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView dot=label("●",19,GREEN,true);
        netRow.addView(dot);
        networkValue=label("Checking network…",19,WHITE,true);
        networkValue.setPadding(dp(9),0,0,0);
        netRow.addView(networkValue);
        net.addView(netRow);
        networkStatus=label("Checking internet availability",12,MUTED,false);
        networkStatus.setPadding(0,dp(7),0,0);
        net.addView(networkStatus);

        space(page,16);
        LinearLayout speed=card(page,PANEL2);
        speed.addView(label("DOWNLOAD SPEED",11,MUTED,true));
        LinearLayout speedRow=new LinearLayout(this);
        speedRow.setGravity(Gravity.BOTTOM);
        speedValue=label("—",44,GREEN,true);
        speedRow.addView(speedValue);
        TextView unit=label(" Mbps",15,WHITE,true);
        speedRow.addView(unit);
        speed.addView(speedRow);
        goalText=label("Target: 10 Mbps · Run a test to measure",12,MUTED,false);
        goalText.setPadding(0,dp(6),0,dp(9));
        speed.addView(goalText);
        goalBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        goalBar.setMax(100);
        goalBar.setProgressTintList(ColorStateList.valueOf(GREEN));
        goalBar.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF2F424C));
        speed.addView(goalBar,new LinearLayout.LayoutParams(-1,dp(7)));

        space(page,12);
        LinearLayout metrics=new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout pingCard=smallCard(metrics);
        pingCard.addView(label("LATENCY",10,MUTED,true));
        pingValue=label("— ms",24,CYAN,true);
        pingValue.setPadding(0,dp(8),0,0);
        pingCard.addView(pingValue);
        TextView pingCaption=label("Test server response",10,MUTED,false);
        pingCaption.setPadding(0,dp(4),0,0);
        pingCard.addView(pingCaption);
        LinearLayout stateCard=smallCard(metrics);
        stateCard.addView(label("CONNECTION",10,MUTED,true));
        TextView ready=label("READY",22,GREEN,true);
        ready.setPadding(0,dp(8),0,0);
        stateCard.addView(ready);
        TextView stateCaption=label("Network status",10,MUTED,false);
        stateCaption.setPadding(0,dp(4),0,0);
        stateCard.addView(stateCaption);
        page.addView(metrics);

        space(page,20);
        page.addView(label("NETWORK TOOLS",12,WHITE,true));
        testButton=button("RUN SPEED TEST  ·  5 MB",GREEN,BG);
        page.addView(testButton);
        testButton.setOnClickListener(v->runSpeedTest());
        Button check=button("REFRESH CONNECTION",PANEL2,WHITE);
        page.addView(check);
        check.setOnClickListener(v->refreshNetwork());
        Button guide=button("OPTIMIZATION GUIDE",PANEL2,WHITE);
        page.addView(guide);
        guide.setOnClickListener(v->showGuide());
        Button dns=button("OPEN PRIVATE DNS SETTINGS",PANEL2,WHITE);
        page.addView(dns);
        dns.setOnClickListener(v->openSettings("dns"));
        Button wifi=button("WI-FI / NETWORK SETTINGS",PANEL2,WHITE);
        page.addView(wifi);
        wifi.setOnClickListener(v->openSettings("network"));
        space(page,14);
        testNote=label("Speed tests use data. Results depend on your plan, signal, router, congestion and test server.",11,MUTED,false);
        testNote.setGravity(Gravity.CENTER);
        page.addView(testNote);
        setContentView(scroll);
    }

    private LinearLayout card(LinearLayout parent,int color) {
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(17),dp(16),dp(17),dp(16)); c.setBackground(round(color,18));
        parent.addView(c,new LinearLayout.LayoutParams(-1,-2)); return c;
    }
    private LinearLayout smallCard(LinearLayout parent) {
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14),dp(14),dp(10),dp(14)); c.setBackground(round(PANEL,16));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
        p.setMargins(0,0,dp(6),0); parent.addView(c,p); return c;
    }
    private Button button(String text,int bg,int fg) {
        Button b=new Button(this); b.setText(text); b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setTextColor(fg);
        b.setAllCaps(false); b.setBackground(round(bg,14));
        b.setPadding(dp(12),dp(8),dp(12),dp(8));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(54));
        p.setMargins(0,dp(5),0,dp(5)); b.setLayoutParams(p); return b;
    }
    private TextView label(String s,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private GradientDrawable round(int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private void space(LinearLayout p,int h) {
        View v=new View(this); p.addView(v,new LinearLayout.LayoutParams(1,dp(h)));
    }
    private int dp(int v) { return (int)(v*getResources().getDisplayMetrics().density+0.5f); }

    private void refreshNetwork() {
        try {
            ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n=cm.getActiveNetwork();
            NetworkCapabilities c=n==null?null:cm.getNetworkCapabilities(n);
            if(c==null){networkValue.setText("No active network");networkStatus.setText("Connect to Wi-Fi or mobile data");return;}
            boolean internet=c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            boolean valid=c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            String kind=c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"Wi-Fi":
                c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)?"Mobile Data":
                c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)?"Ethernet":"Other network";
            networkValue.setText(kind);
            networkStatus.setText(valid?"Internet connected and verified":
                internet?"Network connected · internet not verified":"No internet capability detected");
        } catch(Exception e){networkValue.setText("Network status unavailable");networkStatus.setText("Check network settings");}
    }

    private void runSpeedTest() {
        testButton.setEnabled(false); testButton.setText("TEST RUNNING…");
        speedValue.setText("…"); pingValue.setText("…");
        testNote.setText("Downloading about 5 MB. This uses your data allowance.");
        new Thread(()->{
            long latency=-1; double mbps=0; int total=0;
            try {
                long p0=System.currentTimeMillis();
                HttpURLConnection pc=(HttpURLConnection)new URL("https://www.google.com/generate_204").openConnection();
                pc.setConnectTimeout(8000); pc.setReadTimeout(8000); pc.getResponseCode();
                latency=System.currentTimeMillis()-p0; pc.disconnect();
                long start=System.nanoTime();
                HttpURLConnection dc=(HttpURLConnection)new URL("https://speed.cloudflare.com/__down?bytes=5000000").openConnection();
                dc.setConnectTimeout(10000); dc.setReadTimeout(20000);
                byte[] buf=new byte[8192]; int count; java.io.InputStream in=dc.getInputStream();
                while((count=in.read(buf))!=-1)total+=count;
                in.close(); dc.disconnect();
                double seconds=(System.nanoTime()-start)/1_000_000_000.0;
                if(seconds>0)mbps=total*8.0/seconds/1_000_000.0;
            } catch(Exception ignored) {}
            final long lat=latency; final double result=mbps; final int bytes=total;
            runOnUiThread(()->{
                if(bytes>0){
                    speedValue.setText(String.format(Locale.US,"%.2f",result));
                    int progress=(int)Math.min(100,Math.max(0,result/10.0*100.0));
                    goalBar.setProgress(progress);
                    goalText.setText(result>=10?"10 Mbps target reached · "+progress+"% of target":
                        String.format(Locale.US,"%.1f%% of 10 Mbps target",result/10.0*100.0));
                    testNote.setText("Measured "+(bytes/1024)+" KB · Approximate result; network conditions affect accuracy.");
                } else {
                    speedValue.setText("Error"); goalBar.setProgress(0);
                    goalText.setText("Test failed. Check internet and retry.");
                    testNote.setText("Speed test could not complete. Try again later.");
                }
                pingValue.setText(lat>=0?lat+" ms":"— ms");
                testButton.setEnabled(true); testButton.setText("RUN SPEED TEST  ·  5 MB");
            });
        }).start();
    }

    private void showGuide() {
        String msg = "Wi-Fi\\n" +
            "• Move closer to the router and avoid walls.\\n" +
            "• Pause large downloads on other devices.\\n" +
            "• Restart the router if the connection is unstable.\\n\\n" +
            "Mobile data\\n" +
            "• Test near a window or where signal is stronger.\\n" +
            "• Check your package speed limit and remaining data.\\n" +
            "• Toggle Airplane mode briefly only if the network is stuck.\\n\\n" +
            "Private DNS can improve privacy or name-lookup reliability, but it does not guarantee higher Mbps.";
        new AlertDialog.Builder(this).setTitle("Network optimization guide").setMessage(msg)
            .setPositiveButton("OPEN NETWORK SETTINGS",(d,w)->openSettings("network"))
            .setNegativeButton("CLOSE",null).show();
    }
    private void openSettings(String which) {
        try {
            Intent i;
            if("dns".equals(which)&&android.os.Build.VERSION.SDK_INT>=28)i=new Intent("android.settings.PRIVATE_DNS_SETTINGS");
            else if("network".equals(which))i=new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            else i=new Intent(Settings.ACTION_SETTINGS);
            startActivity(i);
        } catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}
    }
}
