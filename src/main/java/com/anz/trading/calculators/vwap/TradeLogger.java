package com.anz.trading.calculators.vwap;

import java.sql.SQLException;

import com.anz.trading.calculators.vwap.dao.TradeDAO;

public class TradeLogger {
    private final TradeDAO inMemoryDAO;
    private final TradeDAO persistentDAO;

    public TradeLogger(TradeDAO inMemoryDAO, TradeDAO persistentDAO) {
        this.inMemoryDAO = inMemoryDAO;
        this.persistentDAO = persistentDAO;

        try {
            inMemoryDAO.createTable();
            persistentDAO.createTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize databases", e);
        }
    }

    /**
     * Logs a trade into the in-memory database.
     * @param trade The trade to log.
     */
    public void logTrade(Trade trade) {
        try {
            inMemoryDAO.insertTrade(trade);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to log trade", e);
        }
    }

    /**
     * Dumps all trades from the in-memory database to the persistent database and clears the in-memory store.
     */
    public void dumpToPersistentDB() {
        try {
            persistentDAO.insertTradesFrom(inMemoryDAO.getAllTrades());
            inMemoryDAO.clearTrades();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to dump data to persistent DB", e);
        }
    }
}
