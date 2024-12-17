package com.anz.trading.calculators.vwap;

import java.time.LocalDateTime;
import java.util.Map;

public class TradeResourceManager {

    private final VWAPCalculator vwapCalculator;
    private int tradeThresholdPerPair;
    private int totalTradeThreshold;

    public TradeResourceManager(VWAPCalculator vwapCalculator, int tradeThresholdPerPair, int totalTradeThreshold) {
        this.vwapCalculator = vwapCalculator;
        this.tradeThresholdPerPair = tradeThresholdPerPair;
        this.totalTradeThreshold = totalTradeThreshold;
    }

    public void manageResources(TradeLogger tradelogger, long minutesToRestore, long minutesForVWAP) {
        int totalTrades = 0;
        int tradeCount;
        Map<String, VWAPData> dataMap = vwapCalculator.getDataMap();

        // Iterate through all VWAPData instances in the VWAPCalculator
        for (Map.Entry<String, VWAPData> entry : dataMap.entrySet()) {
        	String currencyPair = entry.getKey();
        	VWAPData vwapData = entry.getValue();
            tradeCount = vwapData.getCombinedQueueSize(); // Get number of trades for this pair
            totalTrades += tradeCount;

            // Check if we need to write or restore for this currency pair
            if (tradeCount >= tradeThresholdPerPair) {
            	tradelogger.loadNewestToDB(vwapCalculator, currencyPair);
            	totalTrades -= tradeCount; // cancel out increment if moved to DB
            }

            // Restore old trades if necessary
            LocalDateTime lastRestore = vwapData.getLastRestoredDataTimestamp();
            LocalDateTime now = LocalDateTime.now();
            if (shouldRestoreTrades(now, lastRestore, minutesToRestore)) {
            	tradelogger.restoreOldTradesToQueue(vwapCalculator, currencyPair, minutesToRestore, minutesForVWAP);
            }
        }

        // Check overall trade volume threshold
        if (totalTrades >= totalTradeThreshold) {
            System.out.println("Total trade threshold exceeded. Triggering database writes...");
            tradelogger.emergencyDumpToDB(vwapCalculator);
        }
    }

    private boolean shouldRestoreTrades(LocalDateTime now, LocalDateTime lastRestore, long restoreDuration) {
        return lastRestore == null || now.isAfter(lastRestore.plusMinutes(restoreDuration));
    }
}
