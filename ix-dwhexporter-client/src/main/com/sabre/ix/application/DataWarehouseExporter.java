package com.sabre.ix.application;

import com.sabre.ix.application.dao.FileDataRaw;
import com.sabre.ix.application.input.BookingSelector;
import com.sabre.ix.application.logic.DWMapper;
import com.sabre.ix.application.output.ExportDBConnectionHandler;
import com.sabre.ix.client.Booking;
import com.sabre.ix.client.context.Context;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by IntelliJ IDEA.
 * User: SG0211638 Halldor Gylfason
 * Date: 13.10.2011
 * Time: 18:17
 */
public class DataWarehouseExporter {

    private static Logger log = Logger.getLogger(DataWarehouseExporter.class);

    private BookingSelector bookingSelector;

    private int numberOfThreads = 8;
    private boolean stopping = false;

    private ConcurrentLinkedQueue<Long> bookingsToExport = new ConcurrentLinkedQueue<Long>();
    final private Set<Long> failedBookings = Collections.synchronizedSet(new HashSet<Long>());
    private final DWMapper dwMapper = new DWMapper();
    public List<FileDataRaw> lastExportedRows;
    private ExportDBConnectionHandler exportDBConnectionHandler;
    private Context context;

    public DataWarehouseExporter() {
        //
    }

    public ExportDBConnectionHandler getExportDBConnectionHandler() {
        return exportDBConnectionHandler;
    }

    public void setExportDBConnectionHandler(ExportDBConnectionHandler exportDBConnectionHandler) {
        this.exportDBConnectionHandler = exportDBConnectionHandler;
    }

    public BookingSelector getBookingSelector() {
        return bookingSelector;
    }

    public void setBookingSelector(BookingSelector bookingSelector) {
        this.bookingSelector = bookingSelector;
    }

