package com.anz.trading.calculators.vwap.dao;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Queue;

import com.anz.trading.calculators.vwap.Trade;

public interface TradeDAO {
    void createTable() throws SQLException;
    void insertTrade(Trade trade) throws SQLException;
    void insertTradesFrom(Queue<Trade> trades) throws SQLException;
    void clearTrades() throws SQLException;
    void deleteDB() throws SQLException;
    Queue<Trade> getAllTrades() throws SQLException;
	Queue<Trade> getAllTrades(LocalDateTime startTime, LocalDateTime endTime, String currencyPair) throws SQLException;
}
