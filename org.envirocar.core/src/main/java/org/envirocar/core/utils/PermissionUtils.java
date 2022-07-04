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
package org.envirocar.core.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.functions.Function;

/**
 * @author dewall
 */
public class PermissionUtils {

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 89;
    
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
