package dk.statsbiblioteket.doms.transformers.common;

import dk.statsbiblioteket.doms.transformers.common.autogenerated.BroadcastFileDescriptiveMetadataType;
import dk.statsbiblioteket.doms.transformers.common.autogenerated.ChannelIDsType;
import dk.statsbiblioteket.doms.transformers.common.muxchannels.MuxFileChannelCalculator;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class FileNameParser {

    public static BroadcastFileDescriptiveMetadataType decodeFilename(String filename,
                                                                      Map<String, String> checksums,
                                                                      MuxFileChannelCalculator muxChannelCalculator)
            throws ParseException {

        if (checksums.containsKey(filename)) {
            return decodeFilename(filename, checksums.get(filename), muxChannelCalculator);
        } else {
            return null;
        }
    }

    public static BroadcastFileDescriptiveMetadataType decodeFilename(String filename,
                                                                      String checksum,
                                                                      MuxFileChannelCalculator muxChannelCalculator)
            throws ParseException {

        BroadcastFileDescriptiveMetadataType result = null;

        if (filename.endsWith(".ts")) {
            result = decodeMuxFilename(filename, muxChannelCalculator);
        } else if (filename.endsWith(".wav")) {
            result = decodeRadioFilename(filename);
        } else if (filename.endsWith(".mpeg") || filename.endsWith(".wmv") || filename.endsWith(".mp4")) {
            result = decodeAnalogTVFilename(filename);
        }

        if (result != null) {
            result.setChecksum(checksum);
        }

        return result;
    }

    private static BroadcastFileDescriptiveMetadataType decodeMuxFilename(String filename, MuxFileChannelCalculator muxChannelCalculator) {
        //mux1.1287514800-2010-10-19-21.00.00_1287518400-2010-10-19-22.00.00_dvb1-1.ts
        //(type).(timestart)-(timestart)_(timeend)-(timeEnd)_(recorder).ts
        BroadcastFileDescriptiveMetadataType metadata = new BroadcastFileDescriptiveMetadataType();

        String startUnixTime = filename.split("\\.")[1].split("-")[0];
        String stopUnixTime = filename.split("_")[1].split("-")[0];
        String recorder = filename.split("_")[2].split("\\.")[0];
        String muxName = filename.split("\\.")[0];
        int muxID = Integer.parseInt(muxName.split("mux")[1]);

        ChannelIDsType channels = new ChannelIDsType();
        channels.getChannel().addAll(muxChannelCalculator.getChannelIDsForMux(muxID, CalendarUtils.getDate(startUnixTime)));

        for (int i = 0; i < channels.getChannel().size(); i++) {
            String channelID = channels.getChannel().get(i);
            channelID = ChannelIDToSBChannelIDMapper.getInstance().mapToSBChannelID(channelID);
            channels.getChannel().set(i, channelID);
        }

        metadata.setChannelIDs(channels);
        metadata.setStartTimeDate(CalendarUtils.getXmlGregorianCalendar(startUnixTime));
        metadata.setEndTimeDate(CalendarUtils.getXmlGregorianCalendar(stopUnixTime));
        metadata.setRecorder(recorder);

        return metadata;
    }

    private static BroadcastFileDescriptiveMetadataType decodeRadioFilename(String filename) throws ParseException {
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

        channels.getChannel().add(ChannelIDToSBChannelIDMapper.getInstance().mapToSBChannelID(channelID));
        metadata.setChannelIDs(channels);
        metadata.setFormat(format);
        metadata.setRecorder(recorder);
        metadata.setStartTimeDate(CalendarUtils.getXmlGregorianCalendar(timeStartDate));
        metadata.setEndTimeDate(CalendarUtils.getXmlGregorianCalendar(timeStopDate));

        return metadata;

    }

    private static BroadcastFileDescriptiveMetadataType decodeAnalogTVFilename(String filename) throws ParseException {
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

        channels.getChannel().add(ChannelIDToSBChannelIDMapper.getInstance().mapToSBChannelID(channelID));
        metadata.setChannelIDs(channels);
        metadata.setFormat(formatUri);
        metadata.setRecorder(recorder);
        metadata.setStartTimeDate(CalendarUtils.getXmlGregorianCalendar(timeStartDate));
        metadata.setEndTimeDate(CalendarUtils.getXmlGregorianCalendar(timeStopDate));

        return metadata;
    }
}
