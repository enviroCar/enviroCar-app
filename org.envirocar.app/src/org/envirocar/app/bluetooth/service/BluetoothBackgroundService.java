package org.envirocar.app.bluetooth.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * @author dewall
 */
public class BluetoothBackgroundService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
