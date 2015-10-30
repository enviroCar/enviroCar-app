package org.envirocar.core.injection;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.squareup.otto.Bus;

import java.util.List;

import javax.inject.Inject;

import dagger.ObjectGraph;

/**
 *
 * @author dewall
 */
public abstract class BaseInjectorService extends Service implements Injector, InjectionModuleProvider {

    private ObjectGraph objectGraph;

    // Injected variables.
    @Inject
    protected Bus bus;

    @Override
    public void onCreate() {
        super.onCreate();

        objectGraph = ((Injector) getApplicationContext()).getObjectGraph().plus
                (getInjectionModules().toArray());
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public List<Object> getInjectionModules() {
        return null;
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return null;
    }

    @Override
    public void injectObjects(Object instance) {

    }
}
