package dk.statsbiblioteket.doms.transformers.shardmigrator;

import dk.statsbiblioteket.doms.transformers.shardmigrator.tvmeter.autogenerated.TvmeterProgram;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: 7/19/12
 * Time: 10:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class PBCoreMigratorTest {
    @Test
    public void testAddGallupStructure() throws Exception {
        InputStream samplePBCORE = Thread.currentThread().getContextClassLoader().getResourceAsStream("objects/6a7f270c-a62e-4950-bec1-1ea230bf52ea_pbcore.xml");
        PBCoreMigrator migrator = new PBCoreMigrator(samplePBCORE);
        JAXBContext context = JAXBContext.newInstance(TvmeterProgram.class.getPackage().getName());
        URL sampleTVMeter = Thread.currentThread()
                .getContextClassLoader()
                .getResource("TVMeterExample.xml");
         JAXBElement<TvmeterProgram> tvmeter = (JAXBElement<TvmeterProgram>) context.
                createUnmarshaller()
                .unmarshal(sampleTVMeter);
        /*Method method = TvmeterProgram.class.getDeclaredMethod("set"+"StartDate", String.class);
        method.invoke(tvmeter,"testStartDate");*/
        migrator.addTVMeterStructure(tvmeter.getValue());
        System.out.println(migrator.toString());
    }

    @Test
    public void testAddRitzauStructure() throws Exception {

    }

    @Test
    public void testToString() throws Exception {

    }
}
