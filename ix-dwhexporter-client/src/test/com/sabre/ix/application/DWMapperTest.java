package com.sabre.ix.application;

import com.sabre.ix.application.dao.FileDataRaw;
import com.sabre.ix.application.logic.AirlineDateFormat;
import com.sabre.ix.application.logic.DWMapper;
import com.sabre.ix.client.Booking;
import com.sabre.ix.client.BookingServices;
import com.sabre.ix.client.context.Context;
import com.sabre.ix.client.context.ContextFactory;
import com.sabre.ix.client.dao.MetaModel;
import com.sabre.ix.client.datahandler.DataHandler;
import com.sabre.ix.client.datahandler.MetaModelFactory;
import com.sabre.ix.client.services.MetaModelServices;
import com.sabre.ix.client.spi.ActionHandler;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Henrik Thorburn
 * Date: 2014-04-03
 */
@RunWith(MockitoJUnitRunner.class)
public class DWMapperTest {

    private static Logger log = Logger.getLogger(DWMapper.class);

    BookingServices bookingServices;
    @Mock
    Context mockContext;
    @Mock
    DataHandler mockDataHandler;
    @Mock
    ActionHandler mockActionHandler;
    @Mock
    MetaModelServices mockMetaModelServices;
    MetaModel mockMetaModel;

    DWMapper mapper = new DWMapper();
    Booking booking;
    List<FileDataRaw> fileDataRaws;
    FileDataRaw row;
    List<Integer> multirow;

    private String testdataPath = "will be autodiscovered during setup";

    @Before
    public void setup() throws IOException, DocumentException {
        String hostName = InetAddress.getLocalHost().getHostName();
        if (hostName.contains("htlinux")) {
            testdataPath = "/home/henrik/src/DwhExporter/testdata/";
        } else if (hostName.contains("montecito")) {
            testdataPath = "/Volumes/2TB/src/DwhExporter/testdata/";
        } else if (hostName.contains("H8460305022460")) {
            testdataPath = "C:\\src\\DwhExporter\\testdata\\";
        } else if (hostName.contains("H9470305477081")) {
            testdataPath = "C:\\src\\DwhExporter\\testdata\\";
        } else {
            testdataPath = "C:\\dev\\DwhExporter\\testdata\\";
        }

        mockMetaModel = prepareMetaModel(DocumentHelper.parseText(loadTestData("metamodel.xml")));
        when(mockContext.getMetaModelServices()).thenReturn(mockMetaModelServices);
        when(mockMetaModelServices.getMetaModel()).thenReturn(mockMetaModel);
        bookingServices = new BookingServices(mockContext, mockDataHandler, mockActionHandler);

        booking = null;
        fileDataRaws = null;
        row = null;
        multirow = null;
    }

    @Rule
    public TestWatcher testWatcher = new TestWatcher() {
        @Override
        protected void succeeded(Description description) {
            System.out.println("" + description.getDisplayName() + " succeeded.");
            super.succeeded(description);
        }

        @Override
        protected void failed(Throwable e, Description description) {
            System.out.println("" + description.getDisplayName() + " failed " + e.getMessage());
            super.failed(e, description);
        }

    };


    @Test
    public void verifyStringToCalendar() throws ParseException {

        Calendar calendar;
        String d;

        d = "02MAR12";
        calendar = AirlineDateFormat.stringToCalendar(d);
        assertThat(calendar.get(Calendar.YEAR), equalTo(2012));
        assertThat(calendar.get(Calendar.MONTH), equalTo(Calendar.MARCH));
        assertThat(calendar.get(Calendar.DAY_OF_MONTH), equalTo(2));

        d = "12DEC07";
        calendar = AirlineDateFormat.stringToCalendar(d);
        assertThat(calendar.get(Calendar.YEAR), equalTo(2007));
        assertThat(calendar.get(Calendar.MONTH), equalTo(Calendar.DECEMBER));
        assertThat(calendar.get(Calendar.DAY_OF_MONTH), equalTo(12));
    }

    @Test
    public void verifyCalendarToString() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();

        gregorianCalendar.set(2010, Calendar.APRIL, 20);
        assertThat(AirlineDateFormat.calendarToString(gregorianCalendar), equalTo("20APR10"));

