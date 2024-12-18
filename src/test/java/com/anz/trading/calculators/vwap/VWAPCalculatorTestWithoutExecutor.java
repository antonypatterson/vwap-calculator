package com.anz.trading.calculators.vwap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VWAPCalculatorTestWithoutExecutor {

    private VWAPCalculator vwapCalculator;

    @BeforeEach
    public void setUp() {
        byte nbrThreads = 1; // For trivial testing
        long minutesForVWAP = 60;
        vwapCalculator = new VWAPCalculator(nbrThreads, minutesForVWAP);
    }

    @Test
    public void testProcessDataWithoutExecutor_SingleCurrencyPair_MultipleTrades() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Trade trade1 = new Trade(100.0, 200, now, "EUR/USD");
        Trade trade2 = new Trade(101.0, 300, now, "EUR/USD");

        // Act
        vwapCalculator.processDataWithoutExecutor(trade1);
        vwapCalculator.processDataWithoutExecutor(trade2);

        // Fetch VWAP result
        double result = vwapCalculator.getVWAP("EUR/USD");

        // Calculate Expected VWAP: (100*200 + 101*300) / (200 + 300) = 100.6
        double expectedVWAP = (100.0 * 200 + 101.0 * 300) / (200 + 300);

        // Assert
        assertEquals(expectedVWAP, result, 0.0001, "VWAP for EUR/USD is incorrect without executor");
    }

    @Test
    public void testProcessDataWithoutExecutor_MultipleCurrencyPairs() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        // EUR/USD trades
        Trade eurTrade1 = new Trade(1.0, 200, now, "EUR/USD");
        Trade eurTrade2 = new Trade(1.02, 300, now, "EUR/USD");

        // USD/JPY trades
        Trade jpyTrade1 = new Trade(110.0, 100, now, "USD/JPY");
        Trade jpyTrade2 = new Trade(112.0, 200, now, "USD/JPY");

        // Act
        vwapCalculator.processDataWithoutExecutor(eurTrade1);
        vwapCalculator.processDataWithoutExecutor(eurTrade2);
        vwapCalculator.processDataWithoutExecutor(jpyTrade1);
        vwapCalculator.processDataWithoutExecutor(jpyTrade2);

        double eurResult = vwapCalculator.getVWAP("EUR/USD");
        double jpyResult = vwapCalculator.getVWAP("USD/JPY");

        // Expected VWAP calculations
        double expectedEURVWAP = (1.0 * 200 + 1.02 * 300) / (200 + 300); 
        double expectedJPYVWAP = (110.0 * 100 + 112.0 * 200) / (100 + 200);

        // Assert
        assertEquals(expectedEURVWAP, eurResult, 0.0001, "VWAP for EUR/USD is incorrect without executor");
        assertEquals(expectedJPYVWAP, jpyResult, 0.0001, "VWAP for USD/JPY is incorrect without executor");
    }
}
