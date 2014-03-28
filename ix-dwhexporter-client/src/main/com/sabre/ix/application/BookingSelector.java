package com.sabre.ix.application;

import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created with IntelliJ IDEA.
 * User: Henrik Thorburn SG0211570
 * Date: 2013-03-20
 */
public interface BookingSelector {

    ConcurrentLinkedQueue<Long> getBookingsToExport();

    void setProperties(Properties properties);

}
