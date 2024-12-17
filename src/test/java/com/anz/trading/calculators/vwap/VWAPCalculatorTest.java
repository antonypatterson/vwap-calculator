package com.anz.trading.calculators.vwap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VWAPCalculatorTest {

    private VWAPCalculator vwapCalculator;

    @BeforeEach
    public void setUp() {
    	byte nbrThreads = 1; //for trivial testing
        vwapCalculator = new VWAPCalculator(nbrThreads);
    }

    @Test
    public void testProcessData_SingleCurrencyPair_MultipleTrades() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Trade trade1 = new Trade(100.0, 200, now, "EUR/USD");
        Trade trade2 = new Trade(101.0, 300, now, "EUR/USD");

        // Act
        vwapCalculator.processData(trade1);
        vwapCalculator.processData(trade2);
        double result = vwapCalculator.getVWAP("EUR/USD");

        // Calculate Expected VWAP: (100*200 + 101*300) / (200 + 300) = 100.6
        double expectedVWAP = (100.0 * 200 + 101.0 * 300) / (200 + 300);
        
        // Assert
        assertEquals(expectedVWAP, result, 0.0001, "VWAP for EUR/USD is incorrect");
    }

    @Test
    public void testProcessData_MultipleCurrencyPairs() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        // EUR/USD trades
        Trade eurTrade1 = new Trade(1.0, 200, now, "EUR/USD");
        Trade eurTrade2 = new Trade(1.02, 300, now, "EUR/USD");

        // USD/JPY trades
        Trade jpyTrade1 = new Trade(110.0, 100, now, "USD/JPY");
        Trade jpyTrade2 = new Trade(112.0, 200, now, "USD/JPY");

        // Act
        vwapCalculator.processData(eurTrade1);
        vwapCalculator.processData(eurTrade2);
        vwapCalculator.processData(jpyTrade1);
        vwapCalculator.processData(jpyTrade2);

        double eurResult = vwapCalculator.getVWAP("EUR/USD");
        double jpyResult = vwapCalculator.getVWAP("USD/JPY");

        // Expected VWAP calculations
        double expectedEURVWAP = (1.0 * 200 + 1.02 * 300) / (200 + 300); 
        double expectedJPYVWAP = (110.0 * 100 + 112.0 * 200) / (100 + 200);

        // Assert
        assertEquals(expectedEURVWAP, eurResult, 0.0001, "VWAP for EUR/USD is incorrect");
        assertEquals(expectedJPYVWAP, jpyResult, 0.0001, "VWAP for USD/JPY is incorrect");

        // Verify internal dataMap contains both currency pairs
        Map<String, VWAPData> dataMap = vwapCalculator.getDataMap();
        assertTrue(dataMap.containsKey("EUR/USD"), "EUR/USD data should exist");
        assertTrue(dataMap.containsKey("USD/JPY"), "USD/JPY data should exist");
        
        // Ensure other pairs not explicitly declared aren't accidentally in there
        assertFalse(dataMap.containsKey("AUD/USD"), "AUD/USD should not be in the dataMap as no trades yet added");
        
        // Ensure the hashMap works as expected with respect to buckets
        assertEquals(dataMap.size(), 2, "Only 2 unique ccy pairs have been declared over 4 trades");
    }
}
