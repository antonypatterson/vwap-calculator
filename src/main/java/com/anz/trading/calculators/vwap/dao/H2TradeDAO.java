package com.anz.trading.calculators.vwap.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        String createIndexSQL = "CREATE INDEX IF NOT EXISTS idx_price ON trades(price)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createIndexSQL);
        }

        createIndexSQL = "CREATE INDEX IF NOT EXISTS idx_volume ON trades(volume)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createIndexSQL);
        }

        createIndexSQL = "CREATE INDEX IF NOT EXISTS idx_timestamp ON trades(timestamp)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createIndexSQL);
        }

        createIndexSQL = "CREATE INDEX IF NOT EXISTS idx_currency_pair ON trades(currency_pair)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createIndexSQL);
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
    
    public abstract void insertTradesFrom(List<Trade> trades) throws SQLException;    
    
    public abstract void clearTrades() throws SQLException;
    
    public abstract void deleteDB() throws SQLException;
    
    @Override
    public List<Trade> getAllTrades() throws SQLException {
        List<Trade> trades = new ArrayList<>();
        String selectSQL = "SELECT * FROM trades";
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Map the result set to a Trade object
                    double price = rs.getDouble("price");
                    long volume = rs.getLong("volume");
                    LocalDateTime timestamp = rs.getObject("timestamp", LocalDateTime.class);
                    String currencyPair = rs.getString("currency_pair");

                    Trade trade = new Trade(price, volume, timestamp, currencyPair);
                    trades.add(trade);
                }
            }
        }
        return trades;
    }
}
