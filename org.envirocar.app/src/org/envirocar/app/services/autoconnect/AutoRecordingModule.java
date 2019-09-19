package org.envirocar.app.services.autoconnect;

import com.squareup.otto.Bus;

import org.envirocar.app.handler.BluetoothHandler;

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
    public AutoRecordingStrategy.Factory provideAutoRecordingFactory(Bus eventBus, BluetoothHandler bluetoothHandler) {
        return new AutoRecordingStrategy.Factory() {
            @Override
            public AutoRecordingStrategy create() {
                return new OBDAutoRecordingStrategy(service, eventBus, bluetoothHandler);
            }
        };
    }
}
