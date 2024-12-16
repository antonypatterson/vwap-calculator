package com.anz.trading.calculators.vwap;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

class TradeGeneratorTest {

    @Test
    void testGenerateRandomTradeWithDefaultData() {
        // Use default data
        TradeGenerator tradeGenerator = new TradeGenerator();
        Trade trade = tradeGenerator.generateRandomTrade();

        assertNotNull(trade, "Generated trade should not be null");
        assertNotNull(trade.getPrice(), "Trade price should not be null");
        assertNotNull(trade.getVolume(), "Trade volume should not be null");
        assertNotNull(trade.getTimestamp(), "Trade timestamp should not be null");
        assertNotNull(trade.getCurrencyPair(), "Trade currency pair should not be null");
        assertTrue(trade.getPrice() > 0, "Trade price should be positive");
        assertTrue(trade.getVolume() > 0, "Trade volume should be positive");
        assertTrue(trade.getTimestamp().isAfter(LocalDateTime.now().minus(1, ChronoUnit.MINUTES)),
                   "Trade timestamp should be within the last minute");
    }

    @Test
    void testGenerateRandomTradeWithSingleCurrencyPair() {
        // Use a single currency pair and its stats
        String[] customPairs = { "AUD/USD" };
        double[][] customStats = { { 0.75, 0.03 } };

        // Create custom CurrencyPairData with single entry
        CurrencyPairData customCurrencyPairData = new CurrencyPairData(customPairs, customStats);

        // Inject custom data into the TradeGenerator
        TradeGenerator tradeGenerator = new TradeGenerator(customCurrencyPairData);

        // Generate the "random" trade
        Trade trade = tradeGenerator.generateRandomTrade();

        // Validate the trade properties
        assertNotNull(trade, "Generated trade should not be null");
        assertEquals("AUD/USD", trade.getCurrencyPair(), "Currency pair should be 'AUD/USD'");
        assertTrue(trade.getPrice() > 0, "Trade price should be positive");
        assertTrue(trade.getVolume() > 0, "Trade volume should be positive");
        assertNotNull(trade.getTimestamp(), "Trade timestamp should not be null");
        assertTrue(trade.getTimestamp().isAfter(LocalDateTime.now().minus(1, ChronoUnit.MINUTES)),
                   "Trade timestamp should be within the last minute");
    }
    
    @Test
    void testGenerateRandomTradeWithMockedRandom() {
        // Custom data
        String[] customPairs = { "AUD/USD" };
        double[][] customStats = { { 0.75, 0.03 } };
        CurrencyPairData customCurrencyPairData = new CurrencyPairData(customPairs, customStats);

        // Mock Random
        Random mockRandom = mock(Random.class);
        when(mockRandom.nextInt(1)).thenReturn(0); // Always pick the first currency pair
        when(mockRandom.nextGaussian()).thenReturn(0.5); // Fixed Gaussian value
        when(mockRandom.nextInt(100000)).thenReturn(50000); // Fixed volume offset

        // Create TradeGenerator with mocked Random
        TradeGenerator tradeGenerator = new TradeGenerator(customCurrencyPairData, mockRandom);

        // Generate trade
        Trade trade = tradeGenerator.generateRandomTrade();

        // Assertions
        assertNotNull(trade, "Generated trade should not be null");
        assertEquals("AUD/USD", trade.getCurrencyPair(), "Currency pair should be 'AUD/USD'");
        assertEquals(0.765, trade.getPrice(), 0.0001, "Price should match mocked calculation"); // mean + (0.5 * stdDev)
        assertEquals(50100, trade.getVolume(), "Volume should match mocked calculation"); // 100 + mocked volume
        assertNotNull(trade.getTimestamp(), "Trade timestamp should not be null");
        assertTrue(trade.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)),
                   "Timestamp should be recent");
    }
}
