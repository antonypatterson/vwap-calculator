package com.anz.trading.calculators.vwap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.anz.trading.calculators.vwap.dao.TradeDAO;
import com.anz.trading.calculators.vwap.dao.H2TradePersistent;
import com.anz.trading.calculators.vwap.dao.H2TradeMemory;

class TradeResourceManagerTest {

    private VWAPCalculator mockVWAPCalculator;
    private TradeLogger mockTradeLogger;
    private TradeResourceManager resourceManager;

    @BeforeEach
    void setUp() {
        mockVWAPCalculator = mock(VWAPCalculator.class);
        mockTradeLogger = mock(TradeLogger.class);

        // Initial conditions for the per ccy and overall
        int tradeThresholdPerPair = 100;
        int totalTradeThreshold = 1000;

        resourceManager = new TradeResourceManager(mockVWAPCalculator, tradeThresholdPerPair, totalTradeThreshold) {
            protected TradeLogger createTradeLogger() {
                return mockTradeLogger; // Use mock logger to test interactions
            }
        };
    }

    @Test
    void testManageResources_BackupTradesWhenThresholdExceeded() {
        // Arrange
        VWAPData mockVWAPData = mock(VWAPData.class);
        when(mockVWAPData.getCombinedQueueSize()).thenReturn(150); // Exceeds threshold
        when(mockVWAPData.getNewestQueueSize()).thenReturn(50);
        when(mockVWAPData.getOldQueueSize()).thenReturn(100);
        when(mockVWAPData.getLastBackup()).thenReturn(LocalDateTime.now().minusMinutes(61));

        Map<String, VWAPData> mockDataMap = new HashMap<>();
        mockDataMap.put("USD/EUR", mockVWAPData);

        VWAPCalculator mockVWAPCalculator = mock(VWAPCalculator.class);
        when(mockVWAPCalculator.getDataMap()).thenReturn(mockDataMap);

        TradeLogger mockTradeLogger = mock(TradeLogger.class);
        doNothing().when(mockTradeLogger).loadNewestToDB(any(), anyString(), anyBoolean());

        TradeResourceManager resourceManager = new TradeResourceManager(mockVWAPCalculator, 100, 500);
        resourceManager.setTradeLogger(mockTradeLogger);

        // Act
        resourceManager.manageResources(60);

        // Assert
        verify(mockTradeLogger, times(1)).loadNewestToDB(mockVWAPCalculator, "USD/EUR", true);
    }
    
    @Test
    void testManageResources_BackupTradesDueToElapsedTime() {
        // Arrange
        VWAPData mockVWAPData = mock(VWAPData.class);
        LocalDateTime lastBackupTime = LocalDateTime.now().minusMinutes(120); // Backup was over 119 minutes ago
        LocalDateTime lastBreachTime = LocalDateTime.now().minusMinutes(180); // Breach was over 179 minutes ago

        when(mockVWAPData.getCombinedQueueSize()).thenReturn(50); // Below the threshold
        when(mockVWAPData.getNewestQueueSize()).thenReturn(20);
        when(mockVWAPData.getOldQueueSize()).thenReturn(30);
        when(mockVWAPData.getLastBackup()).thenReturn(lastBackupTime);

        Map<String, VWAPData> mockDataMap = new HashMap<>();
        mockDataMap.put("USD/EUR", mockVWAPData);

        VWAPCalculator mockVWAPCalculator = mock(VWAPCalculator.class);
        when(mockVWAPCalculator.getDataMap()).thenReturn(mockDataMap);

        TradeLogger mockTradeLogger = mock(TradeLogger.class);
        doNothing().when(mockTradeLogger).loadNewestToDB(any(), anyString(), anyBoolean());

        TradeResourceManager resourceManager = new TradeResourceManager(mockVWAPCalculator, 100, 500);
        resourceManager.setTradeLogger(mockTradeLogger);
        resourceManager.setLastBreachTime(lastBreachTime);

        // Act
        resourceManager.manageResources(60); // minutesForVWAP = 60

        // Assert
        verify(mockTradeLogger, times(1)).loadNewestToDB(mockVWAPCalculator, "USD/EUR", false);
    }

    @Test
    void testManageResources_RestoreOldTradesToQueueExecutes() {
        // Arrange
        // Mock VWAPData
        VWAPData mockVWAPData = mock(VWAPData.class);
        when(mockVWAPData.getCombinedQueueSize()).thenReturn(50); // Below trade threshold
        when(mockVWAPData.getNewestQueueSize()).thenReturn(20);
        when(mockVWAPData.getOldQueueSize()).thenReturn(30);
        when(mockVWAPData.getLastBackup()).thenReturn(LocalDateTime.now().minusMinutes(30));
        when(mockVWAPData.getLastRestoredDataTimestamp()).thenReturn(LocalDateTime.now().minusMinutes(70)); // Ensures shouldRestoreTrades == true

        // Mock VWAPCalculator
        VWAPCalculator mockVWAPCalculator = mock(VWAPCalculator.class);
        Map<String, VWAPData> mockDataMap = Map.of("USD/EUR", mockVWAPData); // Single currency pair
        when(mockVWAPCalculator.getDataMap()).thenReturn(mockDataMap);

        // Mock TradeLogger
        TradeLogger mockTradeLogger = mock(TradeLogger.class);
        doNothing().when(mockTradeLogger).restoreOldTradesToQueue(any(), anyString(), anyLong(), anyLong());

        // Spy on TradeResourceManager to mock adjustRestoreFrequency
        TradeResourceManager resourceManager = new TradeResourceManager(mockVWAPCalculator, 100, 1000);
        resourceManager.setTradeLogger(mockTradeLogger); // Inject mock logger
        resourceManager.setLastBreachTime(LocalDateTime.now().minusMinutes(120)); // Ensure no interference with threshold conditions

        TradeResourceManager spyResourceManager = spy(resourceManager);
        doReturn(10L).when(spyResourceManager).adjustRestoreFrequency(anyInt(), anyLong()); // Mock restore frequency to 10 minutes

        // Act
        spyResourceManager.manageResources(60);

        // Assert
        verify(mockTradeLogger, times(1))
                .restoreOldTradesToQueue(mockVWAPCalculator, "USD/EUR", 10L, 60L); // Verify correct parameters
    }


