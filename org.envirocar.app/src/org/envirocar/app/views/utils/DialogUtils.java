/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.envirocar.app.R;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class DialogUtils {

    public static MaterialAlertDialogBuilder createDefaultDialogBuilder(
            Context context, int titleRes, int iconRes, View contentView) {
        return createDefaultDialogBuilder(context, context.getString(titleRes), iconRes,
                contentView);
    }

    public static MaterialAlertDialogBuilder createDefaultDialogBuilder(
            Context context, int titleRes, int iconRes, int contentRes) {
        return createDefaultDialogBuilder(context, context.getString(titleRes), iconRes, context
                .getString(contentRes));
    }

    public static MaterialAlertDialogBuilder createDefaultDialogBuilder(
            Context context, int titleRes, int iconRes, String content) {
        return createDefaultDialogBuilder(context, context.getString(titleRes), iconRes, content);
    }

    public static MaterialAlertDialogBuilder createDefaultDialogBuilder(
            Context context, String title, int iconRes, String content) {
        TextView textView = new TextView(context);
        textView.setText(content);
        return createDefaultDialogBuilder(context, title, iconRes, textView);
    }

    public static MaterialAlertDialogBuilder createDefaultDialogBuilder(
            Context context, String title, int iconRes, View customView) {
        View layout = LayoutInflater.from(context)
                .inflate(R.layout.general_dialog_view, null, false);

        TextView titleView = layout.findViewById(R.id.general_dialog_view_title);
        ImageView imageView = layout.findViewById(R.id.general_dialog_view_icon);
        FrameLayout contentView = layout.findViewById(R.id
                .general_dialog_view_content);

        titleView.setText(title);
        imageView.setImageResource(iconRes);
        contentView.addView(customView);

        return new MaterialAlertDialogBuilder(context).setView(layout);
    }


}
