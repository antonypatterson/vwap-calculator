package com.anz.trading.calculators.vwap;

import java.time.LocalDateTime;
import java.util.Random;

public class TradeGenerator {
    private static final String[] CURRENCY_PAIRS = { "EUR/USD", "GBP/USD", "USD/JPY", "AUD/USD", "USD/CAD" };

    // Define mean and standard deviation for each currency pair
    private static final double[][] CURRENCY_PAIR_STATS = {
        {1.15, 0.05}, // EUR/USD: mean=1.15, standard deviation=0.05
        {1.35, 0.06}, // GBP/USD: mean=1.35, standard deviation=0.06
        {110.50, 1.2}, // USD/JPY: mean=110.50, standard deviation=1.2
        {0.75, 0.03}, // AUD/USD: mean=0.75, standard deviation=0.03
        {1.25, 0.04}  // USD/CAD: mean=1.25, standard deviation=0.04
    };

    private static final Random RANDOM = new Random();

    // Generate a random trade
    public static Trade generateRandomTrade() {
        // Select a random currency pair
        String currencyPair = CURRENCY_PAIRS[RANDOM.nextInt(CURRENCY_PAIRS.length)];

        // Find the index of the selected currency pair
        int index = getCurrencyPairIndex(currencyPair);

        // Get the mean and standard deviation for this currency pair
        double mean = CURRENCY_PAIR_STATS[index][0];
        double standardDeviation = CURRENCY_PAIR_STATS[index][1];

        // Generate a price with Gaussian distribution around the mean
        double price = mean + (RANDOM.nextGaussian() * standardDeviation); // Random price based on mean and standard deviation
        long volume = 100 + RANDOM.nextInt(1000); // Random volume between 100 and 1100
        LocalDateTime timestamp = LocalDateTime.now();

        return new Trade(price, volume, timestamp, currencyPair);
    }

    // Get the index of a given currency pair
    private static int getCurrencyPairIndex(String currencyPair) {
        for (int i = 0; i < CURRENCY_PAIRS.length; i++) {
            if (CURRENCY_PAIRS[i].equals(currencyPair)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Currency pair not found: " + currencyPair);
    }
}
