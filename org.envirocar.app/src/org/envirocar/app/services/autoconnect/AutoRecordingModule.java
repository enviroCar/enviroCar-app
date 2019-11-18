/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.app.services.autoconnect;

import android.bluetooth.BluetoothDevice;

import com.squareup.otto.Bus;

import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.LocationHandler;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;

import dagger.Module;
import dagger.Provides;

@Module
public class AutoRecordingModule {

    private final AutoRecordingService service;

    public AutoRecordingModule(AutoRecordingService service) {
        this.service = service;
    }

    @Provides
    @AutoRecordingScope
    public AutoRecordingStrategy.Factory provideAutoRecordingFactory(Bus eventBus, BluetoothHandler bluetoothHandler, CarPreferenceHandler carPreferenceHandler, LocationHandler locationHandler) {
        return new AutoRecordingStrategy.Factory() {
            @Override
            public AutoRecordingStrategy create() {
                return new OBDAutoRecordingStrategy(service, eventBus, bluetoothHandler, carPreferenceHandler, locationHandler);
            }
        };
    }

//    @Provides
//    @AutoRecordingScope
//    public PowerManager.WakeLock provideWakeLock() {
//        PowerManager powerManager = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
//        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AutoRecordingService::lock");
//        return wakeLock;
//    }

    @Provides
    @AutoRecordingScope
    public AutoRecordingNotification provideAutoRecordingNotification(Bus eventBus, BluetoothHandler bluetoothHandler) {
        return new AutoRecordingNotification(service, eventBus, bluetoothHandler);
    }
}
