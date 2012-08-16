package dk.statsbiblioteket.doms.transformers.shardmigrator;

import dk.statsbiblioteket.doms.central.*;
import dk.statsbiblioteket.doms.transformers.common.CalendarUtils;
import dk.statsbiblioteket.doms.transformers.common.MockWebservice;
import dk.statsbiblioteket.doms.transformers.shardmigrator.programBroadcast.autogenerated.ProgramBroadcast;
import dk.statsbiblioteket.doms.transformers.shardmigrator.programStructure.autogenerated.ProgramStructure;
import dk.statsbiblioteket.doms.transformers.shardmigrator.shardmetadata.autogenerated.ShardMetadata;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.String;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import junit.framework.Assert;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.hamcrest.CoreMatchers;



/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: 7/20/12
 * Time: 1:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class DomsShardMigratorObjectHandlerTest {

    private String testFileObjectPid;
    private String programObjectPid;
    private String shardObjectPid;

    CentralWebservice webservice;
    private static final String TEST_SHARD_METADATA_FILE = "shardMetadata.xml";

    @Before
    public void setUp() throws Exception {
        String testMuxFileName = "mux1.1287514800-2010-10-19-21.00.00_1287518400-2010-10-19-22.00.00_dvb1-1.ts";

        webservice = new MockWebservice();

        testFileObjectPid = webservice.newObject(null, null, null);
        webservice.addFileFromPermanentURL(testFileObjectPid,null,null,"http://bitfinder.statsbiblioteket.dk/bart/"+testMuxFileName,null,null);

        programObjectPid = webservice.newObject(null, null, null);

        shardObjectPid = webservice.newObject(null, null, null);

        webservice.modifyDatastream(programObjectPid,"PBCORE",
                IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("objects/27026d8e-bbb6-499f-b304-8511426ebfdb_pbcore.xml")
                ),"comment");

        webservice.modifyDatastream(programObjectPid,"GALLUP_ORIGINAL",
                IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("objects/27026d8e-bbb6-499f-b304-8511426ebfdb_gallup.xml")
                ),"comment");


        webservice.modifyDatastream(programObjectPid,"RITZAU_ORIGINAL",
                IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("objects/27026d8e-bbb6-499f-b304-8511426ebfdb_ritzau.xml")
                ),"comment");
        Relation shardRelation = new Relation();
        shardRelation.setObject(shardObjectPid);
        shardRelation.setSubject(programObjectPid);
        shardRelation.setPredicate("http://doms.statsbiblioteket.dk/relations/default/0/1/#hasShard");
        shardRelation.setLiteral(false);
        webservice.addRelation(programObjectPid, shardRelation, "comment");
        webservice.markPublishedObject(Arrays.asList(programObjectPid),"comment");

        webservice.modifyDatastream(shardObjectPid,"SHARD_METADATA",
                IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("shardMetadata.xml"))
                ,"comment");
        Relation fileRelation = new Relation();
        fileRelation.setObject(testFileObjectPid);
        fileRelation.setSubject(shardObjectPid);
        fileRelation.setPredicate("http://doms.statsbiblioteket.dk/relations/default/0/1/#consistsOf");
        fileRelation.setLiteral(false);
        webservice.addRelation(shardObjectPid, fileRelation, "comment");
        webservice.markPublishedObject(Arrays.asList(shardObjectPid),"comment");
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testTransform() throws Exception {
        DomsShardMigratorObjectHandler handler = new DomsShardMigratorObjectHandler(null, webservice);
        handler.transform(programObjectPid);
        
        String programBroadcast = webservice.getDatastreamContents(programObjectPid, "PROGRAM_BROADCAST");
        JAXBElement<ProgramBroadcast> obj = (JAXBElement<ProgramBroadcast>) JAXBContext.newInstance(ProgramBroadcast.class.getPackage().getName()).createUnmarshaller().unmarshal(
                new ByteArrayInputStream(programBroadcast.getBytes()));
        ProgramBroadcast pb = obj.getValue();
        
        Date start = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")).parse("2009-06-18 06:35:30.0");
        Date stop = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")).parse("2009-06-18 06:41:36.0");
        assertThat(pb.getTimeStart(), is(CalendarUtils.getXmlGregorianCalendar(start)));
        assertThat(pb.getTimeStop(), is(CalendarUtils.getXmlGregorianCalendar(stop)));
        
    }

    @Test
    public void shardMetadataMigratorTest() throws URISyntaxException, IOException, JAXBException,
            InvalidCredentialsException, InvalidResourceException, MethodFailedException {
        DomsShardMigratorObjectHandler handler = new DomsShardMigratorObjectHandler(null, null);
        File testDataFile =
                new File(Thread.currentThread().getContextClassLoader().getResource(TEST_SHARD_METADATA_FILE).toURI());
        String testShardMetadata = org.apache.commons.io.IOUtils.toString(new FileInputStream(testDataFile));
        ShardMetadata shardMetadata = handler.deserializeShardMetadata(testShardMetadata);
        ProgramStructure programStructure = handler.convertShardStructure(shardMetadata);
    }
}
