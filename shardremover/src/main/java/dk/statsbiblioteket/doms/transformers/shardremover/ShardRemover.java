package dk.statsbiblioteket.doms.transformers.shardremover;

import dk.statsbiblioteket.doms.central.CentralWebservice;
import dk.statsbiblioteket.doms.transformers.common.DomsConfig;
import dk.statsbiblioteket.doms.transformers.common.DomsWebserviceFactory;
import dk.statsbiblioteket.doms.transformers.common.FileRecordingObjectListHandler;
import dk.statsbiblioteket.doms.transformers.common.ObjectHandler;
import dk.statsbiblioteket.doms.transformers.common.ObjectListHandler;
import dk.statsbiblioteket.doms.transformers.common.PropertyBasedDomsConfig;
import dk.statsbiblioteket.doms.transformers.common.TrivialUuidFileReader;
import dk.statsbiblioteket.doms.transformers.common.UuidFileReader;
import dk.statsbiblioteket.doms.transformers.common.callbacks.exceptions.CallbackException;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Tool for removing Radio/TV shards.
 * Takes as input a file with program uuids (one per line). For each program, resolves shards and adds the
 * shard UUID as extra ID for this objects, then deletes the reference to the shard object and the shard object.
 * See the ShardMigrator tool for moving metadata from shards to programs first.
 */
public class ShardRemover {
    public static void main(String[] args) throws IOException, CallbackException {
        //TODO: Setup apache CLI

        if (args.length == 2) {
            File configfile = new File(args[0]);
            File uuidfile = new File(args[1]);
            run(configfile, uuidfile);
        } else {
            System.out.println("bin/shardremover.sh config-file uuid-file");
            System.exit(1);
        }
    }

    public static void run(File configfile, File uuidfile) throws IOException, CallbackException {
        UuidFileReader uuidFileReader = new TrivialUuidFileReader();
        DomsConfig config = new PropertyBasedDomsConfig(configfile);
        CentralWebservice webservice = new DomsWebserviceFactory(config).getWebservice();
        ObjectHandler objectHandler = new DomsShardRemoverObjectHandler(config, webservice);
        ObjectListHandler objectListHandler = new FileRecordingObjectListHandler(config, objectHandler);

        List<String> uuids = uuidFileReader.readUuids(uuidfile);
        objectListHandler.transform(uuids);
    }

}
