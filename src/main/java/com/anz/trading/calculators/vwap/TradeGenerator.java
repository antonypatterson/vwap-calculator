package com.anz.trading.calculators.vwap;

import java.time.LocalDateTime;
import java.util.Random;

public class TradeGenerator {
    private static final Random RANDOM = new Random();

    // Generate a random trade
    public static Trade generateRandomTrade() {
        // Randomly generates an index between 0 and the length of the ccy pairs array - 1
        int index = RANDOM.nextInt(CurrencyPairData.CURRENCY_PAIRS.length);

        // Retrieve currency pair and corresponding stats
        String currencyPair = CurrencyPairData.CURRENCY_PAIRS[index];
        double mean = CurrencyPairData.CURRENCY_PAIR_STATS[index][0];
        double standardDeviation = CurrencyPairData.CURRENCY_PAIR_STATS[index][1];

        // Generate price and volume
        double price = mean + (RANDOM.nextGaussian() * standardDeviation);
        long volume = 100 + RANDOM.nextInt(100000); // Random volume between 100 and 100,100
        LocalDateTime timestamp = LocalDateTime.now();

        return new Trade(price, volume, timestamp, currencyPair);
    }
}
