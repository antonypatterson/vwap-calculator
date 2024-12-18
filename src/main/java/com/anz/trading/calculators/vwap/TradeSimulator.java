package com.anz.trading.calculators.vwap;

import com.google.common.util.concurrent.RateLimiter;
import java.util.Scanner;

public class TradeSimulator {
	static TradeGenerator tradegenerator = new TradeGenerator();
	static long minutesForVWAP = 60;
	static int tradeThresholdPerPair = 50000; // 50K
	static int totalTradeThreshold = 1000000; // 1M
	static VWAPCalculatorService tradeService = new VWAPCalculatorService(tradegenerator, minutesForVWAP, 
											tradeThresholdPerPair, totalTradeThreshold);
	
    public static void main(String[] args) {
        // Get user input for maxUpdatesPerSecond
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the maximum updates per second: ");
        int maxUpdatesPerSecond = scanner.nextInt();
        scanner.close();

        // Initialize the RateLimiter with user-defined rate
        RateLimiter rateLimiter = RateLimiter.create(maxUpdatesPerSecond);

        // Define the manageResourceFlag logic
        final long resourceManageIntervalMillis = 2_000; // 30 seconds in milliseconds
        long lastResourceManageTime = System.currentTimeMillis();

        while (true) {
            // Limit the rate of updates
            rateLimiter.acquire();

            // Check if 30 seconds have elapsed to toggle manageResourceFlag
            boolean manageResourceFlag = false;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastResourceManageTime >= resourceManageIntervalMillis) {
                manageResourceFlag = true;
                lastResourceManageTime = currentTime;
            }

            // Call the TradeService's simulateTradeUpdate method
            tradeService.simulateTradeUpdate(manageResourceFlag);
        }
    }
}