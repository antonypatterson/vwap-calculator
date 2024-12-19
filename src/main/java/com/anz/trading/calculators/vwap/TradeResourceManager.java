package com.anz.trading.calculators.vwap;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.anz.trading.calculators.vwap.dao.H2DatabaseUtility;
import com.anz.trading.calculators.vwap.dao.H2TradeMemory;
import com.anz.trading.calculators.vwap.dao.TradeDAO;
import com.anz.trading.calculators.vwap.dao.H2TradePersistent;

public class TradeResourceManager {

    private final VWAPCalculator vwapCalculator;
    private TradeLogger tradelogger;
    private int tradeThresholdPerPair;
    private int totalTradeThreshold;
    private long minutesToRestore;
    private int totalTradesPastHour = 0;
    
    // Tracking variables for special threshold behavior
    private final double lowestPercentageThreshold = 0.4;
    private LocalDateTime lastBreachTime = LocalDateTime.MIN; // Initially set to the smallest possible time
    private LocalDateTime lastCcyPairBackedUpTime = LocalDateTime.MIN;

    public TradeResourceManager(VWAPCalculator vwapCalculator, int tradeThresholdPerPair, int totalTradeThreshold) {
        this.vwapCalculator = vwapCalculator;
        this.tradeThresholdPerPair = tradeThresholdPerPair;
        this.totalTradeThreshold = totalTradeThreshold;
        this.tradelogger = createTradeLogger();
    }
    
    private final TradeLogger createTradeLogger() {
        try {
            // Create connections using H2DatabaseUtility
            Connection persistentConnection = H2DatabaseUtility.getPersistentConnection();
            Connection inMemoryConnection = H2DatabaseUtility.getInMemoryConnection();

            // Instantiate TradeDAO implementations
            TradeDAO persistentTradeDAO = new H2TradePersistent(persistentConnection);
            TradeDAO inMemoryTradeDAO = new H2TradeMemory(inMemoryConnection);

            // Return the TradeLogger
            return new TradeLogger(inMemoryTradeDAO, persistentTradeDAO);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize TradeLogger due to database connection issues", e);
        }
    }

    public synchronized String manageResources(long minutesForVWAP) {
        int totalTradesInMemory = 0;
        int newTradesToDB;
        int oldQueueSizeBeforeRestore;
        int oldQueueSizeAfterRestore;
        int tradeCount;
        Map<String, VWAPData> dataMap = vwapCalculator.getDataMap();

        int maxTradeCount = 0;
        String maxTradeCurrencyPair = "";
        
        // Iterate through all VWAPData instances in the VWAPCalculator
        for (Map.Entry<String, VWAPData> entry : dataMap.entrySet()) {
        	String currencyPair = entry.getKey();
        	VWAPData vwapData = entry.getValue();
        	newTradesToDB = vwapData.getNewestQueueSize();
        	oldQueueSizeBeforeRestore = vwapData.getOldQueueSize();
            tradeCount = vwapData.getCombinedQueueSize(); // Get number of trades for this pair
            totalTradesInMemory += tradeCount;
            LocalDateTime lastBackup = vwapData.getLastBackup();
            
            // Check if we need to write or restore for this currency pair
            long frequencyOfBackUp = (long) (minutesForVWAP/2 - 1);
            boolean clearMemory;
            if ((tradeCount >= tradeThresholdPerPair) || 
            		(LocalDateTime.now().isAfter(lastBackup.plusMinutes(frequencyOfBackUp)) &&
            				LocalDateTime.now().isBefore(lastBreachTime.plusMinutes(minutesForVWAP + 1)))) {
            	clearMemory = true;
            	tradelogger.loadNewestToDB(vwapCalculator, currencyPair, clearMemory);
            	totalTradesInMemory -= newTradesToDB; // cancel out increment if moved to DB
            	tradeCount -= newTradesToDB;
            	lastCcyPairBackedUpTime = LocalDateTime.now();
            } else if (LocalDateTime.now().isAfter(lastBackup.plusMinutes(minutesForVWAP - 1)) &&
            		LocalDateTime.now().isAfter(lastBreachTime.plusMinutes(minutesForVWAP + 1))) {
            	// Only when last backup more than 59 mins ago and latest breach more than 61 mins ago
            	clearMemory = false;
            	tradelogger.loadNewestToDB(vwapCalculator, currencyPair, clearMemory);
            }        
            

            // Restore old trades if necessary
            LocalDateTime lastRestoreTimeStamp = vwapData.getLastRestoredDataTimestamp();
            LocalDateTime now = LocalDateTime.now();
            if (shouldRestoreTrades(now, lastRestoreTimeStamp, minutesToRestore)) {
            	tradelogger.restoreOldTradesToQueue(vwapCalculator, currencyPair, minutesToRestore, minutesForVWAP);
            	oldQueueSizeAfterRestore = vwapData.getOldQueueSize();
            	totalTradesInMemory += (oldQueueSizeAfterRestore - oldQueueSizeBeforeRestore);
            	tradeCount += (oldQueueSizeAfterRestore - oldQueueSizeBeforeRestore);
            }
            
            // Track max trade count for any currency pair after additions/removals are processed
            if (tradeCount > maxTradeCount) {
                maxTradeCount = tradeCount;
                maxTradeCurrencyPair = currencyPair;
            }
        }
        
        // Log overall trade volume and relevant metrics       
        //double totalTradesInMemoryPct = ((double) totalTradesInMemory / totalTradeThreshold) * 100;
        double totalTradesLastHourPct = ((double) totalTradesPastHour / totalTradeThreshold) * 100;
        double maxCcyTradePercentage = ((double) maxTradeCount / totalTradesPastHour) * 100;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String formattedDateTime = LocalDateTime.now().format(formatter);
               
        System.out.println("Resource Query at " + formattedDateTime);
        System.out.println("Total Trade Limit: " + totalTradeThreshold);
        System.out.println("Total Trade Counter (memory): " + totalTradesInMemory);
        System.out.println("Total Trade Counter (last " + minutesForVWAP + " minutes): " + totalTradesPastHour);
        System.out.println("Percentage of Total Trade Limit: " + String.format("%.2f", totalTradesLastHourPct) + "%");
        System.out.println("Maximum Currency Pair: " + maxTradeCurrencyPair 
                + " | Max Trade Count: " + maxTradeCount 
                + " (" + String.format("%.2f", maxCcyTradePercentage) + "% of Total Trade Count)");
        
        System.out.println("Current Threshold per Pair: " + tradeThresholdPerPair);
        System.out.println("Current restore frequency: " + minutesToRestore);
        
        // Check overall trade volume threshold over last hour
        if (totalTradesInMemory >= totalTradeThreshold) {
            System.out.println("Total trade threshold exceeded. Triggering database writes...");
            int emergencyTradeDump = tradelogger.emergencyDumpToDB(vwapCalculator);
            totalTradesInMemory -= emergencyTradeDump;
        } else { //re-adjust the allowed trade threshold per pair before the backup trigger is scheduled
            minutesToRestore = adjustRestoreFrequency(totalTradesPastHour, minutesForVWAP);
        }
        
        // Finally, return the currency pair with the current maximum trade count
        return maxTradeCurrencyPair;
    }

