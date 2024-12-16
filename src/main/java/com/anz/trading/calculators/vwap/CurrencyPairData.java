package com.anz.trading.calculators.vwap;

public class CurrencyPairData {
    private final String[] currencyPairs;
    private final double[][] currencyPairStats;

    // Constructor to allow custom data
    public CurrencyPairData(String[] currencyPairs, double[][] currencyPairStats) {
        this.currencyPairs = currencyPairs;
        this.currencyPairStats = currencyPairStats;
    }

    // Factory method for default data
    public static CurrencyPairData createDefault() {
        return new CurrencyPairData(
            new String[]{
                "EUR/USD", "GBP/USD", "USD/JPY", "AUD/USD", "USD/CAD",
                "NZD/USD", "USD/CHF", "USD/SEK", "USD/NOK", "USD/HKD"
            },
            new double[][]{
                {1.15, 0.05},  // EUR/USD: mean=1.15, standard deviation=0.05
                {1.35, 0.06},  // GBP/USD: mean=1.35, standard deviation=0.06
                {110.50, 1.2}, // USD/JPY: mean=110.50, standard deviation=1.2
                {0.75, 0.03},  // AUD/USD: mean=0.75, standard deviation=0.03
                {1.25, 0.04},  // USD/CAD: mean=1.25, standard deviation=0.04
                {0.69, 0.02},  // NZD/USD: mean=0.69, standard deviation=0.02
                {0.92, 0.05},  // USD/CHF: mean=0.92, standard deviation=0.05
                {10.0, 0.3},   // USD/SEK: mean=10.0, standard deviation=0.3
                {9.0, 0.4},    // USD/NOK: mean=9.0, standard deviation=0.4
                {7.85, 0.15}   // USD/HKD: mean=7.85, standard deviation=0.15
            }
        );
    }

    public String[] getCurrencyPairs() {
        return currencyPairs;
    }

    public double[][] getCurrencyPairStats() {
        return currencyPairStats;
    }
}
