package com.sabre.ix.application;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;

/**
 * User: Henrik Thorburn (SG0211570)
 * Date: 2011-11-30
 * Copyright (C) Sabre Inc
 */
public class ExportDBConnectionHandlerImpl implements ExportDBConnectionHandler {

    public static final String RUNNING_STATUS = "RUNNING";

    private SessionFactory sessionFactory;
    private String connectionString;
    private String userName;
    private String password;
    private String driver;

    @Override
    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    @Override
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void setDriver(String driver) {
        this.driver = driver;
    }

    @Override
    public Session getSession() throws ClassNotFoundException, SQLException {
        if (sessionFactory == null) {
            Class.forName(driver);
            Configuration cfg = new Configuration();
            cfg.configure("com/sabre/ix/application/dwstaging.cfg.xml");
            sessionFactory = cfg.buildSessionFactory();
            Connection con = DriverManager.getConnection(connectionString, userName, password);
            return sessionFactory.openSession(con);
        } else {
            Connection con = DriverManager.getConnection(connectionString, userName, password);
            return sessionFactory.openSession(con);
        }
    }

    @Override
    public boolean isRunning() throws ClassNotFoundException, SQLException {
        Session stagingSession = getSession();
        Criteria criteria = stagingSession.createCriteria(ProcessStatus.class);
        criteria.add(Restrictions.eq("prozessGruppe", "DWH2_ ASx"));
        ProcessStatus dwProcessStatus = (ProcessStatus) criteria.uniqueResult();
        return dwProcessStatus.getStatus() != null
                && dwProcessStatus.getStatus().equalsIgnoreCase(RUNNING_STATUS);
    }

    @Override
    public void setToRunning() throws ClassNotFoundException, SQLException {
        Session stagingSession = getSession();
        stagingSession.beginTransaction();
        Criteria criteria = stagingSession.createCriteria(ProcessStatus.class);
        criteria.add(Restrictions.eq("prozessGruppe", "ASx_DWH2"));
        ProcessStatus myProcessStatus = (ProcessStatus) criteria.uniqueResult();
        myProcessStatus.setStatus(RUNNING_STATUS);
        myProcessStatus.setStartTime(new Date());
        stagingSession.save(myProcessStatus);
        stagingSession.getTransaction().commit();
    }

    @Override
    public void setToStopped() throws ClassNotFoundException, SQLException {
        Session stagingSession = getSession();
        Criteria criteria = stagingSession.createCriteria(ProcessStatus.class);
        criteria.add(Restrictions.eq("prozessGruppe", "ASx_DWH2"));
        ProcessStatus myProcessStatus = (ProcessStatus) criteria.uniqueResult();
        myProcessStatus.setEndTime(new Date());
        myProcessStatus.setStatus("Finished");
        stagingSession.beginTransaction();
        stagingSession.save(myProcessStatus);
        stagingSession.getTransaction().commit();
        stagingSession.close();
        sessionFactory.close();
    }
}
