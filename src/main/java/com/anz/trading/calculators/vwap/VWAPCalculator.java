package com.anz.trading.calculators.vwap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VWAPCalculator {

    private final Map<String, VWAPData> dataMap = new ConcurrentHashMap<>();

    // Process incoming data
    public void processData(Trade trade) {
    	String currencyPair = trade.getCurrencyPair();
        VWAPData vwapData = dataMap.computeIfAbsent(currencyPair, k -> new VWAPData());
        vwapData.addDataPoint(trade);
    }

    // Get the VWAP for a specific currency pair
    public double getVWAP(String currencyPair) {
        VWAPData vwapData = dataMap.get(currencyPair);
        return vwapData == null ? 0 : vwapData.getVWAP();
    }
}
