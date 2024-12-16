package com.anz.trading.calculators.vwap.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.anz.trading.calculators.vwap.Trade;

public class H2TradePersistent extends H2TradeDAO {

	public H2TradePersistent(Connection connection) {
        super(connection);
    }
	
	@Override
    public void insertTradesFrom(List<Trade> trades) throws SQLException {
        // Reordered to match Trade input order (price, volume, timestamp, currencyPair)
        String insertSQL = "INSERT INTO trades (price, volume, timestamp, currency_pair) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            for (Trade trade : trades) {
                pstmt.setDouble(1, trade.getPrice());        // Set price
                pstmt.setLong(2, trade.getVolume());         // Set volume
                pstmt.setObject(3, trade.getTimestamp());    // Set timestamp (using setObject)
                pstmt.setString(4, trade.getCurrencyPair()); // Set currency pair
                pstmt.addBatch();  // Add to batch
            }
            pstmt.executeBatch();  // Execute the batch of inserts
        } catch (SQLException e) {
            e.printStackTrace();  // Handle exceptions properly
        }
    }
	
	@Override
    public void clearTrades() throws SQLException {
		throw new UnsupportedOperationException("clearTrades is not supported in persistent storage as it's only intended"
        		+ "periodically cleaning up in-memory storage. Please use deleteDB() method instead");
	}

	@Override
	public void deleteDB() throws SQLException {
		try (Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE trades");
        }		
	}	
	
	
}
