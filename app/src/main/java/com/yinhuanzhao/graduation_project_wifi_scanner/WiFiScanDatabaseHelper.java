package com.yinhuanzhao.graduation_project_wifi_scanner;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class WiFiScanDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "wifi_scans.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "scan_results";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_REF_POINT = "ref_point";
    public static final String COLUMN_SCAN_EVENT = "scan_event";
    public static final String COLUMN_SSID = "ssid";
    public static final String COLUMN_BSSID = "bssid";
    public static final String COLUMN_RSSI = "rssi";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    public WiFiScanDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // 创建数据表
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_REF_POINT + " INTEGER, "
                + COLUMN_SCAN_EVENT + " INTEGER, "
                + COLUMN_SSID + " TEXT, "
                + COLUMN_BSSID + " TEXT, "
                + COLUMN_RSSI + " INTEGER, "
                + COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";
        db.execSQL(CREATE_TABLE);
    }

    // 数据库升级处理
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 简单删除后重建
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // 插入扫描记录，refPoint：参考点ID，scanEvent：本次扫描事件号
    public void insertScanResult(int refPoint, int scanEvent, String ssid, String bssid, int rssi) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REF_POINT, refPoint);
        values.put(COLUMN_SCAN_EVENT, scanEvent);
        values.put(COLUMN_SSID, ssid);
        values.put(COLUMN_BSSID, bssid);
        values.put(COLUMN_RSSI, rssi);
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    // 获取指定参考点下次扫描的事件号（即已有最大扫描次数+1）
    public int getNextScanEventForRefPoint(int refPoint) {
        int nextEvent = 1;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(" + COLUMN_SCAN_EVENT + ") FROM " + TABLE_NAME + " WHERE " + COLUMN_REF_POINT + " = ?", new String[]{String.valueOf(refPoint)});
        if (cursor.moveToFirst()) {
            int maxEvent = cursor.getInt(0);
            nextEvent = maxEvent + 1;
        }
        cursor.close();
        db.close();
        return nextEvent;
    }
}