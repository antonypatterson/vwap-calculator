package com.anz.trading.calculators.vwap;

import com.google.common.util.concurrent.RateLimiter;
import java.util.Scanner;

public class TradeSimulator {
	static TradeGenerator tradegenerator = new TradeGenerator();
	static long minutesForVWAP = 60;
	static int tradeThresholdPerPair = 50000; // 50K - initial condition only
	static int totalTradeThreshold = 1000000; // 1M
	static VWAPCalculatorService vwapCalculatorService = new VWAPCalculatorService(tradegenerator, minutesForVWAP, 
											tradeThresholdPerPair, totalTradeThreshold);
	
    public static void main(String[] args) {
        // Get user input for maxUpdatesPerSecond
        Scanner scanner = new Scanner(System.in);
        System.out.print("Trade Simulator - Enter the maximum updates per second: ");
        int maxUpdatesPerSecond = scanner.nextInt();
        scanner.close();

        // Initialize the RateLimiter with user-defined rate
        RateLimiter rateLimiter = RateLimiter.create(maxUpdatesPerSecond);

        // Define the manageResourceFlag logic
        final long resourceManageIntervalMillis = 30_000; // 30 seconds or custom in milliseconds
        long lastResourceManageTime = System.currentTimeMillis();

        while (true) {
            // Limit the rate of updates
            rateLimiter.acquire();

            // Check if 30 seconds (or other custom set value) have elapsed to toggle manageResourceFlag
            boolean manageResourceFlag = false;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastResourceManageTime >= resourceManageIntervalMillis) {
                manageResourceFlag = true;
                lastResourceManageTime = currentTime;
            }

            // Call the vwapCalculatorService's simulateTradeUpdate method
            String currencyPair = vwapCalculatorService.simulateTradeUpdate(manageResourceFlag);
            double vwapOutput = vwapCalculatorService.getVWAP(currencyPair);
            
            // Print out the VWAP but for demonstration purposes, only when the manager resources function is called
            if (manageResourceFlag) {
            	int tradeThresholdPerPair = vwapCalculatorService.getTradeThresholdPerPair();
                long minutesToRestore = vwapCalculatorService.getRestoreFrequency();
            	System.out.printf("VWAP of %s = %.2f%n", currencyPair, vwapOutput);
                System.out.printf("Trade Threshold per pair: %d%n", tradeThresholdPerPair);
                System.out.printf("Minutes per restore: %d%n", minutesToRestore);
            }           
            
        }
    }
}