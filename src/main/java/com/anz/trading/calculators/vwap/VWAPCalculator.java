package com.anz.trading.calculators.vwap;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VWAPCalculator {

    private final Map<String, VWAPData> dataMap = new ConcurrentHashMap<>();

    // Process incoming data
    public void processData(double price, long volume, LocalDateTime timestamp, String currencyPair) {
        VWAPData vwapData = dataMap.computeIfAbsent(currencyPair, k -> new VWAPData());
        Trade priceVolume = new Trade(price, volume, timestamp, currencyPair);
        vwapData.addDataPoint(priceVolume);
    }

    // Get the VWAP for a specific currency pair
    public double getVWAP(String currencyPair) {
        VWAPData vwapData = dataMap.get(currencyPair);
        return vwapData == null ? 0 : vwapData.getVWAP();
    }
}
