package com.anz.trading.calculators.vwap;

import java.time.LocalDateTime;

public final class Trade {
    private final double price;       
    private final long volume;
    private final LocalDateTime timestamp;
    private final String currencyPair;

    public Trade(double price, long volume, LocalDateTime timestamp, String currencyPair) {
        this.price = price;
        this.volume = volume;
        this.timestamp = timestamp;
        this.currencyPair = currencyPair;
    }

    public double getPrice() {
        return price;
    }

    public long getVolume() {
        return volume;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getCurrencyPair() {
        return currencyPair;
    }

}
