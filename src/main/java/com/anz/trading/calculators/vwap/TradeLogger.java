package com.anz.trading.calculators.vwap;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.anz.trading.calculators.vwap.dao.TradeDAO;

/**
 * Logs trades into the chosen location, including the JVM memory, in-memory DB, and persistent DB
 */

public class TradeLogger {
    private final TradeDAO inMemoryDAO;
    private final TradeDAO persistentDAO;
    private final Queue<Trade> tradeList; // In-memory list of trades used for testing cases
    
    // Constructor for both DAOs
    public TradeLogger(TradeDAO inMemoryDAO, TradeDAO persistentDAO) {
        this.inMemoryDAO = inMemoryDAO;
        this.persistentDAO = persistentDAO;
        this.tradeList = new ConcurrentLinkedDeque<>();

        try {
            if (inMemoryDAO != null) {
                inMemoryDAO.createTable();
            }
            persistentDAO.createTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize databases", e);
        }
    }

    // Constructor for persistentDAO only
    public TradeLogger(TradeDAO persistentDAO) {
        this(null, persistentDAO);
    }
    
    /**
     * Queries the persistent database, and restores
     */
    public void restoreOldTradesToQueue(VWAPCalculator vwapCalculator, 
    		String currencyPair, long minutesToRestore, long minutesForVWAP) {
        // Define the time range for restoration
        VWAPData vwapData = vwapCalculator.getVWAPData(currencyPair);
        
        // Determine the start time for the query
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime rollingWindowStartTime = now.minusMinutes(minutesForVWAP + 1);
        LocalDateTime restoreStartTime = rollingWindowStartTime.isAfter(vwapData.getLastRestoredDataTimestamp())
                ? rollingWindowStartTime
                : vwapData.getLastRestoredDataTimestamp();
        LocalDateTime restoreEndTime = restoreStartTime.plusMinutes(minutesToRestore);

        // Fetch trades from the persistent database based on the time range
        Queue<Trade> tradesToRestore;
        try {
            tradesToRestore = persistentDAO.getAllTrades(restoreStartTime, restoreEndTime, currencyPair);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch trades from the database", e);
        }

        // Add the queue elements back into the vwapCalculator object
        for (Trade trade : tradesToRestore) {
        	vwapData.restoreDataPoint(trade);  // Or add the trade to VWAPCalculator
        }
        
        vwapData.setLastRestore(LocalDateTime.now());

        // Update the last restored data timestamp in VWAPData
        if (!tradesToRestore.isEmpty()) {
            Trade latestTrade = tradesToRestore.stream()
                                              .max(Comparator.comparing(Trade::getTimestamp))
                                              .orElseThrow();
            vwapData.setLastRestoredDataTimestamp(latestTrade.getTimestamp());
        }
    }
    
    
    /**
     * Grabs all trades from the newest queue within each VWAPData object, clears it 
     * from memory and then writes to the database
     */
    public void loadNewestToDB(VWAPCalculator vwapCalculator, String currencyPair, boolean clearMemory) {
    	// Define the time range for reloading data
        VWAPData vwapData = vwapCalculator.getVWAPData(currencyPair);
    	
        Queue<Trade> combinedQueue;
    	if (clearMemory) {
        	combinedQueue = vwapData.getNewestQueueAndClear();
        } else {
        	combinedQueue = vwapData.getNewestQueue();
        }
    	
    	try {
    		persistentDAO.insertTradesFrom(combinedQueue);
    		// Write the last backup timestamp to the VWAP object of that currency pair
    		vwapData.setLastBackup(LocalDateTime.now());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to dump data to persistent DB", e);
        }     	
    }
    
    /**
     * ONLY to be used as a last resort when memory is close to completely full
     */
    public int emergencyDumpToDB(VWAPCalculator vwapCalculator) {
    	int tradesThatWereDumped = 0;
		Map<String, VWAPData> dataMap = vwapCalculator.getDataMap();
	    Queue<Trade> snapshot;
	    Queue<Trade> combinedQueue = new ConcurrentLinkedDeque<Trade>();
	    
	    for (Map.Entry<String, VWAPData> entry : dataMap.entrySet()) {
	        VWAPData vwapData = entry.getValue();
	        tradesThatWereDumped += vwapData.getNewestQueueSize();
	        snapshot = vwapData.getNewestQueueAndClear();
	        combinedQueue.addAll(snapshot);
	    }    

	    // Now write to the database
	    try {
	        persistentDAO.insertTradesFrom(combinedQueue);
	    } catch (SQLException e) {
	        throw new RuntimeException("Failed to dump data to persistent DB", e);
	    }    	
	    
	    return tradesThatWereDumped;
    }
       

    /**
     * Logs a single trade into the in-memory database.
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
    
    // Optional method should you choose to append each individual trade to separate list
    public void appendToList(Trade trade) {
        tradeList.add(trade); // Add trade to the in-memory list
    }
    
    // Get the list of trades (for testing purposes)
    public Queue<Trade> getTradeList() {
        return new ConcurrentLinkedDeque<>(tradeList); // Return a copy of the list to avoid external modifications
    }
}
