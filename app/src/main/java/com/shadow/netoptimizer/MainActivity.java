package com.shadow.netoptimizer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int BG=0xFF080E15, PANEL=0xFF121D28, PANEL2=0xFF182633;
    private static final int GREEN=0xFF45E6A8, CYAN=0xFF4CCBFA, WHITE=0xFFEAF3F7, MUTED=0xFF9AAEBB, RED=0xFFFF7272;
    private TextView networkValue, networkStatus, downValue, upValue, pingValue, jitterValue, wifiValue, stabilityValue, historyText, note;
    private Button diagnosticButton;
    private TrafficGraph graph;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private long lastRx=-1,lastTx=-1,lastTick=0;
    private long lastPing=-1;
    private final ArrayDeque<Float> downHistory=new ArrayDeque<>();
    private final ArrayDeque<Float> upHistory=new ArrayDeque<>();
    private final ArrayList<String> history=new ArrayList<>();
    private int stableSamples=0, unstableSamples=0;
    private boolean monitoring=true;

    private final Runnable monitor=new Runnable() {
        @Override public void run() {
            if(!monitoring)return;
            long now=System.currentTimeMillis(), rx=TrafficStats.getTotalRxBytes(), tx=TrafficStats.getTotalTxBytes();
            if(lastTick>0 && rx>=lastRx && tx>=lastTx) {
                double sec=(now-lastTick)/1000.0;
                float down=(float)((rx-lastRx)*8.0/sec/1_000_000.0);
                float up=(float)((tx-lastTx)*8.0/sec/1_000_000.0);
                downValue.setText(String.format(Locale.US,"%.2f",down));
                upValue.setText(String.format(Locale.US,"%.2f",up));
                push(downHistory,down); push(upHistory,up); graph.setData(new ArrayList<>(downHistory),new ArrayList<>(upHistory));
                if(down<0.05f && up<0.05f) stableSamples++; else stableSamples=Math.max(0,stableSamples-1);
                stabilityValue.setText(stableSamples>=5?"IDLE / STABLE":"MONITORING");
            } else {
                downValue.setText("0.00"); upValue.setText("0.00");
            }
            lastRx=rx; lastTx=tx; lastTick=now;
            updateNetwork();
            updateWifi();
            handler.postDelayed(this,1000);
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        buildUI();
    }
    @Override protected void onResume(){super.onResume(); monitoring=true; lastTick=0; handler.removeCallbacks(monitor); handler.post(monitor);}
    @Override protected void onPause(){super.onPause(); monitoring=false; handler.removeCallbacks(monitor);}
    @Override protected void onDestroy(){super.onDestroy();worker.shutdownNow();}

    private void buildUI() {
        ScrollView scroll=new ScrollView(this);
        LinearLayout page=new LinearLayout(this); page.setOrientation(1); page.setPadding(dp(18),dp(22),dp(18),dp(28)); page.setBackgroundColor(BG); scroll.addView(page);
        LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark=label("S",22,BG,true); mark.setGravity(Gravity.CENTER); mark.setBackground(round(GREEN,15)); head.addView(mark,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout brand=new LinearLayout(this); brand.setOrientation(1); brand.setPadding(dp(11),0,0,0);
        brand.addView(label("SHADOW NET",18,WHITE,true)); brand.addView(label("LIVE NETWORK MONITOR",10,MUTED,true));
        head.addView(brand,new LinearLayout.LayoutParams(0,-2,1)); TextView ver=label("V3.0",11,GREEN,true); ver.setPadding(dp(10),dp(7),dp(10),dp(7)); ver.setBackground(round(PANEL2,20)); head.addView(ver); page.addView(head);
        space(page,20);
        LinearLayout connection=card(page,PANEL); connection.addView(label("ACTIVE CONNECTION",10,MUTED,true));
        networkValue=label("Checking…",20,WHITE,true); networkValue.setPadding(0,dp(7),0,0); connection.addView(networkValue);
        networkStatus=label("Reading network state",12,MUTED,false); networkStatus.setPadding(0,dp(4),0,0); connection.addView(networkStatus);
        space(page,12);
        LinearLayout rates= new LinearLayout(this); rates.setOrientation(0);
        LinearLayout d=smallCard(rates); d.addView(label("LIVE DOWNLOAD",10,MUTED,true)); downValue=label("0.00",34,GREEN,true); downValue.setPadding(0,dp(5),0,0); d.addView(downValue); d.addView(label("Mbps",11,MUTED,false));
        LinearLayout u=smallCard(rates); u.addView(label("LIVE UPLOAD",10,MUTED,true)); upValue=label("0.00",34,CYAN,true); upValue.setPadding(0,dp(5),0,0); u.addView(upValue); u.addView(label("Mbps",11,MUTED,false)); page.addView(rates);
        space(page,12);
        LinearLayout chart=card(page,PANEL); chart.addView(label("REAL-TIME TRAFFIC",11,WHITE,true)); space(chart,6);
        graph=new TrafficGraph(this); chart.addView(graph,new LinearLayout.LayoutParams(-1,dp(145)));
        LinearLayout legend=new LinearLayout(this); legend.setGravity(Gravity.CENTER); legend.addView(label("● Download",10,GREEN,true)); TextView gap=label("     ",10,MUTED,false); legend.addView(gap); legend.addView(label("● Upload",10,CYAN,true)); chart.addView(legend);
        space(page,12);
        LinearLayout metrics=new LinearLayout(this); metrics.setOrientation(0);
        LinearLayout p=smallCard(metrics); p.addView(label("PING",10,MUTED,true)); pingValue=label("-- ms",23,CYAN,true); pingValue.setPadding(0,dp(7),0,0); p.addView(pingValue);
        LinearLayout j=smallCard(metrics); j.addView(label("JITTER",10,MUTED,true)); jitterValue=label("-- ms",23,CYAN,true); jitterValue.setPadding(0,dp(7),0,0); j.addView(jitterValue);
        page.addView(metrics);
        space(page,12);
        LinearLayout wifi=card(page,PANEL); wifi.addView(label("WI-FI SIGNAL",10,MUTED,true)); wifiValue=label("Not connected / unavailable",16,WHITE,true); wifiValue.setPadding(0,dp(7),0,0); wifi.addView(wifiValue);
        space(page,12);
        LinearLayout stable=card(page,PANEL); stable.addView(label("CONNECTION STABILITY",10,MUTED,true)); stabilityValue=label("MONITORING",18,GREEN,true); stabilityValue.setPadding(0,dp(7),0,0); stable.addView(stabilityValue);
        space(page,16);
        page.addView(label("TOOLS & DIAGNOSTICS",11,WHITE,true));
        diagnosticButton=button("RUN ONE-TAP DIAGNOSTICS",GREEN,BG); page.addView(diagnosticButton); diagnosticButton.setOnClickListener(v->runDiagnostics());
        Button hist=button("SAVE CURRENT SPEED SNAPSHOT",PANEL2,WHITE); page.addView(hist); hist.setOnClickListener(v->saveSnapshot());
        historyText=label("No saved measurements yet.",11,MUTED,false); historyText.setPadding(dp(4),dp(8),dp(4),dp(8)); page.addView(historyText);
        Button wifiSettings=button("OPEN WI-FI SETTINGS",PANEL2,WHITE); page.addView(wifiSettings); wifiSettings.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        Button networkSettings=button("OPEN NETWORK SETTINGS",PANEL2,WHITE); page.addView(networkSettings); networkSettings.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)));
        note=label("Live rates use Android device-wide traffic counters and may include other apps. Ping and diagnostics use small network requests. Results depend on your ISP, signal and network congestion.",10,MUTED,false);
        note.setGravity(Gravity.CENTER); note.setPadding(dp(4),dp(12),dp(4),0); page.addView(note);
        setContentView(scroll);
    }
    private void runDiagnostics(){
        diagnosticButton.setEnabled(false); diagnosticButton.setText("DIAGNOSTICS RUNNING…");
        worker.execute(()->{
            ArrayList<Long> samples=new ArrayList<>(); String err="";
            for(int i=0;i<5;i++){
                long t=System.currentTimeMillis();
                try{
                    HttpURLConnection c=(HttpURLConnection)new URL("https://www.google.com/generate_204").openConnection();
                    c.setConnectTimeout(5000); c.setReadTimeout(5000); c.setUseCaches(false); c.getResponseCode(); c.disconnect();
                    samples.add(System.currentTimeMillis()-t);
                }catch(Exception e){err="Some test requests failed";}
                try{Thread.sleep(250);}catch(InterruptedException ignored){}
            }
            long avg=-1,jitter=-1;
            if(!samples.isEmpty()){
                long sum=0;for(long x:samples)sum+=x;avg=sum/samples.size();
                if(samples.size()>1){long diff=0;for(int i=1;i<samples.size();i++)diff+=Math.abs(samples.get(i)-samples.get(i-1));jitter=diff/(samples.size()-1);}
            }
            final long a=avg, j=jitter; final String error=err;
            runOnUiThread(()->{
                pingValue.setText(a>=0?a+" ms":"Failed"); jitterValue.setText(j>=0?j+" ms":"-- ms");
                lastPing=a;
                String result=a<0?"Diagnostics failed. Check internet connection.":"Average ping: "+a+" ms\nJitter estimate: "+j+" ms\n"+(a<60?"Response: Good":a<120?"Response: Fair":"Response: High latency")+(error.isEmpty()?"":"\n"+error);
                new android.app.AlertDialog.Builder(this).setTitle("Shadow Net Diagnostics").setMessage(result).setPositiveButton("OK",null).show();
                diagnosticButton.setEnabled(true);diagnosticButton.setText("RUN ONE-TAP DIAGNOSTICS");
            });
        });
    }
    private void saveSnapshot(){
        String item="↓ "+downValue.getText()+" / ↑ "+upValue.getText()+" Mbps";
        history.add(0,item); if(history.size()>5)history.remove(history.size()-1);
        StringBuilder b=new StringBuilder("RECENT SNAPSHOTS\n");for(String s:history)b.append("• ").append(s).append("\n");
        historyText.setText(b.toString());
    }
    private void updateNetwork(){
        try{
            ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE); Network n=cm.getActiveNetwork();
            NetworkCapabilities c=n==null?null:cm.getNetworkCapabilities(n);
            if(c==null){networkValue.setText("No active network");networkStatus.setText("Connect to Wi-Fi or mobile data");return;}
            String type=c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"Wi-Fi":c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)?"Mobile Data":c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)?"Ethernet":"Other";
            networkValue.setText(type); networkStatus.setText(c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)?"Internet connected · verified":"Network connected · internet not verified");
        }catch(Exception e){networkStatus.setText("Network state unavailable");}
    }
    @SuppressWarnings("deprecation") private void updateWifi(){
        try{
            ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);Network n=cm.getActiveNetwork();NetworkCapabilities c=n==null?null:cm.getNetworkCapabilities(n);
            if(c==null||!c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){wifiValue.setText("Wi-Fi not active");return;}
            WifiManager wm=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int r=wm.getConnectionInfo().getRssi(); int level=WifiManager.calculateSignalLevel(r,5);
            String quality=level>=4?"Excellent":level==3?"Good":level==2?"Fair":"Weak";
            wifiValue.setText(quality+"  ·  "+r+" dBm");
        }catch(Exception e){wifiValue.setText("Signal details unavailable");}
    }
    private void push(ArrayDeque<Float> q,float v){q.addLast(Math.max(0,v));while(q.size()>40)q.removeFirst();}
    private LinearLayout card(LinearLayout parent,int color){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(15),dp(14),dp(15),dp(14));c.setBackground(round(color,17));parent.addView(c,new LinearLayout.LayoutParams(-1,-2));return c;}
    private LinearLayout smallCard(LinearLayout parent){LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(13),dp(13),dp(8),dp(13));c.setBackground(round(PANEL,15));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(0,0,dp(6),0);parent.addView(c,p);return c;}
    private Button button(String s,int bg,int fg){Button b=new Button(this);b.setText(s);b.setTextSize(12);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(fg);b.setAllCaps(false);b.setBackground(round(bg,13));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.setMargins(0,dp(4),0,dp(4));b.setLayoutParams(p);return b;}
    private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private void space(LinearLayout p,int h){View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}

    private class TrafficGraph extends View {
        private final Paint paint=new Paint(3); private ArrayList<Float> down=new ArrayList<>(),up=new ArrayList<>();
        TrafficGraph(Context c){super(c);}
        void setData(ArrayList<Float>d,ArrayList<Float>u){down=d;up=u;invalidate();}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);float w=getWidth(),h=getHeight(),pad=dp(8);
            paint.setColor(0xFF263744);paint.setStrokeWidth(1);
            for(int i=1;i<=3;i++){float y=pad+(h-2*pad)*i/4f;canvas.drawLine(pad,y,w-pad,y,paint);}
            float max=1;for(float v:down)max=Math.max(max,v);for(float v:up)max=Math.max(max,v);max=Math.max(5,max*1.2f);
            drawLine(canvas,down,w,h,pad,max,GREEN);drawLine(canvas,up,w,h,pad,max,CYAN);
        }
        private void drawLine(Canvas c,ArrayList<Float> values,float w,float h,float pad,float max,int color){
            if(values.size()<2)return;Path path=new Path();float dx=(w-2*pad)/(float)(Math.max(39,values.size()-1));
            for(int i=0;i<values.size();i++){float x=pad+i*dx;float y=h-pad-(values.get(i)/max)*(h-2*pad);if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}
            paint.setColor(color);paint.setStrokeWidth(dp(2));paint.setStyle(Paint.Style.STROKE);c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);
        }
    }
}
