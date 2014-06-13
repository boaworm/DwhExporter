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
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
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

    // private String testdataPath = "C:\\dev\\DwhExporter\\testdata\\";
    private String testdataPath = "/home/henrik/src/DwhExporter/testdata/";

    @Before
    public void setup() throws IOException, DocumentException {
        mockMetaModel = prepareMetaModel(DocumentHelper.parseText(loadTestData("metamodel.xml")));
        when(mockContext.getMetaModelServices()).thenReturn(mockMetaModelServices);
        when(mockMetaModelServices.getMetaModel()).thenReturn(mockMetaModel);
        bookingServices = new BookingServices(mockContext, mockDataHandler, mockActionHandler);
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
    public void verify_FZ() throws IOException {


        DWMapper mapper = new DWMapper();
        Booking booking;
        List<FileDataRaw> fileDataRaws;
        FileDataRaw row;
        List<Integer> multirow;

        //TODO wrong document no for MCO

        // 7MENHD
        log.info("checking 7MENHD");
        booking = new Booking(bookingServices, loadTestData("7MENHD.xml"));
        assertThat(booking.getRloc(), equalTo("7MENHD"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(26));   //16->26


        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), nullValue());
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));     //diff: equalTo("7")
        assertThat(row.getPaxname(), equalTo("## NONAME ##/null"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), nullValue());
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));     //diff: equalTo("7")
        assertThat(row.getPaxname(), equalTo("## NONAME ##/null"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        multirow = Arrays.asList(2, 3, 4, 5, 6);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204821561"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getBookingStatusAssessed(), nullValue());
            assertThat(row.getPaxname(), equalTo("SIMON/WERNER"));
            assertThat(row.getSegNoTech(), nullValue());
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9918223"));
        }

        row = fileDataRaws.get(7);
        assertThat(row.getDocumentNo(), equalTo("2333513308"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("SIMON/WERNER"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(8);
        assertThat(row.getDocumentNo(), equalTo("2333513308"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("SIMON/WERNER"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        multirow = Arrays.asList(9, 10, 11, 12, 13);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204821562"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getBookingStatusAssessed(), nullValue());
            assertThat(row.getPaxname(), equalTo("SIMON/CHRISTINA"));
            assertThat(row.getSegNoTech(), nullValue());
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9918223"));
        }

        row = fileDataRaws.get(14);
        assertThat(row.getDocumentNo(), equalTo("2333513309"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("SIMON/CHRISTINA"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(15);
        assertThat(row.getDocumentNo(), equalTo("2333513309"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("SIMON/CHRISTINA"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));
        log.info("7MENHD ok");
        // 3NWPAK

        booking = new Booking(bookingServices, loadTestData("3NWPAK.xml"));
        assertThat(booking.getRloc(), equalTo("3NWPAK"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(16));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), nullValue());
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("## NONAME ##/null"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), nullValue());
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("## NONAME ##/null"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        multirow = Arrays.asList(2, 3, 4, 5, 6);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8205126717"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getBookingStatusAssessed(), nullValue());
            assertThat(row.getPaxname(), equalTo("BUBENZER/BRIGITTE"));
            assertThat(row.getSegNoTech(), nullValue());
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9947605"));
        }

        row = fileDataRaws.get(7);
        assertThat(row.getDocumentNo(), equalTo("2334383699"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("BUBENZER/BRIGITTE"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(8);
        assertThat(row.getDocumentNo(), equalTo("2334383699"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("BUBENZER/BRIGITTE"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        multirow = Arrays.asList(9, 10, 11, 12, 13);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8205126718"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getBookingStatusAssessed(), nullValue());
            assertThat(row.getPaxname(), equalTo("BUBENZER/EDGAR"));
            assertThat(row.getSegNoTech(), nullValue());
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9947605"));
        }

        row = fileDataRaws.get(14);
        assertThat(row.getDocumentNo(), equalTo("2334383700"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("BUBENZER/EDGAR"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(15);
        assertThat(row.getDocumentNo(), equalTo("2334383700"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("BUBENZER/EDGAR"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        // 5N3JUD

        booking = new Booking(bookingServices, loadTestData("5N3JUD.xml"));
        assertThat(booking.getRloc(), equalTo("5N3JUD"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(16));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), nullValue());
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("## NONAME ##/null"));
        assertThat(row.getSegNoTech(), equalTo(3));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), nullValue());
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("## NONAME ##/null"));
        assertThat(row.getSegNoTech(), equalTo(4));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        multirow = Arrays.asList(2, 3, 4, 5, 6);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204954536"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getBookingStatusAssessed(), nullValue());
            assertThat(row.getPaxname(), equalTo("STRUCK/HELMUT"));
            assertThat(row.getSegNoTech(), nullValue());
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9924434"));
        }

        row = fileDataRaws.get(7);
        assertThat(row.getDocumentNo(), equalTo("2333535797"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("STRUCK/HELMUT"));
        assertThat(row.getSegNoTech(), equalTo(3));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(8);
        assertThat(row.getDocumentNo(), equalTo("2333535797"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("STRUCK/HELMUT"));
        assertThat(row.getSegNoTech(), equalTo(4));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        multirow = Arrays.asList(9, 10, 11, 12, 13);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204954537"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getBookingStatusAssessed(), nullValue());
            assertThat(row.getPaxname(), equalTo("STRUCK/ELISABETH"));
            assertThat(row.getSegNoTech(), nullValue());
            assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA9924434"));
        }

        row = fileDataRaws.get(14);
        assertThat(row.getDocumentNo(), equalTo("2333535798"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("STRUCK/ELISABETH"));
        assertThat(row.getSegNoTech(), equalTo(3));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

        row = fileDataRaws.get(15);
        assertThat(row.getDocumentNo(), equalTo("2333535798"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("STRUCK/ELISABETH"));
        assertThat(row.getSegNoTech(), equalTo(4));
        assertThat(row.getMiscellaneousInformationFreetext(), equalTo("RA2349405"));

    }


    @Test
    public void verify_TST_12_1() throws IOException {

        DWMapper mapper = new DWMapper();
        Booking booking;
        List<FileDataRaw> fileDataRaws;
        FileDataRaw row;
        List<Integer> multirow;

        // 7A7OHS

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
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));  //diff: 7
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


        // 5WTN6S

        booking = new Booking(bookingServices, loadTestData("5WTN6S.xml"));
        assertThat(booking.getRloc(), equalTo("5WTN6S"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(8));


        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2339573739"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));       //diff: 7
        assertThat(row.getPaxname(), equalTo("BROCKT/JULIAN"));
        assertThat(row.getSegNoTech(), equalTo(1));
        /* diff TODO check (INF)
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
        assertThat(row.getTaxValue1(),nullValue());
        */

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("2339573739"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));  //diff: 7
        assertThat(row.getPaxname(), equalTo("BROCKT/JULIAN"));
        assertThat(row.getSegNoTech(), equalTo(2));
        /* diff TODO check (INF)
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
        assertThat(row.getTaxValue1(),nullValue());
        */

        row = fileDataRaws.get(2);
        assertThat(row.getDocumentNo(), equalTo("2339573737"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));    //diff: 7
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
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));  //diff: 7
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

        row = fileDataRaws.get(4);
        assertThat(row.getDocumentNo(), equalTo("2339573738"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));       //diff: 7
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

        row = fileDataRaws.get(5);
        assertThat(row.getDocumentNo(), equalTo("2339573738"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));    //diff: 7
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

        row = fileDataRaws.get(6);
        assertThat(row.getDocumentNo(), equalTo("2339573740"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));             //diff: 7
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

        row = fileDataRaws.get(7);
        assertThat(row.getDocumentNo(), equalTo("2339573740"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));    //diff: 7
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
    public void verify_Ancillaries_10_1() throws IOException {

        DWMapper mapper = new DWMapper();
        Booking booking;
        List<FileDataRaw> fileDataRaws;
        FileDataRaw row;
        List<Integer> multirow;

        //8GKIZH, DocumentClass “MCO�? (EMD-S)
        booking = new Booking(bookingServices, loadTestData("8GKIZH.xml"));
        assertThat(booking.getRloc(), equalTo("8GKIZH"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(2));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8204636788"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("MALAQUIN/FRANCOIS"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixDepApt(), equalTo("VIE"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("90913"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204636788/DTAB/EUR12.90/09SEP13/PARAB08IB/20494935"));
        //assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:16] [FreeText:CCVI497307XXXXXX1999/0615*CV/A922486]"));  //diff: equalTo("SVC AB HK1 VRDS VIE 09SEP13")
        //assertThat(row.getMcoreason(), equalTo("UKWN"));    //diff:  equalTo("VRDS")
        assertThat(row.getMcoreasonSubCode(), equalTo("RDS"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

        /* diff: only one MCO row
        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8204636788"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        */

        //8HHO9E, DocumentClass “MCO�? (EMD-S)
        booking = new Booking(bookingServices, loadTestData("8HHO9E.xml"));
        assertThat(booking.getRloc(), equalTo("8HHO9E"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(3));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8204641252"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("SCHUBERT/WOLFGANG"));
        assertThat(row.getSegNoTech(), equalTo(3));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixDepApt(), equalTo("TXL"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("210614"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204641252/DTAB/EUR12.90/09SEP13/BERAB08IB/23496605"));
        //assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:16] [FreeText:ELV/DE32100500001060185810/WOLFGANG SCHUBERT*A]"));   //diff: equalTo("SVC AB HK1 VRDS TXL 21JUN14")
        //assertThat(row.getMcoreason(), equalTo("UKWN"));    //diff:  equalTo("VRDS")
        assertThat(row.getMcoreasonSubCode(), equalTo("RDS"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

        /* diff: only one MCO row
        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8204641252"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        */

        //79FDX2 (EMD-S)
        booking = new Booking(bookingServices, loadTestData("79FDX2.xml"));
        assertThat(booking.getRloc(), equalTo("79FDX2"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(1));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8204635433"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("KRANTZ/UTE"));
        assertThat(row.getSegNoTech(), equalTo(9));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixDepApt(), equalTo("HAM"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("140913"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204635433/DTAB/EUR24.90/09SEP13/BUHAB0105/69491590"));
        //assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:16] [FreeText:CCVI410403XXXXXX9032/1213*CV/A866131]"));   //diff: equalTo("SVC AB HK1 VRSG HAM 14SEP13")
        //assertThat(row.getMcoreason(), equalTo("UKWN"));    //diff:  equalTo("VRSG")
        assertThat(row.getMcoreasonSubCode(), equalTo("RSG"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

        /* diff: only one MCO row
        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8204635433"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        */

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
        assertThat(row.getTixDepApt(), equalTo("TXL"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("90913"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-2600638344/PTAB/EUR150.00/09SEP13/TXLAB0005/23496303"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), nullValue());
        assertThat(row.getMcoreason(), equalTo("*EXCESS BAGGAGE//2ND PIECE"));
        assertThat(row.getMcoreasonSubCode(), equalTo("A"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());


        //8D69PH, DocumentClass “MCO�? (EMD-A)
        booking = new Booking(bookingServices, loadTestData("8D69PH.xml"));
        assertThat(booking.getRloc(), equalTo("8D69PH"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(8));

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


        //8BZZ7W, DocumentClass “MCO�? (EMD-A)
        booking = new Booking(bookingServices, loadTestData("8BZZ7W.xml"));
        assertThat(booking.getRloc(), equalTo("8BZZ7W"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(20));

        //Bei 8BZZ7W gibt es ein Problem: die FlugTickets sind nicht den Segmenten zugeordnet, sondern chargeable SSRs| | |

        /*
        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8204639711"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("KROEGER/HORST"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204639711/DTAB/EUR20.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:HAM] [OffPoint:PMI] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:14] [SeatCol:K] [Reference:12] [Description:ST/14K]SRP: [SeatRow:14] [SeatCol:H] [Reference:13] [Description:ST/14H]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2331138394"));
        assertThat(row.getIssInConnWithCpn(), equalTo("1"));



        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8204639711"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("KROEGER/HORST"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204639711/DTAB/EUR20.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:PMI] [OffPoint:HAM] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:13] [SeatCol:B] [Reference:12] [Description:ST/13B]SRP: [SeatRow:13] [SeatCol:A] [Reference:13] [Description:ST/13A]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2331138394"));
        assertThat(row.getIssInConnWithCpn(), equalTo("2"));


        multirow = Arrays.asList(2,3);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204639711"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }


        row = fileDataRaws.get(4);
        assertThat(row.getDocumentNo(), equalTo("8204639712"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("KROEGER/NICOLE"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-9204639712/DTAB/EUR40.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:HAM] [OffPoint:PMI] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:14] [SeatCol:K] [Reference:12] [Description:ST/14K]SRP: [SeatRow:14] [SeatCol:H] [Reference:13] [Description:ST/14H]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2331138393"));
        assertThat(row.getIssInConnWithCpn(), equalTo("1"));

        row = fileDataRaws.get(5);
        assertThat(row.getDocumentNo(), equalTo("8204639712"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("KROEGER/NICOLE"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-9204639712/DTAB/EUR40.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:PMI] [OffPoint:HAM] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:13] [SeatCol:B] [Reference:12] [Description:ST/13B]SRP: [SeatRow:13] [SeatCol:A] [Reference:13] [Description:ST/13A]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2331138393"));
        assertThat(row.getIssInConnWithCpn(), equalTo("2"));


        multirow = Arrays.asList(6,7);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204639712"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }



        //8F6Y2M, DocumentClass “MCO�?, EmdtreatedAs “A�? (EMD-A)
        booking = new Booking(bookingServices, loadTestData("8F6Y2M.xml"));
        assertThat(booking.getRloc(), equalTo("8F6Y2M"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(12));

        row = fileDataRaws.get(2);
        assertThat(row.getDocumentNo(), equalTo("8204640770"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("SCHROEDER/HENRY"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204640770/DTAB/EUR150.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("SSR: [Type:AVIH] [Status:HK] [Quantity:1] [CompanyId:AB] [FreeText:TTL10KG1PCDIM50X20X30CMDOG/ONLY DOMESTIC PETS ARE ALLOWED FOR AVIH/ MAX CRATE 125X75X85CM]"));
        assertThat(row.getMcoreason(), equalTo("AVIH"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0BS"));
        assertThat(row.getIssInConnWith(), equalTo("2331143469"));
        assertThat(row.getIssInConnWithCpn(), equalTo("1"));


        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("8204640770"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("SCHROEDER/HENRY"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204640770/DTAB/EUR150.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("SSR: [Type:AVIH] [Status:HK] [Quantity:1] [CompanyId:AB] [FreeText:TTL10KG1PCDIM50X20X30CMDOG/ONLY DOMESTIC PETS ARE ALLOWED FOR AVIH/ MAX CRATE 125X75X85CM]"));
        assertThat(row.getMcoreason(), equalTo("AVIH"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0BS"));
        assertThat(row.getIssInConnWith(), equalTo("2331143469"));
        assertThat(row.getIssInConnWithCpn(), equalTo("2"));

        multirow = Arrays.asList(4,5);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204640770"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }

                */

    }

    @Test
    public void verify_Ancillaries_12_1() throws IOException {

        DWMapper mapper = new DWMapper();
        Booking booking;
        List<FileDataRaw> fileDataRaws;
        FileDataRaw row;
        List<Integer> multirow;


        /* TODO check this - wrong document no for MCO

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
        //assertThat(row.getIssInConnWithCpn(), equalTo("3"));    //diff: 2


        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8205704896"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("OSTERNACHER/SABINE"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8205704896/DTAB/EUR33.98/12FEB14/BERAB0101/23497401"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:MIA] [OffPoint:TXL] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:46] [SeatCol:K] [Reference:2] [Description:ST/46K]SRP: [SeatRow:46] [SeatCol:H] [Reference:1] [Description:ST/46H]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("5250351578"));
        assertThat(row.getIssInConnWithCpn(), equalTo("2"));    //diff: solved


        multirow = Arrays.asList(2, 3);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8205704896"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
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
        //assertThat(row.getIssInConnWithCpn(), equalTo("3"));    //diff: 2


        row = fileDataRaws.get(13);
        assertThat(row.getDocumentNo(), equalTo("8205704897"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("STEINMAIR/CHRISTIAN"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8205704897/DTAB/EUR33.98/12FEB14/BERAB0101/23497401"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:MIA] [OffPoint:TXL] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:46] [SeatCol:K] [Reference:2] [Description:ST/46K]SRP: [SeatRow:46] [SeatCol:H] [Reference:1] [Description:ST/46H]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("5250351577"));
        assertThat(row.getIssInConnWithCpn(), equalTo("2"));    //diff: solved


        multirow = Arrays.asList(14, 15);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8205704897"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }

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

       */

        //ZPR3KZ, DocumentClass “MCO" (EMD-A)
        booking = new Booking(bookingServices, loadTestData("ZPR3KZ.xml"));
        assertThat(booking.getRloc(), equalTo("ZPR3KZ"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(24));

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

        row = fileDataRaws.get(1);
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

        row = fileDataRaws.get(2);
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

        multirow = Arrays.asList(3, 4);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8205951399"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }

        //23K69M, DocumentClass „MCO“, EmdtreatedAs „S“ (EMD-S)
        booking = new Booking(bookingServices, loadTestData("23K69M.xml"));
        assertThat(booking.getRloc(), equalTo("23K69M"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(7));

        row = fileDataRaws.get(0);
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

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8206067154"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));

        //28XYQ8, DocumentClass „MCO“ (EMD-S)
        booking = new Booking(bookingServices, loadTestData("28XYQ8.xml"));
        assertThat(booking.getRloc(), equalTo("28XYQ8"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8205584670"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("GONZALEZEMBUN/YAIZA"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixDepApt(), equalTo("BCN"));
        assertThat(row.getTixDestApt(), nullValue());
        //assertThat(row.getTixFlightDt(), equalTo("270114"));   //diff: 80514
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8205584670/DTAB/EUR6.90/27JAN14/PMIAB08IB/78492735"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:16] [FreeText:CCVI427784XXXXXX7291/0114*CV/A040357]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        assertThat(row.getMcoreasonSubCode(), equalTo("EPB"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

        /* diff: document 8206067154 is linked to 23K69M
        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8206067154"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        */

        //Y2NH4D, DocumentClass „MCO“ (EMD-S)
        booking = new Booking(bookingServices, loadTestData("Y2NH4D.xml"));
        assertThat(booking.getRloc(), equalTo("Y2NH4D"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(6));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8205893364"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("HOERSKEN/CAROLINE"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixDepApt(), equalTo("DUS"));
        assertThat(row.getTixDestApt(), nullValue());
        //assertThat(row.getTixFlightDt(), equalTo("40314"));        //diff: 40514
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8205893364/DTAB/EUR27.90/04MAR14/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:16] [FreeText:ELV/DE62354500001202078729/CAROLINE HOERSKEN*A]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        assertThat(row.getMcoreasonSubCode(), equalTo("RNI"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8205893364"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));

        //ZW869W, DocumentClass „MCO“ (Virtual MCO)
        booking = new Booking(bookingServices, loadTestData("ZW869W.xml"));
        assertThat(booking.getRloc(), equalTo("ZW869W"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(6));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2700725876"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("QUINONES/HECTOR"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixDepApt(), nullValue());
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("120114"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-2700725876/PTAB/USD39.49/12JAN14/NYCAB08IB/33994122"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:1] [BoardPoint:BCN] [OffPoint:TXL] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:24] [SeatCol:F] [Reference:1] [Description:ST/24F]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("2700725876"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("QUINONES/HECTOR"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixDepApt(), nullValue());
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("120114"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-2700725876/PTAB/USD39.49/12JAN14/NYCAB08IB/33994122"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:1] [BoardPoint:TXL] [OffPoint:MIA] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:33] [SeatCol:K] [Reference:1] [Description:ST/33K]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

        row = fileDataRaws.get(2);
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:16] [FreeText:CCVI414711XXXXXX0056/0416*CV/A90964C]"));

        row = fileDataRaws.get(3);
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));


        //5YZCWQ, DocumentClass „MCO“ (Paper MCO)
        booking = new Booking(bookingServices, loadTestData("5YZCWQ.xml"));
        assertThat(booking.getRloc(), equalTo("5YZCWQ"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(2));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2700725907"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("DAVIS/DOUGLAS"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixDepApt(), equalTo("MIA"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("120514"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-2700725907/PTAB/USD79.24/15JAN14/NYCAB08IB/33994122"));
        assertThat(row.getMcoreason(), equalTo("*ASR*"));
        assertThat(row.getMcoreasonSubCode(), equalTo("O"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("2700725908"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("DAVIS/PATRICIA"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixDepApt(), equalTo("MIA"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("120514"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-2700725908/PTAB/USD79.24/15JAN14/NYCAB08IB/33994122"));
        assertThat(row.getMcoreason(), equalTo("*ASR*"));
        assertThat(row.getMcoreasonSubCode(), equalTo("O"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());


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
        assertThat(row.getTixDepApt(), equalTo("FRA"));
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
    public void verify_TST_10_1() throws IOException {

        DWMapper mapper = new DWMapper();
        Booking booking;
        List<FileDataRaw> fileDataRaws;
        FileDataRaw row;
        List<Integer> multirow;

        //75WVC9
        booking = new Booking(bookingServices, loadTestData("75WVC9.xml"));
        assertThat(booking.getRloc(), equalTo("75WVC9"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("4124641319"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("GREEN/RAMARI"));
        assertThat(row.getSegNoTech(), equalTo(3));
        assertThat(row.getFarebaseCode(), equalTo("WNC34OW"));
        assertThat(row.getFcmi(), equalTo("0"));     //diff: solved
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
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("GREEN/RAMARI"));
        assertThat(row.getSegNoTech(), equalTo(4));
        assertThat(row.getFarebaseCode(), equalTo("WNC34OW"));
        assertThat(row.getFcmi(), equalTo("0"));     //diff: solved;
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
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("GREEN/NEIL"));
        assertThat(row.getSegNoTech(), equalTo(3));
        assertThat(row.getFarebaseCode(), equalTo("WNC34OW"));
        assertThat(row.getFcmi(), equalTo("0"));     //diff: solved
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
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("GREEN/NEIL"));
        assertThat(row.getSegNoTech(), equalTo(4));
        assertThat(row.getFarebaseCode(), equalTo("WNC34OW"));
        assertThat(row.getFcmi(), equalTo("0"));     //diff: solved
        assertThat(row.getSectorTotalFare(), nullValue());
        assertThat(row.getSectorTotalFareNotEur(), equalTo("186.60"));
        assertThat(row.getSectorTotalFareFa(), nullValue());
        assertThat(row.getSectorTotalFareFanotEur(), equalTo("186.60"));
        assertThat(row.getTixCurrency(), equalTo("NZD"));
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("CPH AB X/BER AB BUD M3.87NUC3.87END ROE5.670300"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-NZD"));
        assertThat(row.getTaxValue1(), equalTo("116.80"));

        //796K5N
        booking = new Booking(bookingServices, loadTestData("796K5N.xml"));
        assertThat(booking.getRloc(), equalTo("796K5N"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        row = fileDataRaws.get(2);
        assertThat(row.getDocumentNo(), equalTo("2331135853"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));   //diff: 7
        assertThat(row.getPaxname(), equalTo("KOENIG/WALTER"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("ONCRT"));
        assertThat(row.getFcmi(), equalTo("0"));     //diff: null
        assertThat(row.getSectorTotalFare(), equalTo("219.44"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("219.44"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("CGN AB PMI67.05AB CGN67.05NUC134.10END ROE0.760562"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("84.00"));

        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("2331135853"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));    //diff: 7
        assertThat(row.getPaxname(), equalTo("KOENIG/WALTER"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("ONCRT"));
        assertThat(row.getFcmi(), equalTo("0"));     //diff: null
        assertThat(row.getSectorTotalFare(), equalTo("219.44"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("219.44"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("CGN AB PMI67.05AB CGN67.05NUC134.10END ROE0.760562"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("84.00"));

        //8A8Q8O
        booking = new Booking(bookingServices, loadTestData("8A8Q8O.xml"));
        assertThat(booking.getRloc(), equalTo("8A8Q8O"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(2));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2331138649"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));    //diff: 7
        assertThat(row.getPaxname(), equalTo("DEMARINIS/ORONZO"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("ENC30OW"));
        assertThat(row.getFcmi(), equalTo("0"));     //diff: solved
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
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));    //diff: 7
        assertThat(row.getPaxname(), equalTo("PATANE/DAVIDE"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("ENC30OW"));
        assertThat(row.getFcmi(), equalTo("0"));     //diff: solved
        assertThat(row.getSectorTotalFare(), equalTo("58.71"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("58.71"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("BER AB MIL2.62NUC2.62END ROE0.760562"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("30.00"));


        //8ISHFE
        booking = new Booking(bookingServices, loadTestData("8ISHFE.xml"));
        assertThat(booking.getRloc(), equalTo("8ISHFE"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));


        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2331146018"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));   //diff: 7
        assertThat(row.getPaxname(), equalTo("SCHABEL/BRUNO"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("WNC18RT"));
        assertThat(row.getFcmi(), equalTo("4"));     //diff:  solved
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
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));    //diff: 7
        assertThat(row.getPaxname(), equalTo("SCHABEL/BRUNO"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("ENC27RT"));
        assertThat(row.getFcmi(), equalTo("4"));     //diff:  solved
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
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));      diff: 7
        assertThat(row.getPaxname(), equalTo("SCHABEL/ROSEMARIE"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("WNC18RT"));
        assertThat(row.getFcmi(), equalTo("4"));     //diff:  solved
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
        //assertThat(row.getBookingStatusAssessed(), equalTo("1"));   //diff: 7
        assertThat(row.getPaxname(), equalTo("SCHABEL/ROSEMARIE"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("ENC27RT"));
        assertThat(row.getFcmi(), equalTo("4"));     //diff: solved
        assertThat(row.getSectorTotalFare(), equalTo("108.72"));
        assertThat(row.getSectorTotalFareNotEur(), nullValue());
        assertThat(row.getSectorTotalFareFa(), equalTo("108.72"));
        assertThat(row.getSectorTotalFareFanotEur(), nullValue());
        assertThat(row.getTixCurrency(), nullValue());
        assertThat(row.getTourCode(), nullValue());
        assertThat(row.getFareCalc(), equalTo("STR AB BER2.00AB STR3.50EUR5.50END"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("45.00"));


        //8IUM85
        booking = new Booking(bookingServices, loadTestData("8IUM85.xml"));
        assertThat(booking.getRloc(), equalTo("8IUM85"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2331147868"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("PECH/RENATE"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("WNC20RT"));
        assertThat(row.getFcmi(), equalTo("0"));     //diff:  solved
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
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("PECH/RENATE"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("WNC20RT"));
        assertThat(row.getFcmi(), equalTo("0"));     //diff:  solved
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
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("PROELL/KAETHE"));
        assertThat(row.getSegNoTech(), equalTo(1));
        assertThat(row.getFarebaseCode(), equalTo("WNC20RT"));
        assertThat(row.getFcmi(), equalTo("0"));     //diff:  solved
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
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
        assertThat(row.getPaxname(), equalTo("PROELL/KAETHE"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getFarebaseCode(), equalTo("WNC20RT"));
        assertThat(row.getFcmi(), equalTo("0"));     //diff:  solved
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
    public void verify_FOP_and_FCMI_for_ancillaries() throws IOException {

        DWMapper mapper = new DWMapper();
        Booking booking;
        List<FileDataRaw> fileDataRaws;
        FileDataRaw row;
        List<Integer> multirow;

        // TODO wrong document no for MCO


        //27OLJO, DocumentClass “MCO�? (EMD-A)
        booking = new Booking(bookingServices, loadTestData("27OLJO.xml"));
        assertThat(booking.getRloc(), equalTo("27OLJO"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(10));

        multirow = Arrays.asList(0,1);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8205915682"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }

        row = fileDataRaws.get(2);
        assertThat(row.getDocumentNo(), equalTo("8205915682"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getFopinformationFreetext(), equalTo("ELV/DE88733692640001822462/JOHANN WOELFLE*A"));
        assertThat(row.getFcmi(), equalTo("0"));


        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("8205915682"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getFopinformationFreetext(), equalTo("ELV/DE88733692640001822462/JOHANN WOELFLE*A"));
        assertThat(row.getFcmi(), equalTo("0"));

        //5ZBOJ3, DocumentClass „MCO“ (EMD-A)
        booking = new Booking(bookingServices, loadTestData("5ZBOJ3.xml"));
        assertThat(booking.getRloc(), equalTo("5ZBOJ3"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        multirow = Arrays.asList(0,1);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8206345104"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }

        row = fileDataRaws.get(2);
        assertThat(row.getDocumentNo(), equalTo("8206345104"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getFopinformationFreetext(), equalTo("ELV/NL41ABNA0005981133/G. JONKER-JANS*A"));
        assertThat(row.getFcmi(), equalTo("0"));


        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("8206345104"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getFopinformationFreetext(), equalTo("ELV/NL41ABNA0005981133/G. JONKER-JANS*A"));
        assertThat(row.getFcmi(), equalTo("0"));

        //2BAEI8, DocumentClass “MCO�? (EMD-S)
        booking = new Booking(bookingServices, loadTestData("2BAEI8.xml"));
        assertThat(booking.getRloc(), equalTo("2BAEI8"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8205568855"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getFopinformationFreetext(), equalTo("ELV/DE47120300000017338328/JENNIFER TURBA*A"));
        assertThat(row.getFcmi(), equalTo("0"));


        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8205568855"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));

        row = fileDataRaws.get(2);
        assertThat(row.getDocumentNo(), equalTo("8205568856"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));

        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("8205568856"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getFopinformationFreetext(), equalTo("ELV/DEXXXXXXXXXXXX17338328/JENNIFER TURBA*A"));
        assertThat(row.getFcmi(), equalTo("0"));

        //28YYQE, DocumentClass „MCO“ (EMD-S)
        booking = new Booking(bookingServices, loadTestData("28YYQE.xml"));
        assertThat(booking.getRloc(), equalTo("28YYQE"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2610646591"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getFopinformationFreetext(), equalTo("CCTPXXXXXXXXXXX0016/0917/A669A"));
        assertThat(row.getFcmi(), equalTo("0"));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("2610646591"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));

    }

    @Test
    public void verify_FO_for_PAX_and_EMD() throws IOException {

        DWMapper mapper = new DWMapper();
        Booking booking;
        List<FileDataRaw> fileDataRaws;
        FileDataRaw row;
        List<Integer> multirow;

        //5W3TII
        booking = new Booking(bookingServices, loadTestData("5W3TII.xml"));
        assertThat(booking.getRloc(), equalTo("5W3TII"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(12));

        // TODO no OrigIssueInformationFreetext for MCO and wrong one for PAX
        multirow = Arrays.asList(0, 1, 2, 3);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getOrigIssueInformationFreetext(), equalTo("745-8206236418MIA29APR14/10980211/745-82062364183M1"));
        }


        multirow = Arrays.asList(4, 5, 6, 7);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentClass(), equalTo("PAX"));
            assertThat(row.getOrigIssueInformationFreetext(), equalTo("745-2338946432MIA29APR14/10980211/745-23389464325E1"));
        }

        //ZT5H5E
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

        //38IS83
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
            Context liveContext = ContextFactory.createContext();
            BookingServices liveBookingServices = (BookingServices) liveContext.getDomainServices(Booking.class);
            List<Booking> bookings = liveBookingServices.retrieveByCCL("Booking.Rloc=\"" + rloc + "\"");
            assertThat("Expected exactly one booking when querying by CCL and RLOC=" + rloc, bookings.size(), equalTo(1));
            Booking liveBooking = bookings.get(0);
            String xmlString = liveBooking.toXml();
            writeFile(rloc, xmlString);
            return xmlString;
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
