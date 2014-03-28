package com.sabre.ix.application.input;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import javax.xml.soap.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created with IntelliJ IDEA.
 * User: Henrik Thorburn (sg0211570)
 * Date: 2014-03-27
 */
public class DataServicesBookingSelectorImpl implements BookingSelector {
    private static final Logger log = Logger.getLogger(DataServicesBookingSelectorImpl.class);
    private static final String SOAP_QUERY =
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dat=\"http://www.sabre.com/asx/DataServicesV1\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <dat:executeStoredQueryRequest>\n" +
                    "         <dat:queryName>AbToBeExportedToDwh</dat:queryName>\n" +
                    "         <!--Zero or more repetitions:-->\n" +
                    "         \n" +
                    "      </dat:executeStoredQueryRequest>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";

    private String webServiceHost = "http://uninitialized.com";
    private int readTimeoutMillis = 10000;
    private int connectionTimeoutMillis = 10000;

    public DataServicesBookingSelectorImpl() {
    }

    @Override
    public ConcurrentLinkedQueue<Long> getBookingsToExport() {
        ConcurrentLinkedQueue<Long> returnedIds = new ConcurrentLinkedQueue<Long>();

        try {
            MessageFactory messageFactory = MessageFactory.newInstance();
            SOAPMessage soapMessage = messageFactory.createMessage();
            SOAPPart part = soapMessage.getSOAPPart();
            part.setContent(new StreamSource(new StringReader(SOAP_QUERY)));
            SOAPConnectionFactory connectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = connectionFactory.createConnection();

            URL timedEndpoint = new URL(new URL(webServiceHost),
                    "/asx/dataservices",
                    new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL url) throws IOException {
                            URL target = new URL(url.toString());
                            URLConnection connection = target.openConnection();
                            // Apply connection settings
                            connection.setConnectTimeout(connectionTimeoutMillis);
                            connection.setReadTimeout(readTimeoutMillis);
                            return connection;
                        }
                    }
            );

            SOAPMessage reply = soapConnection.call(soapMessage, timedEndpoint);
            SOAPBody replyBody = reply.getSOAPBody();
            log.debug("reply SoapBody: " + replyBody.getValue());
            if (replyBody.hasFault()) {
                SOAPFault fault = replyBody.getFault();
                Detail detail = fault.getDetail();
                Iterator detailEntries = detail.getDetailEntries();
                StringBuilder details = new StringBuilder();
                while (detailEntries.hasNext()) {
                    DetailEntry detailEntry = (DetailEntry) detailEntries.next();
                    details.append(", Detail: ").append(detailEntry.getValue());
                }
                throw new RuntimeException("SOAP Fault: " + fault.getFaultActor() + "," + fault.getFaultCode() + "," +
                        fault.getFaultString() + details.toString());
            }
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            StreamResult result = new StreamResult(new StringWriter());
            Source source = reply.getSOAPPart().getContent();
            transformer.transform(source, result);

            String xmlResponse = result.getWriter().toString();

            Document document = DocumentHelper.parseText(xmlResponse);
            Element executeStoredQueryResponse = findExecuteStoredQueryResponseElement(document.getRootElement());
            for (Element row : (List<Element>) executeStoredQueryResponse.elements("row")) {
                Element cell = row.element("cell");
                String s = cell.getTextTrim();
                Long l = Long.parseLong(s);
                returnedIds.add(l);
            }

        } catch (Exception e) {
            log.error("Failed to retrieve booking IDs via data services", e);
        }

        return returnedIds;
    }

    @Override
    public void setProperties(Properties properties) {
        webServiceHost = properties.getProperty("DWHExporter_DataServicesHost");
        connectionTimeoutMillis = Integer.parseInt(properties.getProperty("DWHExporter_DataServicesConnectionTimeoutMillis"));
        readTimeoutMillis = Integer.parseInt(properties.getProperty("DWHExporter_DataServicesReadTimeoutMillis"));
    }

    // To avoid namespace issues in xpath, i'm doing it the hard way
    private Element findExecuteStoredQueryResponseElement(Element element) {
        if (element.getName().equalsIgnoreCase("executeStoredQueryResponse")) {
            return element;
        } else {
            for (Element childElement : (List<Element>) element.elements()) {
                Element e = findExecuteStoredQueryResponseElement(childElement);
                if (e != null) {
                    return e;
                }
            }
        }
        return null;
    }
}
