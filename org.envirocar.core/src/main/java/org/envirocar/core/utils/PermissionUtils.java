package org.envirocar.core.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import io.reactivex.Completable;
import io.reactivex.functions.Function;

/**
 * @author dewall
 */
public class PermissionUtils {

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 89;

    public static boolean hasLocationPermission(Context Context) {
        return ActivityCompat.checkSelfPermission(Context, Manifest.permission.ACCESS_FINE_LOCATION)
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
