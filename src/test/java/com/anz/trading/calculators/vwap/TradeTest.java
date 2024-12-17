package com.anz.trading.calculators.vwap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Fairly trivial test file to ensure the constructors and getters 
 * of Trade object are working as expected
 */
public class TradeTest {

    @Test
    public void testTradeConstructorAndGetters() {
        // Given
        double expectedPrice = 100.5;
        long expectedVolume = 500;
        LocalDateTime expectedTimestamp = LocalDateTime.of(2024, 12, 16, 10, 30, 0, 0);
        String expectedCurrencyPair = "USD/EUR";

        // When
        Trade trade = new Trade(expectedPrice, expectedVolume, expectedTimestamp, expectedCurrencyPair);

        // Then
        assertEquals(expectedPrice, trade.getPrice());
        assertEquals(expectedVolume, trade.getVolume());
        assertEquals(expectedTimestamp, trade.getTimestamp());
        assertEquals(expectedCurrencyPair, trade.getCurrencyPair());
    }
}
