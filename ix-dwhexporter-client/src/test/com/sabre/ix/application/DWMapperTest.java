package com.sabre.ix.application;

import com.sabre.ix.application.dao.FileDataRaw;
import com.sabre.ix.application.logic.AirlineDateFormat;
import com.sabre.ix.application.logic.DWMapper;
import com.sabre.ix.client.Booking;
import com.sabre.ix.client.BookingServices;
import com.sabre.ix.client.context.Context;
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
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Henrik Thorburn (sg0211570)
 * Date: 2014-04-03
 */
@RunWith(MockitoJUnitRunner.class)
public class DWMapperTest {

    private static Logger log = Logger.getLogger(DWMapper.class);

    BookingServices bookingServices;
    @Mock
    Context context;
    @Mock
    DataHandler dataHandler;
    @Mock
    ActionHandler actionHandler;
    @Mock
    MetaModelServices metaModelServices;
    MetaModel metaModel;

    @Before
    public void setup() throws IOException, DocumentException {
        metaModel = prepareMetaModel(DocumentHelper.parseText(getFileAsString("metamodel.xml")));
        when(context.getMetaModelServices()).thenReturn(metaModelServices);
        when(metaModelServices.getMetaModel()).thenReturn(metaModel);
        bookingServices = new BookingServices(context, dataHandler, actionHandler);
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
    @Ignore
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
    @Ignore
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

        // 7MENHD
        log.info("checking 7MENHD");
        booking = new Booking(bookingServices, getFileAsString("7MENHD.xml"));
        assertThat(booking.getRloc(), equalTo("7MENHD"));

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

        booking = new Booking(bookingServices, getFileAsString("3NWPAK.xml"));
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

        booking = new Booking(bookingServices, getFileAsString("5N3JUD.xml"));
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

        booking = new Booking(bookingServices, getFileAsString("7A7OHS.xml"));
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
        assertThat(row.getFareCalc (), equalTo("BER AB MUC4.00AB BER5.00EUR9.00END"));
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
        assertThat(row.getFareCalc (), equalTo("BER AB MUC4.00AB BER5.00EUR9.00END"));
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
        assertThat(row.getFareCalc (), equalTo("BER AB MUC4.00AB BER5.00EUR9.00END"));
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
        assertThat(row.getFareCalc (), equalTo("BER AB MUC4.00AB BER5.00EUR9.00END"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("39.00"));

        // Z8MC3Z

        booking = new Booking(bookingServices, getFileAsString("Z8MC3Z.xml"));
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
        assertThat(row.getFareCalc (), equalTo("BER AB BUH11.61AB BER31.42NUC43.03END ROE0.731857"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("80.00"));

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("2338304958"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("7"));  //diff: 1
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
        assertThat(row.getFareCalc (), equalTo("BER AB BUH11.61AB BER31.42NUC43.03END ROE0.731857"));
        assertThat(row.getTaxCode1(), equalTo("XYQAC-EUR"));
        assertThat(row.getTaxValue1(), equalTo("80.00"));


        // 5WTN6S

        booking = new Booking(bookingServices, getFileAsString("5WTN6S.xml"));
        assertThat(booking.getRloc(), equalTo("5WTN6S"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(8));


        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2339573739"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("7"));       //diff: equalTo("1")
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
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
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
        assertThat(row.getBookingStatusAssessed(), equalTo("7"));    //diff: equalTo("1")
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
        assertThat(row.getFareCalc (), equalTo("BER AB MIA AB BER 000.00NUC 000.00 END ROE1.000000"));
        assertThat(row.getTaxCode1(), nullValue());
        assertThat(row.getTaxValue1(),nullValue());

        row = fileDataRaws.get(3);
        assertThat(row.getDocumentNo(), equalTo("2339573737"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
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
        assertThat(row.getFareCalc (), equalTo("BER AB MIA AB BER 000.00NUC 000.00 END ROE1.000000"));
        assertThat(row.getTaxCode1(), nullValue());
        assertThat(row.getTaxValue1(),nullValue());

        row = fileDataRaws.get(4);
        assertThat(row.getDocumentNo(), equalTo("2339573738"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("7"));       //diff: equalTo("1")
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
        assertThat(row.getFareCalc (), equalTo("BER AB MIA AB BER 000.00NUC 000.00 END ROE1.000000"));
        assertThat(row.getTaxCode1(), nullValue());
        assertThat(row.getTaxValue1(),nullValue());

        row = fileDataRaws.get(5);
        assertThat(row.getDocumentNo(), equalTo("2339573738"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
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
        assertThat(row.getFareCalc (), equalTo("BER AB MIA AB BER 000.00NUC 000.00 END ROE1.000000"));
        assertThat(row.getTaxCode1(), nullValue());
        assertThat(row.getTaxValue1(),nullValue());

        row = fileDataRaws.get(6);
        assertThat(row.getDocumentNo(), equalTo("2339573740"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("7"));             //diff: equalTo("1")
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
        assertThat(row.getFareCalc (), equalTo("BER AB MIA AB BER 000.00NUC 000.00 END ROE1.000000"));
        assertThat(row.getTaxCode1(), nullValue());
        assertThat(row.getTaxValue1(),nullValue());

        row = fileDataRaws.get(7);
        assertThat(row.getDocumentNo(), equalTo("2339573740"));
        assertThat(row.getDocumentClass(), equalTo("PAX"));
        assertThat(row.getBookingStatusAssessed(), equalTo("1"));
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
        assertThat(row.getFareCalc (), equalTo("BER AB MIA AB BER 000.00NUC 000.00 END ROE1.000000"));
        assertThat(row.getTaxCode1(), nullValue());
        assertThat(row.getTaxValue1(),nullValue());

    }

    @Test
    public void verify_Ancillaries_10_1() throws IOException {

        DWMapper mapper = new DWMapper();
        Booking booking;
        List<FileDataRaw> fileDataRaws;
        FileDataRaw row;
        List<Integer> multirow;

        //8GKIZH, DocumentClass “MCO” (EMD-S)
        booking = new Booking(bookingServices, getFileAsString("8GKIZH.xml"));
        assertThat(booking.getRloc(), equalTo("8GKIZH"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(3));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8204636788"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("MALAQUIN/FRANCOIS"));
        assertThat(row.getSegNoTech(), equalTo(2));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixDepApt(), equalTo("VIE"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("90913"));
        assertThat(row.getTixInformationFreetext(),equalTo("PAX 745-8204636788/DTAB/EUR12.90/09SEP13/PARAB08IB/20494935"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("LFT: [SubjectQualifier:3] [Type:16] [FreeText:CCVI497307XXXXXX1999/0615*CV/A922486]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        assertThat(row.getMcoreasonSubCode(), equalTo("RDS"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8204636788"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(), equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));

        //8HHO9E, DocumentClass “MCO” (EMD-S)
        booking = new Booking(bookingServices, getFileAsString("8HHO9E.xml"));
        assertThat(booking.getRloc(), equalTo("8HHO9E"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(4));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8204641252"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("SCHUBERT/WOLFGANG"));
        assertThat(row.getSegNoTech(), equalTo(3));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixDepApt(), equalTo("TXL"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("210614"));
        assertThat(row.getTixInformationFreetext(),equalTo("PAX 745-8204641252/DTAB/EUR12.90/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("LFT: [SubjectQualifier:3] [Type:16] [FreeText:ELV/DE32100500001060185810/WOLFGANG SCHUBERT*A]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        assertThat(row.getMcoreasonSubCode(), equalTo("RDS"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8204641252"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));

        //79FDX2 (EMD-S)
        booking = new Booking(bookingServices, getFileAsString("79FDX2.xml"));
        assertThat(booking.getRloc(), equalTo("79FDX2"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(2));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8204635433"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("KRANTZ/UTE"));
        assertThat(row.getSegNoTech(), equalTo(9));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getTixDepApt(), equalTo("HAM"));
        assertThat(row.getTixDestApt(), nullValue());
        assertThat(row.getTixFlightDt(), equalTo("140913"));
        assertThat(row.getTixInformationFreetext(),equalTo("PAX 745-8204635433/DTAB/EUR24.90/09SEP13/BUHAB0105/69491590"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("LFT: [SubjectQualifier:3] [Type:16] [FreeText:CCVI410403XXXXXX9032/1213*CV/A866131]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));
        assertThat(row.getMcoreasonSubCode(), equalTo("RSG"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());

        row = fileDataRaws.get(1);
        assertThat(row.getDocumentNo(), equalTo("8204635433"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getEmdtreatedAs(), equalTo("S"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("LFT: [SubjectQualifier:3] [Type:P18] [FreeText:AB]"));
        assertThat(row.getMcoreason(), equalTo("UKWN"));

        //79LP74 (Paper EBT)
        booking = new Booking(bookingServices, getFileAsString("79LP74.xml"));
        assertThat(booking.getRloc(), equalTo("79LP74"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(1));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("2600638344"));
        assertThat(row.getDocumentClass(), equalTo("EBT"));
        assertThat(row.getPaxname(), equalTo("RAPA/ANETA"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixDepApt(),equalTo("TXL"));
        assertThat(row.getTixDestApt(),nullValue());
        assertThat(row.getTixFlightDt(), equalTo("90913"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-2600638344/PTAB/EUR150.00/09SEP13/TXLAB0005/23496303"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),nullValue());
        assertThat(row.getMcoreason(), equalTo("*EXCESS BAGGAGE//2ND PIECE"));
        assertThat(row.getMcoreasonSubCode(), equalTo("A"));
        assertThat(row.getIssInConnWith(), nullValue());
        assertThat(row.getIssInConnWithCpn(), nullValue());


        //8D69PH, DocumentClass “MCO” (EMD-A)
        booking = new Booking(bookingServices, getFileAsString("8D69PH.xml"));
        assertThat(booking.getRloc(), equalTo("8D69PH"));

        fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(12));

        row = fileDataRaws.get(0);
        assertThat(row.getDocumentNo(), equalTo("8204640286"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("BONA/BIANCA"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204640286/DTAB/EUR40.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:TXL] [OffPoint:BCN] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:15] [SeatCol:B] [Reference:13] [Description:ST/15B]SRP: [SeatRow:15] [SeatCol:A] [Reference:12] [Description:ST/15A]"));
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
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:BCN] [OffPoint:TXL] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:15] [SeatCol:B] [Reference:13] [Description:ST/15B]SRP: [SeatRow:15] [SeatCol:A] [Reference:12] [Description:ST/15A]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2331140477"));
        assertThat(row.getIssInConnWithCpn(), equalTo("2"));

        multirow = Arrays.asList(2,3);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204640286"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }

        multirow = Arrays.asList(4,5);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("2331140477"));
            assertThat(row.getDocumentClass(), equalTo("PAX"));
        }

        row = fileDataRaws.get(6);
        assertThat(row.getDocumentNo(), equalTo("8204640287"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("RAUTMANN/BJOERN"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204640287/DTAB/EUR40.00/09SEP13/BERAB08IB/23496605"));
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:TXL] [OffPoint:BCN] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:15] [SeatCol:B] [Reference:13] [Description:ST/15B]SRP: [SeatRow:15] [SeatCol:A] [Reference:12] [Description:ST/15A]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2331140476"));
        assertThat(row.getIssInConnWithCpn(), equalTo("1"));

        row = fileDataRaws.get(7);
        assertThat(row.getDocumentNo(), equalTo("8204640287"));
        assertThat(row.getDocumentClass(), equalTo("MCO"));
        assertThat(row.getPaxname(), equalTo("RAUTMANN/BJOERN"));
        assertThat(row.getEmdtreatedAs(), equalTo("A"));
        assertThat(row.getTixInformationFreetext(), equalTo("PAX 745-8204640287/DTAB/EUR40.00/09SEP13/BERAB08IB/23496605"));  //diff:  PAX 745-8204640286
        assertThat(row.getMiscellaneousChargeOrderFreetext(),equalTo("[AirlineCode:AB] [ActionCode:HK] [NumberInParty:2] [BoardPoint:BCN] [OffPoint:TXL] [FreeText:]SSR: [NoSmoking:N]SRP: [SeatRow:15] [SeatCol:B] [Reference:13] [Description:ST/15B]SRP: [SeatRow:15] [SeatCol:A] [Reference:12] [Description:ST/15A]"));
        assertThat(row.getMcoreason(), equalTo("RQST"));
        assertThat(row.getMcoreasonSubCode(), equalTo("0B5"));
        assertThat(row.getIssInConnWith(), equalTo("2331140476"));  //diff: equalTo("2331140476")
        assertThat(row.getIssInConnWithCpn(), equalTo("2"));

        multirow = Arrays.asList(8,9);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("8204640287"));
            assertThat(row.getDocumentClass(), equalTo("MCO"));
            assertThat(row.getEmdtreatedAs(), equalTo("A"));
            assertThat(row.getMcoreason(), equalTo("UKWN"));
        }

        multirow = Arrays.asList(10,11);
        for (int rownum : multirow) {
            row = fileDataRaws.get(rownum);
            assertThat(row.getDocumentNo(), equalTo("2331140476"));
            assertThat(row.getDocumentClass(), equalTo("PAX"));
        }

    }



    private String getFileAsString(String fileName) throws IOException {
        File testFile = new File("C:/dev/DwhExporter/testdata/" + fileName);
        return FileUtils.readFileToString(testFile);
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
