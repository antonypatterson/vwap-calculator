package com.anz.trading.calculators.vwap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.anz.trading.calculators.vwap.dao.TradeDAO;

/**
 * Logs trades into the chosen location, including the JVM memory, in-memory DB, and persistent DB
 */

public class TradeLogger {
    private final TradeDAO inMemoryDAO;
    private final TradeDAO persistentDAO;
    private final List<Trade> tradeList; // In-memory list of trades used for testing cases
    

    public TradeLogger(TradeDAO inMemoryDAO, TradeDAO persistentDAO) {
        this.inMemoryDAO = inMemoryDAO;
        this.persistentDAO = persistentDAO;
        this.tradeList = new ArrayList<>();

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
    
    /**
     * Dumps all trades from the tradeList (JVM memory) to the persistent database.
     */
    public void dumpListToPersistentDB() {
        try {
            persistentDAO.insertTradesFrom(tradeList); // Insert trades from the tradeList
            tradeList.clear(); // Clear the tradeList after dumping
        } catch (SQLException e) {
            throw new RuntimeException("Failed to dump trade list to persistent DB", e);
        }
    }
    
    // Append a trade to the in-memory list of trades (used for testing)
    public void appendToList(Trade trade) {
        tradeList.add(trade); // Add trade to the in-memory list
    }
    
    // Get the list of trades (for testing purposes)
    public List<Trade> getTradeList() {
        return new ArrayList<>(tradeList); // Return a copy of the list to avoid external modifications
    }    
    
}
