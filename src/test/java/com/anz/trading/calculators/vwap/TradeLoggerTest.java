package com.anz.trading.calculators.vwap;

import org.junit.jupiter.api.Test;

import com.anz.trading.calculators.vwap.dao.H2TradePersistent;
import com.anz.trading.calculators.vwap.dao.TradeDAO;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;


public class TradeLoggerTest {
		
	@Test
    void testRestoreOldTradesToQueue() throws SQLException {
        // Mock dependencies
        TradeDAO persistentDAO = mock(TradeDAO.class);
        VWAPCalculator mockVWAPCalculator = mock(VWAPCalculator.class);
        VWAPData mockVWAPData = mock(VWAPData.class);
        when(mockVWAPCalculator.getVWAPData("EUR/USD")).thenReturn(mockVWAPData);

        // Prepare test data
        LocalDateTime now = LocalDateTime.now();
        Trade trade1 = new Trade(1.0, 50L, now.minusMinutes(30), "EUR/USD");
        Trade trade2 = new Trade(1.05, 60L, now.minusMinutes(20), "EUR/USD");
        Trade trade3 = new Trade(110.0, 70L, now.minusMinutes(10), "USD/JPY"); // Different currency

        Queue<Trade> trades = new ConcurrentLinkedDeque<>();
        trades.add(trade1);
        trades.add(trade2);

        when(persistentDAO.getAllTrades(any(LocalDateTime.class), any(LocalDateTime.class), eq("EUR/USD")))
                .thenReturn(trades);
        when(mockVWAPData.getLastRestoredDataTimestamp()).thenReturn(now.minusMinutes(90));

        // Instantiate TradeLogger
        TradeLogger tradeLogger = new TradeLogger(persistentDAO);

        // Call the method under test
        long minutesToRestore = 15;
        long minutesForVWAP = 60;
        tradeLogger.restoreOldTradesToQueue(mockVWAPCalculator, "EUR/USD", minutesToRestore, minutesForVWAP);

        // Verify interactions
        verify(persistentDAO, times(1))
                .getAllTrades(any(LocalDateTime.class), any(LocalDateTime.class), eq("EUR/USD"));

        verify(mockVWAPData, times(1)).restoreDataPoint(trade1);
        verify(mockVWAPData, times(1)).restoreDataPoint(trade2);
        verify(mockVWAPData, never()).restoreDataPoint(trade3); // Should not restore unrelated trades

        verify(mockVWAPData, times(1)).setLastRestoredDataTimestamp(trade2.getTimestamp());
        verify(mockVWAPData, times(1)).setLastRestore(any(LocalDateTime.class));
    }
}
