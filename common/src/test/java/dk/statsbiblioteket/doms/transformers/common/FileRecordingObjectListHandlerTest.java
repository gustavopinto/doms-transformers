package dk.statsbiblioteket.doms.transformers.common;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Test list handling.
 */
public class FileRecordingObjectListHandlerTest extends TestCase {
    @Test
    public void testTransform() throws IOException {
        ObjectHandler testObjectHandler = new ObjectHandler() {
            int counter = 0;
            @Override
            public MigrationStatus transform(String uuid) throws Exception {
                if (counter++ % 2 == 0) {
                    return MigrationStatus.FAILED;
                }
                return MigrationStatus.COMPLETE;
            }

            public String getName() {
                return getClass().getName();
            }
        };
        Config config = new Config() {
            @Override
            public String getOutputDirectory() {
                return "target";
            }
        };

        File outputDirectory = new File(config.getOutputDirectory(), testObjectHandler.getName());
        if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs()) {
            throw new IOException("Unable to create directory '" + outputDirectory.getPath() + "'");
        }

        FileRecordingObjectListHandler handler = new FileRecordingObjectListHandler(config, testObjectHandler);
        handler.transform(Arrays.asList("uuid:cb8da856-fae8-473f-9070-8d24b5a84cfc",
                                        "uuid:99c1b516-3ea9-49ce-bfbc-ae1ea8faf0e3"));
        List<String> successes = new TrivialUuidFileReader().readUuids(new File(outputDirectory, MigrationStatus.COMPLETE.name().toLowerCase()));
        List<String> failures = new TrivialUuidFileReader().readUuids(new File(outputDirectory, MigrationStatus.FAILED.name().toLowerCase()));
        assertEquals(1, successes.size());
        assertEquals(1, failures.size());
        assertEquals("uuid:99c1b516-3ea9-49ce-bfbc-ae1ea8faf0e3", successes.get(0));
        assertEquals("uuid:cb8da856-fae8-473f-9070-8d24b5a84cfc", failures.get(0));
    }
}
