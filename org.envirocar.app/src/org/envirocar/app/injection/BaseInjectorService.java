/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.injection;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleService;

import com.squareup.otto.Bus;

import org.envirocar.app.main.BaseApplication;
import org.envirocar.app.main.BaseApplicationComponent;

import javax.inject.Inject;


/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public abstract class BaseInjectorService extends LifecycleService {

    // Injected variables.
    @Inject
    protected Bus bus;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        injectDependencies(getBaseApplicationComponent());
    }


    /**
     * Gets the BaseApplicationComponent for injection.
     *
     * @return
     */
    protected BaseApplicationComponent getBaseApplicationComponent() {
        return BaseApplication.get(this).getBaseApplicationComponent();
    }

    /**
     * Gets the NotificationManager
     *
     * @return returns the NotificationManager
     */
    protected NotificationManager getNotificationManager() {
        return (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
    }

    /**
     * Abstract method for injecting dependencies.
     *
     * @param baseApplicationComponent
     */
    protected abstract void injectDependencies(BaseApplicationComponent baseApplicationComponent);

}
