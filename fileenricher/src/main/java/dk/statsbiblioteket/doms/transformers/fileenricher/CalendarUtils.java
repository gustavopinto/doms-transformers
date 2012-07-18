package dk.statsbiblioteket.doms.transformers.fileenricher;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class CalendarUtils {

    /**
     * Private constructor. 
     */
    private CalendarUtils() {}
    
    /**
     * Turns a string representation of unix time into a XMLGregorianCalendar.
     * @param unixTime The unixtime. If the argument is null, then epoch is returned.
     * @return The XMLGregorianCalendar.
     */
    public static XMLGregorianCalendar getXmlGregorianCalendar(String unixTime) {
        Date date;
        
        if(unixTime == null) {
            date = new Date(0);
        } else {
            date = new Date(Long.parseLong(unixTime)*1000);
        }
        return getXmlGregorianCalendar(date);
    }
    
    /**
     * Turns a date into a XMLGregorianCalendar.
     * @param date The Date. If the argument is null, then epoch is returned.
     * @return The XMLGregorianCalendar.
     */
    public static XMLGregorianCalendar getXmlGregorianCalendar(Date date) {
        if(date == null) {
            date = new Date(0);
        } 
        
        GregorianCalendar gc = new GregorianCalendar();
        try {
            gc.setTime(date);
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
        } catch (Exception e) {
            throw new IllegalStateException("Could not convert the date '" + date + "' into the xml format.", e);
        }
    }
}
