package org.envirocar.app.view.obdselection;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

/**
 * @author dewall
 */
public class OBDSelectedDeviceListAdapter extends OBDDeviceListAdapter{

    /**
     * Constructor.
     *
     * @param context The context of the current scope.
     */
    public OBDSelectedDeviceListAdapter(Context context){
        super(context, true);
    }

    /**
     *
     * @param device
     */
    public void setSelectedDevice(BluetoothDevice device){

    }
}
