package com.anz.trading.calculators.vwap;

public class VWAPCalculatorService {
    private final VWAPCalculator vwapCalculator;
    private final TradeGenerator tradeGenerator;

    public VWAPCalculatorService(TradeGenerator tradeGenerator) {
        vwapCalculator = new VWAPCalculator();
        // Instantiates a tradeGenerator object with the default currency list as per
        // Currency Pair Data object
        this.tradeGenerator = (tradeGenerator != null) ? tradeGenerator : new TradeGenerator();
    }
    
    // Constructor with CurrencyPairData to create TradeGenerator internally
    public VWAPCalculatorService(CurrencyPairData currencyPairData) {
        vwapCalculator = new VWAPCalculator();
        // Create a new TradeGenerator using currencyPairData
        this.tradeGenerator = new TradeGenerator(currencyPairData);
    }

    // Simulate receiving a price update from TradeGenerator
    public void simulateTradeUpdate() {
        // Generate a random trade using the TradeGenerator
        Trade trade = tradeGenerator.generateRandomTrade();

        // Process the trade using the VWAPCalculator
        vwapCalculator.processData(trade);
    }

    // Get the VWAP for a specific currency pair
    public double getVWAP(String currencyPair) {
        return vwapCalculator.getVWAP(currencyPair);
    }
}