    public void run() throws Exception {

        log.info("Starting DWH Exporter");

        // Reset collections
        this.failedBookings.clear();

        // Export DB setup
        if (exportDBConnectionHandler.isRunning()) {
            log.info("Process DWH2_ASx is running so stopping!");
            return;
        }

        // Import DB setup
        bookingsToExport = bookingSelector.getBookingsToExport();

        exportDBConnectionHandler.setToRunning();
        log.info("Finished updating process status");


        log.info("Preparing to export " + bookingsToExport.size() + " bookings");
        //now start threads
        if (numberOfThreads > 0) {
            Collection<Thread> threadPool = new ArrayList<Thread>();
            for (int i = 0; i < numberOfThreads; i++) {
                Thread t = new Thread(new DWWorker());
                t.start();
                threadPool.add(t);
            }

            //wait until all is finished.
            while (!stopping) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    //
                }
                if (bookingsToExport.isEmpty()) {
                    log.info("All bookings have been consumed, checking that all threads are completed");
                    boolean allThreadsDone = true;
                    for (Thread t : threadPool) {
                        if (t.isAlive()) {
                            log.info("Thread " + t + " is still processing. Waiting for it to finish");
                            allThreadsDone = false;
                            break;
                        }
                    }
                    if (allThreadsDone) {
                        log.info("All threads are done processing");
                        break;
                    }
                }
            }
        } else {
            // For debug purposes, if "numberOfThreads" is set to zero
            // we run the exporting in the main thread.
            DWWorker worker = new DWWorker();
            worker.run();
        }

        exportDBConnectionHandler.setToStopped();
        log.info("Finished DWH export, waiting for next scheduled run");
    }

    public void stop() {
        log.info(getClass().getName() + ".stop");
        stopping = true;
    }

    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    class DWWorker implements Runnable {


        @Override
        public void run() {
            log.info("Starting worker thread: " + this);
            Session stagingSession = null;
            Long bookingId;
            try {
                stagingSession = exportDBConnectionHandler.getSession();
                while (!stopping) {
                    bookingId = null;
                    if (!bookingsToExport.isEmpty()) {
                        bookingId = bookingsToExport.remove();
                    }

                    if (bookingId == null) {
                        break;
                    }
                    try {
                        processBooking(bookingId, stagingSession);
                        stagingSession.flush();
                        stagingSession.clear();
                        failedBookings.remove(bookingId);
                    } catch (Exception e) {
                        log.error("Exception occurred in worker thread", e);
                        if (failedBookings.contains(bookingId)) {
                            failedBookings.remove(bookingId);
                            log.error("Repeatedly failed to process booking " + bookingId +
                                    ", discarding it.", e);

                        } else {
                            log.error("Putting booking " + bookingId + " back to queue");
                            bookingsToExport.add(bookingId);
                            failedBookings.add(bookingId);
                        }

                        try {
                            if (stagingSession != null) {
                                stagingSession.close();
                            }
                        } catch (Throwable t) {
                            //pass
                        }
                        try {
                            stagingSession = exportDBConnectionHandler.getSession();
                        } catch (Exception e1) {
                            log.error("Didn't get new session so aborting thread", e1);
                            throw new RuntimeException("Didn't get new session so aborting thread", e1);
                        }
                    }
                }
                log.info("Finishing worker thread");
            } catch (Exception e) {
                log.error("Exception occurred in worker thread -- stopping", e);
            } finally {
                if (stagingSession != null) {
                    stagingSession.close();
                }
            }
            log.info("Finished worker thread");
        }

        void processBooking(Long bookingId, Session stagingSession) {
            Booking booking;
            StopWatch sw = new StopWatch();
            try {

                sw.start();
                log.debug("About to load booking " + bookingId + " from ODS");
                booking = context.getDomainServices(Booking.class).retrieveById(bookingId, "DwhExporterBooking");
                //booking = context.getDomainServices(Booking.class).retrieveById(bookingId, "DwhExporterBooking2");

                log.debug("booking " + bookingId + " with " + booking.getBookingNames().size() + " name loaded in " + sw.getTime() + " ms");
                // booking = context.getDomainServices(Booking.class).retrieveById(bookingId, "DefaultBooking");
            } catch (Exception e) {
                log.warn("Could not load booking " + bookingId + " because of exception: " + e.getMessage());
                return;
            }
            if (booking != null) {

                log.info("Handling booking " + booking.getRloc() + "/" + booking.getBookingId() + "/" + DateFormatUtils.format(booking.getBookingDate(), "yyyy-MM-dd HH:mm:SS"));
                try {
                    sw.reset();
                    sw.start();
                    log.debug("Starting transformation of booking " + bookingId);
                    List<FileDataRaw> rows = dwMapper.mapBooking(booking);
                    log.debug("Transformation of booking " + bookingId + " done in " + sw.getTime() + " ms, it has " + rows.size() + " rows");
                    int rowIndex = 0;
                    sw.reset();
                    sw.start();
                    for (FileDataRaw row : rows) {

                        row.setTms(new Date());
                        log.debug("About to write booking " + bookingId + ", row " + rowIndex);
                        ++rowIndex;
                        try {
                            stagingSession.save(row);
                            log.debug("Successfully wrote booking " + bookingId + ", row " + rowIndex + " to DWH");
                        } catch (org.hibernate.exception.DataException e) {
                            log.error("Failed to save row " + rowIndex + " of " + rows.size() + " : " + row);
                            //log.error(row.toInsertSQL());
                            throw e;
                        }


                        log.debug(("Row " + rowIndex));
                        log.debug("getPaxname " + row.getPaxname());
                        log.debug("getTixDepApt " + row.getTixDepApt());
                        log.debug("getTixDestApt " + row.getTixDestApt());
                        log.debug("getFcmi " + row.getFcmi());
                        log.debug("getDocumentNo " + row.getDocumentNo());
                        log.debug("getDocumentClass " + row.getDocumentClass());
                        log.debug("getSegNoTech " + row.getSegNoTech());
                        log.debug("getIssInConnWith " + row.getIssInConnWith());
                        log.debug("getIssInConnWithCpn " + row.getIssInConnWithCpn());
                        log.debug("getFarebaseCode " + row.getFarebaseCode());
                        log.debug("getTixFlightDt " + row.getTixFlightDt());
                        log.debug("getEmdtreatedAs " + row.getEmdtreatedAs());
                        log.debug("getMcoreason " + row.getMcoreason());
                        log.debug("getMcoreasonSubCode " + row.getMcoreasonSubCode());
                        log.debug("getTixInformationFreetext " + row.getTixInformationFreetext());
                        log.debug("getMiscellaneousChargeOrderFreetext " + row.getMiscellaneousChargeOrderFreetext());
                        log.debug("getMiscellaneousInformationFreetext " + row.getMiscellaneousInformationFreetext());
                        log.debug("getOrigIssueInformationFreetext " + row.getOrigIssueInformationFreetext());
                        log.debug("getFopinformationFreetext " + row.getFopinformationFreetext());
                        log.debug("getSectorFare " + row.getSectorFare());
                        log.debug("getSectorFareNotEur " + row.getSectorFareNotEur());
                        log.debug("getFareCalc " + row.getFareCalc());

                        log.debug("");

                        //log.debug(row.toInsertSQL());

                        lastExportedRows = rows;
                    }
                    log.info("Successfully wrote " + bookingId + ", with " + rows.size() + " rows in " + sw.getTime() + " ms to DWH");
                    sw.stop();
                } catch (NullPointerException e) {
                    log.error("DataWarehouseExporter.processBooking : NullPointerException while processing booking " + bookingId, e);
                    throw e;
                } catch (org.hibernate.exception.DataException e) {
                    stagingSession.close();
                    try {
                        stagingSession = exportDBConnectionHandler.getSession();
                    } catch (ClassNotFoundException e1) {
                        log.error("Failed to reconnect, giving up", e1);
                    } catch (SQLException e1) {
                        log.error("Failed to reconnect, giving up", e1);
                    }
                }
            } else {
                log.error("Could not retrieve booking with id: " + bookingId);
            }
        }
    }
}
