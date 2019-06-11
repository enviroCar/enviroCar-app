package org.envirocar.app.views.statistics;

import android.util.Log;

import org.envirocar.core.entity.Track;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class TrackwDate {

    protected Track track;
    protected Date date;

    public TrackwDate(){

    }

    public void getDateTime(Track mTrack){
        track = mTrack;
        String dateS = track.getCreated();
        SimpleDateFormat dateFormatter=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        try {
            date = dateFormatter.parse(dateS);
        }catch (Exception e)
        {
            Log.d(TAG, "getDate: Error parsing date"+e.toString());
        }
    }

    public boolean checkDate(String S)
    {
        Date testDate = new Date();
        SimpleDateFormat dateFormatter=new SimpleDateFormat("dd-MMM-yyyy");
        try {
            testDate = dateFormatter.parse(S);
        }catch (Exception e)
        {
            Log.d(TAG, "getDate: Error parsing date"+e.toString());
        }

        boolean test = date.equals(testDate);

        return test;
    }

    public int getDate()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int day = cal.get(Calendar.DAY_OF_MONTH);
        return day;
    }

    public boolean checkDay(int d) // Sunday is 1
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int day = cal.get(Calendar.DAY_OF_WEEK);
        boolean test = (day == d);
        return test;
    }

    public int getDay() // Sunday is 1
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int day = cal.get(Calendar.DAY_OF_WEEK);
        return day;
    }

    public boolean checkWeek(int w)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int week = cal.get(Calendar.WEEK_OF_YEAR);
        boolean test = (week == w);
        return test;
    }

    public boolean checkMonth(int m, int y) // Zero based, i.e January is 0
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        boolean test = (month == m && year ==y);
        return test;
    }

    public int getMonth() // Zero based, i.e January is 0
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int day = cal.get(Calendar.MONTH);
        return day;
    }

    public boolean checkYear(int y) // Zero based, i.e January is 0
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        boolean test = (year ==y);
        return test;
    }
}
