package com.anz.trading.calculators.vwap;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

public class TradeLogger {
    private static final String IN_MEMORY_DB_URL = "jdbc:h2:mem:trades_db"; // In-memory DB
    private static final String PERSISTENT_DB_URL = "jdbc:h2:./trades_persistent_db"; // Persistent DB
    private Connection inMemoryConnection;
    private Connection persistentConnection;

    public TradeLogger() {
        try {
            // Initialize both databases
            inMemoryConnection = DriverManager.getConnection(IN_MEMORY_DB_URL, "sa", "");
            persistentConnection = DriverManager.getConnection(PERSISTENT_DB_URL, "sa", "");

            // Create tables in both databases
            createTable(inMemoryConnection);
            createTable(persistentConnection);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize databases", e);
        }
    }

    // Create a table for trade data
    private void createTable(Connection connection) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS trades (" + 
                "id IDENTITY PRIMARY KEY, " +
                "timestamp TIMESTAMP, " +
                "currency_pair VARCHAR(10), " +
                "price DOUBLE, " +
                "volume BIGINT)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }

    // Log a trade into the in-memory database
    public void logTrade(String currencyPair, double price, long volume, LocalDateTime timestamp) {
        String insertSQL = "INSERT INTO trades (timestamp, currency_pair, price, volume) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = inMemoryConnection.prepareStatement(insertSQL)) {
            pstmt.setObject(1, timestamp);
            pstmt.setString(2, currencyPair);
            pstmt.setDouble(3, price);
            pstmt.setLong(4, volume);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to log trade", e);
        }
    }

    // Periodically dump data from in-memory DB to persistent DB
    public void dumpToPersistentDB() {
        String transferSQL = "INSERT INTO trades (timestamp, currency_pair, price, volume) " +
            "SELECT timestamp, currency_pair, price, volume FROM trades";
        try {
            // Transfer data
            try (PreparedStatement transferStmt = persistentConnection.prepareStatement(transferSQL)) {
                transferStmt.executeUpdate();
            }

            // Clear the in-memory table after transfer
            try (Statement clearStmt = inMemoryConnection.createStatement()) {
                clearStmt.execute("TRUNCATE TABLE trades");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to dump data to persistent DB", e);
        }
    }
}
