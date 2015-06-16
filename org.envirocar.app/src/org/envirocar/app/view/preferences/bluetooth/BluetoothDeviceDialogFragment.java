package org.envirocar.app.view.preferences.bluetooth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;

import org.envirocar.app.R;

/**
 * @author dewall
 */
public class BluetoothDeviceDialogFragment extends DialogFragment{
    private static final String TAG_DEVICE = "device";

    /**
     * Creates a new instance of BluetoothDeviceDialogFragmnet, providing "device"
     * as an argument.
     *
     * @param device    the device to show the properties to.
     * @return          the DialogFragment of the device.
     */
    public static BluetoothDeviceDialogFragment newInstance(BluetoothDevice device){
        BluetoothDeviceDialogFragment fragment = new BluetoothDeviceDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(TAG_DEVICE, device);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BluetoothDevice device = getArguments().getParcelable(TAG_DEVICE);

//        View v = inflater.inflate(R.layout.);


        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_laptop)
                .setTitle(device.getName())
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Unpair", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .create();
    }
}
