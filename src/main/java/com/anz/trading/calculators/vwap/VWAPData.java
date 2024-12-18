package com.anz.trading.calculators.vwap;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class VWAPData {
    private double sumPriceVolume = 0;
    private long sumVolume = 0;
    private final long minutesForVWAP;
    
    // Separate queues for newest and oldest data (for pre and post-database writing)
    private final Queue<Trade> tradeQueueNewest = new ConcurrentLinkedDeque<>();
    private final Queue<Trade> tradeQueueOld = new ConcurrentLinkedDeque<>();  
    
    // Initial inputs
    LocalDateTime now = LocalDateTime.now();
    private LocalDateTime lastBackup = LocalDateTime.of(2000, 1, 1, 0, 0, 0, 0);
    private LocalDateTime lastRestore = LocalDateTime.of(2000, 1, 1, 0, 0, 0, 0);
    private LocalDateTime lastRestoredDataTimestamp; 
    
    public VWAPData(long minutesForVWAP) {
        this.minutesForVWAP = minutesForVWAP;
        this.lastRestoredDataTimestamp = now.minusMinutes(minutesForVWAP + 1);
    }

    // Add new data point and maintain the 1-hour window
    public synchronized void addDataPoint(Trade trade) {
    	
        // Remove old data points that are older than 1 hour from the current timestamp
        LocalDateTime currentTime = trade.getTimestamp();
        // 1st while loop only used if trade volume is small enough to entirely retain on memory
        while (!tradeQueueNewest.isEmpty() && 
        		tradeQueueNewest.peek().getTimestamp().isBefore(currentTime.minusMinutes(minutesForVWAP))) {
            Trade oldest = tradeQueueNewest.poll();
            sumPriceVolume -= oldest.getPrice() * oldest.getVolume();
            sumVolume -= oldest.getVolume();
        }
        // 2nd while loop to remove oldest trades from old Queue and adjust numerator/denominator values
        while (!tradeQueueOld.isEmpty() && 
        		tradeQueueOld.peek().getTimestamp().isBefore(currentTime.minusMinutes(minutesForVWAP))) {
            Trade oldest = tradeQueueOld.poll();
            sumPriceVolume -= oldest.getPrice() * oldest.getVolume();
            sumVolume -= oldest.getVolume();
        }     

        // Add the new data point
        tradeQueueNewest.add(trade);
        sumPriceVolume += trade.getPrice() * trade.getVolume();
        sumVolume += trade.getVolume();
    }
    
    // Computationally efficient way to add back to the old queue without performing any other operations
    // As those will be handled separately in the addDataPoint
    public synchronized void restoreDataPoint(Trade trade) {
    	tradeQueueOld.add(trade);    	
    }

    // Get the current VWAP
    public synchronized double getVWAP() {
    	return sumVolume == 0 ? 0 : sumPriceVolume / sumVolume;
    }
    
    // For a queue of Trades fed from an external source (i.e. database snapshot)
    public synchronized void restoreTrades(Queue<Trade> trades) {
        for (Trade trade : trades) {
            tradeQueueOld.add(trade);
        }
    }
    
    public synchronized Queue<Trade> getNewestQueueAndClear() {
        // Create an unmodifiable snapshot as a Queue
        Queue<Trade> snapshot = new ConcurrentLinkedDeque<>(tradeQueueNewest);

        // Clear the tradeQueueNewest and reset variables
        tradeQueueNewest.clear();
        return snapshot;
    }
    
    // Helper methods
    public int getNewestQueueSize() {
        return tradeQueueNewest.size();
    }
    
    public int getOldQueueSize() {
        return tradeQueueOld.size();
    }
    
    public int getCombinedQueueSize() {
    	return tradeQueueNewest.size() + tradeQueueOld.size();
    }
    
    // Make getters of the queue immutable as they are used for the purpose of snapshotting
    public Queue<Trade> getNewestQueue() {
    	return new ConcurrentLinkedDeque<>(tradeQueueNewest);
    }

    public Queue<Trade> getOldQueue() {
        return new ConcurrentLinkedDeque<>(tradeQueueOld);
    }

	public LocalDateTime getLastBackup() {
		return lastBackup;
	}

	public void setLastBackup(LocalDateTime lastBackup) {
		this.lastBackup = lastBackup;
	}

	public LocalDateTime getLastRestore() {
		return lastRestore;
	}

	public void setLastRestore(LocalDateTime lastRestore) {
		this.lastRestore = lastRestore;
	}

	public LocalDateTime getLastRestoredDataTimestamp() {
		return lastRestoredDataTimestamp;
	}

	public void setLastRestoredDataTimestamp(LocalDateTime lastRestoredDataTimestamp) {
		this.lastRestoredDataTimestamp = lastRestoredDataTimestamp;
	}
}