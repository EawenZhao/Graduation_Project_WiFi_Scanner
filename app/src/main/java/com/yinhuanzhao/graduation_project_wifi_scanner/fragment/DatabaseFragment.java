package com.yinhuanzhao.graduation_project_wifi_scanner.fragment;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.yinhuanzhao.graduation_project_wifi_scanner.R;
import com.yinhuanzhao.graduation_project_wifi_scanner.WiFiScanDatabaseHelper;
import com.yinhuanzhao.graduation_project_wifi_scanner.weibull.WeibullEstimator;
import com.yinhuanzhao.graduation_project_wifi_scanner.weibull.WeibullParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DatabaseFragment extends Fragment {

    private Spinner spinnerRefPoint;
    private Spinner spinnerScanEvent;
    private ListView listViewData;
    private Button btnClearData;
    private Button btnGenerateFingerprint;
    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> dataList;
    private WiFiScanDatabaseHelper dbHelper;
    private ArrayList<Integer> refPointList;
    private ArrayList<Integer> scanEventList;

    public DatabaseFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_database, container, false);
        spinnerRefPoint = view.findViewById(R.id.spinnerRefPoint);
        spinnerScanEvent = view.findViewById(R.id.spinnerScanEvent);
        listViewData = view.findViewById(R.id.listViewData);
        btnClearData = view.findViewById(R.id.btnClearData);
        btnGenerateFingerprint = view.findViewById(R.id.btnGenerateFingerprint);

        dataList = new ArrayList<>();
        listAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, dataList);
        listViewData.setAdapter(listAdapter);

        dbHelper = new WiFiScanDatabaseHelper(getActivity());

        btnClearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int deleted = dbHelper.clearAllData();
                Toast.makeText(getActivity(), "清除 " + deleted + " 条记录", Toast.LENGTH_SHORT).show();
                refreshRefPointSpinner();
            }
        });

        btnGenerateFingerprint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateFingerprintLibrary();
            }
        });

        refreshRefPointSpinner();

        spinnerRefPoint.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedRefPoint = refPointList.get(position);
                scanEventList = getScanEventList(selectedRefPoint);
                if (scanEventList.isEmpty()) {
                    Toast.makeText(getActivity(), "该参考点暂无扫描记录", Toast.LENGTH_SHORT).show();
                    dataList.clear();
                    listAdapter.notifyDataSetChanged();
                    spinnerScanEvent.setAdapter(null);
                    return;
                }
                ArrayList<String> scanEventStrList = new ArrayList<>();
                for (int event : scanEventList) {
                    scanEventStrList.add("第 " + event + " 次扫描");
                }
                ArrayAdapter<String> scanEventAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, scanEventStrList);
                scanEventAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerScanEvent.setAdapter(scanEventAdapter);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerScanEvent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedRefPoint = refPointList.get(spinnerRefPoint.getSelectedItemPosition());
                int selectedScanEvent = scanEventList.get(position);
                updateListView(selectedRefPoint, selectedScanEvent);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        return view;
    }

    private void refreshRefPointSpinner() {
        refPointList = getDistinctRefPoints();
        if (refPointList.isEmpty()) {
            Toast.makeText(getActivity(), "暂无任何参考点数据", Toast.LENGTH_SHORT).show();
            spinnerRefPoint.setAdapter(null);
            spinnerScanEvent.setAdapter(null);
            dataList.clear();
            listAdapter.notifyDataSetChanged();
        } else {
            ArrayList<String> refPointStrList = new ArrayList<>();
            for (int ref : refPointList) {
                refPointStrList.add("参考点: " + ref);
            }
            ArrayAdapter<String> refPointAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, refPointStrList);
            refPointAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRefPoint.setAdapter(refPointAdapter);
        }
    }

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


    @Override
    public void onResume() {
        super.onResume();
        // 重新刷新参考点、扫描次数以及 ListView 数据
        refreshRefPointSpinner();
    }


    private void generateFingerprintLibrary() {
        ArrayList<Integer> refPoints = getDistinctRefPoints();
        JSONArray fingerprintArray = new JSONArray();

        for (int refPoint : refPoints) {
            JSONObject refPointObject = new JSONObject();
            try {
                refPointObject.put("ref_point", refPoint);

                // 查询该参考点下所有 AP 的 RSSI 数据
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery(
                        "SELECT " + WiFiScanDatabaseHelper.COLUMN_BSSID + ", " + WiFiScanDatabaseHelper.COLUMN_RSSI +
                                " FROM " + WiFiScanDatabaseHelper.TABLE_NAME +
                                " WHERE " + WiFiScanDatabaseHelper.COLUMN_REF_POINT + " = ?",
                        new String[]{String.valueOf(refPoint)}
                );

                // 将同一 AP 的 RSSI 值分组存入 Map 中
                Map<String, ArrayList<Double>> samplesMap = new HashMap<>();
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String bssid = cursor.getString(0);
                        double rssi = cursor.getDouble(1);
                        if (!samplesMap.containsKey(bssid)) {
                            samplesMap.put(bssid, new ArrayList<Double>());
                        }
                        samplesMap.get(bssid).add(rssi);
                    }
                    cursor.close();
                }
                db.close();

                // 针对每个 AP 的 RSSI 样本，计算 Weibull 模型参数
                JSONObject fingerprintObject = new JSONObject();
                for (Map.Entry<String, ArrayList<Double>> entry : samplesMap.entrySet()) {
                    String bssid = entry.getKey();
                    ArrayList<Double> rssiList = entry.getValue();
                    double[] samples = new double[rssiList.size()];
                    for (int i = 0; i < rssiList.size(); i++) {
                        samples[i] = rssiList.get(i);
                    }
                    // 使用 locater 包中的 WeibullEstimator 计算参数
                    WeibullParameters params = WeibullEstimator.estimate(samples);

                    // 构造 JSON 对象存储 Weibull 参数
                    JSONObject paramObj = new JSONObject();
                    paramObj.put("lambda", params.getLambda());
                    paramObj.put("k", params.getK());
                    paramObj.put("theta", params.getTheta());

                    fingerprintObject.put(bssid, paramObj);
                }
                refPointObject.put("fingerprint", fingerprintObject);
                fingerprintArray.put(refPointObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // 写入 JSON 指纹库到外部存储
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File file = new File(requireActivity().getExternalFilesDir(null), "fingerprint_library.json");
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(fingerprintArray.toString(4).getBytes());
                fos.close();
                Toast.makeText(getActivity(), "指纹库已生成: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), "外部存储不可用", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException | JSONException e) {
            Toast.makeText(getActivity(), "写入文件失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

}
