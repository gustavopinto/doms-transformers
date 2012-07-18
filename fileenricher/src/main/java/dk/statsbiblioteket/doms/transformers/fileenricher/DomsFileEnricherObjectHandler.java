package dk.statsbiblioteket.doms.transformers.fileenricher;

import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import dk.statsbiblioteket.doms.central.CentralWebservice;
import dk.statsbiblioteket.doms.central.InvalidCredentialsException;
import dk.statsbiblioteket.doms.central.InvalidResourceException;
import dk.statsbiblioteket.doms.central.MethodFailedException;
import dk.statsbiblioteket.doms.transformers.common.DomsConfig;
import dk.statsbiblioteket.doms.transformers.common.ObjectHandler;
import dk.statsbiblioteket.doms.transformers.fileenricher.autogenerated.BroadcastFileDescriptiveMetadataType;
import dk.statsbiblioteket.doms.transformers.fileenricher.autogenerated.ChannelIDsType;
import dk.statsbiblioteket.doms.transformers.fileenricher.autogenerated.ObjectFactory;
import dk.statsbiblioteket.doms.transformers.fileenricher.checksums.ChecksumParser;

/**
 * Use DOMS to enrich file metadata.
 */
public class DomsFileEnricherObjectHandler implements ObjectHandler {

    private final DomsConfig config;
    private final CentralWebservice webservice;
    private ChannelIDToSBChannelIDMapper channelIDMapper = ChannelIDToSBChannelIDMapper.getInstance();
    private Marshaller marshaller;
    private ObjectHandler delegate;
    private Map<String, String> checksums;

    /**
     * Initialise object handler.
     *
     * @param config     Configuration.
     * @param webservice The DOMS WebService.
     * @param checksums
     */
    public DomsFileEnricherObjectHandler(FileEnricherConfig config, CentralWebservice webservice, ChecksumParser checksums, ObjectHandler delegate) throws JAXBException {
        this.config = config;
        this.webservice = webservice;
        this.delegate = delegate;
        this.checksums = checksums.getNameChecksumsMap();
        marshaller = JAXBContext.newInstance("dk.statsbiblioteket.doms.transformers.fileenricher.autogenerated").createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

    }

    @Override
    public void transform(String uuid) throws Exception {

        webservice.markInProgressObject(Arrays.asList(uuid), "Modifying object as part of datamodel upgrade");
        if (delegate != null) {
            delegate.transform(uuid);
        }
        String filename = getFilenameFromObject(uuid);
        BroadcastFileDescriptiveMetadataType metadata = decodeFilename(filename);
        storeMetadataInObject(uuid, metadata);
        webservice.markPublishedObject(Arrays.asList(uuid), "Modifying object as part of datamodel upgrade");
    }


    public void storeMetadataInObject(String uuid, BroadcastFileDescriptiveMetadataType metadata) throws InvalidCredentialsException, MethodFailedException, InvalidResourceException, JAXBException {
        StringWriter writer = new StringWriter();
        JAXBElement<BroadcastFileDescriptiveMetadataType> blob = new ObjectFactory().createBroadcastFileDescriptiveMetadata(metadata);
        marshaller.marshal(blob, writer);
        String contents = writer.toString();
        webservice.modifyDatastream(uuid, "BROADCAST_METADATA", contents, "Updating metadata as part of the radio/tv datamodel refactoring");
    }

    public String getFilenameFromObject(String uuid) throws InvalidCredentialsException, MethodFailedException, InvalidResourceException {
        String label = webservice.getObjectProfile(uuid).getTitle();
        String filename = label.substring(label.lastIndexOf("/") + 1);
        return filename;
    }

    public BroadcastFileDescriptiveMetadataType decodeFilename(String filename) throws ParseException {
        BroadcastFileDescriptiveMetadataType result = null;
        if (filename.endsWith(".ts")) {
            result = decodeMuxFilename(filename);
        } else if (filename.endsWith(".wav")) {
            result = decodeRadioFilename(filename);
        } else if (filename.endsWith(".mpeg")) {
            result = decodeAnalogTVFilename(filename);
        } else if (filename.endsWith(".wmv")) {
            result = decodeAnalogTVFilename(filename);
        } else if (filename.endsWith(".mp4")) {
            result = decodeAnalogTVFilename(filename);
        } else {
            result = null;
        }
        if (result != null) {
            result.setChecksum(checksums.get(filename));
        }
        return result;
    }

