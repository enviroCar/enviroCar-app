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
