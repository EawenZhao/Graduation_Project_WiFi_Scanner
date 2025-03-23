package com.yinhuanzhao.graduation_project_wifi_scanner.fragment;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.yinhuanzhao.graduation_project_wifi_scanner.R;
import com.yinhuanzhao.graduation_project_wifi_scanner.WiFiScanDatabaseHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class ScanFragment extends Fragment {

    private static final int MIN_RSSI_THRESHOLD = -74;
    private WifiManager wifiManager;
    private EditText editTextRefPoint;
    private Button btnScan;
    private ProgressBar progressBar;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> scanResultsList;
    // 当前扫描参考点，初始值 -1 表示未设置
    private int currentRefPoint = -1;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 0;
    private WiFiScanDatabaseHelper dbHelper;

    // 用于自动扫描的计数和定时器
    private final int TOTAL_SCANS = 30;
    private int currentScanCount = 0;
    private Handler scanHandler = new Handler();

    // 每隔10秒执行一次扫描，共扫描30次
    private Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentScanCount < TOTAL_SCANS) {
                // 发起一次扫描
                startWifiScan(currentRefPoint);
                currentScanCount++;
                progressBar.setProgress(currentScanCount);
                // 10秒后继续扫描
                scanHandler.postDelayed(this, 10000);
            } else {
                // 扫描结束，恢复按钮可点击状态
                btnScan.setEnabled(true);
                Toast.makeText(getActivity(), "扫描完成", Toast.LENGTH_SHORT).show();
            }
        }
    };

    // 广播接收器：接收到扫描结果后处理数据
    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (currentRefPoint == -1) {
                return;
            }
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                getScanResults();
            } else {
                Toast.makeText(getActivity(), "扫描失败，请重试", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);

        editTextRefPoint = view.findViewById(R.id.editTextRefPoint);
        btnScan = view.findViewById(R.id.btnScan);
        progressBar = view.findViewById(R.id.progressBar);
        listView = view.findViewById(R.id.listView);

        scanResultsList = new ArrayList<>();
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, scanResultsList);
        listView.setAdapter(adapter);

        wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            Toast.makeText(getActivity(), "WiFi服务不可用", Toast.LENGTH_LONG).show();
            return view;
        }
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(getActivity(), "WiFi未开启，正在开启...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }
        dbHelper = new WiFiScanDatabaseHelper(getActivity());

        // 检查权限
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        // 初始化进度条
        progressBar.setMax(TOTAL_SCANS);
        progressBar.setProgress(0);

        // 点击按钮时启动定时扫描
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String refPointStr = editTextRefPoint.getText().toString().trim();
                if (refPointStr.isEmpty()) {
                    Toast.makeText(getActivity(), "请输入参考点ID", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    int refPoint = Integer.parseInt(refPointStr);
                    currentRefPoint = refPoint;
                    // 开始扫描前重置计数及进度条，禁用按钮
                    currentScanCount = 0;
                    progressBar.setProgress(0);
                    btnScan.setEnabled(false);
                    // 立即开始扫描
                    scanHandler.post(scanRunnable);
                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), "参考点ID必须是整数", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 注册扫描结果广播接收器
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getActivity().registerReceiver(wifiScanReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        // 移除定时任务，防止内存泄漏
        scanHandler.removeCallbacks(scanRunnable);
        getActivity().unregisterReceiver(wifiScanReceiver);
    }

    // 发起一次 Wi-Fi 扫描
    private void startWifiScan(int refPoint) {
        boolean success = wifiManager.startScan();
        if (!success) {
            Toast.makeText(getActivity(), "发起扫描失败", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "正在扫描...", Toast.LENGTH_SHORT).show();
        }
    }

    // 处理扫描结果，将符合条件的数据存入数据库并更新列表
    private void getScanResults() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        List<ScanResult> results = wifiManager.getScanResults();
        results.sort(new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult r1, ScanResult r2) {
                return Integer.compare(r2.level, r1.level);
            }
        });

        scanResultsList.clear();
        int scanEvent = dbHelper.getNextScanEventForRefPoint(currentRefPoint);

        for (ScanResult result : results) {
            if (result.level >= MIN_RSSI_THRESHOLD) {
                String info = "SSID: " + result.SSID +
                        "\nBSSID: " + result.BSSID +
                        "\nRSSI: " + result.level;
                scanResultsList.add(info);
                dbHelper.insertScanResult(currentRefPoint, scanEvent, result.SSID, result.BSSID, result.level);
            }
        }
        adapter.notifyDataSetChanged();
        Toast.makeText(getActivity(), "参考点 " + currentRefPoint + " 的第 " + scanEvent + " 次扫描已保存", Toast.LENGTH_SHORT).show();
    }
}
