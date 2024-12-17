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

public class H2TradePersistent extends H2TradeDAO {


	public H2TradePersistent(Connection connection) {
        super(connection);
    }
	
	@Override
	public void insertTradesFrom(Queue<Trade> trades) throws SQLException {
	    String mergeSQL = "MERGE INTO trades AS target " +
	                      "USING (VALUES (?, ?, ?, ?)) AS source(price, volume, timestamp, currency_pair) " +
	                      "ON target.price = source.price " +
	                      "AND target.volume = source.volume " +
	                      "AND target.timestamp = source.timestamp " +
	                      "AND target.currency_pair = source.currency_pair " +
	                      "WHEN MATCHED THEN UPDATE SET target.price = source.price, " +
	                      "target.volume = source.volume, " +
	                      "target.timestamp = source.timestamp, " +
	                      "target.currency_pair = source.currency_pair " +
	                      "WHEN NOT MATCHED THEN INSERT (price, volume, timestamp, currency_pair) " +
	                      "VALUES (source.price, source.volume, source.timestamp, source.currency_pair)";

	    try (PreparedStatement pstmt = connection.prepareStatement(mergeSQL)) {
	        for (Trade trade : trades) {
	            pstmt.setDouble(1, trade.getPrice());        // Set price
	            pstmt.setLong(2, trade.getVolume());         // Set volume
	            pstmt.setObject(3, trade.getTimestamp());    // Set timestamp
	            pstmt.setString(4, trade.getCurrencyPair()); // Set currency pair
	            pstmt.addBatch();  // Add to batch
	        }
	        pstmt.executeBatch();  // Execute the batch of merges
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

	@Override
	public Queue<Trade> getAllTrades() throws SQLException {
		throw new UnsupportedOperationException("Operation prohibited without time constraints as performing "
				+ "this on persistent DB as an unfiltered query will likely crash the machine.");
	}
	
}
