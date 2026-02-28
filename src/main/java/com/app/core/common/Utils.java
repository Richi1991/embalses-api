package com.app.core.common;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.springframework.stereotype.Component;

@Component
public class Utils {

    public record Coordinates(double latitud, double longitud) {}

    // Puntos de referencia: {x, y, lat, lon}
    private static final double[][] REFERENCE = {
            {194,  76,  38.392572, -2.206658},
            {193, 156,  38.364133, -1.774856},
            {119, 155,  38.509778, -1.863250},
            {169, 260,  38.341161, -1.647550},
            { 67, 294,  38.522645, -1.762885},
            { 67, 360,  38.649176, -1.700140},
            { 67, 425,  38.641624, -1.501538},
            {343, 177,  38.192778, -2.065278},
            {251, 147,  38.243555, -1.751659},
            {290, 271,  38.173611, -1.732778},
            {290, 336,  38.224444, -1.598333},
            {164, 343,  38.283333, -1.432222},
            {224, 461,  38.232770, -1.366472},
            {223, 357,  38.524722, -1.524722},
            {397, 213,  38.060000, -1.487500},
            {447, 379,  37.986389, -1.523611},
            {408, 300,  38.023333, -1.552778},
            {405, 388,  38.044167, -1.293056},
            {346, 381,  38.112369, -1.375155},
            {387, 464,  38.093694, -1.087222},
            {420,  61,  37.806111, -1.964167},
            {541, 136,  37.735833, -1.818056},
            {450, 206,  37.889972, -1.384722},
            {530, 383,  37.883333, -1.342222},
            {489, 599,  38.038166, -0.875260},
            {351, 566,  38.256944, -0.791443},
    };

    private static final double[] COEF_LAT;
    private static final double[] COEF_LON;

    static {
        int n = REFERENCE.length;
        double[][] x = new double[n][2];
        double[] lat = new double[n];
        double[] lon = new double[n];

        for (int i = 0; i < n; i++) {
            x[i][0] = REFERENCE[i][0]; // pixel x
            x[i][1] = REFERENCE[i][1]; // pixel y
            lat[i]  = REFERENCE[i][2];
            lon[i]  = REFERENCE[i][3];
        }

        OLSMultipleLinearRegression regressionLat = new OLSMultipleLinearRegression();
        regressionLat.newSampleData(lat, x);
        COEF_LAT = regressionLat.estimateRegressionParameters();

        OLSMultipleLinearRegression regressionLon = new OLSMultipleLinearRegression();
        regressionLon.newSampleData(lon, x);
        COEF_LON = regressionLon.estimateRegressionParameters();
    }

    public static Coordinates convert(int pixelX, int pixelY) {
        double lat = COEF_LAT[0] + COEF_LAT[1] * pixelX + COEF_LAT[2] * pixelY;
        double lon = COEF_LON[0] + COEF_LON[1] * pixelX + COEF_LON[2] * pixelY;
        return new Coordinates(
                Math.round(lat * 1_000_000.0) / 1_000_000.0,
                Math.round(lon * 1_000_000.0) / 1_000_000.0
        );
    }
}
