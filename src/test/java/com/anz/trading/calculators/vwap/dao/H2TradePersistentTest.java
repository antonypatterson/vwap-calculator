package com.anz.trading.calculators.vwap.dao;

import com.anz.trading.calculators.vwap.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class H2TradePersistentTest {
    
    private TradeDAO tradeDAO;
    private Connection mockConnection;

    @BeforeEach
    void setUp() throws SQLException {
        // Setup a mock connection
        mockConnection = mock(Connection.class);
        tradeDAO = new H2TradePersistent(mockConnection);
    }

    @Test
    void testCreateTable() throws SQLException {
        // Mock the connection behavior for creating a table
        Statement mockStatement = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        
        // Call the method
        tradeDAO.createTable();
        
        // Verify if the correct SQL was executed
        verify(mockStatement, times(1)).execute(anyString());
        
    }

    @Test
    void testInsertTrade() throws SQLException {
        // Create a sample trade
        Trade trade = new Trade(100.0, 50L, LocalDateTime.now(), "EUR/USD");

        // Prepare mock behavior for PreparedStatement
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        
        // Call the method
        tradeDAO.insertTrade(trade);
        
        // Verify that the PreparedStatement was executed
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void testInsertTradesFrom() throws SQLException {
        // Create a queue of trades
        Queue<Trade> trades = new ConcurrentLinkedDeque<>();
        trades.add(new Trade(100.0, 50L, LocalDateTime.now(), "EUR/USD"));
        trades.add(new Trade(105.0, 60L, LocalDateTime.now(), "USD/JPY"));

        // Prepare the mock behavior for the PreparedStatement
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        
        // Call the method
        tradeDAO.insertTradesFrom(trades);
        
        // Verify that executeBatch() was called once for the batch insert
        verify(mockPreparedStatement, times(1)).executeBatch();
    }

    @Test
    void testGetAllTrades() throws SQLException {
        // Create a queue of trades (mocked result set data)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tradeTimestamp1 = now.minusHours(2);
        LocalDateTime tradeTimestamp2 = now.minusHours(1);

        // Mock the behavior of ResultSet
        ResultSet mockResultSet = mock(ResultSet.class);

        // Simulate two rows, but only one matches the filter
        when(mockResultSet.next())
            .thenReturn(true)  // First row exists
            .thenReturn(false); // No second row (simulate filter behavior)

        // Configure first row data to match the filter
        when(mockResultSet.getDouble("price")).thenReturn(100.0);
        when(mockResultSet.getLong("volume")).thenReturn(50L);
        when(mockResultSet.getObject("timestamp", LocalDateTime.class)).thenReturn(tradeTimestamp1);
        when(mockResultSet.getString("currency_pair")).thenReturn("EUR/USD");

        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        // Call the method
        Queue<Trade> result = tradeDAO.getAllTrades(now.minusDays(1), now, "EUR/USD");

        // Verify the result
        assertNotNull(result);
        assertEquals(1, result.size(), "Only one trade should match the given currency pair.");

        // Assert the trade details
        Trade trade = result.peek();
        assertEquals(100.0, trade.getPrice(), 0.001, "Trade price should match.");
        assertEquals(50L, trade.getVolume(), "Trade volume should match.");
        assertEquals("EUR/USD", trade.getCurrencyPair(), "Trade currency pair should match.");
        assertEquals(tradeTimestamp1, trade.getTimestamp(), "Trade timestamp should match.");
    }

    @Test
    void testDeleteDB() throws SQLException {
        // Mock the PreparedStatement for the existence check
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockConnection.prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TRADES'"))
            .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        // Mock the ResultSet to return that the table exists
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1); // Simulate table exists

        // Mock the Statement for truncating the table
        Statement mockStatement = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        // Call the method
        tradeDAO.deleteDB();

        // Verify the existence check was performed
        verify(mockPreparedStatement, times(1)).executeQuery();

        // Verify that the TRUNCATE TABLE SQL was executed
        verify(mockStatement, times(1)).execute("TRUNCATE TABLE TRADES");
    }
    
    @Test
    void testDeleteDBWhenTableDoesNotExist() throws SQLException {
        // Mock the PreparedStatement for the existence check
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockConnection.prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TRADES'"))
            .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        // Mock the ResultSet to return that the table does not exist
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(0); // Simulate table does not exist

        // Call the method
        tradeDAO.deleteDB();

        // Verify the existence check was performed
        verify(mockPreparedStatement, times(1)).executeQuery();

        // Verify that the TRUNCATE TABLE SQL was not executed
        verify(mockConnection, never()).createStatement();
    }
    
    @Test
    void testCreateTableIntegration() throws SQLException {
        // Use the persistent H2 database connection string
        String jdbcUrl = "jdbc:h2:C:/NotBackedUp/tradeDAO;AUTO_SERVER=TRUE";  // Emulate your persistent DB URL
        
        try (Connection realConnection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            // Pass the real connection to the H2TradeDAO constructor
            TradeDAO tradeDAO = new H2TradePersistent(realConnection);

            // Call the method that creates the table
            tradeDAO.createTable();
            
            // Verify if the table was created by querying the information schema
            String checkTableSQL = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TRADES'";  // Use lowercase table name
            try (Statement stmt = realConnection.createStatement(); 
                 ResultSet rs = stmt.executeQuery(checkTableSQL)) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    assertEquals(1, count, "Table 'TRADES' should be created.");
                }
            }
            
            // Optionally, check if indexes were created (you can adapt this part to check the indexes you expect)
            String checkIndexSQL = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'TRADES'";  // Use lowercase table name
            try (Statement stmt = realConnection.createStatement(); 
                 ResultSet rs = stmt.executeQuery(checkIndexSQL)) {
                if (rs.next()) {
                    int indexCount = rs.getInt(1);
                    assertTrue(indexCount > 0, "Expected indexes on the 'TRADES' table.");
                }
            }
        }
    }
    
    @Test
    void testGetAllTradesIntegration() throws SQLException {
        String jdbcUrl = "jdbc:h2:C:/NotBackedUp/tradeDAO;AUTO_SERVER=TRUE"; // In-memory H2 database for testing

        try (Connection realConnection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            // Set up the DAO with the real connection
            TradeDAO tradeDAO = new H2TradePersistent(realConnection);

            // 1. Delete the database if exists
            tradeDAO.deleteDB();

            // 2. Create the table
            tradeDAO.createTable();

            // 3. Insert dummy data
            String insertSQL = "INSERT INTO TRADES (price, volume, timestamp, currency_pair) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = realConnection.prepareStatement(insertSQL)) {
                // Add 2 trades for EUR/USD
                pstmt.setDouble(1, 1.0);
                pstmt.setLong(2, 50);
                pstmt.setObject(3, LocalDateTime.now().minusHours(2));
                pstmt.setString(4, "EUR/USD");
                pstmt.executeUpdate();

                pstmt.setDouble(1, 1.05);
                pstmt.setLong(2, 60);
                pstmt.setObject(3, LocalDateTime.now().minusHours(1));
                pstmt.setString(4, "EUR/USD");
                pstmt.executeUpdate();

                // Add 1 trade for USD/JPY
                pstmt.setDouble(1, 110.0);
                pstmt.setLong(2, 70);
                pstmt.setObject(3, LocalDateTime.now().minusHours(3));
                pstmt.setString(4, "USD/JPY");
                pstmt.executeUpdate();
            }

            // 4. Call getAllTrades with EUR/USD filter
            LocalDateTime startTime = LocalDateTime.now().minusDays(1);
            LocalDateTime endTime = LocalDateTime.now();
            Queue<Trade> result = tradeDAO.getAllTrades(startTime, endTime, "EUR/USD");

            // 5. Verify the result
            assertNotNull(result, "The result should not be null.");
            assertEquals(2, result.size(), "There should be 2 trades for EUR/USD.");

            // Assert trade details
            Iterator<Trade> iterator = result.iterator();

            Trade trade1 = iterator.next();
            assertEquals(1.0, trade1.getPrice(), 0.001, "First trade price should match.");
            assertEquals(50, trade1.getVolume(), "First trade volume should match.");
            assertEquals("EUR/USD", trade1.getCurrencyPair(), "First trade currency pair should match.");

            Trade trade2 = iterator.next();
            assertEquals(1.05, trade2.getPrice(), 0.001, "Second trade price should match.");
            assertEquals(60, trade2.getVolume(), "Second trade volume should match.");
            assertEquals("EUR/USD", trade2.getCurrencyPair(), "Second trade currency pair should match.");
        }
    }
}
