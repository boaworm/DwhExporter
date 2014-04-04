package com.sabre.ix.application;

import com.sabre.ix.application.logic.AirlineDateFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Henrik Thorburn (sg0211570)
 * Date: 2014-04-03
 */
@RunWith(MockitoJUnitRunner.class)
public class DWMapperTest {

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

        gregorianCalendar.set(2010,Calendar.APRIL, 20);
        assertThat(AirlineDateFormat.calendarToString(gregorianCalendar), equalTo("20APR10"));

        gregorianCalendar.set(2014,Calendar.JANUARY, 1);
        assertThat(AirlineDateFormat.calendarToString(gregorianCalendar), equalTo("01JAN14"));
    }
}
