package com.anz.trading.calculators.vwap.dao;

import java.sql.SQLException;
import java.util.List;

import com.anz.trading.calculators.vwap.Trade;

public interface TradeDAO {
    void createTable() throws SQLException;
    void insertTrade(Trade trade) throws SQLException;
    void insertTradesFrom(List<Trade> trades) throws SQLException;
    void clearTrades() throws SQLException;
    void deleteDB() throws SQLException;
    List<Trade> getAllTrades() throws SQLException;
}
