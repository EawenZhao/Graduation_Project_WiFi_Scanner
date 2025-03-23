package com.yinhuanzhao.graduation_project_wifi_scanner.weibull;



public class WeibullEstimator {
    /**
     * 根据一组 RSSI 样本数据估计 Weibull 模型参数。
     * @param samples RSSI 样本数组（单位 dBm）
     * @return WeibullParameters 实例，包含 lambda, k, theta
     */
    public static WeibullParameters estimate(double[] samples) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("Samples array is null or empty");
        }

        // 计算均值和标准差
        double sum = 0.0;
        for (double s : samples) {
            sum += s;
        }
        double mean = sum / samples.length;

        double sqSum = 0.0;
        for (double s : samples) {
            sqSum += (s - mean) * (s - mean);
        }
        double std = Math.sqrt(sqSum / samples.length);

        // 计算形状参数 k，公式： k = std / ln(2)
        double k = std / Math.log(2);
        // 限制 k 在 [1.5, 2.5] 范围内
        if (k < 1.5) {
            k = 1.5;
        } else if (k > 2.5) {
            k = 2.5;
        }

        // 根据标准差选择比例参数 lambda
        double lambda;
        if (std < 2) {
            lambda = 2 * (k + 0.15);
        } else if (std <= 3.5) {
            lambda = std * (k + 0.15);
        } else {
            lambda = 3.5 * (k + 0.15);
        }

        // 计算位移参数 theta： theta = mean - lambda * Gamma(1 + 1/k)
        double gammaVal = gamma(1 + 1.0 / k);
        double theta = mean - lambda * gammaVal;

        return new WeibullParameters(lambda, k, theta);
    }

    /**
     * Lanczos 近似实现的 Gamma 函数
     * 参考：<a href="https://en.wikipedia.org/wiki/Lanczos_approximation">...</a>
     * @param x 输入值
     * @return Gamma(x)
     */
    private static double gamma(double x) {
        double[] p = {
                676.5203681218851,
                -1259.1392167224028,
                771.32342877765313,
                -176.61502916214059,
                12.507343278686905,
                -0.13857109526572012,
                9.9843695780195716e-6,
                1.5056327351493116e-7
        };
        int g = 7;
        if(x < 0.5) {
            return Math.PI / (Math.sin(Math.PI * x) * gamma(1 - x));
        }
        x -= 1;
        double a = 0.99999999999980993;
        for (int i = 0; i < p.length; i++) {
            a += p[i] / (x + i + 1);
        }
        double t = x + g + 0.5;
        return Math.sqrt(2 * Math.PI) * Math.pow(t, x + 0.5) * Math.exp(-t) * a;
    }
}