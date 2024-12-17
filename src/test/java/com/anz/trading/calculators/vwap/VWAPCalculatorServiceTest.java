package com.anz.trading.calculators.vwap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class VWAPCalculatorServiceTest {

    private TradeGenerator mockTradeGenerator;
    private VWAPCalculatorService vwapCalculatorService;

    @BeforeEach
    void setUp() {
        // Create a mock of TradeGenerator
        mockTradeGenerator = Mockito.mock(TradeGenerator.class);

        // Initialize the service with the mocked TradeGenerator
        vwapCalculatorService = new VWAPCalculatorService(mockTradeGenerator);
    }

    @Test
    void testSimulateTradeUpdateAndGetVWAP() {
        // Arrange: Set up the deterministic Trade object
        Trade mockTrade = new Trade(
                0.765, // price
                100100L, // volume
                LocalDateTime.now(), // ensures does not get trimmed via the time-based cleanup
                "AUD/USD" // currencyPair
        );

        // Mock the behavior of generateRandomTrade()
        when(mockTradeGenerator.generateRandomTrade()).thenReturn(mockTrade);

        // Act: Simulate a trade update
        vwapCalculatorService.simulateTradeUpdate();

        // Retrieve VWAP for "AUD/USD"
        double vwap = vwapCalculatorService.getVWAP("AUD/USD");

        // Assert: Validate that VWAP is calculated correctly
        double expectedVWAP = 0.765; // As only one trade has been processed, VWAP = price
        assertEquals(expectedVWAP, vwap, 0.0001); // Allow a small margin for floating-point precision
    }
    
    @Test
    void testSimulateMultipleTradesAndCalculateVWAP() {
        // Arrange: Set up 3 distinct mock trades with varying prices and volumes
        List<Trade> mockTrades = Arrays.asList(
            new Trade(0.760, 1000, LocalDateTime.now(), "AUD/USD"),  // price: 0.760, volume: 1000
            new Trade(0.770, 2000, LocalDateTime.now(), "AUD/USD"),  // price: 0.770, volume: 2000
            new Trade(0.780, 3000, LocalDateTime.now(), "AUD/USD")   // price: 0.780, volume: 3000
        );

        // Mock the behavior of generateRandomTrade() to return these trades in sequence
        when(mockTradeGenerator.generateRandomTrade())
            .thenReturn(mockTrades.get(0))  // First trade
            .thenReturn(mockTrades.get(1))  // Second trade
            .thenReturn(mockTrades.get(2)); // Third trade

        // Act: Simulate 3 trade updates
        vwapCalculatorService.simulateTradeUpdate(); // Process first trade
        vwapCalculatorService.simulateTradeUpdate(); // Process second trade
        vwapCalculatorService.simulateTradeUpdate(); // Process third trade

        // Calculate the expected VWAP manually
        double totalPriceVolume = (0.760 * 1000) + (0.770 * 2000) + (0.780 * 3000);
        long totalVolume = 1000 + 2000 + 3000;
        double expectedVWAP = totalPriceVolume / totalVolume;

        // Retrieve VWAP from the service
        double actualVWAP = vwapCalculatorService.getVWAP("AUD/USD");

        // Assert: Validate the calculated VWAP
        assertEquals(expectedVWAP, actualVWAP, 0.0001); 
    }
    
    @Test
    void testSimulateTradesWithOldDataPointTrimming() {
        // Arrange: Create 3 trades where the first trade is older than 1 hour
        LocalDateTime now = LocalDateTime.now();
        Trade oldTrade = new Trade(0.760, 1000, now.minusMinutes(61), "AUD/USD");
        Trade trade2 = new Trade(0.770, 2000, now, "AUD/USD"); 
        Trade trade3 = new Trade(0.780, 3000, now, "AUD/USD");

        List<Trade> mockTrades = Arrays.asList(oldTrade, trade2, trade3);

        // Mock the behavior of generateRandomTrade() to return trades in sequence
        when(mockTradeGenerator.generateRandomTrade())
            .thenReturn(mockTrades.get(0)) 
            .thenReturn(mockTrades.get(1))  
            .thenReturn(mockTrades.get(2));

        // Act: Simulate 3 trade updates
        vwapCalculatorService.simulateTradeUpdate();
        vwapCalculatorService.simulateTradeUpdate(); 
        vwapCalculatorService.simulateTradeUpdate();

        // Calculate the expected VWAP manually (excluding the first trade)
        double totalPriceVolume = (0.770 * 2000) + (0.780 * 3000);
        long totalVolume = 2000 + 3000;
        double expectedVWAP = totalPriceVolume / totalVolume;

        // Retrieve VWAP from the service
        double actualVWAP = vwapCalculatorService.getVWAP("AUD/USD");

        // Assert: Validate the calculated VWAP
        assertEquals(expectedVWAP, actualVWAP, 0.0001);
    }
    
    
}
