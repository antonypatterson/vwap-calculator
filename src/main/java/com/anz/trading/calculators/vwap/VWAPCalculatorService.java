package com.anz.trading.calculators.vwap;

import java.time.LocalDateTime;

public class VWAPCalculatorService {
    private final VWAPCalculator vwapCalculator;

    public VWAPCalculatorService() {
        vwapCalculator = new VWAPCalculator();
    }

    // Simulate receiving a price update
    public void processPriceUpdate(double price, long volume, String currencyPair) {
        LocalDateTime timestamp = LocalDateTime.now(); // Assume real-time updates
        vwapCalculator.processData(price, volume, timestamp, currencyPair);
    }

    // Get the VWAP for a specific currency pair
    public double getVWAP(String currencyPair) {
        return vwapCalculator.getVWAP(currencyPair);
    }
}
