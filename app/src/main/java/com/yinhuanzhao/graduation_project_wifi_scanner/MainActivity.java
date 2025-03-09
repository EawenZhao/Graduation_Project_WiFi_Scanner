package com.yinhuanzhao.graduation_project_wifi_scanner;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private ListView listView;
    private Button btnScan;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> scanResultsList;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 0;

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                getScanResults();
            } else {
                Toast.makeText(MainActivity.this, "扫描失败，请重试", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        btnScan = findViewById(R.id.btnScan);
        scanResultsList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, scanResultsList);
        listView.setAdapter(adapter);

        // 获取Wi-Fi服务
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            Toast.makeText(this, "WiFi服务不可用", Toast.LENGTH_LONG).show();
            return;
        }

        // 如果Wi-Fi未开启，则开启
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "WiFi未开启，正在开启...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        // 注册接收扫描结果的BroadcastReceiver
        IntentFilter intentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, intentFilter);

        // 检查定位权限（扫描Wi-Fi需要定位权限）
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        // 按钮点击事件，触发Wi-Fi扫描
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWifiScan();
            }
        });
    }

    // 发起Wi-Fi扫描
    private void startWifiScan() {
        boolean success = wifiManager.startScan();
        if (!success) {
            Toast.makeText(this, "发起扫描失败", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "正在扫描...", Toast.LENGTH_SHORT).show();
        }
    }

    // 处理扫描结果
    private void getScanResults() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        List<ScanResult> results = wifiManager.getScanResults();
        scanResultsList.clear();
        for (ScanResult result : results) {
            String info = "SSID: " + result.SSID + "\nBSSID: " + result.BSSID + "\nRSSI: " + result.level;
            scanResultsList.add(info);
        }
        adapter.notifyDataSetChanged();
    }

    // 处理权限请求回调
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "权限已授予，请点击按钮扫描Wi-Fi", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "定位权限未授予，无法扫描Wi-Fi", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiScanReceiver);
    }
}