    @Test
    void testadjustTradeThresholdPerPair_LowUsage() {
        // Arrange
        int totalTrades = 300; // Below 50% usage

        // Act
        int newThreshold = resourceManager.adjustTradeThresholdPerPair(totalTrades);

        // Assert
        assertEquals(100, newThreshold); // 10% of total threshold
    }

    @Test
    void testadjustTradeThresholdPerPair_HighUsage() {
        // Arrange
        int totalTrades = 950; // Above 95% usage

        // Act
        int newThreshold = resourceManager.adjustTradeThresholdPerPair(totalTrades);

        // Assert
        assertEquals(5, newThreshold); // 0.5% of total threshold
    }

    @Test
    void testAdjustRestoreFrequency_LowUsage_WithinRestoreThreshold() {
        // Arrange
        int tradeCount = 200; // Below 40% usage
        long minutesForVWAP = 60;
        
        // Mock the last breach time to 30 minutes ago
        resourceManager.setLastBreachTime(LocalDateTime.now().minusMinutes(30)); 

        // Act
        long restoreFrequency = resourceManager.adjustRestoreFrequency(tradeCount, minutesForVWAP);

        // Assert
        assertEquals(29, restoreFrequency, 1); // Expected 29, 48.33% of minutesForVWAP
    }

    @Test
    void testAdjustRestoreFrequency_LowUsage_ExceededRestoreThreshold() {
        // Arrange
        int tradeCount = 200; // Below 40% usage
        long minutesForVWAP = 60;

        // Mock the last breach time to 65 minutes ago
        resourceManager.setLastBreachTime(LocalDateTime.now().minusMinutes(65));

        // Act
        long restoreFrequency = resourceManager.adjustRestoreFrequency(tradeCount, minutesForVWAP);

        // Assert
        assertEquals(20000, restoreFrequency, 1); // Expected Long.MAX_VALUE as threshold exceeded
    }

    @Test
    void testAdjustRestoreFrequency_HighUsage() {
        // Arrange
        int tradeCount = 900; // Above 90% usage
        long minutesForVWAP = 60;

        // Act
        long restoreFrequency = resourceManager.adjustRestoreFrequency(tradeCount, minutesForVWAP);

        // Assert
        assertEquals(1, restoreFrequency); // Minimal restore frequency
    }

    @Test
    void testManageResources_EmergencyDumpToDBWhenTotalTradeThresholdExceeded() {
        // Arrange
        // Mock VWAPData object and its methods
        VWAPData mockVWAPData = mock(VWAPData.class);
        when(mockVWAPData.getCombinedQueueSize()).thenReturn(600); // High trade count
        when(mockVWAPData.getNewestQueueSize()).thenReturn(100);
        when(mockVWAPData.getOldQueueSize()).thenReturn(0);
        when(mockVWAPData.getLastBackup()).thenReturn(LocalDateTime.now().minusMinutes(120));
        when(mockVWAPData.getLastRestoredDataTimestamp()).thenReturn(LocalDateTime.now().minusMinutes(120));

        // Mock VWAPCalculator and provide mock data map
        VWAPCalculator mockVWAPCalculator = mock(VWAPCalculator.class);
        Map<String, VWAPData> mockDataMap = new HashMap<>();
        mockDataMap.put("USD/EUR", mockVWAPData);
        mockDataMap.put("JPY/USD", mockVWAPData); // Ensure total exceeds the threshold
        when(mockVWAPCalculator.getDataMap()).thenReturn(mockDataMap);

        // Mock TradeLogger methods
        TradeLogger mockTradeLogger = mock(TradeLogger.class);
        doNothing().when(mockTradeLogger).loadNewestToDB(any(), anyString(), anyBoolean());
        doNothing().when(mockTradeLogger).restoreOldTradesToQueue(any(), anyString(), anyLong(), anyLong());
        doNothing().when(mockTradeLogger).emergencyDumpToDB(any());

        // Inject dependencies into TradeResourceManager
        TradeResourceManager resourceManager = new TradeResourceManager(mockVWAPCalculator, 500, 1000);
        resourceManager.setTradeLogger(mockTradeLogger);

        // Spy on resourceManager to control adjustRestoreFrequency
        TradeResourceManager resourceManagerSpy = spy(resourceManager);
        doReturn(10L).when(resourceManagerSpy).adjustRestoreFrequency(anyInt(), anyLong());

        // Act
        resourceManagerSpy.manageResources(60);

        // Assert
        // Verify that emergencyDumpToDB was called once
        verify(mockTradeLogger, times(1)).emergencyDumpToDB(mockVWAPCalculator);

        // Ensure loadNewestToDB was invoked at least once
        verify(mockTradeLogger, atLeastOnce()).loadNewestToDB(any(), anyString(), anyBoolean());
    }
}
