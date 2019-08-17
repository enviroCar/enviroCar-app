package org.envirocar.app.views.statistics;

import org.envirocar.core.entity.Track;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.Util;

import java.util.Calendar;
import java.util.Date;

/**
 * Utility class to get which day, date, month or year a Track has occurred in
 * Create an object of the class and pass the track as an argument to the constructor
 */
public class TrackDateUtil {

    private static final Logger LOG = Logger.getLogger(TrackDateUtil.class);

    protected Track track;
    protected Date date;
    protected Calendar cal;

    public TrackDateUtil(Track track) {
        this.track = track;
        cal = Calendar.getInstance();
        date = new Date();
        String dateS = track.getCreated();

        try {
            date = new Date(Util.isoDateToLong(dateS));
        } catch (Exception e) {
            LOG.info("getDate: Error parsing date"+e.toString());
        }

        cal.setTime(date);
    }

    public Date getDateObject() {
        return date;
    }

    public boolean checkDate(String S) {
        Date testDate = new Date();
        try {
            testDate = new Date(Util.isoDateToLong(S));
        } catch (Exception e) {
            LOG.info("getDate: Error parsing date"+e.toString());
        }

        return date.equals(testDate);
    }

    public int getDate() {
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    public boolean checkDay(int d) {
        int day = cal.get(Calendar.DAY_OF_WEEK);
        return (day == d);
    }

    // Sunday is 1
    public int getDay() {
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    public boolean checkWeek(int w) {
        int week = cal.get(Calendar.WEEK_OF_YEAR);

        return (week == w);
    }

    public boolean checkMonth(int m, int y) {
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);

        return (month == m && year ==y);
    }

    // Zero based, i.e January is 0
    public int getMonth() {
        return cal.get(Calendar.MONTH);
    }

    public boolean checkYear(int y) {
        int year = cal.get(Calendar.YEAR);
        return (year ==y);
    }

    // Zero based, i.e January is 0
    public int getYear() {
        return cal.get(Calendar.YEAR);
    }
}
