package com.yinhuanzhao.graduation_project_wifi_scanner;

import android.os.Bundle;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private ListView listView;
    private Button btnScan;
    private EditText editTextRefPoint;
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


    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_view_data) {
            // 点击“查看数据”时，启动 DatabaseViewerActivity
            Intent intent = new Intent(MainActivity.this, DatabaseViewerActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        btnScan = findViewById(R.id.btnScan);
        editTextRefPoint = findViewById(R.id.editTextRefPoint);

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

        // 按钮点击事件：先获取参考点ID，再启动扫描
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String refPointStr = editTextRefPoint.getText().toString().trim();
                if (refPointStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "请输入参考点ID", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    final int refPoint = Integer.parseInt(refPointStr);
                    startWifiScan(refPoint);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "参考点ID必须是整数", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // 发起Wi-Fi扫描，同时传入参考点ID
    private void startWifiScan(final int refPoint) {
        boolean success = wifiManager.startScan();
        if (!success) {
            Toast.makeText(this, "发起扫描失败", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "正在扫描...", Toast.LENGTH_SHORT).show();
            // 扫描结果会通过广播回调 onReceive() 处理
            // 此处通过成员变量保存,记录当前参考点ID，以便在 getScanResults() 中使用
            currentRefPoint = refPoint;
        }
    }

    // 用于保存当前扫描时的参考点ID（由startWifiScan传入）
    private int currentRefPoint = -1;

    // 处理扫描结果
    private void getScanResults() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        List<ScanResult> results = wifiManager.getScanResults();

        // 对扫描结果按信号强度排序：数值越大（接近0）表示信号越强；自定义comparator
        results.sort(new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult r1, ScanResult r2) {
                return Integer.compare(r2.level, r1.level);
            }
        });

        scanResultsList.clear();

        // 创建数据库帮助类对象
        WiFiScanDatabaseHelper dbHelper = new WiFiScanDatabaseHelper(this);
        // 获取当前参考点下次扫描的事件号（即第几次扫描）
        int scanEvent = dbHelper.getNextScanEventForRefPoint(currentRefPoint);

        // 将每个扫描结果存入数据库，同时更新ListView
        for (ScanResult result : results) {
            String info = "SSID: " + result.SSID
                    + "\nBSSID: " + result.BSSID
                    + "\nRSSI: " + result.level;
            scanResultsList.add(info);
            // 插入数据库记录
            dbHelper.insertScanResult(currentRefPoint, scanEvent, result.SSID, result.BSSID, result.level);
        }
        adapter.notifyDataSetChanged();

        // 提示当前参考点第几次扫描
        Toast.makeText(this, "参考点 " + currentRefPoint + " 的第 " + scanEvent + " 次扫描已保存", Toast.LENGTH_SHORT).show();
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