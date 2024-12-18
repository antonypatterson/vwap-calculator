package com.anz.trading.calculators.vwap.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.anz.trading.calculators.vwap.Trade;

public abstract class H2TradeDAO implements TradeDAO {
    protected final Connection connection;
    
    

    public H2TradeDAO(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void createTable() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS trades (" +
                "id IDENTITY PRIMARY KEY, " +
                "price DOUBLE, " +              
                "volume BIGINT, " +             
                "timestamp TIMESTAMP, " +    
                "currency_pair VARCHAR(10))";   

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
        }

        // Create indexes on the columns used in the MERGE statement for faster lookups
        createIndexIfNotExists("idx_price", "price");
        createIndexIfNotExists("idx_volume", "volume");
        createIndexIfNotExists("idx_timestamp", "timestamp");
        createIndexIfNotExists("idx_currency_pair", "currency_pair");
    }
    
    private void createIndexIfNotExists(String indexName, String columnName) throws SQLException {
        String checkIndexSQL = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES " +
                               "WHERE UPPER(TABLE_NAME) = 'TRADES' AND UPPER(INDEX_NAME) = UPPER(?)";

        try (PreparedStatement stmt = connection.prepareStatement(checkIndexSQL)) {
            if (stmt == null) {
                System.err.println("PreparedStatement is null, check your connection or query.");
                return; // Exit if PreparedStatement could not be created
            }

            stmt.setString(1, indexName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    // Index doesn't exist, create it
                    String createIndexSQL = "CREATE INDEX " + indexName + " ON trades(" + columnName + ")";
                    try (Statement createStmt = connection.createStatement()) {
                        createStmt.execute(createIndexSQL);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking or creating index: " + e.getMessage());
            throw e; // Rethrow to propagate the error
        }
    }

    @Override
    public void insertTrade(Trade trade) throws SQLException {
        // Reordered to match Trade input order (price, volume, timestamp, currencyPair)
        String insertSQL = "INSERT INTO trades (price, volume, timestamp, currency_pair) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            pstmt.setDouble(1, trade.getPrice());        // Set price
            pstmt.setLong(2, trade.getVolume());         // Set volume
            pstmt.setObject(3, trade.getTimestamp());    // Set timestamp (using setObject)
            pstmt.setString(4, trade.getCurrencyPair()); // Set currency pair
            pstmt.executeUpdate();  // Execute the insert
        }
    }
    
    @Override
    public Queue<Trade> getAllTrades(LocalDateTime startTime, LocalDateTime endTime, String currencyPair) throws SQLException {
        Queue<Trade> trades = new ConcurrentLinkedDeque<>();
        
        // Updated SQL query to include currencyPair filter
        String selectSQL = "SELECT * FROM trades WHERE timestamp BETWEEN ? AND ? AND currency_pair = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setObject(1, startTime);  // Set the start time parameter
            pstmt.setObject(2, endTime);    // Set the end time parameter
            pstmt.setString(3, currencyPair); // Set the currency pair parameter
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Map the result set to a Trade object
                    double price = rs.getDouble("price");
                    long volume = rs.getLong("volume");
                    LocalDateTime timestamp = rs.getObject("timestamp", LocalDateTime.class);

                    Trade trade = new Trade(price, volume, timestamp, currencyPair);
                    trades.add(trade);
                }
            }
        }
        return trades;
    }
    
    public abstract void insertTradesFrom(Queue<Trade> trades) throws SQLException;    
    
    public abstract void clearTrades() throws SQLException;
    
    public abstract void deleteDB() throws SQLException;
    
    public abstract Queue<Trade> getAllTrades() throws SQLException;
}
