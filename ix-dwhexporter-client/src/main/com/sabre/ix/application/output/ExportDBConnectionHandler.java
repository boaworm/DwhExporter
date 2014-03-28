package com.sabre.ix.application.output;

import org.hibernate.Session;

import java.sql.SQLException;

/**
 * User: Henrik Thorburn (SG0211570)
 * Date: 2011-11-30
 * Copyright (C) Sabre Inc
 */
public interface ExportDBConnectionHandler {

    boolean isRunning() throws ClassNotFoundException, SQLException;
    void setToRunning() throws ClassNotFoundException, SQLException;
    void setToStopped() throws ClassNotFoundException, SQLException;

    Session getSession() throws ClassNotFoundException, SQLException;

    void setDriver(String driver);
    void setConnectionString(String connectionString);
    void setUserName(String userName);
    void setPassword(String password);
}
