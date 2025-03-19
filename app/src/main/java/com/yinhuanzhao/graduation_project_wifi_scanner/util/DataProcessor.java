package com.yinhuanzhao.graduation_project_wifi_scanner.util;

import java.util.HashMap;
import java.util.Map;

public class DataProcessor {

    /**
     * 对给定的指纹数据进行Min-Max归一化处理
     * 公式：normalized = (x - min) / (max - min)
     *
     * @param fingerprint 原始指纹数据，键为BSSID，值为平均RSSI
     * @return 归一化后的指纹数据
     */
    public static HashMap<String, Double> minMaxNormalize(HashMap<String, Double> fingerprint) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (Double value : fingerprint.values()) {
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }
        HashMap<String, Double> normalized = new HashMap<>();
        if (max == min) {
            // 若所有值相等，统一归一化为1.0
            for (String key : fingerprint.keySet()) {
                normalized.put(key, 1.0);
            }
        } else {
            for (Map.Entry<String, Double> entry : fingerprint.entrySet()) {
                double normValue = (entry.getValue() - min) / (max - min);
                normalized.put(entry.getKey(), normValue);
            }
        }
        return normalized;
    }
}
