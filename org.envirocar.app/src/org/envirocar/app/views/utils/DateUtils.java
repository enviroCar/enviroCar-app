/**
 * Copyright (C) 2013 - 2021 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.utils;

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
