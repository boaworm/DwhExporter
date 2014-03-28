package com.sabre.ix.application;

import com.sabre.ix.client.context.Context;

import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created with IntelliJ IDEA.
 * User: Henrik Thorburn SG0211570
 * Date: 2013-03-20
 */
public class StaticBookingSelectorImpl implements BookingSelector {

    public StaticBookingSelectorImpl() {
    }

    ConcurrentLinkedQueue<Long> bookings = new ConcurrentLinkedQueue<Long>();

    @Override
    public ConcurrentLinkedQueue<Long> getBookingsToExport() {
        return bookings;
    }

    @Override
    public void setProperties(Properties properties) {
        String allIds = properties.getProperty("DWHExporter_StaticBookingSelectorList");
        String[] arr = allIds.split(",");
        for (String s : arr) {
            Long l = Long.parseLong(s);
            bookings.add(l);
        }
    }
}
