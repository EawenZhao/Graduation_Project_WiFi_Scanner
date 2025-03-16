package com.yinhuanzhao.graduation_project_wifi_scanner;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class DatabaseViewerActivity extends AppCompatActivity {

    private Spinner spinnerRefPoint;
    private Spinner spinnerScanEvent;
    private ListView listViewData;
    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> dataList;
    private WiFiScanDatabaseHelper dbHelper;

    // 存放数据库中所有不同的参考点ID
    private ArrayList<Integer> refPointList;
    // 存放所选参考点下所有的扫描次数（scan_event）
    private ArrayList<Integer> scanEventList;

    // 添加清除按钮引用
    private Button btnClearData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_viewer);

        spinnerRefPoint = findViewById(R.id.spinnerRefPoint);
        spinnerScanEvent = findViewById(R.id.spinnerScanEvent);
        listViewData = findViewById(R.id.listViewData);
        dataList = new ArrayList<>();
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);
        listViewData.setAdapter(listAdapter);

        dbHelper = new WiFiScanDatabaseHelper(this);


        // 清除按钮点击事件
        btnClearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int deleted = dbHelper.clearAllData();
                Toast.makeText(DatabaseViewerActivity.this, "清除 " + deleted + " 条记录", Toast.LENGTH_SHORT).show();

                // 刷新界面数据
                refPointList = getDistinctRefPoints();
                if (refPointList.isEmpty()) {
                    spinnerRefPoint.setAdapter(null);
                    spinnerScanEvent.setAdapter(null);
                    dataList.clear();
                    listAdapter.notifyDataSetChanged();
                } else {
                    // 重新填充参考点选择器
                    ArrayList<String> refPointStrList = new ArrayList<>();
                    for (int ref : refPointList) {
                        refPointStrList.add("参考点: " + ref);
                    }
                    ArrayAdapter<String> refPointAdapter = new ArrayAdapter<>(DatabaseViewerActivity.this, android.R.layout.simple_spinner_item, refPointStrList);
                    refPointAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerRefPoint.setAdapter(refPointAdapter);
                }
            }
        });

        // 1. 查询数据库中所有不同的参考点ID
        refPointList = getDistinctRefPoints();
        if (refPointList.isEmpty()) {
            Toast.makeText(this, "暂无任何参考点数据", Toast.LENGTH_SHORT).show();
            return;
        }
        // 将参考点转换为字符串数组，例如 "参考点: 1"
        ArrayList<String> refPointStrList = new ArrayList<>();
        for (int ref : refPointList) {
            refPointStrList.add("参考点: " + ref);
        }
        ArrayAdapter<String> refPointAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, refPointStrList);
        refPointAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRefPoint.setAdapter(refPointAdapter);

        // 2. 当参考点选择变化时，更新扫描次数选择器；以及选中参考点时的初次扫描次数选择器
        spinnerRefPoint.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedRefPoint = refPointList.get(position);
                // 查询该参考点下所有不同的扫描事件号
                scanEventList = getScanEventList(selectedRefPoint);
                if (scanEventList.isEmpty()) {
                    Toast.makeText(DatabaseViewerActivity.this, "该参考点暂无扫描记录", Toast.LENGTH_SHORT).show();
                    dataList.clear();
                    listAdapter.notifyDataSetChanged();
                    spinnerScanEvent.setAdapter(null);
                    return;
                }
                ArrayList<String> scanEventStrList = new ArrayList<>();
                for (int event : scanEventList) {
                    scanEventStrList.add("第 " + event + " 次扫描");
                }
                ArrayAdapter<String> scanEventAdapter = new ArrayAdapter<>(DatabaseViewerActivity.this, android.R.layout.simple_spinner_item, scanEventStrList);
                scanEventAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerScanEvent.setAdapter(scanEventAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 无操作
            }
        });

        // 3. 当扫描次数选择变化时，根据所选参考点和扫描次数更新ListView
        spinnerScanEvent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedRefPoint = refPointList.get(spinnerRefPoint.getSelectedItemPosition());
                int selectedScanEvent = scanEventList.get(position);
                updateListView(selectedRefPoint, selectedScanEvent);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 无操作
            }
        });
    }

    // 查询数据库，获取所有不同的参考点ID
    private ArrayList<Integer> getDistinctRefPoints() {
        ArrayList<Integer> refPoints = new ArrayList<>();
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT DISTINCT " + WiFiScanDatabaseHelper.COLUMN_REF_POINT +
                        " FROM " + WiFiScanDatabaseHelper.TABLE_NAME +
                        " ORDER BY " + WiFiScanDatabaseHelper.COLUMN_REF_POINT,
                null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                refPoints.add(cursor.getInt(0));
            }
            cursor.close();
        }
        return refPoints;
    }

    // 查询数据库，获取指定参考点下所有扫描事件号（去重排序）
    private ArrayList<Integer> getScanEventList(int refPoint) {
        ArrayList<Integer> events = new ArrayList<>();
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT DISTINCT " + WiFiScanDatabaseHelper.COLUMN_SCAN_EVENT +
                        " FROM " + WiFiScanDatabaseHelper.TABLE_NAME +
                        " WHERE " + WiFiScanDatabaseHelper.COLUMN_REF_POINT + " = ? " +
                        " ORDER BY " + WiFiScanDatabaseHelper.COLUMN_SCAN_EVENT,
                new String[]{String.valueOf(refPoint)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                events.add(cursor.getInt(0));
            }
            cursor.close();
        }
        return events;
    }

    // 根据参考点和扫描事件查询数据库，更新 ListView 数据
    private void updateListView(int refPoint, int scanEvent) {
        dataList.clear();
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT " + WiFiScanDatabaseHelper.COLUMN_SSID + ", " +
                        WiFiScanDatabaseHelper.COLUMN_BSSID + ", " +
                        WiFiScanDatabaseHelper.COLUMN_RSSI + ", " +
                        WiFiScanDatabaseHelper.COLUMN_TIMESTAMP +
                        " FROM " + WiFiScanDatabaseHelper.TABLE_NAME +
                        " WHERE " + WiFiScanDatabaseHelper.COLUMN_REF_POINT + " = ? AND " +
                        WiFiScanDatabaseHelper.COLUMN_SCAN_EVENT + " = ?",
                new String[]{String.valueOf(refPoint), String.valueOf(scanEvent)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String ssid = cursor.getString(0);
                String bssid = cursor.getString(1);
                int rssi = cursor.getInt(2);
                String timestamp = cursor.getString(3);
                String record = "SSID: " + ssid +
                        "\nBSSID: " + bssid +
                        "\nRSSI: " + rssi +
                        "\n时间: " + timestamp;
                dataList.add(record);
            }
            cursor.close();
        }
        listAdapter.notifyDataSetChanged();
    }
}
