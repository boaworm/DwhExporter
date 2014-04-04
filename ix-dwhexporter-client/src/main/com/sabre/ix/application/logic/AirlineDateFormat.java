package com.sabre.ix.application.logic;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Henrik Thorburn (sg0211570)
 * Date: 2014-04-04
 */
public class AirlineDateFormat {

    public static final Map<String, Integer> monthMap = new HashMap<String, Integer>() {{
        put("JAN", Calendar.JANUARY);
        put("FEB", Calendar.FEBRUARY);
        put("MAR", Calendar.MARCH);
        put("APR", Calendar.APRIL);
        put("MAY", Calendar.MAY);
        put("JUN", Calendar.JUNE);
        put("JUL", Calendar.JULY);
        put("AUG", Calendar.AUGUST);
        put("SEP", Calendar.SEPTEMBER);
        put("OCT", Calendar.OCTOBER);
        put("NOV", Calendar.NOVEMBER);
        put("DEC", Calendar.DECEMBER);
    }};


    // Input support: ddMMMyy
    // Example : 2014-02-03 = 03FEB14 = Third of february 2014
    public static GregorianCalendar stringToCalendar(String dateString) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();

        String day = dateString.substring(0, 2);
        if (day.startsWith("0") || day.startsWith("O")) {
            day = day.substring(1, 2);
        }
        String month = dateString.substring(2, 5);
        int monthInt = monthMap.get(month);

        String year = dateString.substring(5, 7);
        if (year.startsWith("0") || year.startsWith("O")) {
            year = year.substring(1, 2);
        }

        int dayInt = Integer.parseInt(day);
        int yearInt = 2000 + Integer.parseInt(year);


        gregorianCalendar.set(yearInt, monthInt, dayInt);
        return gregorianCalendar;
    }


    public static String calendarToString(GregorianCalendar gregorianCalendar) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMMyy");
        String s = simpleDateFormat.format(gregorianCalendar.getTime());
        s = s.toUpperCase();
        return s;
    }
}
