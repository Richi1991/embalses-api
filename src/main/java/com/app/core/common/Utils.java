package com.app.core.common;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class Utils {

    public static final Map<String, Double[]> obtenerMapaCoordenadas() {
        Map<String, Double[]> mapCodigoCoordenadas = new HashMap<>();
        mapCodigoCoordenadas.put("04A06A1", new Double[]{38.291702,-2.423468});
        mapCodigoCoordenadas.put("07R04A1", new Double[] {38.078153,-0.852445});
        mapCodigoCoordenadas.put("01O03A1",new Double[]{38.030251,-1.364355});
        mapCodigoCoordenadas.put("02R02A1",new Double[]{38.129038,-1.303865});
        mapCodigoCoordenadas.put("05A10A1",new Double[]{37.644853,-2.059307});
        mapCodigoCoordenadas.put("03R02A1",new Double[]{38.501859,-1.853859});
        mapCodigoCoordenadas.put("01O05A1",new Double[]{37.944428,-1.159303});
        mapCodigoCoordenadas.put("07A04A1",new Double[]{38.089101,-0.721382});
        mapCodigoCoordenadas.put("01O02A1",new Double[]{38.018234,-1.497756});
        mapCodigoCoordenadas.put("02R01A1",new Double[]{38.237688,-1.430557});
        mapCodigoCoordenadas.put("01A03A1",new Double[]{37.979773,-1.136469});
        mapCodigoCoordenadas.put("02A05A1",new Double[]{38.239263,-1.682346});
        mapCodigoCoordenadas.put("05R01A1",new Double[]{37.675988,-1.691148});
        mapCodigoCoordenadas.put("01A01A1",new Double[]{38.009491,-1.214103});
        mapCodigoCoordenadas.put("03A02A1",new Double[]{38.285537,-1.690234});
        mapCodigoCoordenadas.put("02A02A1",new Double[]{38.149426,-1.340094});
        mapCodigoCoordenadas.put("03R04A1",new Double[]{38.321203,-1.662790});
        mapCodigoCoordenadas.put("01C02C1",new Double[]{37.724721,-1.458682});
        mapCodigoCoordenadas.put("04A03A1",new Double[]{38.396246,-2.208964});
        mapCodigoCoordenadas.put("02A01A1",new Double[]{38.238040,-1.556224});
        mapCodigoCoordenadas.put("01O06A1",new Double[]{37.724721,-1.458682});
        mapCodigoCoordenadas.put("07C02A1",new Double[]{38.076547,-0.970948});
        mapCodigoCoordenadas.put("02C01C1",new Double[]{38.239263,-1.682346});
        mapCodigoCoordenadas.put("03M01A1",new Double[]{38.400905,-1.638671});
        mapCodigoCoordenadas.put("07C08C5",new Double[]{37.947639,-1.144588});
        mapCodigoCoordenadas.put("04A02A1",new Double[]{38.368137,-1.769823});
        mapCodigoCoordenadas.put("03L01C1",new Double[]{38.542463,-1.963828});
        mapCodigoCoordenadas.put("05A12A1",new Double[]{37.593829,-2.267818});
        mapCodigoCoordenadas.put("04A01A1",new Double[]{38.393579,-2.149848});
        mapCodigoCoordenadas.put("05A11A1",new Double[]{37.644853,-2.059307});
        mapCodigoCoordenadas.put("01L01C1",new Double[]{38.239263,-1.682346});
        mapCodigoCoordenadas.put("05A09A1",new Double[]{37.593829,-2.267818});
        mapCodigoCoordenadas.put("07C10C5",new Double[]{38.239263,-1.682346});
        return mapCodigoCoordenadas;
    }
}
