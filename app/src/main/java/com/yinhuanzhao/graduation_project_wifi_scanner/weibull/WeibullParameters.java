package com.yinhuanzhao.graduation_project_wifi_scanner.weibull;

public class WeibullParameters {
    private double lambda; // 比例参数
    private double k; // 形状参数
    private double theta; // 位移参数

    public WeibullParameters(double lambda, double k, double theta) {
        this.lambda = lambda;
        this.k = k;
        this.theta = theta;
    }

    public double getLambda() {
        return lambda;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    public double getK() {
        return k;
    }

    public void setK(double k) {
        this.k = k;
    }

    public double getTheta() {
        return theta;
    }

    public void setTheta(double theta) {
        this.theta = theta;
    }

    @Override
    public String toString() {
        return "WeibullParameters{" +
                "lambda=" + lambda +
                ", k=" + k +
                ", theta=" + theta +
                '}';
    }
}