package com.sabre.ix.application;

import com.sabre.ix.client.DcsFlightLeg;
import com.sabre.ix.client.DcsFlightLegServices;
// import com.sabre.ix.client.OpsDcsLink;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DcsFlightLegClientTest {

    DcsFlightLegServices dcsFlightLegServices;
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
        dcsFlightLegServices = new DcsFlightLegServices(context, dataHandler, actionHandler);
    }

    @Test
    @Ignore
    public void verifyBasicTransformationOfDcsFlightLeg() throws IOException {
        DcsFlightLeg flightLeg = new DcsFlightLeg(dcsFlightLegServices, getFileAsString("example_Dcs_message.xml"));
        assertThat(flightLeg.getOrigin(), equalTo("VIE"));
        assertThat(flightLeg.getDestination(), equalTo("DME"));
        assertThat(flightLeg.getDcsPaxs().size(), equalTo(59));
    }

    @Test
    @Ignore
    public void pullDcsOpsLinkClasses() throws IOException {
        DcsFlightLeg flightLeg = new DcsFlightLeg(dcsFlightLegServices, getFileAsString("example_Dcs_message.xml"));
        //List<OpsDcsLink> linkedDataObjects = flightLeg.getLinkedDataObjects(OpsDcsLink.class);
        //assertTrue(linkedDataObjects.isEmpty());
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
