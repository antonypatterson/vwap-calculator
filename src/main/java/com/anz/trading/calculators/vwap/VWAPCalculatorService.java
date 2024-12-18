package com.anz.trading.calculators.vwap;
/**
 * Acts as the front end layer to generate new trades and them pass them to the lower-level
 * Classes to manage the list of trades and 
 */
public class VWAPCalculatorService {
    private final VWAPCalculator vwapCalculator;
    private final TradeGenerator tradeGenerator;
    private final TradeResourceManager tradeResourceManager;
    private byte nbrThreads = 4;
    
    private long minutesForVWAP;

    public VWAPCalculatorService(TradeGenerator tradeGenerator, long minutesForVWAP, int tradeThresholdPerPair, int totalTradeThreshold) {
        vwapCalculator = new VWAPCalculator(nbrThreads, minutesForVWAP);
        this.minutesForVWAP = minutesForVWAP;
        // Instantiates a tradeGenerator object with the default currency list as per
        // Currency Pair Data object
        this.tradeGenerator = (tradeGenerator != null) ? tradeGenerator : new TradeGenerator();
        this.tradeResourceManager = new TradeResourceManager(vwapCalculator, tradeThresholdPerPair, totalTradeThreshold);
    }
    
    // Constructor with CurrencyPairData to create TradeGenerator internally
    public VWAPCalculatorService(CurrencyPairData currencyPairData, long minutesForVWAP,
    		int tradeThresholdPerPair, int totalTradeThreshold) {
        vwapCalculator = new VWAPCalculator(nbrThreads, minutesForVWAP);
        this.minutesForVWAP = minutesForVWAP;
        // Create a new TradeGenerator using currencyPairData
        this.tradeGenerator = new TradeGenerator(currencyPairData);
        this.tradeResourceManager = new TradeResourceManager(vwapCalculator, tradeThresholdPerPair, totalTradeThreshold);
    }

    // Simulate receiving a price update from TradeGenerator
    public void simulateTradeUpdate(boolean manageResourceFlag) {
        // Generate a random trade using the TradeGenerator
        Trade trade = tradeGenerator.generateRandomTrade();

        // Process the trade using the VWAPCalculator
        vwapCalculator.processData(trade);
        
        // When the flag is set to true, also trigger the resource manager
        if (manageResourceFlag) {
        	tradeResourceManager.manageResources(minutesForVWAP);
        }
    }

    // Get the VWAP for a specific currency pair
    public double getVWAP(String currencyPair) {
        return vwapCalculator.getVWAP(currencyPair);
    }
    
    public void shutdown(VWAPCalculator vwapCalculator) {
    	vwapCalculator.shutdown();
    }
}
