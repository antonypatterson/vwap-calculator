package com.anz.trading.calculators.vwap;

import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VWAPData {
    private double sumPriceVolume = 0;
    private long sumVolume = 0;
    
    // Use ConcurrentLinkedQueue to allow concurrent access
    private Queue<Trade> tradeQueue = new ConcurrentLinkedQueue<>();

    // Add new data point and maintain the 1-hour window
    public synchronized void addDataPoint(Trade trade) {
        // Remove old data points that are older than 1 hour from the current timestamp
        LocalDateTime currentTime = trade.getTimestamp();
        while (!tradeQueue.isEmpty() && tradeQueue.peek().getTimestamp().isBefore(currentTime.minusHours(1))) {
            Trade oldest = tradeQueue.poll();
            sumPriceVolume -= oldest.getPrice() * oldest.getVolume();
            sumVolume -= oldest.getVolume();
        }

        // Add the new data point
        tradeQueue.add(trade);
        sumPriceVolume += trade.getPrice() * trade.getVolume();
        sumVolume += trade.getVolume();
    }

    // Get the current VWAP
    public synchronized double getVWAP() {
        return sumVolume == 0 ? 0 : sumPriceVolume / sumVolume;
    }
}