    private boolean shouldRestoreTrades(LocalDateTime now, LocalDateTime lastRestoreTimeStamp, long minutesToRestore) {
        boolean result;
        if (lastRestoreTimeStamp == null || minutesToRestore <= 0) {
            result = false; // Avoid invalid restoration attempts
        } else {
        	result = now.isAfter(lastRestoreTimeStamp.plusMinutes(minutesToRestore));
        }        
    	return result;
    }
    
    // Method to dynamically adjust the trade threshold per pair based on total trade usage
    // Currently unused as using fixed 5% in current implementation
    public int adjustTradeThresholdPerPair(int tradeCount) {
        double usagePercentage = (double) tradeCount / totalTradeThreshold;
        
        if (usagePercentage >= lowestPercentageThreshold) {
        	lastBreachTime = LocalDateTime.now();
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
    public long adjustRestoreFrequency(int tradeCount, long minutesForVWAP) {
        double usagePercentage = (double) tradeCount / totalTradeThreshold;
        
        long result;
        // Check the special condition for restoring frequency
        // Only if lower threshold not breached and NOT a single currency pair backed up
        if (usagePercentage < lowestPercentageThreshold && 
            lastBreachTime != null && 
            lastBreachTime.isBefore(LocalDateTime.now().minusMinutes(minutesForVWAP + 1)) &&
            lastCcyPairBackedUpTime.isBefore(LocalDateTime.now().minusMinutes(minutesForVWAP + 1))) {
        	result = 20000;  // ~ 13 days, negligible frequency       
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
    
    // Only to be used for mock injections for testing
    public void setTradeLogger(TradeLogger tradelogger) {
    	this.tradelogger = tradelogger;
    }
    
    // Again only for testing
    public void setLastBreachTime(LocalDateTime lastBreachTime) {
    	this.lastBreachTime = lastBreachTime;
    }
    
    public int getTradeThresholdPerPair() {
    	return tradeThresholdPerPair;
    }
    
    public long getRestoreFrequency() {
    	return minutesToRestore;
    }
    
    public synchronized void adjustTradesInPastHour(int netTradeAdjustment)  {
    	totalTradesPastHour += netTradeAdjustment;
    }
}
