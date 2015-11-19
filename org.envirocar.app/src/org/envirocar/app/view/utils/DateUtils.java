package org.envirocar.app.view.utils;

import android.content.Context;
import android.text.format.DateFormat;

import org.envirocar.app.R;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class DateUtils {

    public static String getDayOfWeekString(Calendar calendar) {
        return calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
    }

    public static String getDateString(Context context, long date){
        Calendar now = Calendar.getInstance();
        now.setTimeZone(TimeZone.getDefault());
        Calendar time = Calendar.getInstance();
        time.setTimeZone(TimeZone.getDefault());
        time.setTimeInMillis(date);

        if(now.get(Calendar.DATE) == time.get(Calendar.DATE)){
            return context.getString(R.string.today);
        } else if(now.get(Calendar.DATE) - time.get(Calendar.DATE) == 1){
            return context.getString(R.string.yesterday);
        } else {
            return DateFormat.format("EE, MM yyyy", time).toString();
//            return DateFormat.format("MMMM dd yyyy, h:mm aa", time).toString();
        }
    }
}
