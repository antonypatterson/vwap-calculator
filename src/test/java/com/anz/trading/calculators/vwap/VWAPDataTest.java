package com.anz.trading.calculators.vwap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VWAPDataTest {

    private VWAPData vwapData;

    @BeforeEach
    public void setUp() {
        long minutesForVWAP = 60; // 1-hour window
        vwapData = new VWAPData(minutesForVWAP);
    }
    @Test
    public void testTradeQueueSize() {
        // Assert initial size is zero
        assertEquals(0, vwapData.getNewestQueueSize(), "Initial queue size should be 0");

        // Create 3 new trades
        LocalDateTime now = LocalDateTime.now();
        Trade trade1 = new Trade(1.0, 200, now.minusMinutes(30), "EUR/USD");
        Trade trade2 = new Trade(1.01, 300, now.minusMinutes(10), "EUR/USD");
        Trade trade3 = new Trade(1.02, 150, now.minusMinutes(5), "EUR/USD");

        // Add trades
        vwapData.addDataPoint(trade1);
        vwapData.addDataPoint(trade2);
        vwapData.addDataPoint(trade3);

        // Assert size is 3
        assertEquals(3, vwapData.getNewestQueueSize(), "Queue size should be 3 after adding 3 trades");
    }

    @Test
    public void testAddDataPoint_WithinWindow() {
        LocalDateTime now = LocalDateTime.now();
        Trade trade1 = new Trade(1.0, 200, now.minusMinutes(30), "EUR/USD");
        Trade trade2 = new Trade(1.01, 300, now, "EUR/USD");

        vwapData.addDataPoint(trade1);
        vwapData.addDataPoint(trade2);

        // Calculate expected VWAP
        double expectedVWAP = (1.0 * 200 + 1.01 * 300) / (200 + 300);
        assertEquals(expectedVWAP, vwapData.getVWAP(), 0.0001, "VWAP is incorrect");
    }

    @Test
    public void testAddDataPoint_OutsideWindow() {
        LocalDateTime now = LocalDateTime.now();
        Trade trade1 = new Trade(1.0, 200, now.minusMinutes(61), "EUR/USD");
        Trade trade2 = new Trade(1.01, 300, now, "EUR/USD");

        vwapData.addDataPoint(trade1);
        vwapData.addDataPoint(trade2);

        // Expected VWAP should only consider trade2
        double expectedVWAP = 1.01; // trade2 only
        assertEquals(expectedVWAP, vwapData.getVWAP(), 0.0001, "VWAP is incorrect");
    }

    @Test
    public void testGetVWAP_EmptyTrades() {
        // No trades added
        assertEquals(0.0, vwapData.getVWAP(), "VWAP should be 0 for empty trades");
    }
}