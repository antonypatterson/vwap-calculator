package com.anz.trading.calculators.vwap;

import java.time.LocalDateTime;
import java.util.Map;

public class TradeResourceManager {

    private final VWAPCalculator vwapCalculator;
    private int tradeThresholdPerPair;
    private int totalTradeThreshold;
    
    // Tracking variables for special threshold behavior
    private final double lowestPercentage = 0.4;
    private boolean isAboveLowestPercentage = false;
    private LocalDateTime lastBreachTime = LocalDateTime.MIN; // Initially set to the smallest possible time

    public TradeResourceManager(VWAPCalculator vwapCalculator, int tradeThresholdPerPair, int totalTradeThreshold) {
        this.vwapCalculator = vwapCalculator;
        this.tradeThresholdPerPair = tradeThresholdPerPair;
        this.totalTradeThreshold = totalTradeThreshold;
    }

    public void manageResources(TradeLogger tradelogger, long minutesForVWAP) {
        int totalTrades = 0;
        int newTradesToDB;
        int oldQueueSizeBeforeRestore;
        int oldQueueSizeAfterRestore;
        int tradeCount;
        Map<String, VWAPData> dataMap = vwapCalculator.getDataMap();

        // Iterate through all VWAPData instances in the VWAPCalculator
        for (Map.Entry<String, VWAPData> entry : dataMap.entrySet()) {
        	String currencyPair = entry.getKey();
        	VWAPData vwapData = entry.getValue();
        	newTradesToDB = vwapData.getNewestQueueSize();
        	oldQueueSizeBeforeRestore = vwapData.getOldQueueSize();
            tradeCount = vwapData.getCombinedQueueSize(); // Get number of trades for this pair
            totalTrades += tradeCount;
            LocalDateTime lastBackup = vwapData.getLastBackup();

            // Check if we need to write or restore for this currency pair
            long frequencyOfBackUp = (long) (minutesForVWAP/2 - 1);
            boolean clearMemory;
            if ((tradeCount >= tradeThresholdPerPair) || 
            		(LocalDateTime.now().isAfter(lastBackup.plusMinutes(frequencyOfBackUp)) &&
            				LocalDateTime.now().isBefore(lastBreachTime.plusMinutes(minutesForVWAP + 1)))) {
            	clearMemory = true;
            	tradelogger.loadNewestToDB(vwapCalculator, currencyPair, clearMemory);
            	totalTrades -= newTradesToDB; // cancel out increment if moved to DB
            } else if (LocalDateTime.now().isAfter(lastBackup.plusMinutes(minutesForVWAP - 1)) &&
            		LocalDateTime.now().isAfter(lastBreachTime.plusMinutes(minutesForVWAP + 1))) {
            	// Only when last backup more than 59 mins ago and latest breach more than 61 mins ago
            	clearMemory = false;
            	tradelogger.loadNewestToDB(vwapCalculator, currencyPair, clearMemory);
            }
            
            // Determine the minutes to restore based on trade count
            long minutesToRestore = adjustRestoreFrequency(tradeCount, minutesForVWAP);

            // Restore old trades if necessary
            LocalDateTime lastRestoreTimeStamp = vwapData.getLastRestoredDataTimestamp();
            LocalDateTime now = LocalDateTime.now();
            if (shouldRestoreTrades(now, lastRestoreTimeStamp, minutesToRestore)) {
            	tradelogger.restoreOldTradesToQueue(vwapCalculator, currencyPair, minutesToRestore, minutesForVWAP);
            	oldQueueSizeAfterRestore = vwapData.getOldQueueSize();
            	totalTrades += (oldQueueSizeAfterRestore - oldQueueSizeBeforeRestore);
            }
        }

        // Check overall trade volume threshold
        if (totalTrades >= totalTradeThreshold) {
            System.out.println("Total trade threshold exceeded. Triggering database writes...");
            tradelogger.emergencyDumpToDB(vwapCalculator);
        } else { //re-adjust the allowed trade threshold per pair before the backup trigger is scheduled
        	tradeThresholdPerPair = adjustTradeThresholdPerPair(totalTrades);
        }
    }

    private boolean shouldRestoreTrades(LocalDateTime now, LocalDateTime lastRestoreTimeStamp, long minutesToRestore) {
        return lastRestoreTimeStamp == null || now.isAfter(lastRestoreTimeStamp.plusMinutes(minutesToRestore));
    }
    
    // Method to dynamically adjust the trade threshold per pair based on total trade usage
    private int adjustTradeThresholdPerPair(int totalTrades) {
        double usagePercentage = (double) totalTrades / totalTradeThreshold;
        
        if (usagePercentage >= lowestPercentage) {
        	isAboveLowestPercentage = true;
        	lastBreachTime = LocalDateTime.now();
        } else {
        	isAboveLowestPercentage = false;
        }
        
        
        if (usagePercentage < 0.5) {
            return (int) (totalTradeThreshold * 0.10);  // 10% of totalTradeThreshold
        } else if (usagePercentage < 0.75) {
            return (int) (totalTradeThreshold * 0.05);  // 5%
        } else if (usagePercentage < 0.85) {
            return (int) (totalTradeThreshold * 0.03);  // 3%
        } else if (usagePercentage < 0.95) {
            return (int) (totalTradeThreshold * 0.015);  // 1.5%
        } else {
            return (int) (totalTradeThreshold * 0.005);  // 0.5%
        }
    }
    
    // Method to determine the minutes to restore based on the current trade count
    private long adjustRestoreFrequency(int tradeCount, long minutesForVWAP) {
        double usagePercentage = (double) tradeCount / totalTradeThreshold;
        
        long result;
        // Check the special condition for restoring frequency
        if (usagePercentage < lowestPercentage && 
            lastBreachTime != null && 
            lastBreachTime.isBefore(LocalDateTime.now().minusMinutes(minutesForVWAP + 1))) {
        	result = Long.MAX_VALUE;  // No need to restore as enough memory is available
        
        } else if (usagePercentage < 0.4) {
            result = Math.max((long) (minutesForVWAP * 0.483333333), 1);
        } else if (usagePercentage < 0.6) {
            result = Math.max((long) (minutesForVWAP * 0.416666667), 1);
        } else if (usagePercentage < 0.7) {
            result = Math.max((long) (minutesForVWAP * 0.25), 1);
        } else if (usagePercentage < 0.8) {
            result = Math.max((long) (minutesForVWAP * 0.166666667), 1);
        } else if (usagePercentage < 0.9) {
            result = Math.max((long) (minutesForVWAP * 0.083333333), 1);
        } else if (usagePercentage < 0.95) {
            result = Math.max((long) (minutesForVWAP * 0.033333333), 1);
        } else {
            result = Math.max((long) (minutesForVWAP * 0.016666667), 1);
        }
        return result;
    }
}
