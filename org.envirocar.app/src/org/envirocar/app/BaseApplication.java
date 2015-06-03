package org.envirocar.app;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.google.common.base.Preconditions;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.logging.ACRACustomSender;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.util.Util;

import java.util.Arrays;
import java.util.List;

import dagger.ObjectGraph;

/**
 * @author dewall
 */
@ReportsCrashes
public class BaseApplication extends Application implements Injector, InjectionModuleProvider {
    private static final String TAG = BaseApplication.class.getSimpleName();
    private static final Logger LOGGER = Logger.getLogger(BaseApplication.class);

    protected ObjectGraph mObjectGraph;

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener
            = new SharedPreferences.OnSharedPreferenceChangeListener() {


        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                              String key) {
            if (SettingsActivity.ENABLE_DEBUG_LOGGING.equals(key)) {
                Logger.initialize(Util.getVersionString(BaseApplication.this),
                        sharedPreferences.getBoolean(SettingsActivity.ENABLE_DEBUG_LOGGING, false));
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // create initial ObjectGraph
        mObjectGraph = ObjectGraph.create(getInjectionModules().toArray());
        mObjectGraph.validate();

        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener);

        // Initialize ACRA
        ACRA.init(this);
        ACRACustomSender yourSender = new ACRACustomSender();
        ACRA.getErrorReporter().setReportSender(yourSender);
        ACRA.getConfig().setExcludeMatchingSharedPreferencesKeys(SettingsActivity
                .resolveIndividualKeys());
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        LOGGER.info("onLowMemory called");
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        LOGGER.info("onTrimMemory called");
        LOGGER.info("maxMemory: " + Runtime.getRuntime().maxMemory());
        LOGGER.info("totalMemory: " + Runtime.getRuntime().totalMemory());
        LOGGER.info("freeMemory: " + Runtime.getRuntime().freeMemory());
    }


    @Override
    public ObjectGraph getObjectGraph() {
        return mObjectGraph;
    }

    @Override
    public List<Object> getInjectionModules() {
        return Arrays.<Object>asList(new InjectionApplicationModule(this));
    }

    @Override
    public void injectObjects(Object instance) {
        Preconditions.checkNotNull(instance, "Cannot inject into Null objects.");
        Preconditions.checkNotNull(mObjectGraph, "The ObjectGraph must be initialized before use.");
        mObjectGraph.inject(instance);
    }
}
