package com.anz.trading.calculators.vwap.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.anz.trading.calculators.vwap.Trade;

public class H2TradeMemory extends H2TradeDAO {
	
	public H2TradeMemory(Connection connection) {
        super(connection);
    }

	@Override
    public void insertTradesFrom(Queue<Trade> trades) throws SQLException{
        throw new UnsupportedOperationException("insertTradesFrom is not supported in memory storage as it's only intended"
        		+ "for batch updating into the persistent database.");
    }
	
	@Override
    public void clearTrades() throws SQLException {
		String checkTableSQL = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TRADES'";
	    try (PreparedStatement pstmt = connection.prepareStatement(checkTableSQL);
	         ResultSet rs = pstmt.executeQuery()) {
	        if (rs.next() && rs.getInt(1) > 0) {
	            // Table exists, truncate it
	            try (Statement stmt = connection.createStatement()) {
	                stmt.execute("TRUNCATE TABLE TRADES");
	            }
	        }
	    }
    }

	@Override
	public void deleteDB() throws SQLException {
		throw new UnsupportedOperationException("deleteDB is not supported in memory storage as it's only intended"
        		+ "for resetting the persistent DB. Please use clearTrades() method instead");		
	}
	
	@Override
    public Queue<Trade> getAllTrades() throws SQLException {
    	Queue<Trade> trades = new ConcurrentLinkedDeque<>();
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
