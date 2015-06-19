package org.envirocar.app.injection;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.List;

import dagger.ObjectGraph;

/**
 * Created by Peter on 17.06.2015.
 */
public class BaseInjectionService extends Service implements Injector, InjectionModuleProvider {
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
