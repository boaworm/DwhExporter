package com.sabre.ix.application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: Henrik Thorburn (sg0211570)
 * Date: 2014-04-03
 */
@RunWith(MockitoJUnitRunner.class)
public class DWMapperTest {

    @Test
    public void verifyDateParsing() throws ParseException {

        String d = "O3APR14";
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyy");
        Date parse = sdf.parse(d);

    }

}
