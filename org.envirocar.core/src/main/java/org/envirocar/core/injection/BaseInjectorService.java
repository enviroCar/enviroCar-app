/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
