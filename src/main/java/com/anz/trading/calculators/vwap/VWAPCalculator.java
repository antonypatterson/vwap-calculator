package com.anz.trading.calculators.vwap;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VWAPCalculator {

	private final ExecutorService executorService;
    private final Map<String, VWAPData> dataMap = new ConcurrentHashMap<>();
    private final long minutesForVWAP;
    
    public VWAPCalculator(byte numberOfThreads, long minutesForVWAP) {
        // Create a thread pool for managing concurrent tasks
        executorService = Executors.newFixedThreadPool(numberOfThreads);
        this.minutesForVWAP = minutesForVWAP;
    }

    // Process incoming data
    public void processData(Trade trade) {
    	String currencyPair = trade.getCurrencyPair();
    	// Submit the task of adding data point to the executor
        executorService.submit(() -> {
            VWAPData vwapData = dataMap.computeIfAbsent(currencyPair, k -> new VWAPData(minutesForVWAP));
            vwapData.addDataPoint(trade);
        });
    }
    
    public void restoreData(Queue<Trade> trades) {
        // Loop through each trade in the queue and add it to the appropriate VWAPData
        for (Trade trade : trades) {
            String currencyPair = trade.getCurrencyPair();
            
            // Shouldn't ever need new VWAPData but leaving there to avoid any weird errors
            VWAPData vwapData = dataMap.computeIfAbsent(currencyPair, k -> new VWAPData(minutesForVWAP));
            
            // Add the trade data point to the VWAPData object
            vwapData.addDataPoint(trade);
        }
    }

    // Get the VWAP for a specific currency pair
    public double getVWAP(String currencyPair) {
        VWAPData vwapData = dataMap.get(currencyPair);
        return vwapData == null ? 0 : vwapData.getVWAP();
    }
    
    // For testing purposes only
    public Map<String, VWAPData> getDataMap() {
        return dataMap;
    }
    
    public VWAPData getVWAPData(String currencyPair) {
    	return dataMap.get(currencyPair);
    }
    
    // Optionally, shut down the executor service when done
    public void shutdown() {
        executorService.shutdown();
    }
}
