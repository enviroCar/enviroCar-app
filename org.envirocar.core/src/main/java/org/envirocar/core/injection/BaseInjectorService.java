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
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.ObjectGraph;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public abstract class BaseInjectorService extends Service implements Injector, InjectionModuleProvider {

    private ObjectGraph objectGraph;

    // Injected variables.
    @Inject
    protected Bus bus;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // extend the object graph of the application scope
        objectGraph = ((Injector) getApplicationContext())
                .getObjectGraph().plus(getInjectionModules().toArray());

        // Inject objects
        objectGraph.inject(this);
    }

    @Override
    public List<Object> getInjectionModules() {
        return new ArrayList<>();
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return objectGraph;
    }

    @Override
    public void injectObjects(Object instance) {
        Preconditions.checkNotNull(objectGraph,
                "Object graph has to be initialized before inejcting");
        objectGraph.inject(instance);
    }
}
