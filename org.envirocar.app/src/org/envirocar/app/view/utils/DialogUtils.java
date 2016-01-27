package org.envirocar.app.view.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class DialogUtils {

    public static MaterialDialog.Builder createDefaultDialogBuilder(
            Context context, int titleRes, int iconRes, View contentView) {
        return createDefaultDialogBuilder(context, context.getString(titleRes), iconRes,
                contentView);
    }

    public static MaterialDialog.Builder createDefaultDialogBuilder(
            Context context, int titleRes, int iconRes, int contentRes) {
        return createDefaultDialogBuilder(context, context.getString(titleRes), iconRes, context
                .getString(contentRes));
    }

    public static MaterialDialog.Builder createDefaultDialogBuilder(
            Context context, int titleRes, int iconRes, String content) {
        return createDefaultDialogBuilder(context, context.getString(titleRes), iconRes, content);
    }

    public static MaterialDialog.Builder createDefaultDialogBuilder(
            Context context, String title, int iconRes, String content) {
        TextView textView = new TextView(context);
        textView.setText(content);
        return createDefaultDialogBuilder(context, title, iconRes, textView);
    }

    public static MaterialDialog.Builder createDefaultDialogBuilder(
            Context context, String title, int iconRes, View customView) {
        View layout = LayoutInflater.from(context)
                .inflate(R.layout.general_dialog_view, null, false);

        TextView titleView = (TextView) layout.findViewById(R.id.general_dialog_view_title);
        ImageView imageView = (ImageView) layout.findViewById(R.id.general_dialog_view_icon);
        FrameLayout contentView = (FrameLayout) layout.findViewById(R.id
                .general_dialog_view_content);

        titleView.setText(title);
        imageView.setImageResource(iconRes);
        contentView.addView(customView);

        return new MaterialDialog.Builder(context)
                .customView(layout, false);
    }


}
