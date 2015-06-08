package org.envirocar.app.bluetooth.service;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import org.envirocar.app.Injector;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by Peter on 08.06.2015.
 */
public class BluetoothHandler {

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothConstants.MESSAGE_WRITE:

                    break;
                case BluetoothConstants.MESSAGE_READ:

                    break;
                case BluetoothConstants.MESSAGE_DEVICE_NAME:

                    break;
                case BluetoothConstants.MESSAGE_STATE_CHANGE:

                    break;
                default:
                    break;
            }

            super.handleMessage(msg);
        }
    };
    private final List<BluetoothConnectionListener> mConnectionListener = new
            ArrayList<BluetoothConnectionListener>();
    @Inject
    protected Context mContext;

    private boolean mIsAutoconnecting;

    public BluetoothHandler(Context context) {
        ((Injector) context).injectObjects(this);
    }

    public boolean isAutoconnecting() {
        return mIsAutoconnecting;
    }

    public void addBluetoothConnectionListener(BluetoothConnectionListener listener) {
        if (!mConnectionListener.contains(listener)) {
            mConnectionListener.add(listener);
        }
    }

    public void removeBluetoothConnectionListener(BluetoothConnectionListener listener) {
        if (mConnectionListener.contains(listener)) {
            mConnectionListener.remove(listener);
        }
    }

    public interface BluetoothConnectionListener {
        void onDeviceConnected(String deviceName, String deviceAddress);
        void onDeviceDisconnected(String deviceName, String deviceAddress);
        void onConnectionFailure(String deviceName, String deviceAddress);
    }


}
