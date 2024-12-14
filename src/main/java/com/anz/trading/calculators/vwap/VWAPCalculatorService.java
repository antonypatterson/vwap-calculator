package com.anz.trading.calculators.vwap;

import java.time.LocalDateTime;

public class VWAPCalculatorService {
    private final VWAPCalculator vwapCalculator;

    public VWAPCalculatorService() {
        vwapCalculator = new VWAPCalculator();
    }

    // Simulate receiving a price update from TradeGenerator
    public void simulateTradeUpdate() {
        // Generate a random trade using the TradeGenerator
        Trade trade = TradeGenerator.generateRandomTrade();

        // Extract details from the generated trade
        double price = trade.getPrice();
        long volume = trade.getVolume();
        String currencyPair = trade.getCurrencyPair();
        LocalDateTime timestamp = trade.getTimestamp(); // Use the timestamp from the Trade object

        // Process the trade using the VWAPCalculator
        vwapCalculator.processData(price, volume, timestamp, currencyPair);
    }

    // Get the VWAP for a specific currency pair
    public double getVWAP(String currencyPair) {
        return vwapCalculator.getVWAP(currencyPair);
    }
}