        gregorianCalendar.set(2014, Calendar.JANUARY, 1);
        assertThat(AirlineDateFormat.calendarToString(gregorianCalendar), equalTo("01JAN14"));
    }

    @Test
    public void verify_FZ_7MENHD() throws IOException {

        log.info("checking 7MENHD");
        booking = new Booking(bookingServices, loadTestData("7MENHD.xml"));
        assertThat(booking.getRloc(), equalTo("7MENHD"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(22));


        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), nullValue());
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("## NONAME ##/null"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), nullValue());
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("## NONAME ##/null"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        multirow = Arrays.asList(2, 3, 4, 5);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204821561"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getPaxname(), equalTo("SIMON/WERNER"));
            assertThat(row.getSegNoTech(), equalTo(1));
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9918223"));
        }

        multirow = Arrays.asList(6, 7, 8, 9);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204821561"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getPaxname(), equalTo("SIMON/WERNER"));
            assertThat(row.getSegNoTech(), equalTo(2));
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9918223"));
        }

        row = fileDataRaws.get(10);
        assertThat(row.getDocumentNo(), equalTo("2333513308"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("SIMON/WERNER"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(11);
        assertThat(row.getDocumentNo(), equalTo("2333513308"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("SIMON/WERNER"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        multirow = Arrays.asList(12, 13, 14, 15);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204821562"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getPaxname(), equalTo("SIMON/CHRISTINA"));
            assertThat(row.getSegNoTech(), equalTo(1));
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9918223"));
        }
        multirow = Arrays.asList(16, 17, 18, 19);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204821562"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getPaxname(), equalTo("SIMON/CHRISTINA"));
            assertThat(row.getSegNoTech(), equalTo(2));
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9918223"));
        }

        row = fileDataRaws.get(20);
        assertThat(row.getDocumentNo(), equalTo("2333513309"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("SIMON/CHRISTINA"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(21);
        assertThat(row.getDocumentNo(), equalTo("2333513309"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("SIMON/CHRISTINA"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

    }

    @Test
    public void verify_FZ_3NWPAK() throws IOException {

        booking = new Booking(bookingServices, loadTestData("3NWPAK.xml"));
        assertThat(booking.getRloc(), equalTo("3NWPAK"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(22));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), nullValue());
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("## NONAME ##/null"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), nullValue());
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("## NONAME ##/null"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        multirow = Arrays.asList(2, 3, 4, 5);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8205126717"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getPaxname(), equalTo("BUBENZER/BRIGITTE"));
            assertThat(row.getSegNoTech(), equalTo(1));
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9947605"));
        }
        multirow = Arrays.asList(6, 7, 8, 9);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8205126717"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getPaxname(), equalTo("BUBENZER/BRIGITTE"));
            assertThat(row.getSegNoTech(), equalTo(2));
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9947605"));
        }

        row = fileDataRaws.get(10);
        assertThat(row.getDocumentNo(), equalTo("2334383699"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("BUBENZER/BRIGITTE"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(11);
        assertThat(row.getDocumentNo(), equalTo("2334383699"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("BUBENZER/BRIGITTE"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        multirow = Arrays.asList(12, 13, 14, 15);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8205126718"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getPaxname(), equalTo("BUBENZER/EDGAR"));
            assertThat(row.getSegNoTech(), equalTo(1));
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9947605"));
        }

        multirow = Arrays.asList(16, 17, 18, 19);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8205126718"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getPaxname(), equalTo("BUBENZER/EDGAR"));
            assertThat(row.getSegNoTech(), equalTo(2));
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9947605"));
        }

        row = fileDataRaws.get(20);
        assertThat(row.getDocumentNo(), equalTo("2334383700"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("BUBENZER/EDGAR"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(21);
        assertThat(row.getDocumentNo(), equalTo("2334383700"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("BUBENZER/EDGAR"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

    }

    @Test
    public void verify_FZ_5N3JUD() throws IOException {

        booking = new Booking(bookingServices, loadTestData("5N3JUD.xml"));
        assertThat(booking.getRloc(), equalTo("5N3JUD"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(22));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), nullValue());
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("## NONAME ##/null"));
        assertThat(row.getSegNoTech(), equalTo(3));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), nullValue());
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("## NONAME ##/null"));
        assertThat(row.getSegNoTech(), equalTo(4));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        multirow = Arrays.asList(2, 3, 4, 5);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204954536"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getPaxname(), equalTo("STRUCK/HELMUT"));
            assertThat(row.getSegNoTech(), equalTo(3));
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9924434"));
        }

        multirow = Arrays.asList(6, 7, 8, 9);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204954536"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getPaxname(), equalTo("STRUCK/HELMUT"));
            assertThat(row.getSegNoTech(), equalTo(4));
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9924434"));
        }

        row = fileDataRaws.get(10);
        assertThat(row.getDocumentNo(), equalTo("2333535797"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("STRUCK/HELMUT"));
        assertThat(row.getSegNoTech(), equalTo(3));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(11);
        assertThat(row.getDocumentNo(), equalTo("2333535797"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("STRUCK/HELMUT"));
        assertThat(row.getSegNoTech(), equalTo(4));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        multirow = Arrays.asList(12, 13, 14, 15);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204954537"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getPaxname(), equalTo("STRUCK/ELISABETH"));
            assertThat(row.getSegNoTech(), equalTo(3));
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9924434"));
        }

        multirow = Arrays.asList(16, 17, 18, 19);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204954537"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getPaxname(), equalTo("STRUCK/ELISABETH"));
            assertThat(row.getSegNoTech(), equalTo(4));
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9924434"));
        }

        row = fileDataRaws.get(20);
        assertThat(row.getDocumentNo(), equalTo("2333535798"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("STRUCK/ELISABETH"));
        assertThat(row.getSegNoTech(), equalTo(3));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(21);
        assertThat(row.getDocumentNo(), equalTo("2333535798"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("STRUCK/ELISABETH"));
        assertThat(row.getSegNoTech(), equalTo(4));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

    }


    @Test
    public void verify_TST_12_1_7A7OHS() throws IOException {

        booking = new Booking(bookingServices, loadTestData("7A7OHS.xml"));
        assertThat(booking.getRloc(), equalTo("7A7OHS"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2337131631"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("7"));
        assertThat(row.getPaxname(), equalTo("SPADINA/AXEL"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("WNC15RT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("117.04"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("117.04"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB MUC4.00AB BER5.00EUR9.00END"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("39.00"));


        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("2337131631"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("7"));
        assertThat(row.getPaxname(), equalTo("SPADINA/AXEL"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("ENC24RT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("117.04"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("117.04"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB MUC4.00AB BER5.00EUR9.00END"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("39.00"));

        row = fileDataRaws.get(2);
        assertThat(row.getDocumentNo(), equalTo("2337131630"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("7"));
        assertThat(row.getPaxname(), equalTo("SPADINA/KERSTIN"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("WNC15RT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("117.04"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("117.04"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB MUC4.00AB BER5.00EUR9.00END"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("39.00"));

        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("2337131630"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("7"));
        assertThat(row.getPaxname(), equalTo("SPADINA/KERSTIN"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("ENC24RT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("117.04"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("117.04"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB MUC4.00AB BER5.00EUR9.00END"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("39.00"));

        // Z8MC3Z
    }

    @Test
    public void verify_TST_12_1_Z8MC3Z() throws IOException {

        booking = new Booking(bookingServices, loadTestData("Z8MC3Z.xml"));
        assertThat(booking.getRloc(), equalTo("Z8MC3Z"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(2));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2338304958"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("7"));
        assertThat(row.getPaxname(), equalTo("AUTH/JOERG"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("PNCRT"));
        assertThat(row.getFcmi(), equalTo("F"));
        assertThat(row.getSectorTotalFare(), equalTo("161.19"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("161.19"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB BUH11.61AB BER31.42NUC43.03END ROE0.731857"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("80.00"));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("2338304958"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("AUTH/JOERG"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("ONCRT"));
        assertThat(row.getFcmi(), equalTo("F"));
        assertThat(row.getSectorTotalFare(), equalTo("161.19"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("161.19"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB BUH11.61AB BER31.42NUC43.03END ROE0.731857"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("80.00"));

    }

    @Test
    public void verify_TST_12_1_5WTN6S() throws IOException {

        booking = new Booking(bookingServices, loadTestData("5WTN6S.xml"));
        assertThat(booking.getRloc(), equalTo("5WTN6S"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(6));

/* ROCKT/JULIAN has been canceled
        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2339573739"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("BROCKT/JULIAN"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("YID00R7"));
        assertThat(row.getFcmi(), equalTo("1"));
        assertThat(row.getSectorTotalFare(), equalTo("0.00"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("0.00"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB MIA AB BER 000.00NUC 000.00 END ROE1.000000"));
        assertThat(row.getTaxCode1(), nullValue());
        assertThat(row.getTaxValue1(), nullValue());


        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("2339573739"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("BROCKT/JULIAN"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("YID00R7"));
        assertThat(row.getFcmi(), equalTo("1"));
        assertThat(row.getSectorTotalFare(), equalTo("0.00"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("0.00"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB MIA AB BER 000.00NUC 000.00 END ROE1.000000"));
        assertThat(row.getTaxCode1(), nullValue());
        assertThat(row.getTaxValue1(), nullValue());
  */

        row = fileDataRaws.get(2);
        assertThat(row.getDocumentNo(), equalTo("2339573737"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("BROCKT/DANIEL"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("YID00R7"));
        assertThat(row.getFcmi(), equalTo("1"));
        assertThat(row.getSectorTotalFare(), equalTo("0.00"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("0.00"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB MIA AB BER 000.00NUC 000.00 END ROE1.000000"));
        assertThat(row.getTaxCode1(), nullValue());
        assertThat(row.getTaxValue1(), nullValue());

        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("2339573737"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("BROCKT/DANIEL"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("YID00R7"));
        assertThat(row.getFcmi(), equalTo("1"));
        assertThat(row.getSectorTotalFare(), equalTo("0.00"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("0.00"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB MIA AB BER 000.00NUC 000.00 END ROE1.000000"));
        assertThat(row.getTaxCode1(), nullValue());
        assertThat(row.getTaxValue1(), nullValue());

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2339573738"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("BROCKT/CORNELIA"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("YID00R7"));
        assertThat(row.getFcmi(), equalTo("1"));
        assertThat(row.getSectorTotalFare(), equalTo("0.00"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("0.00"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB MIA AB BER 000.00NUC 000.00 END ROE1.000000"));
        assertThat(row.getTaxCode1(), nullValue());
        assertThat(row.getTaxValue1(), nullValue());

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("2339573738"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("BROCKT/CORNELIA"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("YID00R7"));
        assertThat(row.getFcmi(), equalTo("1"));
        assertThat(row.getSectorTotalFare(), equalTo("0.00"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("0.00"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB MIA AB BER 000.00NUC 000.00 END ROE1.000000"));
        assertThat(row.getTaxCode1(), nullValue());
        assertThat(row.getTaxValue1(), nullValue());

        row = fileDataRaws.get(4);
        assertThat(row.getDocumentNo(), equalTo("2339573740"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("BROCKT/LARISSA"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("YID00R7"));
        assertThat(row.getFcmi(), equalTo("1"));
        assertThat(row.getSectorTotalFare(), equalTo("0.00"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("0.00"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB MIA AB BER 000.00NUC 000.00 END ROE1.000000"));
        assertThat(row.getTaxCode1(), nullValue());
        assertThat(row.getTaxValue1(), nullValue());

        row = fileDataRaws.get(5);
        assertThat(row.getDocumentNo(), equalTo("2339573740"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("BROCKT/LARISSA"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("YID00R7"));
        assertThat(row.getFcmi(), equalTo("1"));
        assertThat(row.getSectorTotalFare(), equalTo("0.00"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("0.00"));
        assertThat(row.getSectorGrandTotalFareNotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB MIA AB BER 000.00NUC 000.00 END ROE1.000000"));
        assertThat(row.getTaxCode1(), nullValue());
        assertThat(row.getTaxValue1(), nullValue());

    }

    @Test
    public void verify_Ancillaries_10_1_8ZHGKI() throws IOException {

        //8GKIZH, DocumentClass MCO (EMD-S)
        booking = new Booking(bookingServices, loadTestData("8GKIZH.xml"));
        assertThat(booking.getRloc(), equalTo("8GKIZH"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(3));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8204636788"));
        assertThat(row.getPaxname(), equalTo("MALAQUIN/FRANCOIS"));
        assertThat(row.getTixDepApt(), equalTo("VIE"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("90913"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204636788/DTAB/EUR12.90/09SEP13/PARAB08IB/20494935"));
        assertThat(row.getFopinformationFreetext(), equalTo("CCVI497307XXXXXX1999/0615*CV/A922486"));
        assertThat(row.getMcoreason(), equalTo("VRDS"));
        assertThat(row.getMcoreasonSubCode(), equalTo("RDS"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());
    }

    @Test
    public void verify_Ancillaries_10_1_8HHO9E() throws IOException {

        //8HHO9E, DocumentClass MCO (EMD-S)
        booking = new Booking(bookingServices, loadTestData("8HHO9E.xml"));
        assertThat(booking.getRloc(), equalTo("8HHO9E"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8204641252"));
        assertThat(row.getPaxname(), equalTo("SCHUBERT/WOLFGANG"));
        assertThat(row.getTixDepApt(), equalTo("TXL"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getSegNoTech(), equalTo(3));
        assertThat(row.getTixFlightDt(), equalTo("210614"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204641252/DTAB/EUR12.90/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getFopinformationFreetext(), equalTo("ELV/DE32100500001060185810/WOLFGANG SCHUBERT*A"));
        assertThat(row.getMcoreason(), equalTo("VRDS"));
        assertThat(row.getMcoreasonSubCode(), equalTo("RDS"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());
    }

    @Test
    public void verify_Ancillaries_10_1_79FDX2() throws IOException {
        //79FDX2 (EMD-S)
        booking = new Booking(bookingServices, loadTestData("79FDX2.xml"));
        assertThat(booking.getRloc(), equalTo("79FDX2"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(5));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8204635433"));
        assertThat(row.getPaxname(), equalTo("KRANTZ/UTE"));
        assertThat(row.getTixDepApt(), equalTo("HAM"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getSegNoTech(), equalTo(9));
        assertThat(row.getTixFlightDt(), equalTo("140913"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204635433/DTAB/EUR24.90/09SEP13/BUHAB0105/69491590"));
        assertThat(row.getFopinformationFreetext(), equalTo("CCVI410403XXXXXX9032/1213*CV/A866131"));
        assertThat(row.getMcoreason(), equalTo("VRSG"));
        assertThat(row.getMcoreasonSubCode(), equalTo("RSG"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

    }

    @Test
    public void verify_Ancillaries_10_1_79LP74() throws IOException {
        //79LP74 (Paper EBT)
        booking = new Booking(bookingServices, loadTestData("79LP74.xml"));
        assertThat(booking.getRloc(), equalTo("79LP74"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(1));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2600638344"));
        assertThat(row.getDocumentClass(), equalTo("EBT"));
        assertThat(row.getPaxname(), equalTo("RAPA/ANETA"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixDepApt(), nullValue());  // equalTo("TXL") changed by request from DWH
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("90913"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-2600638344/PTAB/EUR150.00/09SEP13/TXLAB0005/23496303"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), nullValue());
        assertThat(row.getMcoreason(), equalTo("*EXCESS BAGGAGE//2ND PIECE"));
        assertThat(row.getMcoreasonSubCode(), equalTo("A"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

    }

    @Test
    public void verify_Ancillaries_10_1_8D69PH() throws IOException {
        //8D69PH, DocumentClass MCO (EMD-A)
        booking = new Booking(bookingServices, loadTestData("8D69PH.xml"));
        assertThat(booking.getRloc(), equalTo("8D69PH"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(16));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8204640286"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("BONA/BIANCA"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204640286/DTAB/EUR40.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:TXL] [OffPoint:BCN] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:15] [SeatCol:B] [Reference:13] [Description:ST/15B]SRP: [SeatRow:15] [SeatCol:A] [Reference:12] [Description:ST/15A]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2331140477"));
        assertThat(row.getIssInConnWithCpn(), equalTo("1"));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8204640286"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("BONA/BIANCA"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204640286/DTAB/EUR40.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:BCN] [OffPoint:TXL] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:15] [SeatCol:B] [Reference:13] [Description:ST/15B]SRP: [SeatRow:15] [SeatCol:A] [Reference:12] [Description:ST/15A]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2331140477"));
        assertThat(row.getIssInConnWithCpn(), equalTo("2"));

        multirow = Arrays.asList(2, 3);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("2331140477"));
            assertThat(row.getDocumentClass(), equalTo("PAX"));
        }

        row = fileDataRaws.get(4);
        assertThat(row.getDocumentNo(), equalTo("8204640287"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("RAUTMANN/BJOERN"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204640287/DTAB/EUR40.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:TXL] [OffPoint:BCN] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:15] [SeatCol:B] [Reference:13] [Description:ST/15B]SRP: [SeatRow:15] [SeatCol:A] [Reference:12] [Description:ST/15A]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2331140476"));
        assertThat(row.getIssInConnWithCpn(), equalTo("1"));

        row = fileDataRaws.get(5);
        assertThat(row.getDocumentNo(), equalTo("8204640287"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("RAUTMANN/BJOERN"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204640287/DTAB/EUR40.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:BCN] [OffPoint:TXL] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:15] [SeatCol:B] [Reference:13] [Description:ST/15B]SRP: [SeatRow:15] [SeatCol:A] [Reference:12] [Description:ST/15A]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2331140476"));
        assertThat(row.getIssInConnWithCpn(), equalTo("2"));

        multirow = Arrays.asList(6, 7);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("2331140476"));
            assertThat(row.getDocumentClass(), equalTo("PAX"));
        }

    }

    @Test
    public void verify_Ancillaries_10_1_8BZZ7W() throws IOException {
        //8BZZ7W, DocumentClass MCO (EMD-A)
        booking = new Booking(bookingServices, loadTestData("8BZZ7W.xml"));
        assertThat(booking.getRloc(), equalTo("8BZZ7W"));

        fileDataRaws = mapper.mapBooking(booking);
        // printRows(fileDataRaws);

        assertThat(fileDataRaws.size(), equalTo(16));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8204639711"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("KROEGER/HORST"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204639711/DTAB/EUR20.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:HAM] [OffPoint:PMI] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:14] [SeatCol:K] [Reference:12] [Description:ST/14K]SRP: [SeatRow:14] [SeatCol:H] [Reference:13] [Description:ST/14H]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2331138394"));
        assertThat(row.getIssInConnWithCpn(), equalTo("1"));

        multirow = Arrays.asList(1, 2);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204639711"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }

        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("8204639711"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("KROEGER/HORST"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204639711/DTAB/EUR20.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:PMI] [OffPoint:HAM] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:13] [SeatCol:B] [Reference:12] [Description:ST/13B]SRP: [SeatRow:13] [SeatCol:A] [Reference:13] [Description:ST/13A]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2331138394"));
        assertThat(row.getIssInConnWithCpn(), equalTo("2"));

        multirow = Arrays.asList(4, 5);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204639711"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }


        row = fileDataRaws.get(8);
        assertThat(row.getDocumentNo(), equalTo("8204639712"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("KROEGER/NICOLE"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204639712/DTAB/EUR40.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:HAM] [OffPoint:PMI] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:14] [SeatCol:K] [Reference:12] [Description:ST/14K]SRP: [SeatRow:14] [SeatCol:H] [Reference:13] [Description:ST/14H]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2331138393"));
        assertThat(row.getIssInConnWithCpn(), equalTo("1"));

        multirow = Arrays.asList(9, 10);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204639712"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }

        row = fileDataRaws.get(11);
        assertThat(row.getDocumentNo(), equalTo("8204639712"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("KROEGER/NICOLE"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204639712/DTAB/EUR40.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:PMI] [OffPoint:HAM] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:13] [SeatCol:B] [Reference:12] [Description:ST/13B]SRP: [SeatRow:13] [SeatCol:A] [Reference:13] [Description:ST/13A]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2331138393"));
        assertThat(row.getIssInConnWithCpn(), equalTo("2"));

        multirow = Arrays.asList(12, 13);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204639712"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }

    }

    @Test
    public void verify_Ancillaries_10_1_8F6Y2M() throws IOException {

        //8F6Y2M, DocumentClass MCO, EmdtreatedAs “A�? (EMD-A)
        booking = new Booking(bookingServices, loadTestData("8F6Y2M.xml"));
        assertThat(booking.getRloc(), equalTo("8F6Y2M"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(8));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8204640770"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("SCHROEDER/HENRY"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204640770/DTAB/EUR150.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("SSR: [Type:AVIH] [Status:HK] [Quantity:1] [CompanyId:AB] [FreeText:TTL10KG1PCDIM50X20X30CMDOG/ONLY DOMESTIC PETS ARE ALLOWED FOR AVIH/ MAX CRATE 125X75X85CM]"));
        assertThat(row.getMcoreason(), equalTo("AVIH"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0BS"));
        assertThat(row.getIssInConnWith(), equalTo("2331143469"));
        assertThat(row.getIssInConnWithCpn(), equalTo("1"));


        row = fileDataRaws.get(2);
        assertThat(row.getDocumentNo(), equalTo("8204640770"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("SCHROEDER/HENRY"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204640770/DTAB/EUR150.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("SSR: [Type:AVIH] [Status:HK] [Quantity:1] [CompanyId:AB] [FreeText:TTL10KG1PCDIM50X20X30CMDOG/ONLY DOMESTIC PETS ARE ALLOWED FOR AVIH/ MAX CRATE 125X75X85CM]"));
        assertThat(row.getMcoreason(), equalTo("AVIH"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0BS"));
        assertThat(row.getIssInConnWith(), equalTo("2331143469"));
        assertThat(row.getIssInConnWithCpn(), equalTo("2"));

    }

    @Test
    public void verify_Ancillaries_12_1_3WKOGY() throws IOException {

        //3WKOGY, DocumentClass “MCO" (EMD-A)
        booking = new Booking(bookingServices, loadTestData("3WKOGY.xml"));
        assertThat(booking.getRloc(), equalTo("3WKOGY"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(24));


        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8205704896"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("OSTERNACHER/SABINE"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8205704896/DTAB/EUR33.98/12FEB14/BERAB0101/23497401"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:DUS] [OffPoint:MIA] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:38] [SeatCol:K] [Reference:2] [Description:ST/38K]SRP: [SeatRow:38] [SeatCol:H] [Reference:1] [Description:ST/38H]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("5250351578"));
        assertThat(row.getIssInConnWithCpn(), equalTo("2"));

        multirow = Arrays.asList(1, 2);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8205704896"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getPaxname(), equalTo("OSTERNACHER/SABINE"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }


        row = fileDataRaws.get(4);
        assertThat(row.getDocumentNo(), equalTo("8205704896"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("OSTERNACHER/SABINE"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8205704896/DTAB/EUR33.98/12FEB14/BERAB0101/23497401"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:MIA] [OffPoint:TXL] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:46] [SeatCol:K] [Reference:2] [Description:ST/46K]SRP: [SeatRow:46] [SeatCol:H] [Reference:1] [Description:ST/46H]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("5250351578"));
        assertThat(row.getIssInConnWithCpn(), equalTo("3"));

        multirow = Arrays.asList(5, 6, 7);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8205704896"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getPaxname(), equalTo("OSTERNACHER/SABINE"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }

        multirow = Arrays.asList(8, 9, 10, 11);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentClass(), equalTo("PAX"));
            assertThat(row.getPaxname(), equalTo("OSTERNACHER/SABINE"));

        }


        row = fileDataRaws.get(12);
        assertThat(row.getDocumentNo(), equalTo("8205704897"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("STEINMAIR/CHRISTIAN"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8205704897/DTAB/EUR33.98/12FEB14/BERAB0101/23497401"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:DUS] [OffPoint:MIA] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:38] [SeatCol:K] [Reference:2] [Description:ST/38K]SRP: [SeatRow:38] [SeatCol:H] [Reference:1] [Description:ST/38H]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("5250351577"));
        assertThat(row.getIssInConnWithCpn(), equalTo("2"));

        multirow = Arrays.asList(13, 14, 15);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8205704897"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getPaxname(), equalTo("STEINMAIR/CHRISTIAN"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }

        row = fileDataRaws.get(16);
        assertThat(row.getDocumentNo(), equalTo("8205704897"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("STEINMAIR/CHRISTIAN"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8205704897/DTAB/EUR33.98/12FEB14/BERAB0101/23497401"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:MIA] [OffPoint:TXL] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:46] [SeatCol:K] [Reference:2] [Description:ST/46K]SRP: [SeatRow:46] [SeatCol:H] [Reference:1] [Description:ST/46H]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("5250351577"));
        assertThat(row.getIssInConnWithCpn(), equalTo("3"));


    }

    @Test
    public void verify_Ancillaries_12_1_4UBC28() throws IOException {
        //4UBC28, DocumentClass “MCO" (EMD-A)
        booking = new Booking(bookingServices, loadTestData("4UBC28.xml"));
        assertThat(booking.getRloc(), equalTo("4UBC28"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(24));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8206348657"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("BARBARYAN/ROMEO"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8206348657/DTAB/EUR0.00/09MAY14/BUHAB0105/69491590"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("SSR: [Type:AEML] [Status:HK] [Quantity:1] [CompanyId:AB]"));
        assertThat(row.getMcoreason(), equalTo("AEML"));
        assertThat(row.getMcoreasonSubCode(), equalTo("MAE"));
        assertThat(row.getIssInConnWith(), equalTo("2340264353"));
        assertThat(row.getIssInConnWithCpn(), equalTo("1"));

        multirow = Arrays.asList(1, 2, 3);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8206348657"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }

        row = fileDataRaws.get(6);
        assertThat(row.getDocumentNo(), equalTo("8206348659"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("BARBARYAN/PLATON"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8206348659/DTAB/EUR0.00/09MAY14/BUHAB0105/69491590"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("SSR: [Type:AFML] [Status:HK] [Quantity:1] [CompanyId:AB]"));
        assertThat(row.getMcoreason(), equalTo("AFML"));
        assertThat(row.getMcoreasonSubCode(), equalTo("MAF"));
        assertThat(row.getIssInConnWith(), equalTo("2340264352"));
        assertThat(row.getIssInConnWithCpn(), equalTo("1"));

        multirow = Arrays.asList(7, 8, 9);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8206348659"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }

        row = fileDataRaws.get(12);
        assertThat(row.getDocumentNo(), equalTo("8206348660"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("BARBARYAN/ALISA"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8206348660/DTAB/EUR0.00/09MAY14/BUHAB0105/69491590"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("SSR: [Type:AEML] [Status:HK] [Quantity:1] [CompanyId:AB]"));
        assertThat(row.getMcoreason(), equalTo("AEML"));
        assertThat(row.getMcoreasonSubCode(), equalTo("MAE"));
        assertThat(row.getIssInConnWith(), equalTo("2340264354"));
        assertThat(row.getIssInConnWithCpn(), equalTo("1"));

        multirow = Arrays.asList(13, 14, 15);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8206348660"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }

        row = fileDataRaws.get(18);
        assertThat(row.getDocumentNo(), equalTo("8206348658"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("BUTYMOVA/NATALIA"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8206348658/DTAB/EUR0.00/09MAY14/BUHAB0105/69491590"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("SSR: [Type:ANML] [Status:HK] [Quantity:1] [CompanyId:AB]"));
        assertThat(row.getMcoreason(), equalTo("ANML"));
        assertThat(row.getMcoreasonSubCode(), equalTo("MAN"));
        assertThat(row.getIssInConnWith(), equalTo("2340264351"));
        assertThat(row.getIssInConnWithCpn(), equalTo("1"));

        multirow = Arrays.asList(19, 20, 21);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8206348658"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }
    }

    @Test
    public void verify_Ancillaries_12_1_ZPR3KZ() throws IOException {

        //ZPR3KZ, DocumentClass “MCO" (EMD-A)
        booking = new Booking(bookingServices, loadTestData("ZPR3KZ.xml"));
        assertThat(booking.getRloc(), equalTo("ZPR3KZ"));

        fileDataRaws = mapper.mapBooking(booking);

        printRows(fileDataRaws);

        assertThat(fileDataRaws.size(), equalTo(30));

        // Cpn 1. HAM -> DUS, seat 11A
        // Cpn 2. DUS -> ALC, seat 26A
        // Cpn 3. ALC -> HAM, seat 13F


        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8205951399"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("JACOBMEYER/LOTHAR"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8205951399/DTAB/EUR60.00/11MAR14/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:1] [BoardPoint:HAM] [OffPoint:DUS] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:11] [SeatCol:A] [Reference:28] [Description:ST/11A]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2332658437"));
        assertThat(row.getIssInConnWithCpn(), equalTo("1"));

        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("8205951399"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("JACOBMEYER/LOTHAR"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8205951399/DTAB/EUR60.00/11MAR14/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:1] [BoardPoint:DUS] [OffPoint:ALC] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:26] [SeatCol:A] [Reference:28] [Description:ST/26A]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2332658437"));
        assertThat(row.getIssInConnWithCpn(), equalTo("2"));

        row = fileDataRaws.get(6);
        assertThat(row.getDocumentNo(), equalTo("8205951399"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("JACOBMEYER/LOTHAR"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8205951399/DTAB/EUR60.00/11MAR14/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:1] [BoardPoint:ALC] [OffPoint:HAM] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:13] [SeatCol:F] [Reference:28] [Description:ST/13F]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2332658437"));
        assertThat(row.getIssInConnWithCpn(), equalTo("3"));

        multirow = Arrays.asList(1, 2, 4, 5, 7, 8);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8205951399"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }

    }

    @Test
    public void verify_Ancillaries_12_1_23K69M() throws IOException {

        //23K69M, DocumentClass „MCO“, EmdtreatedAs „S“ (EMD-S)
        booking = new Booking(bookingServices, loadTestData("23K69M.xml"));
        assertThat(booking.getRloc(), equalTo("23K69M"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(7));

        row = fileDataRaws.get(4);
        assertThat(row.getDocumentNo(), equalTo("8206067154"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("KOMMERELL/ROLAND"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixDepApt(), equalTo("HAM"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("100514"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8206067154/DTAB/EUR13.90/26MAR14/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:16] [FreeText:ELV/DE43200505501009776921/JULIA BUECHSENMANN*A]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        assertThat(row.getMcoreasonSubCode(), equalTo("EPD"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("8206067154"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));


    }

    @Test
    public void verify_Ancillaries_12_1_28XYQ8() throws IOException {

        //28XYQ8, DocumentClass „MCO“ (EMD-S)
        booking = new Booking(bookingServices, loadTestData("28XYQ8.xml"));
        assertThat(booking.getRloc(), equalTo("28XYQ8"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8205584670"));
        assertThat(row.getPaxname(), equalTo("GONZALEZEMBUN/YAIZA"));
        assertThat(row.getTixDepApt(), equalTo("BCN"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("80514"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8205584670/DTAB/EUR6.90/27JAN14/PMIAB08IB/78492735"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:16] [FreeText:CCVI427784XXXXXX7291/0114*CV/A040357]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        assertThat(row.getMcoreasonSubCode(), equalTo("EPB"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());
    }

    @Test
    public void verify_Ancillaries_12_1_Y2NH4D() throws IOException {

        //Y2NH4D, DocumentClass „MCO“ (EMD-S)
        booking = new Booking(bookingServices, loadTestData("Y2NH4D.xml"));
        assertThat(booking.getRloc(), equalTo("Y2NH4D"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(6));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8205893364"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("HOERSKEN/CAROLINE"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixDepApt(), equalTo("DUS"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("40514"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8205893364/DTAB/EUR27.90/04MAR14/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:16] [FreeText:ELV/DE62354500001202078729/CAROLINE HOERSKEN*A]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        assertThat(row.getMcoreasonSubCode(), equalTo("RNI"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8205893364"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));

    }

    @Test
    public void verify_Ancillaries_12_1_ZW869W() throws IOException {

        //ZW869W, DocumentClass „MCO“ (Virtual MCO)
        booking = new Booking(bookingServices, loadTestData("ZW869W.xml"));
        assertThat(booking.getRloc(), equalTo("ZW869W"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(9));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2700725876"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("QUINONES/HECTOR"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixDepApt(), equalTo("BCN"));
        assertThat(row.getTixDestApt(), equalTo("TXL"));
        assertThat(row.getTixFlightDt(), equalTo("290414"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-2700725876/PTAB/USD39.49/12JAN14/NYCAB08IB/33994122"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:1] [BoardPoint:BCN] [OffPoint:TXL] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:24] [SeatCol:F] [Reference:1] [Description:ST/24F]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), nullValue());
        //assertThat(row.getIssInConnWithCpn(), nullValue());  //diff: 2        //TODO IssInConnWithCpn

        row = fileDataRaws.get(2);
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixDepApt(), equalTo("BCN"));
        assertThat(row.getTixDestApt(), equalTo("TXL"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:16] [FreeText:CCVI414711XXXXXX0056/0416*CV/A90964C]"));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixDepApt(), equalTo("BCN"));
        assertThat(row.getTixDestApt(), equalTo("TXL"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));

        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("2700725876"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("QUINONES/HECTOR"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixDepApt(), equalTo("TXL"));
        assertThat(row.getTixDestApt(), equalTo("MIA"));
        assertThat(row.getTixFlightDt(), equalTo("110514"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-2700725876/PTAB/USD39.49/12JAN14/NYCAB08IB/33994122"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:1] [BoardPoint:TXL] [OffPoint:MIA] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:33] [SeatCol:K] [Reference:1] [Description:ST/33K]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), nullValue());
        //assertThat(row.getIssInConnWithCpn(), nullValue());   //diff: 3             //TODO IssInConnWithCpn

        row = fileDataRaws.get(5);
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixDepApt(), equalTo("TXL"));
        assertThat(row.getTixDestApt(), equalTo("MIA"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:16] [FreeText:CCVI414711XXXXXX0056/0416*CV/A90964C]"));

        row = fileDataRaws.get(4);
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixDepApt(), equalTo("TXL"));
        assertThat(row.getTixDestApt(), equalTo("MIA"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));

    }

    @Test
    public void verify_Ancillaries_12_1_5YZCWQ() throws IOException {
        //5YZCWQ, DocumentClass „MCO“ (Paper MCO)
        booking = new Booking(bookingServices, loadTestData("5YZCWQ.xml"));
        assertThat(booking.getRloc(), equalTo("5YZCWQ"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(12));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2700725907"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("DAVIS/DOUGLAS"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixDepApt(), nullValue());  // equalTo("MIA") changed by request from DWH
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("120514"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-2700725907/PTAB/USD79.24/15JAN14/NYCAB08IB/33994122"));
        assertThat(row.getMcoreason(), equalTo("*ASR*"));
        assertThat(row.getMcoreasonSubCode(), equalTo("O"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

        row = fileDataRaws.get(6);
        assertThat(row.getDocumentNo(), equalTo("2700725908"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("DAVIS/PATRICIA"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixDepApt(), nullValue());  // equalTo("MIA") changed by request from DWH
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("120514"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-2700725908/PTAB/USD79.24/15JAN14/NYCAB08IB/33994122"));
        assertThat(row.getMcoreason(), equalTo("*ASR*"));
        assertThat(row.getMcoreasonSubCode(), equalTo("O"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

    }

    @Test
    public void verify_Ancillaries_12_1_YZQSP9() throws IOException {
        //YZQSP9, DocumentClass “EBT" (Paper EBT)
        booking = new Booking(bookingServices, loadTestData("YZQSP9.xml"));
        assertThat(booking.getRloc(), equalTo("YZQSP9"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(6));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2600645020"));
        assertThat(row.getDocumentClass(), equalTo("EBT"));
        assertThat(row.getPaxname(), equalTo("VULPIUS/ANDREAS"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixDepApt(), nullValue());  // equalTo("FRA") changed by request from DWH
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("20514"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-2600645020/PTAB/EUR70.00/02MAY14/FRAAB0005/23496782"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), nullValue());
        assertThat(row.getMcoreason(), equalTo("*BIKE"));
        assertThat(row.getMcoreasonSubCode(), equalTo("A"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

    }

    @Test
    public void verify_TST_10_1_75WVC9() throws IOException {

        //75WVC9
        booking = new Booking(bookingServices, loadTestData("75WVC9.xml"));
        assertThat(booking.getRloc(), equalTo("75WVC9"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("4124641319"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("GREEN/RAMARI"));
        assertThat(row.getSegNoTech(), equalTo(3));
        assertThat(row.getFarebaseCode(), equalTo("WNC34OW"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), nullValue());
        assertThat(row.getSectorTotalFareNotEur(), equalTo("186.60"));
        assertThat(row.getSectorTotalFareFa(), nullValue());
        assertThat(row.getSectorTotalFareFanotEur(), equalTo("186.60"));
        assertThat(row.getTixCurrency(), equalTo("NZD"));
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("CPH AB X/BER AB BUD M3.87NUC3.87END ROE5.670300"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-NZD"));
        assertThat(row.getTaxValue1(), equalTo("116.80"));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("4124641319"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("GREEN/RAMARI"));
        assertThat(row.getSegNoTech(), equalTo(4));
        assertThat(row.getFarebaseCode(), equalTo("WNC34OW"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), nullValue());
        assertThat(row.getSectorTotalFareNotEur(), equalTo("186.60"));
        assertThat(row.getSectorTotalFareFa(), nullValue());
        assertThat(row.getSectorTotalFareFanotEur(), equalTo("186.60"));
        assertThat(row.getTixCurrency(), equalTo("NZD"));
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("CPH AB X/BER AB BUD M3.87NUC3.87END ROE5.670300"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-NZD"));
        assertThat(row.getTaxValue1(), equalTo("116.80"));

        row = fileDataRaws.get(2);
        assertThat(row.getDocumentNo(), equalTo("4124641320"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("GREEN/NEIL"));
        assertThat(row.getSegNoTech(), equalTo(3));
        assertThat(row.getFarebaseCode(), equalTo("WNC34OW"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), nullValue());
        assertThat(row.getSectorTotalFareNotEur(), equalTo("186.60"));
        assertThat(row.getSectorTotalFareFa(), nullValue());
        assertThat(row.getSectorTotalFareFanotEur(), equalTo("186.60"));
        assertThat(row.getTixCurrency(), equalTo("NZD"));
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("CPH AB X/BER AB BUD M3.87NUC3.87END ROE5.670300"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-NZD"));
        assertThat(row.getTaxValue1(), equalTo("116.80"));

        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("4124641320"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("GREEN/NEIL"));
        assertThat(row.getSegNoTech(), equalTo(4));
        assertThat(row.getFarebaseCode(), equalTo("WNC34OW"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), nullValue());
        assertThat(row.getSectorTotalFareNotEur(), equalTo("186.60"));
        assertThat(row.getSectorTotalFareFa(), nullValue());
        assertThat(row.getSectorTotalFareFanotEur(), equalTo("186.60"));
        assertThat(row.getTixCurrency(), equalTo("NZD"));
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("CPH AB X/BER AB BUD M3.87NUC3.87END ROE5.670300"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-NZD"));
        assertThat(row.getTaxValue1(), equalTo("116.80"));

    }

    @Test
    public void verify_TST_10_1_796K5N() throws IOException {

        //796K5N
        booking = new Booking(bookingServices, loadTestData("796K5N.xml"));
        assertThat(booking.getRloc(), equalTo("796K5N"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(8));

        row = fileDataRaws.get(6);
        assertThat(row.getDocumentNo(), equalTo("2331135853"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("KOENIG/WALTER"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("ONCRT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("219.44"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("219.44"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("CGN AB PMI67.05AB CGN67.05NUC134.10END ROE0.760562"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("84.00"));

        row = fileDataRaws.get(7);
        assertThat(row.getDocumentNo(), equalTo("2331135853"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("KOENIG/WALTER"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("ONCRT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("219.44"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("219.44"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("CGN AB PMI67.05AB CGN67.05NUC134.10END ROE0.760562"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("84.00"));

    }

    @Test
    public void verify_TST_10_1_8A8Q8O() throws IOException {

        booking = new Booking(bookingServices, loadTestData("8A8Q8O.xml"));
        assertThat(booking.getRloc(), equalTo("8A8Q8O"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(2));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2331138649"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("DEMARINIS/ORONZO"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("ENC30OW"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("58.71"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("58.71"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB MIL2.62NUC2.62END ROE0.760562"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("30.00"));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("2331138648"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("PATANE/DAVIDE"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("ENC30OW"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("58.71"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("58.71"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB MIL2.62NUC2.62END ROE0.760562"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("30.00"));
    }

    @Test
    public void verify_TST_10_1_8ISHFE() throws IOException {

        booking = new Booking(bookingServices, loadTestData("8ISHFE.xml"));
        assertThat(booking.getRloc(), equalTo("8ISHFE"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));


        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2331146018"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("SCHABEL/BRUNO"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("WNC18RT"));
        assertThat(row.getFcmi(), equalTo("4"));
        assertThat(row.getSectorTotalFare(), equalTo("108.72"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("108.72"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("STR AB BER2.00AB STR3.50EUR5.50END"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("45.00"));


        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("2331146018"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("SCHABEL/BRUNO"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("ENC27RT"));
        assertThat(row.getFcmi(), equalTo("4"));
        assertThat(row.getSectorTotalFare(), equalTo("108.72"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("108.72"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("STR AB BER2.00AB STR3.50EUR5.50END"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("45.00"));

        row = fileDataRaws.get(2);
        assertThat(row.getDocumentNo(), equalTo("2331146017"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("SCHABEL/ROSEMARIE"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("WNC18RT"));
        assertThat(row.getFcmi(), equalTo("4"));
        assertThat(row.getSectorTotalFare(), equalTo("108.72"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("108.72"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("STR AB BER2.00AB STR3.50EUR5.50END"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("45.00"));

        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("2331146017"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("SCHABEL/ROSEMARIE"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("ENC27RT"));
        assertThat(row.getFcmi(), equalTo("4"));
        assertThat(row.getSectorTotalFare(), equalTo("108.72"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("108.72"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("STR AB BER2.00AB STR3.50EUR5.50END"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("45.00"));

    }

    @Test
    public void verify_TST_10_1_8IUM85() throws IOException {

        booking = new Booking(bookingServices, loadTestData("8IUM85.xml"));
        assertThat(booking.getRloc(), equalTo("8IUM85"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2331147868"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("PECH/RENATE"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("WNC20RT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("98.27"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("98.27"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("NUE AB HAM2.00AB NUE2.00EUR4.00END"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("40.00"));


        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("2331147868"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("PECH/RENATE"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("WNC20RT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("98.27"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("98.27"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("NUE AB HAM2.00AB NUE2.00EUR4.00END"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("40.00"));


        row = fileDataRaws.get(2);
        assertThat(row.getDocumentNo(), equalTo("2331147865"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("PROELL/KAETHE"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("WNC20RT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("98.27"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("98.27"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("NUE AB HAM2.00AB NUE2.00EUR4.00END"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("40.00"));


        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("2331147865"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getPaxname(), equalTo("PROELL/KAETHE"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("WNC20RT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("98.27"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("98.27"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("NUE AB HAM2.00AB NUE2.00EUR4.00END"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("40.00"));


    }

    @Test
    public void verify_FOP_and_FCMI_for_ancillaries_27OLJO() throws IOException {

        //27OLJO, DocumentClass MCO (EMD-A)
        booking = new Booking(bookingServices, loadTestData("27OLJO.xml"));
        assertThat(booking.getRloc(), equalTo("27OLJO"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(9));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8205915682"));
        assertThat(row.getTixDepApt(), equalTo("MUC"));
        assertThat(row.getTixDestApt(), equalTo("TXL"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getFopinformationFreetext(), equalTo("ELV/DE88733692640001822462/JOHANN WOELFLE*A"));
        assertThat(row.getFcmi(), equalTo("0"));

        row = fileDataRaws.get(4);
        assertThat(row.getDocumentNo(), equalTo("8205915682"));
        assertThat(row.getTixDepApt(), equalTo("TXL"));
        assertThat(row.getTixDestApt(), equalTo("MUC"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getFopinformationFreetext(), equalTo("ELV/DE88733692640001822462/JOHANN WOELFLE*A"));
        assertThat(row.getFcmi(), equalTo("0"));

    }

    @Test
    public void verify_FOP_and_FCMI_for_ancillaries_5ZBOJ3() throws IOException {
        //5ZBOJ3, DocumentClass „MCO“ (EMD-A)
        booking = new Booking(bookingServices, loadTestData("5ZBOJ3.xml"));
        assertThat(booking.getRloc(), equalTo("5ZBOJ3"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(8));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8206345104"));
        assertThat(row.getTixDepApt(), equalTo("CGN"));
        assertThat(row.getTixDestApt(), equalTo("CLY"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getFopinformationFreetext(), equalTo("ELV/NL41ABNA0005981133/G. JONKER-JANS*A"));
        assertThat(row.getFcmi(), equalTo("0"));


        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("8206345104"));
        assertThat(row.getTixDepApt(), equalTo("CLY"));
        assertThat(row.getTixDestApt(), equalTo("CGN"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getFopinformationFreetext(), equalTo("ELV/NL41ABNA0005981133/G. JONKER-JANS*A"));
        assertThat(row.getFcmi(), equalTo("0"));

    }

    @Test
    public void verify_FOP_and_FCMI_for_ancillaries_2BAEI8() throws IOException {

        //2BAEI8, DocumentClass MCO (EMD-S)
        booking = new Booking(bookingServices, loadTestData("2BAEI8.xml"));
        assertThat(booking.getRloc(), equalTo("2BAEI8"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(10));

        row = fileDataRaws.get(2);
        assertThat(row.getDocumentNo(), equalTo("8205568856"));
        assertThat(row.getTixDepApt(), equalTo("FRA"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getSegNoTech(), equalTo(4));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getFopinformationFreetext(), equalTo("ELV/DE47120300000017338328/JENNIFER TURBA*A"));
        assertThat(row.getFcmi(), equalTo("0"));


        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("8205568856"));
        assertThat(row.getTixDepApt(), equalTo("FRA"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getSegNoTech(), equalTo(4));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));

        row = fileDataRaws.get(6);
        assertThat(row.getDocumentNo(), equalTo("8205568855"));
        assertThat(row.getTixDepApt(), equalTo("FRA"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getSegNoTech(), equalTo(3));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getFopinformationFreetext(), equalTo("ELV/DE47120300000017338328/JENNIFER TURBA*A"));
        assertThat(row.getFcmi(), equalTo("0"));
        row = fileDataRaws.get(7);
        assertThat(row.getDocumentNo(), equalTo("8205568855"));
        assertThat(row.getTixDepApt(), equalTo("FRA"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getSegNoTech(), equalTo(3));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));


    }

    @Test
    public void verify_FOP_and_FCMI_for_ancillaries_28YYQE() throws IOException {

        // One PAX: ERICH BEAU...
        // 3 x BNI
        //  AIR    TXL - MUC
        //  AIR    MUC - TXL
        //  IU / PENF
        //      LFT: [SubjectQualifier:3] [Type:P07] [FreeText:PAX 3000443529 TTM/RT OK EMD]
        //      TicketDocumentData: [FreeText:PAX 745-2610646591/DTAB/EUR60.00/30APR14/FDHLT2204/23203585]
        //          [Type:T] [DataIndicator:D] [RequestNotification:S] [Status:T] [CouponNumber:1] [PaxDetails:A]REF:
        //          Reference: [Qualifier:PT] [Number:1]: Reference: [Qualifier:ST] [Number:11]:

        //28YYQE, DocumentClass „MCO“ (EMD-S)
        booking = new Booking(bookingServices, loadTestData("28YYQE.xml"));
        assertThat(booking.getRloc(), equalTo("28YYQE"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getDocumentNo(), equalTo("2610646591"));
        assertThat(row.getFopinformationFreetext(), equalTo("CCTP122088XXXXX0016/0917/A669A/ADB"));
        assertThat(row.getFcmi(), equalTo("0"));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getDocumentNo(), equalTo("2610646591"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));

        row = fileDataRaws.get(2);
        assertThat(row.getDocumentNo(), equalTo("4843782710"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getTixDepApt(), equalTo("TXL"));
        assertThat(row.getTixDestApt(), equalTo("MUC"));
        assertThat(row.getPaxfirstName(), equalTo("ERICH"));

        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("4843782710"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getTixDepApt(), equalTo("MUC"));
        assertThat(row.getTixDestApt(), equalTo("TXL"));
        assertThat(row.getPaxfirstName(), equalTo("ERICH"));
    }

    @Test
    public void verify_FO_for_PAX_and_EMD_5W3TII() throws IOException {

        booking = new Booking(bookingServices, loadTestData("5W3TII.xml"));
        assertThat(booking.getRloc(), equalTo("5W3TII"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(12));

        multirow = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getOrigIssueInformationFreetext(), equalTo("745-8206236418MIA29APR14/10980211/745-82062364183M1"));
        }


        multirow = Arrays.asList(8, 9);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentClass(), equalTo("PAX"));
            assertThat(row.getOrigIssueInformationFreetext(), equalTo("745-2338946432MIA29APR14/10980211/745-23389464325E1"));
        }
    }

    @Test
    public void verify_FO_for_PAX_and_EMD_ZT5H5E() throws IOException {

        booking = new Booking(bookingServices, loadTestData("ZT5H5E.xml"));
        assertThat(booking.getRloc(), equalTo("ZT5H5E"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(6));

        multirow = Arrays.asList(0, 1, 2, 3);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getOrigIssueInformationFreetext(), equalTo("745-8205670647PAR05FEB14/20494935/745-82056706470M1"));
        }

        multirow = Arrays.asList(4, 5);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentClass(), equalTo("PAX"));
            assertThat(row.getOrigIssueInformationFreetext(), equalTo("745-2336768887PAR05FEB14/20494935/745-23367688876E1"));
        }

    }

    @Test
    public void verify_FO_for_PAX_and_EMD_38IS83() throws IOException {

        booking = new Booking(bookingServices, loadTestData("38IS83.xml"));
        assertThat(booking.getRloc(), equalTo("38IS83"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(6));

        multirow = Arrays.asList(0, 1, 2, 3);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getOrigIssueInformationFreetext(), equalTo("745-8205325996BER29DEC13/23496605/745-82053259961M1"));
        }

        multirow = Arrays.asList(4, 5);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentClass(), equalTo("PAX"));
            assertThat(row.getOrigIssueInformationFreetext(), equalTo("PAX 745-2335302045BER29DEC13/23496605/745-23353020450E1"));
        }
    }

    // todo: This is correctly failing in staging. We need to fix the CIC.BOOKING_NAME_ITEM_ID
    // bug, then we can reprocess this booking in staging, and this test should become green again
    // IE: Not an exporter but a platform bug!
    @Test
    @Ignore
    public void verify_TST_for_non_AB_5YEFK4() throws IOException {

        booking = new Booking(bookingServices, loadTestData("5YEFK4.xml"));
        assertThat(booking.getRloc(), equalTo("5YEFK4"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(2));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8504449104"));
        assertThat(row.getFarebaseCode(), equalTo("PAFM03"));
        assertThat(row.getFcmi(), equalTo("1"));
        assertThat(row.getSectorTotalFare(), nullValue());
        assertThat(row.getSectorTotalFareNotEur(), equalTo("0.00"));
        assertThat(row.getTixCurrency(), equalTo("USD"));
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("MIA AB X/BER Q000.00AB HEL0000.00NUC0000.00END ROE1.000000"));
        assertThat(row.getTaxCode1(), equalTo("XDE---EUR"));
        assertThat(row.getTaxValue1(), equalTo("6.01"));


        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8504449104"));
        assertThat(row.getFarebaseCode(), equalTo("PAFM03"));
        assertThat(row.getFcmi(), equalTo("1"));
        assertThat(row.getSectorTotalFare(), nullValue());
        assertThat(row.getSectorTotalFareNotEur(), equalTo("0.00"));
        assertThat(row.getTixCurrency(), equalTo("USD"));
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("MIA AB X/BER Q000.00AB HEL0000.00NUC0000.00END ROE1.000000"));
        assertThat(row.getTaxCode1(), equalTo("XDE---EUR"));
        assertThat(row.getTaxValue1(), equalTo("6.01"));

    }

    // todo: This may be the order of the PNRs. Need to figure this one out
    @Test
    @Ignore
    public void verify_TST_for_INF() throws IOException {

        //X3ZBXG
        booking = new Booking(bookingServices, loadTestData("X3ZBXG.xml"));
        assertThat(booking.getRloc(), equalTo("X3ZBXG"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(6));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2336165513"));
        assertThat(row.getFarebaseCode(), equalTo("ONCRT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("7.00"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("MUC AB PMI5.53AB MUC4.16NUC9.69END ROE0.731857"));
        assertThat(row.getTaxCode1(), equalTo("XJDAE-EUR"));
        assertThat(row.getTaxValue1(), equalTo("7.17"));


        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("2336165513"));
        assertThat(row.getFarebaseCode(), equalTo("PNCRT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("7.00"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("MUC AB PMI5.53AB MUC4.16NUC9.69END ROE0.731857"));
        assertThat(row.getTaxCode1(), equalTo("XJDAE-EUR"));
        assertThat(row.getTaxValue1(), equalTo("7.17"));


        row = fileDataRaws.get(2);
        assertThat(row.getDocumentNo(), equalTo("2336165514"));
        assertThat(row.getFarebaseCode(), equalTo("ONCRT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("71.00"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("MUC AB PMI55.33AB MUC41.67NUC97.00END ROE0.731857"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("84.00"));


        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("2336165514"));
        assertThat(row.getFarebaseCode(), equalTo("PNCRT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("71.00"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("MUC AB PMI55.33AB MUC41.67NUC97.00END ROE0.731857"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR "));
        assertThat(row.getTaxValue1(), equalTo("84.00"));


        row = fileDataRaws.get(4);
        assertThat(row.getDocumentNo(), equalTo("2336165512"));
        assertThat(row.getFarebaseCode(), equalTo("ONCRT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("71.00"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("MUC AB PMI55.33AB MUC41.67NUC97.00END ROE0.731857"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("84.00"));


        row = fileDataRaws.get(5);
        assertThat(row.getDocumentNo(), equalTo("2336165512"));
        assertThat(row.getFarebaseCode(), equalTo("PNCRT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("71.00"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("MUC AB PMI55.33AB MUC41.67NUC97.00END ROE0.731857"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR "));
        assertThat(row.getTaxValue1(), equalTo("84.00"));


    }


    @Test
    public void verify_TST_with_INF_2GJIXE() throws IOException {

        //2GJIXE
        booking = new Booking(bookingServices, loadTestData("2GJIXE.xml"));
        assertThat(booking.getRloc(), equalTo("2GJIXE"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(6));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2339733264"));
        assertThat(row.getFarebaseCode(), equalTo("ORCRTIL"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("2.00"));
        assertThat(row.getFareCalc(), equalTo("TLV AB BER9.25AB TLV7.00NUC16.25END ROE1.000000"));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("2339733264"));
        assertThat(row.getFarebaseCode(), equalTo("PNCRTIL"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("2.00"));
        assertThat(row.getFareCalc(), equalTo("TLV AB BER9.25AB TLV7.00NUC16.25END ROE1.000000"));

        row = fileDataRaws.get(4);
        assertThat(row.getDocumentNo(), equalTo("2339733263"));
        assertThat(row.getFarebaseCode(), equalTo("ORCRTIL"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("15.71"));
        assertThat(row.getFareCalc(), equalTo("TLV AB BER92.50AB TLV70.00NUC162.50END ROE1.000000"));

        row = fileDataRaws.get(5);
        assertThat(row.getDocumentNo(), equalTo("2339733263"));
        assertThat(row.getFarebaseCode(), equalTo("PNCRTIL"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("15.71"));
        assertThat(row.getFareCalc(), equalTo("TLV AB BER92.50AB TLV70.00NUC162.50END ROE1.000000"));


    }

    @Test
    public void verify_TST_with_INF_2WKW66() throws IOException {

        //2GJIXE
        booking = new Booking(bookingServices, loadTestData("2WKW66.xml"));
        assertThat(booking.getRloc(), equalTo("2WKW66"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2340280451"));
        assertThat(row.getFarebaseCode(), equalTo("SNCRT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("0.00"));
        assertThat(row.getFareCalc(), equalTo("DUS AB BER0.00AB DUS0.00EUR0.00END"));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("2340280451"));
        assertThat(row.getFarebaseCode(), equalTo("LNCRT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("0.00"));
        assertThat(row.getFareCalc(), equalTo("DUS AB BER0.00AB DUS0.00EUR0.00END"));

        row = fileDataRaws.get(2);
        assertThat(row.getDocumentNo(), equalTo("4639515248"));
        assertThat(row.getFarebaseCode(), equalTo("SNCRT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("268.32"));
        assertThat(row.getFareCalc(), equalTo("DUS AB BER46.50AB DUS76.50EUR123.00END"));

        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("4639515248"));
        assertThat(row.getFarebaseCode(), equalTo("LNCRT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("268.32"));
        assertThat(row.getFareCalc(), equalTo("DUS AB BER46.50AB DUS76.50EUR123.00END"));


    }

    //5DEJNU
    @Test
    public void verify_ParentPNR_5DEJNU() throws IOException {

        //X3ZBXG
        booking = new Booking(bookingServices, loadTestData("5DEJNU.xml"));
        assertThat(booking.getRloc(), equalTo("5DEJNU"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        multirow = Arrays.asList(0, 1, 2, 3);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getParentPnr(), equalTo("5WS6NT"));
        }

    }

    @Test
    public void verify_differnt_Payment_for_TST_and_TSM_37PLZO() throws IOException {
        //37PLZO
        booking = new Booking(bookingServices, loadTestData("37PLZO.xml"));
        assertThat(booking.getRloc(), equalTo("37PLZO"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(5));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8206172417"));
        assertThat(row.getTixDepApt(), equalTo("SKG"));
        assertThat(row.getTixDestApt(), equalTo("DUS"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getTixFlightDt(), equalTo("40514"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8206172417/DTAB/EUR9.00/10APR14/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("SSR: [Type:APML] [Status:HK] [Quantity:1] [CompanyId:AB]"));
        assertThat(row.getFopinformationFreetext(), equalTo("ELV/DE24200411440613868900/SYLKE GIPPNER*A"));
        assertThat(row.getMcoreason(), equalTo("APML"));
        assertThat(row.getMcoreasonSubCode(), equalTo("MAP"));
        assertThat(row.getIssInConnWith(), equalTo("2339188938"));
        assertThat(row.getIssInConnWithCpn(), equalTo("1"));

        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("2339188938"));
        assertThat(row.getTixDepApt(), equalTo("SKG"));
        assertThat(row.getTixDestApt(), equalTo("DUS"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("YIDZL3R1"));
        assertThat(row.getTixFlightDt(), equalTo("40514"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        ;
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-2339188938/ETAB/EUR95.78/10APR14/BERAB0111/23498915"));
        assertThat(row.getFopinformationFreetext(), equalTo("CVI426354XXXXXX8970/0118*CV/A162598"));

        row = fileDataRaws.get(4);
        assertThat(row.getDocumentNo(), equalTo("2339188938"));
        assertThat(row.getTixDepApt(), equalTo("DUS"));
        assertThat(row.getTixDestApt(), equalTo("TXL"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("YIDZL1R1"));
        assertThat(row.getTixFlightDt(), equalTo("40514"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        ;
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-2339188938/ETAB/EUR95.78/10APR14/BERAB0111/23498915"));
        assertThat(row.getFopinformationFreetext(), equalTo("CVI426354XXXXXX8970/0118*CV/A162598"));

    }

    @Test
    @Ignore
    public void verify_TST_63HXK7() throws IOException {
        //63HXK7
        booking = new Booking(bookingServices, loadTestData("63HXK7.xml"));
        assertThat(booking.getRloc(), equalTo("63HXK7"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        row = fileDataRaws.get(0);
        assertThat(row.getPaxname(), equalTo("ABDULMAJID/ADEL"));
        assertThat(row.getFarebaseCode(), equalTo("WNC20RT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("98.27"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("98.27"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());

        row = fileDataRaws.get(0);
        assertThat(row.getPaxname(), equalTo("ABDULMAJID/ALICE"));
        assertThat(row.getFarebaseCode(), equalTo("WNC20RT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("98.27"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("98.27"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());

        row = fileDataRaws.get(0);
        assertThat(row.getPaxname(), equalTo("ABDULMAJID/DJAMIL"));
        assertThat(row.getFarebaseCode(), equalTo("WNC20RT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("98.27"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("98.27"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());

        row = fileDataRaws.get(0);
        assertThat(row.getPaxname(), equalTo("ABDULMAJID/ADEL"));
        assertThat(row.getFarebaseCode(), equalTo("WNC20RT"));
        assertThat(row.getFcmi(), equalTo("0"));
        assertThat(row.getSectorTotalFare(), equalTo("98.27"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("98.27"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
    }


    @Test
    @Ignore
    public void verifyOpenJawsWithTicketNullValues_399ZTG() throws IOException {
        booking = new Booking(bookingServices, loadTestData("399ZTG.xml"));
        assertThat(booking.getRloc(), equalTo("399ZTG"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(20));

        for (FileDataRaw row : fileDataRaws) {
            String tixdepApt = row.getTixDepApt();
            String tixdestApt = row.getTixDestApt();

            if (tixdepApt == null && tixdestApt != null) {
                fail("Found a problem");
            } else if (tixdepApt != null && tixdestApt == null) {
                assertThat(row.getPaxfirstName(), equalTo("GRADY"));
                fail("Found another problem");
            }
        }

    }

    private String readFile(String fileName) throws IOException {
        File testFile = new File(testdataPath + fileName);
        if (testFile.exists() && testFile.canRead()) {
            return FileUtils.readFileToString(testFile);
        } else {
            return null;
        }
    }

    private void writeFile(String rloc, String content) throws IOException {
        File testFile = new File(testdataPath + rloc + ".xml");
        FileUtils.writeStringToFile(testFile, content);
    }


    private String loadTestData(String fileName) throws IOException {
        String s = readFile(fileName);
        if (s != null) {
            return s;
        } else {
            // Attempting to read file from IX system
            String rloc = fileName.substring(0, 6);
            try {
                Context liveDBContext = ContextFactory.createContext();
                Context liveWSContext = ContextFactory.createContext(new File("C:/dev/DwhExporter/DwhExporter/config/contextConfiguration_WS.xml"));
                BookingServices liveDBBookingServices = (BookingServices) liveDBContext.getDomainServices(Booking.class);
                BookingServices liveWSBookingServices = (BookingServices) liveWSContext.getDomainServices(Booking.class);
                List<Booking> bookings = liveWSBookingServices.retrieveByCCL("Booking.Rloc=\"" + rloc + "\"");
                assertThat("Expected exactly one booking when querying by CCL and RLOC=" + rloc, bookings.size(), equalTo(1));
                Booking liveWSBooking = bookings.get(0);
                Booking liveDBBooking  = liveDBBookingServices.retrieveById(liveWSBooking.getBookingId());

                String xmlString = liveDBBooking.toXml();
                writeFile(rloc, xmlString);
                return xmlString;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load RLOC from live system: " + rloc, e);
            }
        }
    }

    private void printRows(List<FileDataRaw> rows) {
        int i = 1;
        for (FileDataRaw row : rows) {
            System.out.println("Row " + i++);
            System.out.println("getPaxname " + row.getPaxname());
            System.out.println("getTixDepApt " + row.getTixDepApt());
            System.out.println("getTixDestApt " + row.getTixDestApt());
            System.out.println("getFcmi " + row.getFcmi());
            System.out.println("getDocumentNo " + row.getDocumentNo());
            System.out.println("getDocumentClass " + row.getDocumentClass());
            System.out.println("getSegNoTech " + row.getSegNoTech());
            System.out.println("getIssInConnWith " + row.getIssInConnWith());
            System.out.println("getIssInConnWithCpn " + row.getIssInConnWithCpn());
            System.out.println("getFarebaseCode " + row.getFarebaseCode());
            System.out.println("getTixFlightDt " + row.getTixFlightDt());
            System.out.println("getEmdtreatedAs " + row.getEmdtreatedAs());
            System.out.println("getMcoreason " + row.getMcoreason());
            System.out.println("getMcoreasonSubCode " + row.getMcoreasonSubCode());
            System.out.println("getTixInformationFreetext " + row.getTixInformationFreetext());
            System.out.println("getMiscellaneousChargeOrderFreetext " + row.getMiscellaneousChargeOrderFreetext());
            System.out.println("getMiscellaneousInformationFreetext " + row.getMiscellaneousInformationFreetext());
            System.out.println("getOrigIssueInformationFreetext " + row.getOrigIssueInformationFreetext());
            System.out.println("getFopinformationFreetext " + row.getFopinformationFreetext());
            System.out.println("");
        }
    }

    // Test setup
    @SuppressWarnings("unchecked")
    private MetaModel prepareMetaModel(Document metaModel) {
        org.dom4j.Element root = metaModel.getRootElement();
        org.dom4j.Element body = root.element("Body");
        org.dom4j.Element response = body.element("getMetaModelFullRequestResponse");
        return MetaModelFactory.create(response);
    }
}
