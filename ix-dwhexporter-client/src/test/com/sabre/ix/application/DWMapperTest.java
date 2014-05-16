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
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Henrik Thorburn (sg0211570)
 * Date: 2014-04-03
 */
@RunWith(MockitoJUnitRunner.class)
public class DWMapperTest {

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
    public void verify_FZ_for7MENHD() throws IOException {
        Booking booking = new Booking(bookingServices, getFileAsString("7MENHD.xml"));
        assertThat(booking.getRloc(), equalTo("7MENHD"));

        DWMapper mapper = new DWMapper();
        List<FileDataRaw> fileDataRaws = mapper.mapBooking(booking);
        assertThat(fileDataRaws.size(), equalTo(16));
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