    //TODO: get fixed channel list for Mux1 and mux2
    private BroadcastFileDescriptiveMetadataType decodeMuxFilename(String filename) {
        //mux1.1287514800-2010-10-19-21.00.00_1287518400-2010-10-19-22.00.00_dvb1-1.ts
        //(type).(timestart)-(timestart)_(timeend)-(timeEnd)_(recorder).ts
        BroadcastFileDescriptiveMetadataType metadata = new BroadcastFileDescriptiveMetadataType();

        ChannelIDsType channels = new ChannelIDsType();

        for (int i = 0; i < channels.getChannel().size(); i++) {
            String channelID = channels.getChannel().get(i);
            channelID = ChannelIDToSBChannelIDMapper.getInstance().mapToSBChannelID(channelID);
            channels.getChannel().set(i, channelID);
        }

        metadata.setChannelIDs(channels);

        String startUnixTime = filename.split("\\.")[1].split("-")[0];
        String stopUnixTime = filename.split("_")[1].split("-")[0];
        String recorder = filename.split("_")[2].split("\\.")[0];

        metadata.setStartTimeDate(CalendarUtils.getXmlGregorianCalendar(startUnixTime));
        metadata.setEndTimeDate(CalendarUtils.getXmlGregorianCalendar(stopUnixTime));
        metadata.setRecorder(recorder);
        return metadata;
    }

    private BroadcastFileDescriptiveMetadataType decodeRadioFilename(String filename) throws ParseException {
        //drp1_88.100_DR-P1_pcm_20080509045602_20080510045501_encoder5-2.wav
        //(channelID)_(frequency)_(CHANNELID)_(format)_(timeStart)_(timeEnd)_(recorder).wav
        BroadcastFileDescriptiveMetadataType metadata = new BroadcastFileDescriptiveMetadataType();
        ChannelIDsType channels = new ChannelIDsType();

        String[] tokens = filename.split("_");
        String channelID = tokens[0];
        String format = "info:pronom/fmt/6"; // WAV pronom format uri
        String timeStart = tokens[4];
        String timeStop = tokens[5];
        String recorder = null;
        if (tokens.length >= 7) {
            recorder = tokens[6].split("\\.")[0];
        }

        DateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date timeStartDate = dateformat.parse(timeStart);
        Date timeStopDate = dateformat.parse(timeStop);

        channels.getChannel().add(channelIDMapper.mapToSBChannelID(channelID));
        metadata.setChannelIDs(channels);
        metadata.setFormat(format);
        metadata.setRecorder(recorder);
        metadata.setStartTimeDate(CalendarUtils.getXmlGregorianCalendar(timeStartDate));
        metadata.setEndTimeDate(CalendarUtils.getXmlGregorianCalendar(timeStopDate));

        return metadata;

    }

    private BroadcastFileDescriptiveMetadataType decodeAnalogTVFilename(String filename) throws ParseException {
        //tv2c_623.250_K40-TV2-Charlie_mpeg1_20080503121001_20080504030601_encoder3-2.mpeg
        //kanal4_359.250_K42-Kanal4_mpeg1_20101023195601_20101023231601_encoder7-2.mpeg
        //tv3_161.250_S09-TV3_mpeg1_20101021175601_20101022010602_encoder6-2.mpeg
        //(channelID)_(frequency)_(CHANNELID)_(format)_(timeStart)_(timeEnd)_(recorder).mpeg
        BroadcastFileDescriptiveMetadataType metadata = new BroadcastFileDescriptiveMetadataType();
        ChannelIDsType channels = new ChannelIDsType();

        String[] tokens = filename.split("_");

        String channelID = tokens[0];
        String format = tokens[3];
        String timeStart = tokens[4];
        String timeStop = tokens[5];
        String recorder = null;
        if (tokens.length >= 7) {
            recorder = tokens[6].split("\\.")[0];
        }
        String formatUri;
        if (format.equals("mpeg1")) {
            formatUri = "info:pronom/x-fmt/385";
        } else if (format.equals("mpeg2")) {
            formatUri = "info:pronom/x-fmt/386";
        } else if (format.equals("wmv")) {
            formatUri = "info:pronom/fmt/133";
        } else if (format.equals("mp4")) {
            formatUri = "info:pronom/fmt/199";
        } else {
            throw new ParseException("Failed to parse format string '" + format + "'", 0);
        }

        DateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date timeStartDate = dateformat.parse(timeStart);
        Date timeStopDate = dateformat.parse(timeStop);

        channels.getChannel().add(channelIDMapper.mapToSBChannelID(channelID));
        metadata.setChannelIDs(channels);
        metadata.setFormat(formatUri);
        metadata.setRecorder(recorder);
        metadata.setStartTimeDate(CalendarUtils.getXmlGregorianCalendar(timeStartDate));
        metadata.setEndTimeDate(CalendarUtils.getXmlGregorianCalendar(timeStopDate));

        return metadata;
    }


}
