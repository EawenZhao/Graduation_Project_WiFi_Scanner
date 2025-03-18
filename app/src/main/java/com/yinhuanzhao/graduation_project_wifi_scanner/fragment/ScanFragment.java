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
import android.widget.Toast;

import com.yinhuanzhao.graduation_project_wifi_scanner.R;
import com.yinhuanzhao.graduation_project_wifi_scanner.WiFiScanDatabaseHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScanFragment extends Fragment {
    private WifiManager wifiManager;
    private EditText editTextRefPoint;
    private Button btnScan;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> scanResultsList;
    // 保存当前扫描的参考点ID，初始值为 -1 表示未设置
    private int currentRefPoint = -1;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 0;
    private WiFiScanDatabaseHelper dbHelper;  // 新增：数据库帮助类

    // 广播接收器：接收到扫描结果后处理数据
    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 判断当前参考点是否有效，若为 -1 则不处理扫描结果
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

    public ScanFragment() {
        // 必须的空构造函数
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 加载 fragment_scan.xml 布局
        View view = inflater.inflate(R.layout.fragment_scan, container, false);

        // 绑定布局控件
        editTextRefPoint = view.findViewById(R.id.editTextRefPoint);
        btnScan = view.findViewById(R.id.btnScan);
        listView = view.findViewById(R.id.listView);

        // 初始化数据列表和适配器
        scanResultsList = new ArrayList<>();
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, scanResultsList);
        listView.setAdapter(adapter);

        // 获取 Wi-Fi 服务
        wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            Toast.makeText(getActivity(), "WiFi服务不可用", Toast.LENGTH_LONG).show();
            return view;
        }
        // 如果 Wi-Fi 未开启，则自动开启
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(getActivity(), "WiFi未开启，正在开启...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        // 初始化数据库帮助类
        dbHelper = new WiFiScanDatabaseHelper(getActivity());

        // 检查权限，确保扫描需要的定位权限已经被授予
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }

        // 按钮点击事件：先获取用户输入的参考点ID，再触发 Wi-Fi 扫描
        btnScan.setOnClickListener(v -> {
            String refPointStr = editTextRefPoint.getText().toString().trim();
            if (refPointStr.isEmpty()) {
                Toast.makeText(getActivity(), "请输入参考点ID", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int refPoint = Integer.parseInt(refPointStr);
                startWifiScan(refPoint);
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), "参考点ID必须是整数", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 注册广播接收器，监听 Wi-Fi 扫描结果
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getActivity().registerReceiver(wifiScanReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        // 注销广播接收器
        getActivity().unregisterReceiver(wifiScanReceiver);
    }

    // 发起 Wi-Fi 扫描，同时传入参考点ID，并更新当前的 currentRefPoint
    private void startWifiScan(int refPoint) {
        boolean success = wifiManager.startScan();
        if (!success) {
            Toast.makeText(getActivity(), "发起扫描失败", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "正在扫描...", Toast.LENGTH_SHORT).show();
            // 保存当前参考点ID，后续处理扫描结果时会用到
            currentRefPoint = refPoint;
        }
    }

    // 处理扫描结果，并将结果存入数据库
    private void getScanResults() {
        // 再次检查权限
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        List<ScanResult> results = wifiManager.getScanResults();

        // 按信号强度排序：RSSI 数值越大（接近0）表示信号越强
        results.sort((r1, r2) -> Integer.compare(r2.level, r1.level));

        // 清空原有结果并添加新结果
        scanResultsList.clear();

        // 获取当前参考点下次扫描的事件号（即已有最大扫描次数+1）
        int scanEvent = dbHelper.getNextScanEventForRefPoint(currentRefPoint);

        // 遍历扫描结果，更新列表，并存入数据库
        for (ScanResult result : results) {
            String info = "SSID: " + result.SSID
                    + "\nBSSID: " + result.BSSID
                    + "\nRSSI: " + result.level;
            scanResultsList.add(info);
            dbHelper.insertScanResult(currentRefPoint, scanEvent, result.SSID, result.BSSID, result.level);
        }
        adapter.notifyDataSetChanged();

        // 提示用户扫描完成，并显示保存的扫描次数
        Toast.makeText(getActivity(), "参考点 " + currentRefPoint + " 的第 " + scanEvent + " 次扫描已保存", Toast.LENGTH_SHORT).show();
    }
}