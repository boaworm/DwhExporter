package com.sabre.ix.application;

import org.hibernate.*;
import org.hibernate.stat.SessionStatistics;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: Henrik Thorburn (sg0211570)
 * Date: 2014-03-26
 */
public class MockExportDBConnectionHandlerImpl implements ExportDBConnectionHandler {
    @Override
    public boolean isRunning() throws ClassNotFoundException, SQLException {
        return false;
    }

    @Override
    public void setToRunning() throws ClassNotFoundException, SQLException {

    }

    @Override
    public void setToStopped() throws ClassNotFoundException, SQLException {

    }

    @Override
    public Session getSession() throws ClassNotFoundException, SQLException {
        return new Session() {
            @Override
            public EntityMode getEntityMode() {
                return null;
            }

            @Override
            public Session getSession(EntityMode entityMode) {
                return null;
            }

            @Override
            public void flush() throws HibernateException {

            }

            @Override
            public void setFlushMode(FlushMode flushMode) {

            }

            @Override
            public FlushMode getFlushMode() {
                return null;
            }

            @Override
            public void setCacheMode(CacheMode cacheMode) {

            }

            @Override
            public CacheMode getCacheMode() {
                return null;
            }

            @Override
            public SessionFactory getSessionFactory() {
                return null;
            }

            @Override
            public Connection connection() throws HibernateException {
                return null;
            }

            @Override
            public Connection close() throws HibernateException {
                return null;
            }

            @Override
            public void cancelQuery() throws HibernateException {

            }

            @Override
            public boolean isOpen() {
                return false;
            }

            @Override
            public boolean isConnected() {
                return false;
            }

            @Override
            public boolean isDirty() throws HibernateException {
                return false;
            }

            @Override
            public Serializable getIdentifier(Object object) throws HibernateException {
                return null;
            }

            @Override
            public boolean contains(Object object) {
                return false;
            }

            @Override
            public void evict(Object object) throws HibernateException {

            }

            @Override
            public Object load(Class theClass, Serializable id, LockMode lockMode) throws HibernateException {
                return null;
            }

            @Override
            public Object load(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
                return null;
            }

            @Override
            public Object load(Class theClass, Serializable id) throws HibernateException {
                return null;
            }

            @Override
            public Object load(String entityName, Serializable id) throws HibernateException {
                return null;
            }

            @Override
            public void load(Object object, Serializable id) throws HibernateException {

            }

            @Override
            public void replicate(Object object, ReplicationMode replicationMode) throws HibernateException {

            }

            @Override
            public void replicate(String entityName, Object object, ReplicationMode replicationMode) throws HibernateException {

            }

            @Override
            public Serializable save(Object object) throws HibernateException {
                System.out.println(object.toString());
                return null;
            }

            @Override
            public Serializable save(String entityName, Object object) throws HibernateException {
                return null;
            }

            @Override
            public void saveOrUpdate(Object object) throws HibernateException {

            }

            @Override
            public void saveOrUpdate(String entityName, Object object) throws HibernateException {

            }

            @Override
            public void update(Object object) throws HibernateException {

            }

            @Override
            public void update(String entityName, Object object) throws HibernateException {

            }

            @Override
            public Object merge(Object object) throws HibernateException {
                return null;
            }

            @Override
            public Object merge(String entityName, Object object) throws HibernateException {
                return null;
            }

            @Override
            public void persist(Object object) throws HibernateException {

            }

            @Override
            public void persist(String entityName, Object object) throws HibernateException {

            }

            @Override
            public void delete(Object object) throws HibernateException {

            }

            @Override
            public void delete(String entityName, Object object) throws HibernateException {

            }

            @Override
            public void lock(Object object, LockMode lockMode) throws HibernateException {

            }

            @Override
            public void lock(String entityName, Object object, LockMode lockMode) throws HibernateException {

            }

            @Override
            public void refresh(Object object) throws HibernateException {

            }

            @Override
            public void refresh(Object object, LockMode lockMode) throws HibernateException {

            }

            @Override
            public LockMode getCurrentLockMode(Object object) throws HibernateException {
                return null;
            }

            @Override
            public Transaction beginTransaction() throws HibernateException {
                return null;
            }

            @Override
            public Transaction getTransaction() {
                return null;
            }

            @Override
            public Criteria createCriteria(Class persistentClass) {
                return null;
            }

            @Override
            public Criteria createCriteria(Class persistentClass, String alias) {
                return null;
            }

            @Override
            public Criteria createCriteria(String entityName) {
                return null;
            }

            @Override
            public Criteria createCriteria(String entityName, String alias) {
                return null;
            }

            @Override
            public Query createQuery(String queryString) throws HibernateException {
                return null;
            }

            @Override
            public SQLQuery createSQLQuery(String queryString) throws HibernateException {
                return null;
            }

            @Override
            public Query createFilter(Object collection, String queryString) throws HibernateException {
                return null;
            }

            @Override
            public Query getNamedQuery(String queryName) throws HibernateException {
                return null;
            }

            @Override
            public void clear() {

            }

            @Override
            public Object get(Class clazz, Serializable id) throws HibernateException {
                return null;
            }

            @Override
            public Object get(Class clazz, Serializable id, LockMode lockMode) throws HibernateException {
                return null;
            }

            @Override
            public Object get(String entityName, Serializable id) throws HibernateException {
                return null;
            }

            @Override
            public Object get(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
                return null;
            }

            @Override
            public String getEntityName(Object object) throws HibernateException {
                return null;
            }

            @Override
            public Filter enableFilter(String filterName) {
                return null;
            }

            @Override
            public Filter getEnabledFilter(String filterName) {
                return null;
            }

            @Override
            public void disableFilter(String filterName) {

            }

            @Override
            public SessionStatistics getStatistics() {
                return null;
            }

            @Override
            public void setReadOnly(Object entity, boolean readOnly) {

            }

            @Override
            public Connection disconnect() throws HibernateException {
                return null;
            }

            @Override
            public void reconnect() throws HibernateException {

            }

            @Override
            public void reconnect(Connection connection) throws HibernateException {

            }
        };
    }

    @Override
    public void setDriver(String driver) {

    }

    @Override
    public void setConnectionString(String connectionString) {

    }

    @Override
    public void setUserName(String userName) {

    }

    @Override
    public void setPassword(String password) {

    }
}
