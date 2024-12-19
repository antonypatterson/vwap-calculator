package com.anz.trading.calculators.vwap;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class VWAPCalculator {

	private final ExecutorService executorService;
    private final Map<String, VWAPData> dataMap = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Void>> taskCompletionMap = new ConcurrentHashMap<>();
    private final long minutesForVWAP;
    
    public VWAPCalculator(byte numberOfThreads, long minutesForVWAP) {
        // Create a thread pool for managing concurrent tasks
        executorService = Executors.newFixedThreadPool(numberOfThreads);
        this.minutesForVWAP = minutesForVWAP;
    }

    // Process incoming data
    public int processData(Trade trade) {
    	AtomicInteger nbrTradesTrimmed = new AtomicInteger(0);
    	String currencyPair = trade.getCurrencyPair();
    	CompletableFuture<Void> taskFuture = new CompletableFuture<>();
    	// Submit the task of adding data point to the executor
    	executorService.submit(() -> {
            try {
                VWAPData vwapData = dataMap.computeIfAbsent(currencyPair, k -> new VWAPData(minutesForVWAP));
                nbrTradesTrimmed.set(vwapData.addDataPoint(trade));
            } finally {
                // Mark this task as completed
                taskFuture.complete(null);
            }
        });
    	
    	// Replace or merge task futures for the same currency pair
        taskCompletionMap.merge(
            currencyPair,
            taskFuture,
            (existingFuture, newFuture) -> existingFuture.thenCombine(newFuture, (a, b) -> null)
        );
        
        return nbrTradesTrimmed.get();
    }
    
    // Used to test the cases where the executor isn't working as expected
    public void processDataWithoutExecutor(Trade trade) {
    	String currencyPair = trade.getCurrencyPair();
        VWAPData vwapData = dataMap.computeIfAbsent(currencyPair, k -> new VWAPData(minutesForVWAP));
        vwapData.addDataPoint(trade);
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
    	CompletableFuture<Void> taskFuture = taskCompletionMap.get(currencyPair);

        // Wait for all tasks related to this currency pair to complete
        if (taskFuture != null) {
            try {
                taskFuture.join();
            } catch (CompletionException e) {
                throw new RuntimeException("Error while waiting for VWAP calculation tasks", e);
            }
        }

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
