/**
 * Copyright (C) 2013 - 2026 the enviroCar community
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

package org.envirocar.core.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import io.reactivex.Completable;
import io.reactivex.functions.Function;

/**
 * @author dewall
 */
public class PermissionUtils {

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 89;
    public static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 90;

    /**
     * Whether the app is allowed to post notifications. Always {@code true} below
     * Android 13 (Tiramisu), where {@code POST_NOTIFICATIONS} did not yet exist as
     * a runtime permission.
     *
     * @param context the context to check against.
     * @return {@code true} if notifications may be posted.
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests the {@code POST_NOTIFICATIONS} runtime permission on Android 13+ if
     * it has not been granted yet. Recording and upload rely on foreground-service
     * notifications, which are suppressed when this permission is denied. No-op on
     * older versions.
     *
     * @param activity the activity used to request the permission.
     */
    public static void requestNotificationPermissionIfRequired(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !hasNotificationPermission(activity)) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
        }
    }


    public static boolean hasLocationPermission(Context Context) {
        return ActivityCompat.checkSelfPermission(Context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(Context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    public static Completable requestLocationPermissionIfRequired(Activity activity) {
        return Completable.create(emitter -> {
            if (!hasLocationPermission(activity)) {
                requestLocationPermission(activity);
                emitter.onComplete();
            }
        });
    }

    public static Completable requestLocationPermissionIfRequired(Activity activity, Function<Function<Void, Void>, Void> dialogCallback) {
        return Completable.create(emitter -> {
            if (!hasLocationPermission(activity)) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    dialogCallback.apply((voaid) -> {
                        requestLocationPermission(activity);
                        emitter.onComplete();
                        return null;
                    });
                } else {
                    requestLocationPermission(activity);
                    emitter.onComplete();
                }
            }
        });
    }

    private static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

}
