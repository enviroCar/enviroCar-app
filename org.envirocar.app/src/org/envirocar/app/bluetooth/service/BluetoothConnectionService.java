package org.envirocar.app.bluetooth.service;

import android.content.Context;

import org.envirocar.core.logging.Handler;


/**
 * @author dewall
 */
public class BluetoothConnectionService {
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private final Context mContext;
    private final Handler mHandler;

    private int mState;

//    private ConnectThread mConnectThread;
//    private ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context context, Handler handler){
        this.mContext = context;
        this.mHandler = handler;
        this.mState = STATE_NONE;
    }

    public synchronized int getState(){
        return mState;
    }

    public synchronized void startBluetooth(){

//        if(mConnectThread != null)
    }
}
