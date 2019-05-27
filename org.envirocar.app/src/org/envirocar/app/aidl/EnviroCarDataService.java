package org.envirocar.app.aidl;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

/**
 * @author dewall
 */
public class EnviroCarDataService extends Service {

    private int speed = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private final IEnviroCarDataService.Stub binder = new IEnviroCarDataService.Stub() {

        @Override
        public int getSpeed() throws RemoteException {
            speed += 1;
            return speed;
        }
    };
}
