package com.anz.trading.calculators.vwap.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.anz.trading.calculators.vwap.Trade;

public class H2TradeMemory extends H2TradeDAO {
	
	public H2TradeMemory(Connection connection) {
        super(connection);
    }

	@Override
    public void insertTradesFrom(List<Trade> trades) {
        throw new UnsupportedOperationException("insertTradesFrom is not supported in memory storage as it's only intended"
        		+ "for batch updating into the persistent database.");
    }
	
	@Override
    public void clearTrades() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE trades");
        }
    }

	@Override
	public void deleteDB() throws SQLException {
		throw new UnsupportedOperationException("deleteDB is not supported in memory storage as it's only intended"
        		+ "for resetting the persistent DB. Please use clearTrades() method instead");		
	}

}
