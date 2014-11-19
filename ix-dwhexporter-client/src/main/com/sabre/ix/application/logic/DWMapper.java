package com.sabre.ix.application.logic;

import com.sabre.ix.application.dao.FileDataRaw;
import com.sabre.ix.client.*;
import org.apache.log4j.Logger;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DWMapper {


    private static Logger log = Logger.getLogger(DWMapper.class);


    public DWMapper() {

    }

    public List<FileDataRaw> mapBooking(Booking booking) {
        List<FileDataRaw> result = new LinkedList<FileDataRaw>();
        try {
            LinkedList<BookingName> adults = new LinkedList<BookingName>();
            ArrayList<BookingName> infants = new ArrayList<BookingName>();
            for (BookingName name : booking.getBookingNames()) {
                if (name.getPassengerType() == BookingName.PASS_TYPE_INFANT) {
                    infants.add(name);
                } else {
                    adults.add(name);
                }
            }
            log.debug("Processing " + adults.size() + " adults and " + infants.size() + " infants");
            //start with infants as they may need to be processed with their responsible adult
            for (BookingName infant : infants) {
                //if infant has active SSR INFT line then it is processed on its own.
                boolean processIndependently = false;
                L:
                for (BookingNameItem item : infant.getBookingNameItems()) {
                    // We need to get also inactive ones as we may export the SL after the b, bn or bni is flown.
                    ArrayList<ServiceLine> activeServiceLines = getAllNonCancelledServiceLinesFromItem(item);
                    //SSR INFT lines can be on the responsible adult
                    BookingName responsibleAdult = null;
                    Long responsibleAdultId = infant.getDynamicAttribute("ResponsibleAdult");
                    if (responsibleAdultId != null) {
                        for (BookingName bookingName : booking.getBookingNames()) {
                            if (bookingName.getBookingNameId() == responsibleAdultId) {
                                responsibleAdult = bookingName;
                                log.debug("Found responsible adult: " + responsibleAdult);
                                break;
                            }
                        }
                    }

                    if (responsibleAdult != null) {
                        for (BookingNameItem adultItem : responsibleAdult.getBookingNameItems()) {
                            if (adultItem.getCrsSegmentLineNum() == item.getCrsSegmentLineNum()) {
                                // We need to get also inactive ones as we may export the SL after the b, bn or bni is flown.
                                activeServiceLines.addAll(getAllNonCancelledServiceLinesFromItem(adultItem));
                            }
                        }
                    }
                    for (ServiceLine sLine : activeServiceLines) {
                        if (sLine.getServiceLineTypeCode() != null && sLine.getServiceLineTypeCode().equalsIgnoreCase("SSR") &&
                                sLine.getSecondaryType() != null && sLine.getSecondaryType().equalsIgnoreCase("INFT")) {
                            processIndependently = true;
                            break L;
                        }
                    }
                }
                if (processIndependently) {
                    processName(booking, infant, null, result);
                } else {
                    //find adult with same tattoo
                    boolean found = false;
                    for (Iterator<BookingName> adultIt = adults.iterator(); adultIt.hasNext(); ) {
                        BookingName adult = adultIt.next();
                        log.debug("Comparing adult and infant crs: " + adult.getCrsNameLineNum() + "/" + infant.getCrsNameLineNum());
                        if (adult.getCrsNameLineNum() == infant.getCrsNameLineNum()) {
                            found = true;
                            processName(booking, adult, infant, result);
                            adultIt.remove();
                        }
                    }
                    if (!found) {
                        processName(booking, infant, null, result);
                    }
                }
            }
            for (BookingName adult : adults) {
                processName(booking, adult, null, result);
            }
            populateOtherStagingRecords(booking, null, null, result, null, null);
        } catch (NullPointerException e) {
            log.error("DWMapper.mapBooking: Failed to parse booking " + booking.getRloc() + "/" + booking.getBookingId(), e);
            throw e;
        }
        return result;
    }

    private ArrayList<ServiceLine> getAllNonCancelledServiceLinesFromItem(BookingNameItem item) {
        ArrayList<ServiceLine> list = new ArrayList<ServiceLine>();
        for (ServiceLine sl : item.getServiceLines()) {
            if (sl.getServiceLineState() != ServiceLine.STATUS_DELETED) {
                list.add(sl);
            }
        }
        return list;
    }

    private void processName(Booking booking, BookingName name, BookingName infant, List<FileDataRaw> rows) {
        List<FileDataRaw> itemRows = new ArrayList<FileDataRaw>();
        boolean insurance = false;
        String insurances = null;
        for (BookingNameItem item : name.getBookingNameItems()) {
            if (item.getItemStatus() != BookingNameItem.SPLIT_ITEM &&
                    (item.getType() == null || item.getType().equalsIgnoreCase("AIR"))) {
                FileDataRaw row = new FileDataRaw();
                row.setDocumentClass("PAX");
                populatePAXStagingRecord(booking, name, infant, item, row);
                itemRows.add(row);
                populateOtherStagingRecords(booking, name, item, rows, row, null);
            }
            if (item.getItemStatus() != BookingNameItem.SPLIT_ITEM &&
                    item.getType() != null && item.getType().equalsIgnoreCase("IU")) {
                if (item.getAircraftType() != null && item.getAircraftType().startsWith("V")) {
                    insurance = true;
                    insurances = wrap(insurances) + getInsuranceFreeText(booking, item);
                }
                populateOtherStagingRecords(booking, name, item, rows, null, null);
            }
        }

        populateOtherStagingRecords(booking, name, null, rows, null, itemRows);
        if (insurance) {
            for (FileDataRaw row : itemRows) {
                row.setInsuranceFreetext(insurances);
            }
        }
        rows.addAll(itemRows);
    }

    private String getInsuranceFreeText(Booking booking, BookingNameItem item) {
        return "SVC " + item.getOperatingCarrier() + " " + item.getCrsSegStatus() + booking.getReservationCount() + " " +
                item.getAircraftType() + " " + (item.getOrigin() == null ? "" : item.getOrigin()) + " " +
                (item.getDepartureDate() == null ? "" : getResFormatDate(item.getDepartureDate()));
    }

    private void populateOtherStagingRecords(Booking booking, BookingName name, BookingNameItem item, List<FileDataRaw> rows, FileDataRaw tstRow, List<FileDataRaw> itemRows) {
        // ItemRows will be populated when we're not processing on a particular item, but on the BN.
        if (item != null) {
            //need to find the TSM row that has this coupon:
            for (ChargeableItem chargeableItem : name.getChargeableItems()) {
                for (ChargeableItemCoupon coupon : chargeableItem.getChargeableItemCoupons()) {
                    BookingNameItem bni = getBookingNameItemById(booking, coupon.getBookingNameItemId());
                    if (bni != null && bni.getCrsSegmentLineNum() == item.getCrsSegmentLineNum()) {
                        populateOtherStagingRecord(chargeableItem, booking, name, item, rows, tstRow, itemRows);
                        break;
                    }
                }
            }
            if (tstRow != null) {
                tstRow.setNegoSpacePnr(item.getNegoSpace());
            }

        } else if (name != null) {
            for (ChargeableItem chargeableItemEntity : name.getChargeableItems()) {
                populateOtherStagingRecord(chargeableItemEntity, booking, name, item, rows, tstRow, itemRows);
            }
        } else if (booking != null) {
            for (ChargeableItem chargeableItemEntity : booking.getChargeableItems()) {
                populateOtherStagingRecord(chargeableItemEntity, booking, name, item, rows, tstRow, itemRows);
            }
        }
    }

    private BookingNameItem getBookingNameItemById(Booking booking, long id) {
        for (BookingName bookingName : booking.getBookingNames()) {
            for (BookingNameItem bookingNameItem : bookingName.getBookingNameItems()) {
                if (bookingNameItem.getBookingNameItemId() == id) {
                    return bookingNameItem;
                }
            }
        }
        return null;
    }
        /*
    private ChargeableItem getChargeableItemByBookingNameItemId(BookingNameItem item) {
        for (BookingName bookingName : booking.getBookingNames()) {
            for (BookingNameItem bookingNameItem : bookingName.getBookingNameItems()) {
                if (bookingNameItem.getBookingNameItemId() == id) {
                    return bookingNameItem;
                }
            }
        }
        return null;
    }
          */

    /*
    private List<ServiceLine> getServiceLinesAssociatedWithChargeableItem(ChargeableItem cie) {
        List<ServiceLine> associatedServiceLines = new ArrayList<ServiceLine>();

        BookingName bookingName = cie.getBookingName();
        if (bookingName != null) {
            for (ServiceLine serviceLine : bookingName.getServiceLines()) {
                try {
                    if (serviceLine.getChargeableItemId() == cie.getChargeableItemId()) {
                        associatedServiceLines.add(serviceLine);
                    }
                } catch (NullPointerException npe) {
                    // This can safely be ignored, this happens if the delegate has a NULL id
                }
            }

            // Also take a look in bookingNameItem
            for (BookingNameItem item : bookingName.getBookingNameItems()) {
                for (ServiceLine serviceLine : item.getServiceLines()) {
                    if (serviceLine.getChargeableItemId() == cie.getChargeableItemId()) {
                        associatedServiceLines.add(serviceLine);
                    }
                }
            }
        }

        return associatedServiceLines;
    }
    */

    private List<ServiceLine> getServiceLinesForChargeableItem(ChargeableItem chargeableItem) {
        BookingName bn = chargeableItem.getBookingName();
        if (bn == null) return Collections.emptyList();
        Booking booking = bn.getBooking();
        if (booking == null) return Collections.emptyList();
        Set<Long> serviceLineIDs = new HashSet<Long>();
        List<ServiceLine> serviceLines = new ArrayList<ServiceLine>();

        Map<Long, ServiceLine> allServiceLines = new HashMap<Long, ServiceLine>();

        for (BookingNameItem bookingNameItem : bn.getBookingNameItems()) {
            for (ServiceLine serviceLine : bookingNameItem.getServiceLines()) {
                if (serviceLine.getChargeableItemId() == chargeableItem.getChargeableItemId() && !serviceLineIDs.contains(serviceLine.getServiceLineId())) {
                    serviceLineIDs.add(serviceLine.getServiceLineId());
                    serviceLines.add(serviceLine);
                }
                allServiceLines.put(serviceLine.getServiceLineId(), serviceLine);
            }
        }
        for (ServiceLine serviceLine : bn.getServiceLines()) {
            if (serviceLine.getChargeableItemId() == chargeableItem.getChargeableItemId() && !serviceLineIDs.contains(serviceLine.getServiceLineId())) {
                serviceLineIDs.add(serviceLine.getServiceLineId());
                serviceLines.add(serviceLine);
            }

            allServiceLines.put(serviceLine.getServiceLineId(), serviceLine);
        }

        for (ServiceLine serviceLine : booking.getServiceLines()) {
            if (serviceLine.getChargeableItemId() == chargeableItem.getChargeableItemId() && !serviceLineIDs.contains(serviceLine.getServiceLineId())) {
                serviceLineIDs.add(serviceLine.getServiceLineId());
                serviceLines.add(serviceLine);
            }

            allServiceLines.put(serviceLine.getServiceLineId(), serviceLine);
        }

        for (ChargeableItemCoupon coupon : chargeableItem.getChargeableItemCoupons()) {

            if (!serviceLineIDs.contains(coupon.getServiceLineId())) {
                // Possible a new one we should add...
                ServiceLine serviceLine = allServiceLines.get(coupon.getServiceLineId());
                if (serviceLine != null) {
                    serviceLineIDs.add(serviceLine.getServiceLineId());
                    serviceLines.add(serviceLine);
                }
            }
        }

        return serviceLines;
    }

    //#MH 22MAY14# renamed - was getServiceLinesForChargeableItemCoupon
    private ServiceLine getServiceLineForChargeableItemCoupon(ChargeableItemCoupon coupon) {
        ChargeableItem chargeableItem = coupon.getChargeableItem();
        if (chargeableItem == null) return null;
        BookingName bn = chargeableItem.getBookingName();
        if (bn == null) return null;
        Booking booking = bn.getBooking();
        for (BookingName bookingName : booking.getBookingNames()) {
            for (BookingNameItem bookingNameItem : bookingName.getBookingNameItems()) {
                for (ServiceLine serviceLine : bookingNameItem.getServiceLines()) {
                    if (serviceLine.getServiceLineId() == coupon.getServiceLineId()) {
                        return serviceLine;
                    }
                }
            }
            for (ServiceLine serviceLine : bookingName.getServiceLines()) {
                if (serviceLine.getServiceLineId() == coupon.getServiceLineId()) {
                    return serviceLine;
                }
            }

        }
        for (ServiceLine serviceLine : booking.getServiceLines()) {
            if (serviceLine.getServiceLineId() == coupon.getServiceLineId()) {
                return serviceLine;
            }
        }
        return null;
    }

    private void populateOtherStagingRecord(ChargeableItem chargeableItem, Booking booking, BookingName name, BookingNameItem item, List<FileDataRaw> rows, FileDataRaw tstRow, List<FileDataRaw> itemRows) {
        if (chargeableItem.getType() == null) {
            return;
        }
        if (chargeableItem.getType().equalsIgnoreCase("TSM")) {

            if (item == null) {
                //if this chargeable item is associated with a booking name item we have already processed it
                for (ChargeableItemCoupon coupon : chargeableItem.getChargeableItemCoupons()) {
                    if (getBookingNameItemById(booking, coupon.getBookingNameItemId()) != null) {
                        return;
                    }
                }
            }

            List<ServiceLine> serviceLinesAssociatedWithChargeableItem = getServiceLinesForChargeableItem(chargeableItem);
            if (!serviceLinesAssociatedWithChargeableItem.isEmpty()) {
                //if this is a service line associated TSM (e.g. seat request)
                //we create on row for each such service line. Each TSM may have multiple coupons each of which is
                //associated with one service line.
                for (ServiceLine sLine : serviceLinesAssociatedWithChargeableItem) {

                    // 2014-06-17 - SBR12.1 ZPR3KZ blowup fix.
                    // Experiment to remove duplicates. We arrive here because we have ServiceLines that are all
                    // associated with the CI as expected, but not all of them are related to the BNI we are
                    // currently processing. Lets see if this can fix that.
                    if (item != null && sLine.getBookingNameItem() != null && !item.equals(sLine.getBookingNameItem())) {
                        // Ignore it, wrong BNI
                        continue;
                    }

                    FileDataRaw row = new FileDataRaw();
                    populateMCOFields(chargeableItem, booking, name, item, sLine, tstRow, row, itemRows);

                    if (item != null && item.getType() != null &&  item.getType().equalsIgnoreCase("IU")) {
                        // As of 12.1, we correctly associate a service line to these chargeables. Thus there is now
                        // a secondary code that is being used. Given that the code is always UKWN, this if/else
                        // falls back to pulling the aircraft type for all IU segments
                        // Here we say that if the type is UI, we use the aircraft code (which is now an insurance type)
                        row.setMcoreason(ensureLength(item.getAircraftType(), 100));
                    } else {
                        row.setMcoreason(ensureLength(sLine.getSecondaryType(), 100));
                    }

                    row.setMiscellaneousChargeOrderFreetext(ensureLength(sLine.getFreeText(), 999));
                    if (sLine.getServiceLineTypeCode().equalsIgnoreCase("SEA")) {
                        SBRFreeTextParser parser = new SBRFreeTextParser(sLine.getFreeText());
                        handleSEA(row, sLine, parser);
                    }

                    //these two line replace  the code below
                    ServiceLine ticketingLine = null;
                    ticketingLine = getRelevantTicketingLineNew(booking, sLine, item, name);


                    /*
                    //we get the ticket fields by finding the FA line that has a reference to this service line:
                    //we assume the FA line can be at booking name or booking name item level.
                    ServiceLine ticketingLine = null;
                    if (item != null) {
                        ticketingLine = getRelevantTicketingLine(booking, sLine, item.getServiceLines());
                    }
                    if (ticketingLine == null && chargeableItem.getBookingName() != null) {
                        ticketingLine = getRelevantTicketingLine(booking, sLine, chargeableItem.getBookingName().getServiceLines());
                    }
                    */

                    if (ticketingLine != null) {
                        SBRFreeTextParser parser = new SBRFreeTextParser(ticketingLine.getFreeText());
                        String freeText = parser.get("FreeText");
                        handleTicketing(row, parser, freeText, false);
                    }

                    rows.add(row);
                }

            } else {
                //If this is associated with a manual auxiliary segment (represented by a booking name item of type IU) then
                //we generate one row.
                FileDataRaw row = new FileDataRaw();
                populateMCOFields(chargeableItem, booking, name, item, null, tstRow, row, itemRows);

                //we get the ticketing  from the booking name item associated ticketing line
                if (item != null) {
                    ServiceLine ticketingLine = null;
                    List<ServiceLine> serviceLines = item.getServiceLines();
                    serviceLines.addAll(name.getServiceLines());

                    GregorianCalendar mostRecentCancellationDate = null;
                    if (booking.getBookingStatus() == Booking.CANCELLED_BOOKING) {
                        for (ServiceLine searchLine : serviceLines) {
                            if (searchLine.getFreeText().contains("TicketDocumentData")) {
                                SBRFreeTextParser parser = new SBRFreeTextParser(searchLine.getFreeText());
                                String freeText = parser.get("FreeText");

                                if (freeText != null) {
                                    String comps[] = freeText.split("/");
                                    if (comps.length > 2 && comps[2].length() > 3) {
                                        String currency = comps[2].substring(0, 3);
                                        String totalFareFa = comps[2].substring(3);

                                        // Default to EUR if no currency set. Best effort.
                                        String tixCurrency = row.getTixCurrency();
                                        if (tixCurrency == null) {
                                            tixCurrency = "EUR";
                                        }
                                        if (currency.equals(tixCurrency) && totalFareFa.equals(row.getSectorTotalFare())) {
                                            if (mostRecentCancellationDate == null) {
                                                ticketingLine = searchLine;
                                                mostRecentCancellationDate = searchLine.getCancellationDate();
                                            } else {
                                                if (searchLine.getCancellationDate().after(mostRecentCancellationDate)) {
                                                    ticketingLine = searchLine;
                                                    mostRecentCancellationDate = searchLine.getCancellationDate();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    } else {
                        for (ServiceLine searchLine : serviceLines) {
                            if (searchLine.getFreeText().contains("TicketDocumentData") && searchLine.getServiceLineState() != ServiceLine.STATUS_DELETED) {
                                SBRFreeTextParser parser = new SBRFreeTextParser(searchLine.getFreeText());
                                String freeText = parser.get("FreeText");

                                if (freeText != null) {
                                    String comps[] = freeText.split("/");
                                    if (comps.length > 2 && comps[2].length() > 3) {
                                        String currency = comps[2].substring(0, 3);
                                        String totalFareFa = comps[2].substring(3);

                                        // Default to EUR if no currency set. Best effort.
                                        String tixCurrency = row.getTixCurrency();
                                        if (tixCurrency == null) {
                                            tixCurrency = "EUR";
                                        }
                                        if (currency.equals(tixCurrency) && totalFareFa.equals(row.getSectorTotalFare())) {
                                            ticketingLine = searchLine;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }


                    if (ticketingLine != null) {
                        SBRFreeTextParser parser = new SBRFreeTextParser(ticketingLine.getFreeText());
                        String freeText = parser.get("FreeText");
                        handleTicketing(row, parser, freeText, false);
                    }
                    //    /SVC AB HK1 VRSB TXL 02FEB
                    row.setMiscellaneousChargeOrderFreetext(getInsuranceFreeText(booking, item));
                    row.setMcoreason(ensureLength(item.getAircraftType(), 100));
                    if (item.getItemStatus() != BookingNameItem.SPLIT_ITEM &&
                            item.getType() != null && item.getType().equalsIgnoreCase("IU") &&
                            item.getAircraftType() != null && item.getAircraftType().startsWith("V")) {
                        row.setInsuranceFreetext(getInsuranceFreeText(booking, item));

                    }
                }


                rows.add(row);
            }

        } else if (chargeableItem.getType().equalsIgnoreCase("MCO")) {
            FileDataRaw row = new FileDataRaw();
            row.setEmdtreatedAs("S");
            handlePaperMCO(chargeableItem, booking, name, item, tstRow, chargeableItem, row);
            rows.add(row);

        } else if (chargeableItem.getType().equalsIgnoreCase("XSB")) {
            FileDataRaw row = new FileDataRaw();
            handlePaperMCO(chargeableItem, booking, name, item, tstRow, chargeableItem, row);
            row.setDocumentClass("EBT");
            row.setMiscellaneousChargeOrderFreetext(null);
            setCommonFields(booking, name, null, item, row);
            setXSBStandardFields(row, chargeableItem);
            row.setEmdtreatedAs("A");
            rows.add(row);
        }
    }

    private ServiceLine getRelevantTicketingLineNew(Booking booking, ServiceLine sLine, BookingNameItem item, BookingName name) {
        ServiceLine ticketingLine = null;

        if (name == null) return null;

        if (booking.getBookingStatus() == Booking.CANCELLED_BOOKING) {
            // In case of cancelled bookings, we want to find the most recently cancelled TicketDocumentData service line (if any)
            GregorianCalendar mostRecentCancellationDate = null;
            for (ServiceLine searchLine : name.getServiceLines()) {
                if (searchLine.getFreeText().contains("TicketDocumentData")) {
                    if (searchLine.getFreeText().contains("Reference: [Qualifier:" + sLine.getServiceLineTypeCode() +
                            "] [Number:" + sLine.getCrsId() + "]")) {
                        if (mostRecentCancellationDate == null) {
                            ticketingLine = searchLine;
                            mostRecentCancellationDate = ticketingLine.getCancellationDate();
                        } else {
                            if (searchLine.getCancellationDate().after(mostRecentCancellationDate)) {
                                ticketingLine = searchLine;
                                mostRecentCancellationDate = ticketingLine.getCancellationDate();
                            }
                        }
                    }
                }
            }
        } else {
            for (ServiceLine searchLine : name.getServiceLines()) {
                if (searchLine.getFreeText().contains("TicketDocumentData") && searchLine.getServiceLineState() != ServiceLine.STATUS_DELETED) {
                    if (searchLine.getFreeText().contains("Reference: [Qualifier:" + sLine.getServiceLineTypeCode() +
                            "] [Number:" + sLine.getCrsId() + "]")) {
                        ticketingLine = searchLine;
                        break;
                    }
                }
            }
        }

        if (ticketingLine != null) {
            return ticketingLine;
        }

        if (ticketingLine == null && item != null) {
            //searchline may be a service line related to an MCO, but has no Reference: in free text (e.g. FP,FV,FZ)
            //get service lines related to the same chargeable item
            if (sLine.getBookingNameItemId() == 0) {
                //need to check service lines at BookingName Level
                List<ServiceLine> searchLines = item.getServiceLines();
                for (ServiceLine searchLine : searchLines) {
                    if (searchLine.getServiceLineState() != ServiceLine.STATUS_DELETED)
                        if (searchLine.getChargeableItemId() == sLine.getChargeableItemId()) {
                            //and grab ticketing line from there
                            ServiceLine sLineAlternative = searchLine;
                            for (ServiceLine candidate : name.getServiceLines()) {
                                if (candidate.getFreeText().contains("TicketDocumentData") && candidate.getServiceLineState() != ServiceLine.STATUS_DELETED) {
                                    if (candidate.getFreeText().contains("Reference: [Qualifier:" + sLineAlternative.getServiceLineTypeCode() +
                                            "] [Number:" + sLineAlternative.getCrsId() + "]")) {
                                        ticketingLine = candidate;
                                    }
                                }
                            }
                        }
                }
            }
        }
        return ticketingLine;
    }

    private ServiceLine getRelevantTicketingLine(Booking booking, ServiceLine sLine, List<ServiceLine> searchLines) {
        ServiceLine ticketingLine = null;


        if (booking.getBookingStatus() == Booking.CANCELLED_BOOKING) {
            // In case of cancelled bookings, we want to find the most recently cancelled TicketDocumentData service line (if any)
            GregorianCalendar mostRecentCancellationDate = null;
            for (ServiceLine searchLine : searchLines) {
                if (searchLine.getFreeText().contains("TicketDocumentData")) {
                    if (searchLine.getFreeText().contains("Reference: [Qualifier:" + sLine.getServiceLineTypeCode() +
                            "] [Number:" + sLine.getCrsId() + "]")) {
                        if (mostRecentCancellationDate == null) {
                            ticketingLine = searchLine;
                            mostRecentCancellationDate = ticketingLine.getCancellationDate();
                        } else {
                            if (searchLine.getCancellationDate().after(mostRecentCancellationDate)) {
                                ticketingLine = searchLine;
                                mostRecentCancellationDate = ticketingLine.getCancellationDate();
                            }
                        }
                    }
                }
            }
            if (ticketingLine != null) {
                return ticketingLine;
            }
        }

        for (ServiceLine searchLine : searchLines) {
            if (searchLine.getFreeText().contains("TicketDocumentData") && searchLine.getServiceLineState() != ServiceLine.STATUS_DELETED) {
                if (searchLine.getFreeText().contains("Reference: [Qualifier:" + sLine.getServiceLineTypeCode() +
                        "] [Number:" + sLine.getCrsId() + "]")) {
                    ticketingLine = searchLine;
                }
            }
        }

        /*
        //14.05.14 assign the first ticket line
        if (ticketingLine == null) {
            for (ServiceLine searchLine : searchLines) {
                if (searchLine.getFreeText().contains("TicketDocumentData") && searchLine.getServiceLineState() != ServiceLine.STATUS_DELETED) {
                    return searchLine;
                }
            }
        }
        */

        return ticketingLine;
    }

    private void handlePaperMCO(ChargeableItem chargeableItemEntity, Booking booking, BookingName name, BookingNameItem item, FileDataRaw tstRow, ChargeableItem chargeableItem, FileDataRaw row) {

        populateMCOFields(chargeableItemEntity, booking, name, item, null, tstRow, row, null);
        ServiceLine ticketingLine = null;

        GregorianCalendar mostRecentCancellationDate = null;
        if (booking.getBookingStatus() == Booking.CANCELLED_BOOKING) {
            for (ServiceLine searchLine : getServiceLinesForChargeableItem(chargeableItemEntity)) {
                if (searchLine.getFreeText().contains("TicketDocumentData")) {
                    if (mostRecentCancellationDate == null) {
                        ticketingLine = searchLine;
                        mostRecentCancellationDate = searchLine.getCancellationDate();
                    } else {
                        if (searchLine.getCancellationDate().after(mostRecentCancellationDate)) {
                            ticketingLine = searchLine;
                            mostRecentCancellationDate = searchLine.getCancellationDate();
                        }
                    }
                }
            }
        } else {
            for (ServiceLine searchLine : getServiceLinesForChargeableItem(chargeableItemEntity)) {
                if (searchLine.getFreeText().contains("TicketDocumentData") && searchLine.getServiceLineState() != ServiceLine.STATUS_DELETED) {
                    ticketingLine = searchLine;
                    break;
                }
            }
        }

        if (ticketingLine != null) {
            SBRFreeTextParser parser = new SBRFreeTextParser(ticketingLine.getFreeText());
            String freeText = parser.get("FreeText");
            handleTicketing(row, parser, freeText, false);
        }

        for (ChargeableItemData chargeableItemData : chargeableItem.getChargeableItemDatas()) {
            if (chargeableItemData.getType() != null && chargeableItemData.getType().equalsIgnoreCase("CD")) {
                row.setMcoreason(ensureLength(chargeableItemData.getValue(), 100));
                row.setMiscellaneousChargeOrderFreetext(ensureLength(chargeableItemData.getValue(), 999));
            } else if (chargeableItemData.getType() != null && chargeableItemData.getType().equalsIgnoreCase("RC")) {
                row.setMcoreasonSubCode(ensureLength(chargeableItemData.getValue(), 100));
            }
        }
    }

    private void populateMCOFields(ChargeableItem chargeableItem, Booking booking, BookingName name,
                                   BookingNameItem itemForCoupon, ServiceLine serviceLineForCoupon,
                                   FileDataRaw tstRow, FileDataRaw row, List<FileDataRaw> itemRows) {
        row.setDocumentClass("MCO");
        setCommonFields(booking, name, null, itemForCoupon, row);
        setStandardChargeableFields(row, itemForCoupon, chargeableItem);

        /*
        //need to add coupon data depending on whether we have item or service line
        ArrayList<ChargeableItemData> chargeableItemData = new ArrayList<ChargeableItemData>();
        chargeableItemData.addAll(chargeableItem.getChargeableItemDatas());
        for (ChargeableItemCoupon coupon : chargeableItem.getChargeableItemCoupons()) {
            if (itemForCoupon != null && getBookingNameItemById(booking, coupon.getBookingNameItemId()) != null &&
                    itemForCoupon.getCrsSegmentLineNum() == getBookingNameItemById(booking, coupon.getBookingNameItemId()).getCrsSegmentLineNum()) {
                chargeableItemData.addAll(coupon.getChargeableItemDatas());
            }
            if (serviceLineForCoupon != null && getServiceLineForChargeableItemCoupon(coupon) != null &&
                    serviceLineForCoupon.getCrsId() == getServiceLineForChargeableItemCoupon(coupon).getCrsId()) {
                chargeableItemData.addAll(coupon.getChargeableItemDatas());
            }

        }
        */
        // #MH 23MAY14# clear logic and break if found TODO validate

        // get chargeable item data for chargeable item
        ArrayList<ChargeableItemData> chargeableItemData = new ArrayList<ChargeableItemData>();
        chargeableItemData.addAll(chargeableItem.getChargeableItemDatas());

        //also need to add coupon data depending on whether we have item or service line
        for (ChargeableItemCoupon couponEntity : chargeableItem.getChargeableItemCoupons()) {
            if (itemForCoupon != null) {
                BookingNameItem bookingNameItem = getBookingNameItemById(booking, couponEntity.getBookingNameItemId());
                if (bookingNameItem != null && itemForCoupon.getBookingNameItemId() == bookingNameItem.getBookingNameItemId()) {
                    chargeableItemData.addAll(couponEntity.getChargeableItemDatas());
                    break;
                }
            }
            if (serviceLineForCoupon != null) {
                ServiceLine serviceLine = getServiceLineForChargeableItemCoupon(couponEntity);
                if (serviceLine != null && serviceLineForCoupon.getServiceLineId() == serviceLine.getServiceLineId()) {
                    chargeableItemData.addAll(couponEntity.getChargeableItemDatas());
                    break;
                }
            }
        }


        for (ChargeableItemData data : chargeableItemData) {
            if (data.getName() != null) {
                if (data.getName().equalsIgnoreCase("GeneralIndicator")) {
                    if (data.getType() != null) {
                        if (data.getType().equalsIgnoreCase("EMD")) {
                            String emdtreatedAs = ensureLength(data.getValue(), 1);
                            row.setEmdtreatedAs(ensureLength(emdtreatedAs, 3));

                            if ("A".equals(emdtreatedAs)) {
                                // If we have a specific tstRow, use it
                                if (tstRow != null) {
                                    row.setIssInConnWith(ensureLength(tstRow.getDocumentNo(), 10));
                                    row.setIssInConnWithCpn(ensureLength(tstRow.getSegNoTech().toString(), 3));
                                } else {
                                    // Grab the ticket number from the first segment
                                    //todo: This code should go, should it not?
                                    if (itemRows != null && !itemRows.isEmpty()) {
                                        row.setIssInConnWith(ensureLength(itemRows.get(0).getDocumentNo(), 10));
                                        // If we have exactly one segment, we know coupon# is 1, else leave as null
                                        if (itemRows.size() == 1) {
                                            row.setIssInConnWithCpn(ensureLength("1", 1));
                                        } else {
                                            row.setIssInConnWithCpn(null);
                                        }
                                    }
                                }
                            } else if (emdtreatedAs.equals("S")) {
                                row.setIssInConnWith(null);
                                row.setIssInConnWithCpn(null);

                                // See if we have a Ticketing service line on the item
                                if (itemForCoupon != null) {
                                    for (ServiceLine serviceLine : itemForCoupon.getServiceLines()) {
                                        //if(serviceLine.getServiceLineState() == ServiceLine.STATUS_ACTIVE &&
                                        if (serviceLine.getServiceLineState() != ServiceLine.STATUS_DELETED &&
                                                serviceLine.getFreeText().contains("TicketDocumentData")) {
                                            // This is an active ticketing line:
                                            SBRFreeTextParser parser = new SBRFreeTextParser(serviceLine.getFreeText());
                                            String freeText = parser.get("FreeText");
                                            handleTicketing(row, parser, freeText, false);
                                            break;
                                        }

                                    }
                                }
                            }

                        }
                        // TODO validate
                        else if (data.getType().equalsIgnoreCase("FCP")) {
                            //#MH# 04.07.2014 FCMI for MCO is being read from FCP (Fare Calculation Pricing indicator)
                            row.setFcmi(data.getValue());
                        }
                    }
                } else if (data.getName().equalsIgnoreCase("Rfics")) {
                    row.setMcoreason(ensureLength(data.getValue(), 100));
                } else if (data.getName().equalsIgnoreCase("Rfisc")) {
                    row.setMcoreasonSubCode(ensureLength(data.getSubType(), 100));
                } else if (data.getName().equalsIgnoreCase("ICW")) {
                    row.setIssInConnWith(data.getValue());
                    row.setIssInConnWithCpn(data.getAdditionalData1());
                }
            }

        }

        // AVIX-11374 : We needed to add iterating over insurances as well
        // Pending deletion if 6.2 CVT goes well and we dont lose all insurance
        /*
        if (itemForCoupon != null && itemForCoupon.getType().equals("IU")) {
            for (ServiceLine sLine : itemForCoupon.getServiceLines()) {
                if (sLine.getServiceLineTypeCode().equals("FA")) {
                    if (row.getTixInformationFreetext() == null || !row.getTixInformationFreetext().contains(sLine.getFreeText())) {
                        row.setTixInformationFreetext(ensureLength(wrap(row.getTixInformationFreetext()) + sLine.getFreeText(), 990));
                    }
                }
            }
        }
        */


        Collection<ServiceLine> serviceLinesAssociatedWithChargeableItem = getServiceLinesForChargeableItem(chargeableItem);

        for (ServiceLine sLine : serviceLinesAssociatedWithChargeableItem) {
                /* TODO validate general use of handleServiceLinerow
                if (sLine.getServiceLineTypeCode().equalsIgnoreCase("FZ")) {
                    SBRFreeTextParser parser = new SBRFreeTextParser(sLine.getFreeText());
                    String freeText = parser.get("FreeText");
                    row.setMiscellaneousInformationFreetext(ensureLength(wrap(row.getMiscellaneousInformationFreetext()) + freeText, 990));
                }
                */
            handleServiceLine(row, sLine);
        }

        //special SBR batch 10.1 insurance handling (no link from chargeableItem to service line FP, so use service lines on name level
        if (serviceLinesAssociatedWithChargeableItem.size() == 0 && itemForCoupon != null && itemForCoupon.getType().equals("IU")) {
            for (ServiceLine sLine : chargeableItem.getBookingName().getServiceLines()) {
                if (sLine.getServiceLineTypeCode() != null && sLine.getServiceLineTypeCode().equalsIgnoreCase("FP")) {
                    SBRFreeTextParser parser = new SBRFreeTextParser(sLine.getFreeText());
                    String freeText = parser.get("FreeText");
                    handleFP(row, freeText);
                }
            }
        }

    }

    private void setCommonFields(Booking booking, BookingName name, BookingName infant, BookingNameItem item, FileDataRaw row) {
        setBookingFields(booking, row);
        if (name != null) {
            setNameFields(name, infant, row);
        }
        if (item != null) {
            setItemFields(booking, item, row);
        }
    }

    private void populatePAXStagingRecord(Booking booking, BookingName bn, BookingName infant, BookingNameItem bni, FileDataRaw row) {
        setCommonFields(booking, bn, infant, bni, row);
        handleServiceLines(booking, bn, bni, row);
        handleChargeableItems(bni, row);
    }

    private void handleServiceLines(Booking booking, BookingName bn, BookingNameItem bni, FileDataRaw row) {
        final Set<ServiceLine> alreadyAssociatedServices = new HashSet<ServiceLine>();

        Collection<ServiceLine> allServiceLines = new ArrayList<ServiceLine>();
        allServiceLines.addAll(bni.getServiceLines());
        allServiceLines.addAll(bn.getServiceLines());
        allServiceLines.addAll(booking.getServiceLines());

        for (ServiceLine serviceLine : allServiceLines) {
            if (serviceLine.getServiceLineState() != ServiceLine.STATUS_DELETED &&
                    !alreadyAssociatedServices.contains(serviceLine)) {

                alreadyAssociatedServices.add(serviceLine);
                handleServiceLine(row, serviceLine);
            }
        }
    }


    private void setItemFields(Booking booking, BookingNameItem item, FileDataRaw row) {
        //item
        if (item.getSegmentReach() == 1) {
            row.setIntDomTixInd("D");
        } else if (item.getSegmentReach() == 2 || item.getSegmentReach() == 3) {
            row.setIntDomTixInd("I");
        }
        row.setSegNoTech((int) item.getCrsSegmentLineNum());
        row.setTixOperCarr(ensureLength(item.getOperatingCarrier(), 3));
        row.setTixMarkCarr(ensureLength(item.getCommercialCarrier(), 3));
        row.setTixOperFlNo(ensureLength(Integer.toString(item.getOperatingFlightNumber()), 4));
        row.setTixMarkFlNo(ensureLength(Integer.toString(item.getCommercialFlightNumber()), 4));
        row.setTixDepApt(ensureLength(item.getOrigin(), 3));
        row.setTixDestApt(ensureLength(item.getDestination(), 3));
        row.setTixFlightDt(ensureLength(getDate(item.getDepartureDate()), 10));
        row.setTixFlightTime(ensureLength(getTime(item.getDepartureDate()), 8));
        GregorianCalendar departureDateInGMT = item.getDepartureDateTimeUTC();
        if (departureDateInGMT != null) {
            row.setTixFlightDtUtc(departureDateInGMT.getTime());
        }
        row.setTixDestDt(ensureLength(getDate(item.getArrivalDateTime()), 10));
        row.setTixDestTime(ensureLength(getTime(item.getArrivalDateTime()), 8));
        row.setTixDepTerminal(ensureLength(item.getDepartureTerminal(), 2));
        row.setTixDestTerminal(ensureLength(item.getArrivalTerminal(), 2));
        row.setTixCheckinTime(ensureLength(getTime(item.getCheckInDate()), 8));
        row.setOperatingClass(ensureLength(item.getOperatingBookingClass(), 1));
        row.setTicketedRbd(ensureLength(item.getCommercialBookingClass(), 1));
        row.setCompartment(ensureLength(item.getCabinCode(), 1));
        row.setAircraftType(ensureLength(item.getAircraftType(), 8));
        String sellAccessLevel = item.getDynamicAttribute("SellAccessLevel");
        row.setTixType(ensureLength(sellAccessLevel, 255));
        String overbookingType = item.getDynamicAttribute("OverbookingType");
        row.setOverbookingStatus(ensureLength(overbookingType, 3));
        String overbookingReason = item.getDynamicAttribute("OverbookingReason");
        row.setOverbookingStatusReason(ensureLength(overbookingReason, 3));
        row.setBookingStatus(ensureLength(item.getCrsSegStatus(), 3));
        row.setBookingStatusBoarded(ensureLength(item.getItemStatus() == BookingNameItem.FLOWN_ITEM ? "B" : null, 3));
        row.setBookingStatusAssessed(ensureLength(Integer.toString(item.getItemStatus()), 2));
        if (item.getItemStatus() == BookingNameItem.FLOWN_ITEM) {
            row.setFlownCabin(ensureLength(item.getCabinCodeFlown(), 1));
            row.setFlownCarr(ensureLength(item.getOperatingCarrier(), 3));
            row.setFlownDepApt(ensureLength(item.getOrigin(), 3));
            row.setFlownDestApt(ensureLength(item.getDestination(), 3));
            row.setFlownFlightDt(getDate(item.getDepartureDate()));
            row.setFlownFlNo(ensureLength(Integer.toString(item.getOperatingFlightNumber()), 4));
        }
        row.setReservationDate(ensureLength(getDate(item.getDateItemAdded()), 10));
        row.setReservationTime(ensureLength(getTime(item.getDateItemAdded()), 8));
        row.setSegType(ensureLength(item.getType(), 3));
        row.setBookingProductCategory(ensureLength(getProductCategory(item), 15));
        row.setCodeshareAgreement(ensureLength(unmapCSA(item.getCodeShareStatus()), 3));
        row.setCodeshareInd(ensureLength(item.getCodeShareIndicator(), 1));

        if (item.getPosCountry() != null && item.getPosCountry().length() > 0) {
            row.setIssueAgntCntryCode(ensureLength(item.getPosCountry(), 2));
        } else {
            row.setIssueAgntCntryCode(ensureLength(booking.getPosCountry(), 2));
        }
        if (item.getPosCity() != null && item.getPosCity().length() > 0) {
            row.setBookingPoscity(ensureLength(item.getPosCity(), 10));
        } else {
            row.setBookingPoscity(ensureLength(booking.getPosCity(), 10));
        }
        if (item.getPosType() != null && item.getPosType().length() > 0) {
            row.setBookingPosagentType(ensureLength(item.getPosType(), 1));
        } else {
            row.setBookingPosagentType(ensureLength(booking.getPosType(), 1));
        }
        if (booking.hasDynamicAttribute("SplitParentRLOC")) {
            String s = booking.getDynamicAttribute("SplitParentRLOC");
            row.setParentPnr(s);
        }
    }

    private void setNameFields(BookingName name, BookingName infant, FileDataRaw row) {
        //name
        row.setPaxfirstName(ensureLength(name.getFirstName(), 49));
        row.setPaxlastName(ensureLength(name.getLastName(), 49));
        if (name.getGender() == 1) {
            row.setGender("M");
        } else if (name.getGender() == 2) {
            row.setGender("F");
        }
        row.setPaxtitle(ensureLength(name.getTitle(), 10));
        row.setPaxType(Integer.toString(name.getPassengerType()));
        row.setPaxname(ensureLength(name.getLastName() + "/" + name.getFirstName(), 49));
        if (infant != null) {
            row.setInfFirstName(ensureLength(infant.getFirstName(), 49));
            row.setInfLastName(ensureLength(infant.getLastName(), 49));
            row.setInfIdentCdFreetext(ensureLength(infant.getIdentificationCode(), 70));
        }
        row.setPaxIdentCdFreetext(ensureLength(name.getIdentificationCode(), 70));
        row.setNoPaxInGroup(name.getSeatCount());
        row.setPaxInfInd(ensureLength(name.getPassengerType() == BookingName.PASS_TYPE_INFANT ? "1" : null, 3));
        row.setStaffInd(ensureLength(name.getStandbyCode(), 3));
        row.setFrquntFlyerCd(ensureLength(name.getFqtNumber(), 16));
        row.setFrquntFlyerProgr(ensureLength(name.getFqtProgram(), 20));
        row.setPaxNoTec(name.getCrsNameLineNum());
    }

    private void setBookingFields(Booking booking, FileDataRaw row) {
        row.setPnr(ensureLength(booking.getRloc(), 13));
        row.setOrigBookingDate(ensureLength(getDate(booking.getBookingDate()), 10));
        row.setOrigBookingAgnt(ensureLength(booking.getCreatingIataCode(), 8));
        row.setOrigBookingTime(ensureLength(getTime(booking.getBookingDate()), 8));
        row.setOrigBookingPurgeDate(ensureLength(getDate(booking.getPurgeDate()), 10));
        row.setBookingCancelledInd(booking.getBookingStatus() == Booking.CANCELLED_BOOKING);
        row.setBookingDate(ensureLength(getDate(booking.getVersionTimestamp()), 12));
        row.setBookingTime(ensureLength(getTime(booking.getVersionTimestamp()), 10));
        row.setBookingType(ensureLength(booking.getBookingAttributes(), 3));
        row.setGds(ensureLength(booking.getGdsCode(), 10));
        row.setHostGds(ensureLength(booking.getGdsRloc(), 13));//they wanted it like this
        row.setGroupBooking(ensureLength(booking.getBookingType() == Booking.GROUP_BOOKING_TYPE ? "G" : null, 1));
        row.setOrigAgnt(ensureLength(booking.getCreatingIataCode(), 8));
        row.setBookingPosagent(ensureLength(booking.getResponsibleOfficeIdentifier(), 10));
        row.setQueueingOfficeIdFreetext(ensureLength(booking.getQueueingOfficeIdentifier(), 24));
        row.setBookingPosamaagent(ensureLength(booking.getCreatingOfficeIdentifier(), 24));
        row.setTixQueueingOffice(ensureLength(booking.getQueueingOfficeIdentifier(), 10));
        row.setPaxcount(ensureLength(Integer.toString(booking.getReservationCount()), 3));
        row.setCustCompanyCode(ensureLength(booking.getGdsCode(), 3));
        row.setAgntSign(ensureLength(booking.getCreatingAgentSignature(), 8));

    }

    String getProductCategory(BookingNameItem item) {
        String productCategory = item.getProductCategory();
        if (productCategory == null) {
            return null;
        }
        if (productCategory.startsWith("/")) {
            return productCategory.substring(1);//backwards comptatibility
        }
        return productCategory;
    }

    String ensureLength(String s, int length) {
        if (s == null || s.length() <= length) {
            return s;
        }
        return s.substring(0, length);
    }

    boolean isTicketed(ChargeableItem cie) {
        for (ChargeableItemData data : cie.getChargeableItemDatas()) {
            String name = data.getName();
            if (name != null) {
                if (name.equalsIgnoreCase("IssueIdentifier")) {
                    // IssueIdentifier is populated by SBR >= 12.1
                    // #MH 26MAY14# check for sub type disabled TODO validate
                    //if ("I".equalsIgnoreCase(data.getSubType())) {
                    return true;
                    //}
                } else if (name.equalsIgnoreCase("TicketingIndicator")) {
                    // TicketingIndicator is populated by SBR < 12.1
                    if ("T".equalsIgnoreCase(data.getValue())) {
                        return true;
                    }
                }

            }
        }
        return false;
    }

    boolean isForInfant(ChargeableItem cie) {
        for (ChargeableItemData data : cie.getChargeableItemDatas()) {
            String type = data.getType();
            String name = data.getName();
            if (type != null && name != null) {
                if (type.equalsIgnoreCase("INF") && name.equalsIgnoreCase("DiscountCode")) {
                    return true;
                }
            }
        }
        return false;
    }


    void handleChargeableItems(BookingNameItem item, FileDataRaw row) {

        //responsible adults may have 2 chargeable items, one for the adult and the other for the infant
        // with release 5.3.2 of legacy server they are now correctly associated

        ChargeableItem chargeableItem = null;

        /* old code
        Booking booking = item.getBookingName().getBooking();
        for (ChargeableItem chargeableItemEntity : item.getBookingName().getChargeableItems()) {
            for (ChargeableItemCoupon couponEntity : chargeableItemEntity.getChargeableItemCoupons()) {
                BookingNameItem bookingNameItem = getBookingNameItemById(booking, couponEntity.getBookingNameItemId());
                if (bookingNameItem != null && bookingNameItem.getCrsSegmentLineNum() == item.getCrsSegmentLineNum()) {
                    chargeableItem = chargeableItemEntity;
                    break;
                }
            }
        }
        */


        //#MH 07/01/14 - if no chargeableItem found, we item my belong to an INF - logic to retrieve ChargeableItem for Infants

        Booking booking = item.getBookingName().getBooking();
        for (ChargeableItem chargeableItemEntity : item.getBookingName().getChargeableItems()) {
            if (!isForInfant(chargeableItemEntity)) {          //assigning INF TST's like below causes duplicates
                for (ChargeableItemCoupon couponEntity : chargeableItemEntity.getChargeableItemCoupons()) {
                    BookingNameItem bookingNameItem = getBookingNameItemById(booking, couponEntity.getBookingNameItemId());
                    if (bookingNameItem != null && bookingNameItem.getCrsSegmentLineNum() == item.getCrsSegmentLineNum()) {
                        chargeableItem = chargeableItemEntity;
                        break;
                    }
                }
            }
        }

        if (chargeableItem == null) {

            BookingName name = item.getBookingName();

            if (name.getPassengerType() == BookingName.PASS_TYPE_INFANT) {
                //need to find the chargeable item for the INF by looking at the responsible adult's cid  for type INF
                BookingName responsibleAdult = null;
                Long responsibleAdultId = name.getDynamicAttribute("ResponsibleAdult");
                if (responsibleAdultId != null) {
                    for (BookingName bookingName : booking.getBookingNames()) {
                        if (bookingName.getBookingNameId() == responsibleAdultId) {
                            responsibleAdult = bookingName;
                            break;
                        }
                    }
                }
                if (responsibleAdult != null) {
                    for (ChargeableItem chargeableItemEntity : responsibleAdult.getChargeableItems()) {
                        for (ChargeableItemData chargeableItemDataEntity : chargeableItemEntity.getChargeableItemDatas()) {
                            if (chargeableItemDataEntity.getType().equalsIgnoreCase("INF") && chargeableItemDataEntity.getName().equalsIgnoreCase("DiscountCode")) {
                                chargeableItem = chargeableItemDataEntity.getChargeableItem();
                                break;
                            }
                        }
                    }
                }

            }
        }
        //MH 07/01/14 - end


        if (chargeableItem != null) {

            if (chargeableItem.getType() != null) {

                if (chargeableItem.getType().equalsIgnoreCase("TST")) {
                    if (isTicketed(chargeableItem)) {
                        setStandardChargeableFields(row, item, chargeableItem);
                    }
                    // Regardless if we're ticketed or not, we want to set FCMI
                    for (ChargeableItemData data : chargeableItem.getChargeableItemDatas()) {
                        if (data.getType() != null && data.getType().equals("FCM")) {
                            row.setFcmi(data.getValue());
                        }
                    }
                } else if (chargeableItem.getType().equalsIgnoreCase("XSB")) {
                    setXSBStandardFields(row, chargeableItem);
                } else if (chargeableItem.getType().equalsIgnoreCase("TSM")) {
                    for (ChargeableItemData data : chargeableItem.getChargeableItemDatas()) {
                        if (data.getName() != null && data.getName().equalsIgnoreCase("GeneralIndicator")) {
                            if (data.getType() != null && data.getType().equalsIgnoreCase("EMD")) {
                                row.setEmdtype(ensureLength(data.getValue(), 1));
                            }
                        }
                    }
                }
            }
        }
    }

    private void setXSBStandardFields(FileDataRaw row, ChargeableItem cie) {
        for (ChargeableItemData data : cie.getChargeableItemDatas()) {
            if (data.getName() != null && data.getName().equalsIgnoreCase("Fare")) {
                row.setEbtchargeCrncy(ensureLength(data.getCurrency(), 4));
                row.setEbtchargeQualifier(ensureLength(data.getSubType(), 4));
            } else if (data.getName() != null && data.getName().equalsIgnoreCase("Weight")) {
                row.setEbtunitQualifier(ensureLength(data.getSubType(), 4));
            } else if (data.getName() != null && data.getName().equalsIgnoreCase("BagAllowance")) {
                row.setEbtunits(ensureLength(data.getCategory(), 12));
            }
        }
    }

    private void setStandardChargeableFields(FileDataRaw row, BookingNameItem item, ChargeableItem chargeableItem) {
        int taxIndex = 1;
        //removed 20140717 by request from DWH to avoid having null in tixDestApt and not null in tixDepApt
        /*
        if (row.getTixDepApt() == null && chargeableItem.getIssuanceDestination() != null) {
            row.setTixDepApt(chargeableItem.getIssuanceDestination());
        }*/

        if (row.getTixFlightDt() == null && chargeableItem.getDateAdded() != null) {
            row.setTixFlightDt(getDate(chargeableItem.getDateAdded()));
        }
        row.setTixConfidentialOffice(ensureLength(chargeableItem.getOfficeName(), 9));
        row.setTixEnteringType(ensureLength(chargeableItem.getSubType(), 1));
        ArrayList<ChargeableItemData> chargeableItemData = new ArrayList<ChargeableItemData>();
        chargeableItemData.addAll(chargeableItem.getChargeableItemDatas());
        if (item != null) {
            //also need to add coupon lines
            Booking booking = item.getBookingName().getBooking();
            for (ChargeableItemCoupon couponEntity : chargeableItem.getChargeableItemCoupons()) {
                BookingNameItem bookingNameItem = getBookingNameItemById(booking, couponEntity.getBookingNameItemId());
                if (bookingNameItem != null && bookingNameItem.getCrsSegmentLineNum() == item.getCrsSegmentLineNum()) {
                    chargeableItemData.addAll(couponEntity.getChargeableItemDatas());
                    break;
                }
            }
        }

        for (ChargeableItemData data : chargeableItemData) {
            if (data.getName() != null && data.getName().equalsIgnoreCase("StatusInformation")) {
                row.setTixType2(ensureLength(data.getSubType(), 3));   //
            } else if (data.getName() != null && data.getName().equalsIgnoreCase("BagAllowance")) {
                row.setBaggageQuantity(ensureLength(data.getValue(), 10));
                row.setBaggageWeight(ensureLength(data.getCategory(), 10));
                row.setBaggageType(ensureLength(data.getType(), 3));
                row.setBaggageWeightUnit(ensureLength(data.getSubType(), 1));//
            } else if (data.getName() != null && data.getName().equalsIgnoreCase("FareBasis")) {
                String farebaseCode = ensureLength(data.getCategory() + data.getValue(), 15);
                row.setFarebaseCode(farebaseCode);
            } else if (data.getSubType() != null && data.getSubType().equalsIgnoreCase("Oth_Fare")) {
                if (data.getName().equalsIgnoreCase("FCA")) {
                    String value = data.getValue();
                    if (value != null && value.startsWith("I-")) {
                        row.setInvoluntaryInd("Y");
                        row.setVoluntaryInd(false);
                    }
                    row.setFareCalc(ensureLength(value, 500));
                } else if (data.getName().equalsIgnoreCase("CRE")) {
                    row.setTixBestBuyFare(ensureLength(data.getValue(), 250));
                } else if (data.getName().equalsIgnoreCase("PAY")) {
                    row.setTixPaymentRestriction(ensureLength(data.getValue(), 250));
                }
            } else if (data.getName() != null && data.getName().equalsIgnoreCase("Fare")) {
                String currency = data.getCurrency();
                if (data.getSubType().equalsIgnoreCase("GT")) {
                    if (currency != null && currency.equalsIgnoreCase("EUR")) {
                        row.setSectorGrandTotalFare(ensureLength(data.getValue(), 12));
                    } else {
                        row.setSectorGrandTotalFareNotEur(ensureLength(data.getValue(), 12));
                        row.setTixCurrencyGrand(ensureLength(currency, 3));
                    }
                } else if (data.getSubType().equalsIgnoreCase("T")) {
                    if (currency != null && currency.equalsIgnoreCase("EUR")) {
                        row.setSectorTotalFare(ensureLength(data.getValue(), 12));
                    } else {
                        row.setSectorTotalFareNotEur(ensureLength(data.getValue(), 12));
                        row.setTixCurrency(ensureLength(currency, 3));
                    }
                } else if (data.getSubType().equalsIgnoreCase("B")) {
                    if (currency != null && currency.equalsIgnoreCase("EUR")) {
                        row.setSectorFare(ensureLength(data.getValue(), 12));
                    } else {
                        row.setCurrency(ensureLength(currency, 3));
                        row.setSectorFareNotEur(ensureLength(data.getValue(), 12));
                    }
                } else if (data.getSubType().equalsIgnoreCase("E")) {
                    if (currency != null && currency.equalsIgnoreCase("EUR")) {
                        row.setSectorFare(ensureLength(data.getValue(), 12));
                    }
                } else if (data.getSubType().equalsIgnoreCase("N")) {
                    row.setRefundableAmount(ensureLength(data.getValue(), 12));
                } else if (data.getSubType().equalsIgnoreCase("H")) {
                    row.setSectorFareNet(ensureLength(data.getValue(), 12));
                }
            } else if (data.getName() != null && data.getName().equalsIgnoreCase("ExchangeRate")) {
                row.setExchangeRate(ensureLength(data.getValue(), 12));
                row.setExchangeRate2(ensureLength(data.getAdditionalData1(), 12));
            } else if (data.getName() != null && data.getName().equalsIgnoreCase("TAX")) {

                try {
                    Method setTaxCode = row.getClass().getMethod("setTaxCode" + taxIndex, String.class);
                    StringBuilder taxCode = new StringBuilder();
                    //tax identifier
                    if (data.getSubType() != null && data.getSubType().length() == 1) {
                        taxCode.append(data.getSubType());
                    } else {
                        taxCode.append("-");
                    }
                    //iso country code
                    if (data.getType() != null && data.getType().length() == 2) {
                        taxCode.append(data.getType());
                    } else {
                        taxCode.append("--");
                    }
                    //tax nature
                    if (data.getCategory() != null && data.getCategory().length() == 2) {
                        taxCode.append(data.getCategory());
                    } else {
                        taxCode.append("--");
                    }
                    //tax exemption
                    if (data.getAdditionalData1() != null && data.getAdditionalData1().length() == 1) {
                        taxCode.append(data.getAdditionalData1());
                    } else {
                        taxCode.append("-");
                    }
                    //tax currency
                    if (data.getCurrency() != null && data.getCurrency().length() == 3) {
                        taxCode.append(data.getCurrency());
                    } else {
                        taxCode.append("---");
                    }
                    setTaxCode.invoke(row, taxCode.toString());
                    Method setTaxValue = row.getClass().getMethod("setTaxValue" + taxIndex, String.class);
                    setTaxValue.invoke(row, ensureLength(data.getValue(), 12));
                    taxIndex++;
                } catch (Exception e) {
                    log.warn("Couldn't store tax", e);
                }
            } else if (data.getName() != null && data.getName().equalsIgnoreCase("TicketingIndicator")) {
                if ("T".equalsIgnoreCase(data.getValue())) {
                    row.setBookingTicketedInd(ensureLength("PT", 2));
                }
            } else if (data.getName() != null && data.getName().equalsIgnoreCase("IssueIdentifier")) {
                if ("I".equalsIgnoreCase(data.getValue())) {
                    row.setBookingTicketedInd(ensureLength("PT", 2));
                }
            }

        }
    }

    String wrap(String s) {
        if (s == null) {
            return "";
        }
        if (s.length() > 0) {
            return s + "##";
        }
        return s;
    }

    void handleServiceLine(FileDataRaw row, ServiceLine sLine) {
        SBRFreeTextParser parser = new SBRFreeTextParser(sLine.getFreeText());
        String freeText = parser.get("FreeText");
        if (sLine.getServiceLineTypeCode() == null) {
            return;
        }

        if (sLine.getServiceLineTypeCode().equalsIgnoreCase("SEA")) {
            handleSEA(row, sLine, parser);
        } else if (sLine.getFreeText().contains("TicketDocumentData")) {
            handleTicketing(row, parser, freeText);
            if (sLine.getServiceLineTypeCode().equalsIgnoreCase("FHE")) {
                row.setElectronicFhFreetext(ensureLength(wrap(row.getElectronicFhFreetext()) + freeText, 999));
            }
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("FO")) {
            if ((sLine.getChargeableItemId() == 0 && row.getDocumentClass().equals("PAX")) ||
                    (sLine.getChargeableItemId() != 0 && row.getDocumentClass().equals("MCO"))) {
                handleFO(row, freeText);
            }
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("FV")) {
            handleFV(row, parser, freeText);
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("FE")) {
            row.setEndorsementFreetext(ensureLength(wrap(row.getEndorsementFreetext()) + freeText, 999));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("FHE")) {
            row.setElectronicFhFreetext(ensureLength(wrap(row.getElectronicFhFreetext()) + freeText, 999));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("FHM")) {
            row.setManuelFhFreetext(ensureLength(wrap(row.getManuelFhFreetext()) + freeText, 999));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("FD")) {
            row.setTixDiscountInfoFreetext(ensureLength(wrap(row.getTixDiscountInfoFreetext()) + freeText, 999));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("FZ")) {
            if ((sLine.getChargeableItemId() == 0 && row.getDocumentClass().equals("PAX")) ||
                    sLine.getChargeableItemId() != 0 && row.getDocumentClass().equals("MCO")) {
                row.setMiscellaneousInformationFreetext(ensureLength(wrap(row.getMiscellaneousInformationFreetext()) + freeText, 999));
            }
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("FS")) {
            row.setMiscellaneousTicketingInformationFreetext(ensureLength(wrap(row.getMiscellaneousTicketingInformationFreetext()) + freeText, 999));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("FK")) {
            row.setFkFreetext(ensureLength(wrap(row.getFkFreetext()) + freeText, 999));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("FM")) {
            handleFM(row, freeText);
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("FI")) {
            row.setTixInvoiceNoInfoFreetext(ensureLength(wrap(row.getTixInvoiceNoInfoFreetext()) + freeText, 999));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("FP")) {
            if ((sLine.getChargeableItemId() == 0 && row.getDocumentClass().equals("PAX")) ||
                    (sLine.getChargeableItemId() != 0 && row.getDocumentClass().equals("MCO"))) {
                handleFP(row, freeText);
            }
            //handleFP(row, freeText);
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("FT")) {
            handleFT(row, freeText);
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("OS")) {
            handleOS(row, parser.get("CompanyId"), freeText);
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("OP")) {
            row.setOptionFreetext(ensureLength(wrap(row.getOptionFreetext()) + freeText, 999));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("TK")) {
            row.setTicketFreetext(ensureLength(wrap(row.getTicketFreetext()) + sLine.getFreeText(), 999));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("AP")) {
            handleAP(row, freeText);
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("APE")) {
            row.setCustEmail(ensureLength(wrap(row.getCustEmail()) + freeText, 100));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("APF")) {
            row.setCustFax(ensureLength(wrap(row.getCustFax()) + freeText, 100));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("APB")) {
            row.setCustPhoneBusiness(ensureLength(wrap(row.getCustPhoneBusiness()) + freeText, 100));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("APH")) {
            row.setCustPhonePrivate(ensureLength(wrap(row.getCustPhonePrivate()) + freeText, 100));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("AM") || sLine.getServiceLineTypeCode().equalsIgnoreCase("AM/")) {
            handleAM(row, sLine.getFreeText(), freeText);
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("AB") || sLine.getServiceLineTypeCode().equalsIgnoreCase("AB/")) {
            handleAB(row, sLine.getFreeText(), freeText);
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("RM")) {
            row.setGeneralRemarkFreetext(ensureLength(wrap(row.getGeneralRemarkFreetext()) + freeText, 999));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("RC")) {
            row.setConfidentialRemarkFreetext(ensureLength(wrap(row.getConfidentialRemarkFreetext()) + freeText, 999));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("RX")) {
            row.setCorporateRemarkFreetext(ensureLength(wrap(row.getCorporateRemarkFreetext()) + freeText, 999));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("RI")) {
            row.setInvoiceRemarkFreetext(ensureLength(wrap(row.getInvoiceRemarkFreetext()) + freeText, 999));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("RP")) {
            row.setRoutingPartyFreetext(ensureLength(wrap(row.getRoutingPartyFreetext()) + freeText, 999));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("SK")) {
            row.setSpecialKeywordsFreetext(ensureLength(wrap(row.getSpecialKeywordsFreetext()) + sLine.getFreeText(), 999));
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("SSR")) {
            row.setSpecialServiceRequestFreetext(ensureLength(wrap(row.getSpecialServiceRequestFreetext()) + sLine.getFreeText(), 999));
            if (sLine.getSecondaryType() != null && sLine.getSecondaryType().equalsIgnoreCase("DOCS")) {
                String comps[] = freeText.split("/");
                row.setTixPassportNoFreetext(ensureLength(freeText, 999));
                if (comps.length > 2) {
                    row.setTixPassportNo(ensureLength(comps[2], 20));
                }
            }
        } else if (sLine.getServiceLineTypeCode().equalsIgnoreCase("AI")) {
            row.setAccountingInformationFreetext(ensureLength(wrap(row.getAccountingInformationFreetext()) + sLine.getFreeText(), 999));
        }
    }

    private void handleFT(FileDataRaw row, String freeText) {
        // PAX *F*IT7AB2COCOCA
        row.setTourcodeFreetext(ensureLength(wrap(row.getTourcodeFreetext()) + freeText, 999));
        Pattern p = Pattern.compile("^.*?([A-Za-z0-9]*)$");
        Matcher m = p.matcher(freeText);

        if (m.matches() && m.groupCount() == 1) {
            //  IT7AB2COCOCA
            row.setTourCode(m.group(1));
        }
    }

    void handleFM(FileDataRaw row, String freeText) {
        String comps[] = freeText.split("\\*");
        if (comps.length > 2) {
            try {
                double commission = Double.parseDouble(comps[2]);
                row.setCommissionRate(Double.toString(commission));
            } catch (NumberFormatException e) {
                //no worries just best effort as some lines do not conform
            }
        }
        row.setCommissionFreetext(ensureLength(wrap(row.getCommissionFreetext()) + freeText, 999));
    }

    Map<String, String> parseStructuredAddress(String freeText) {
        //e.g. SAD: Address: [Option:CY] [OptionText:KNAUF GIPS KG] Address: [Option:A1] [OptionText:AM BAHNHOF 7] Address: [Option:CI] [OptionText:97346 IPHOFEN]
        HashMap<String, String> result = new HashMap<String, String>();
        int currentIndex = 0;
        while (true) {
            currentIndex = freeText.indexOf("[Option:", currentIndex);
            if (currentIndex == -1) {
                break;
            }
            String option = freeText.substring(currentIndex + 8, currentIndex + 10);
            currentIndex = freeText.indexOf("[OptionText:", currentIndex);
            String optionText = freeText.substring(currentIndex + 12, freeText.indexOf("]", currentIndex));
            result.put(option, optionText);
        }
        return result;
    }

    void handleAB(FileDataRaw row, String freeText, String freeTextComponent) {
        row.setBillingAddressFreetext(freeTextComponent);
        Map<String, String> structuredAddress = parseStructuredAddress(freeText);
        for (Map.Entry<String, String> entry : structuredAddress.entrySet()) {
            String option = entry.getKey();
            String optionText = entry.getValue();
            if (option.equalsIgnoreCase("CY")) {
                row.setCustBillCompanyName(ensureLength(wrap(row.getCustBillCompanyName()) + optionText, 50));
            } else if (option.equalsIgnoreCase("NA")) {
                row.setCustBillName(ensureLength(wrap(row.getCustBillName()) + optionText, 50));
            } else if (option.equalsIgnoreCase("L1") || option.equalsIgnoreCase("A1")) {
                row.setCustBillAddressLine1(ensureLength(wrap(row.getCustBillAddressLine1()) + optionText, 50));
            } else if (option.equalsIgnoreCase("L2") || option.equalsIgnoreCase("A2")) {
                row.setCustBillAddressLine2(ensureLength(wrap(row.getCustBillAddressLine2()) + optionText, 50));
            } else if (option.equalsIgnoreCase("CI")) {
                row.setCustBillCity(ensureLength(wrap(row.getCustBillCity()) + optionText, 50));
            } else if (option.equalsIgnoreCase("PO")) {
                row.setCustBillPo(ensureLength(wrap(row.getCustBillPo()) + optionText, 50));
            } else if (option.equalsIgnoreCase("ZP")) {
                row.setCustBillZipcode(ensureLength(wrap(row.getCustBillZipcode()) + optionText, 50));
            } else if (option.equalsIgnoreCase("ST")) {
                row.setCustBillState(ensureLength(wrap(row.getCustBillState()) + optionText, 50));
            } else if (option.equalsIgnoreCase("CO")) {
                row.setCustBillCountry(ensureLength(wrap(row.getCustBillCountry()) + optionText, 50));
            }
        }
    }

    void handleAM(FileDataRaw row, String freeText, String freeTextComponent) {
        row.setMailingAddressFreetext(freeTextComponent);
        Map<String, String> structuredAddress = parseStructuredAddress(freeText);
        for (Map.Entry<String, String> entry : structuredAddress.entrySet()) {
            String option = entry.getKey();
            String optionText = entry.getValue();
            if (option.equalsIgnoreCase("CY")) {
                row.setCustMailCompanyName(ensureLength(wrap(row.getCustMailCompanyName()) + optionText, 50));
            } else if (option.equalsIgnoreCase("NA")) {
                row.setCustMailName(ensureLength(wrap(row.getCustMailName()) + optionText, 50));
            } else if (option.equalsIgnoreCase("L1") || option.equalsIgnoreCase("A1")) {
                row.setCustMailAddressLine1(ensureLength(wrap(row.getCustMailAddressLine1()) + optionText, 50));
            } else if (option.equalsIgnoreCase("L2") || option.equalsIgnoreCase("A2")) {
                row.setCustMailAddressLine2(ensureLength(wrap(row.getCustMailAddressLine2()) + optionText, 50));
            } else if (option.equalsIgnoreCase("CI")) {
                row.setCustMailCity(ensureLength(wrap(row.getCustMailCity()) + optionText, 50));
            } else if (option.equalsIgnoreCase("PO")) {
                row.setCustMailPo(ensureLength(wrap(row.getCustMailPo()) + optionText, 50));
            } else if (option.equalsIgnoreCase("ZP")) {
                row.setCustMailZipcode(ensureLength(wrap(row.getCustMailZipcode()) + optionText, 50));
            } else if (option.equalsIgnoreCase("ST")) {
                row.setCustMailState(ensureLength(wrap(row.getCustMailState()) + optionText, 50));
            } else if (option.equalsIgnoreCase("CO")) {
                row.setCustMailCountry(ensureLength(wrap(row.getCustMailCountry()) + optionText, 50));
            }
        }
    }

    void handleAP(FileDataRaw row, String freeText) {
        if (freeText != null && freeText.startsWith("PAS/")) {
            row.setTixPassportNoFreetext(ensureLength(wrap(row.getTixPassportNoFreetext()) + freeText, 999));
        }
    }

    void handleOS(FileDataRaw row, String companyId, String freeText) {

        if (companyId != null) {
            // We overwrite here as there should never be multiple company ids.
            row.setOsicompanyId(ensureLength(companyId, 2));
        }

        if (freeText != null) {
            if (freeText.startsWith("ABCP")) {
                row.setOsibusinessPoints(ensureLength(wrap(row.getOsibusinessPoints()) + freeText.substring(4).trim(), 20));
                row.setOtherServicesInformationFreetext(ensureLength(wrap(row.getOtherServicesInformationFreetext()) + freeText, 999));
            } else if (freeText.startsWith("CORPORATE")) {
                row.setOsicorporateName(ensureLength(wrap(row.getOsicorporateName()) + freeText.substring(9).trim(), 255));
                row.setOtherServicesInformationFreetext(ensureLength(wrap(row.getOtherServicesInformationFreetext()) + freeText, 999));
            } else if (freeText.startsWith("CORP")) {
                row.setOsicorporateName(ensureLength(wrap(row.getOsicorporateName()) + freeText.substring(4).trim(), 255));
                row.setOtherServicesInformationFreetext(ensureLength(wrap(row.getOtherServicesInformationFreetext()) + freeText, 999));
            } else if (freeText.startsWith("CMP")) {
                row.setOsicorporateName(ensureLength(wrap(row.getOsicorporateName()) + freeText.substring(3).trim(), 255));
                row.setOtherServicesInformationFreetext(ensureLength(wrap(row.getOtherServicesInformationFreetext()) + freeText, 999));
            } else if (freeText.startsWith("OIN")) {
                row.setOsicorporateName(ensureLength(wrap(row.getOsicorporateName()) + freeText.substring(3).trim(), 255));
                row.setOtherServicesInformationFreetext(ensureLength(wrap(row.getOtherServicesInformationFreetext()) + freeText, 999));
            } else {
                // We want the OSI freetext but not assign the corporate name
                // row.setOsicorporateName(ensureLength(wrap(row.getOsicorporateName()) + freeText.trim(), 255));
                row.setOtherServicesInformationFreetext(ensureLength(wrap(row.getOtherServicesInformationFreetext()) + freeText.trim(), 999));
            }
        }
    }

    void handleFP(FileDataRaw row, String freeText) {
        if (freeText.startsWith("PAX ")) freeText = freeText.substring(4);
        row.setFopinformationFreetext(ensureLength(freeText, 999));
        if (freeText != null) {
            if (freeText.startsWith("CC")) {
                if (freeText.charAt(4) == 'X') {
                    row.setCreditCardNoHash(ensureLength(wrap(row.getCreditCardNoHash()) + freeText.substring(4, 16), 40));
                } else {
                    row.setCreditCardNo(ensureLength(wrap(row.getCreditCardNo()) + freeText.substring(4, 16), 20));
                }
            }
        }
    }

    void handleFV(FileDataRaw row, SBRFreeTextParser parser, String freeText) {
        String type = parser.get("Type");
        row.setTicketingCarrierDesignatorFreetext(ensureLength(wrap(row.getTicketingCarrierDesignatorFreetext()) + freeText, 999));
        if (type != null && type.equalsIgnoreCase("P18") && freeText != null && freeText.length() > 5) {
            String carrier = freeText.substring(4, 6);
            row.setIssueAirline(ensureLength(carrier, 3));
        }
    }

    void handleFO(FileDataRaw row, String freeText) {
        if (row.getOrigIssueInformationFreetext() == null || !row.getOrigIssueInformationFreetext().contains(freeText)) {
            row.setOrigIssueInformationFreetext(ensureLength(wrap(row.getOrigIssueInformationFreetext()) + freeText, 999));
        }
        if (freeText != null) {
            String comps[] = freeText.split("/");
            if (comps.length > 0 && comps[0].length() > 6) {
                String date = comps[0].substring(comps[0].length() - 7);
                try {
                    GregorianCalendar cal = AirlineDateFormat.stringToCalendar(date);
                    row.setOrigIssueDate(ensureLength(getDate(cal), 10));
                } catch (Exception e) {
                    log.error("Failed to parse date: " + date);
                }
            }
            if (comps.length > 2 && comps[2].length() > 15) {
                if (comps[2].endsWith("*V")) {
                    row.setVoluntaryInd(true);
                } else {
                    row.setVoluntaryInd(null);
                }
            }

        }
    }

    void handleTicketing(FileDataRaw row, SBRFreeTextParser parser, String freeText) {
        handleTicketing(row, parser, freeText, true);
    }

    void handleTicketing(FileDataRaw row, SBRFreeTextParser parser, String freeText, boolean filter) {
        if (row.getTixInformationFreetext() != null && row.getTixInformationFreetext().length() > 0) {
            return;//only provide one ticket number
        }
        String type = parser.get("Type");
        String dataIndicator = parser.get("DataIndicator");
        String paxDetails = parser.get("PaxDetails");
        if (paxDetails != null && paxDetails.equals("IN")) {
            //correct due to bug
            freeText = freeText.replace("PAX ", "INF ");
        }
        if ((!filter) || (type != null && type.equalsIgnoreCase("T") && dataIndicator != null && dataIndicator.equalsIgnoreCase("E"))) {
            if (freeText != null) {
                String comps[] = freeText.split("/");
                if (row.getTixInformationFreetext() != null && row.getTixInformationFreetext().contains(freeText)) {
                    return; //avoid duplicates
                }
                row.setTixInformationFreetext(ensureLength(wrap(row.getTixInformationFreetext()) + freeText, 999));
                boolean refundIndicator = false;
                if (comps.length > 0 && comps[0].length() > 3) {
                    int index = comps[0].indexOf("-");
                    if (index == -1 && comps[0].length() > 8) {
                        row.setDocumentNo(ensureLength(comps[0].substring(8), 20));
                    } else {
                        row.setDocumentNo(ensureLength(comps[0].substring(index + 1), 20));
                    }
                }
                if (comps.length > 1 && comps[1].length() > 3) {
                    row.setOrigIssueAirline(ensureLength(comps[1].substring(2, 4), 3));
                    refundIndicator = comps[1].substring(1, 2).equalsIgnoreCase("R");
                    row.setTixVoidRefundSalesInd(ensureLength(refundIndicator ? "1" : null, 1));

                }
                if (comps.length > 2 && comps[2].length() > 3) {
                    String currency = comps[2].substring(0, 3);

                    row.setCrncyOfTxn(ensureLength(currency, 3));
                    String totalFareFa = comps[2].substring(3);
                    if (currency.equalsIgnoreCase("EUR")) {
                        row.setSectorTotalFareFa(ensureLength(totalFareFa, 12));
                    } else {
                        row.setSectorTotalFareFanotEur(ensureLength(totalFareFa, 12));
                    }
                    if (refundIndicator) {
                        row.setRefundableAmount(ensureLength(totalFareFa, 12));
                    }

                }
                if (comps.length > 4) {
                    row.setIssueAgnt(ensureLength(comps[4], 7));
                }
                if (comps.length > 5) {
                    row.setIatauser(ensureLength(comps[5], 10));
                }
            }

        }
    }

    void handleSEA(FileDataRaw row, ServiceLine sLine, SBRFreeTextParser parser) {
        row.setSsrextraSeatCode(ensureLength(sLine.getSecondaryType(), 4));
        row.setSsrseatRow(ensureLength(parser.get("SeatRow"), 10));
        String seatCol = parser.get("SeatCol");
        if (seatCol != null) {
            seatCol = seatCol.trim();
            if (seatCol.length() > 0) {
                row.setSsrseatColumn(seatCol.charAt(0));
            }
        }
        row.setSsrseatType(ensureLength(parser.get("Characteristics"), 15));
        row.setSsrseatStatus(ensureLength(sLine.getCrsStatus(), 3));
    }

    String unmapCSA(int codeShareStatus) {
        switch (codeShareStatus) {
            case BookingNameItem.CODE_SHARE_STATUS_UNDEFINED:
                return "";
            case BookingNameItem.CODE_SHARE_STATUS_RES:
                return "C1A";
            case BookingNameItem.CODE_SHARE_BLOCKED_SPACE:
                return "CBS";
            case BookingNameItem.CODE_SHARE_STATUS_CAPPED_FREE_FLOW:
                return "CCF";
            case BookingNameItem.CODE_SHARE_STATUS_FREE_FLOW:
                return "CFF";
            case BookingNameItem.CODE_SHARE_STATUS_PLAN_B:
                return "CPB";
            case BookingNameItem.CODE_SHARE_STATUS_SINGLE_PNR:
                return "CSG";
            case BookingNameItem.CODE_SHARE_STATUS_TRUE_INVENTORY:
                return "CTI";
        }
        return "";
    }

    String getResFormatDate(GregorianCalendar c) {
        if (c == null) {
            return null;
        }
        return AirlineDateFormat.calendarToString(c);
/*
        AirlineDateFormat df = new AirlineDateFormat("ddMMMyy");
        return df.format(c.getTime());
        */
    }

    String getDate(GregorianCalendar c) {
        if (c == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        buf.append(c.get(Calendar.DAY_OF_MONTH));
        String month = Integer.toString(c.get(Calendar.MONTH) + 1);
        if (month.length() == 1) {
            buf.append("0");
        }
        buf.append(month);
        String year = Integer.toString(c.get(Calendar.YEAR));
        buf.append(year.substring(2, 4));
        return buf.toString();
    }


    private String getTime(GregorianCalendar c) {
        if (c == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        String hour = Integer.toString(c.get(Calendar.HOUR_OF_DAY));
        if (hour.length() == 1) {
            buf.append("0");
        }
        buf.append(hour);
        String minute = Integer.toString(c.get(Calendar.MINUTE));
        if (minute.length() == 1) {
            buf.append("0");
        }
        buf.append(minute);
        return buf.toString();
    }

}