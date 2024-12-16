package com.anz.trading.calculators.vwap;

import java.time.LocalDateTime;
import java.util.Random;

public class TradeGenerator {
    private final Random random;
    private final CurrencyPairData currencyPairData;

    // Constructor to inject CurrencyPairData and Random
    public TradeGenerator(CurrencyPairData currencyPairData, Random random) {
        this.currencyPairData = currencyPairData;
        this.random = random;
    }

    // Overloaded constructor to use default Random
    public TradeGenerator(CurrencyPairData currencyPairData) {
        this(currencyPairData, new Random());
    }

    // Default constructor with default data and Random
    public TradeGenerator() {
        this(CurrencyPairData.createDefault(), new Random());
    }

    public Trade generateRandomTrade() {
        int index = random.nextInt(currencyPairData.getCurrencyPairs().length);

        // Retrieve currency pair and corresponding stats
        String currencyPair = currencyPairData.getCurrencyPairs()[index];
        double mean = currencyPairData.getCurrencyPairStats()[index][0];
        double standardDeviation = currencyPairData.getCurrencyPairStats()[index][1];

        // Generate price and volume
        double price = mean + (random.nextGaussian() * standardDeviation);
        long volume = 100 + random.nextInt(100000);
        LocalDateTime timestamp = LocalDateTime.now();

        return new Trade(price, volume, timestamp, currencyPair);
    }
}
