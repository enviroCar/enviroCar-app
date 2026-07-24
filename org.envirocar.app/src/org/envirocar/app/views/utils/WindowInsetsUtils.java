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

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Helper for applying window insets on screens that do not already handle them
 * through {@code android:fitsSystemWindows}. Apps are displayed edge-to-edge
 * (enforced for apps targeting API 35+, and no longer opt-out-able on API 36),
 * so without this the content would be drawn underneath the status and
 * navigation bars.
 *
 * @author enviroCar
 */
public final class WindowInsetsUtils {

    private WindowInsetsUtils() {
    }

    /**
     * Applies the system bar insets (status + navigation bars) as padding to the
     * root content view of the given activity.
     *
     * @param activity the activity whose content view should be inset.
     */
    public static void applySystemBarInsetsAsPadding(@NonNull Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (content != null) {
            applySystemBarInsetsAsPadding(content);
        }
    }

    /**
     * Applies the system bar insets (status + navigation bars) as padding to the
     * given view, preserving any padding already set on it.
     *
     * @param view the view to inset.
     */
    public static void applySystemBarInsetsAsPadding(@NonNull View view) {
        final int paddingLeft = view.getPaddingLeft();
        final int paddingTop = view.getPaddingTop();
        final int paddingRight = view.getPaddingRight();
        final int paddingBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    paddingLeft + bars.left,
                    paddingTop + bars.top,
                    paddingRight + bars.right,
                    paddingBottom + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    /**
     * Applies only the bottom system bar (navigation bar) inset as padding to the
     * given view, preserving any padding already set. Useful for a full-width bar
     * anchored to the bottom of the screen whose background should still extend
     * behind the navigation bar.
     *
     * @param view the view to inset.
     */
    public static void applyBottomSystemBarInsetAsPadding(@NonNull View view) {
        final int paddingLeft = view.getPaddingLeft();
        final int paddingTop = view.getPaddingTop();
        final int paddingRight = view.getPaddingRight();
        final int paddingBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    /**
     * Applies the system bar insets (status + navigation bars) as margins to the
     * given view, preserving any margins already set. Useful for controls that
     * overlay a full-bleed surface (e.g. a map) which itself should draw behind
     * the system bars.
     *
     * @param view the view to inset; must use {@link ViewGroup.MarginLayoutParams}.
     */
    public static void applySystemBarInsetsAsMargin(@NonNull View view) {
        if (!(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams initial = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        final int leftMargin = initial.leftMargin;
        final int topMargin = initial.topMargin;
        final int rightMargin = initial.rightMargin;
        final int bottomMargin = initial.bottomMargin;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.leftMargin = leftMargin + bars.left;
            params.topMargin = topMargin + bars.top;
            params.rightMargin = rightMargin + bars.right;
            params.bottomMargin = bottomMargin + bars.bottom;
            v.setLayoutParams(params);
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
