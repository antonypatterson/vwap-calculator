package com.anz.trading.calculators.vwap.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class H2DatabaseUtility {

    // H2 persistent database connection URL
    private static final String PERSISTENT_DB_URL = "jdbc:h2:C:/NotBackedUp/tradeDAO;AUTO_SERVER=TRUE";
    
    // H2 in-memory database connection URL
    private static final String IN_MEMORY_DB_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";

    // H2 username and password (default is 'sa' with no password for local dev)
    private static final String USERNAME = "sa";
    private static final String PASSWORD = "";
    
    // Persistent and in-memory connections
    private static Connection persistentConnection;
    private static Connection inMemoryConnection;

    /**
     * Returns a connection to the persistent H2 database.
     */
    public static Connection getPersistentConnection() throws SQLException {
    	if (persistentConnection == null || persistentConnection.isClosed()) {
    		persistentConnection = DriverManager.getConnection(PERSISTENT_DB_URL, USERNAME, PASSWORD);
     	}    	
    	return persistentConnection;
    }

    /**
     * Returns a connection to the in-memory H2 database.
     */
    public static Connection getInMemoryConnection() throws SQLException {
        if (inMemoryConnection == null || inMemoryConnection.isClosed()) {
            inMemoryConnection = DriverManager.getConnection(IN_MEMORY_DB_URL, USERNAME, PASSWORD);
        }
        return inMemoryConnection;
    }

    // Optional: You could create a method to initialize the H2 database if needed
    public static void initializeDatabase() {
        try {
            // Ensures the H2 driver is loaded
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Closes the given persistent database connection.
     */
    public static void closePersistentConnection() throws SQLException {
        if (persistentConnection != null && !persistentConnection.isClosed()) {
            persistentConnection.close();
        }
    }
    
    /**
     * Closes the given in-memory database connection.
     */
    public static void closeInMemoryConnection() throws SQLException {
        if (inMemoryConnection != null && !inMemoryConnection.isClosed()) {
            inMemoryConnection.close();
        }
    }    
}