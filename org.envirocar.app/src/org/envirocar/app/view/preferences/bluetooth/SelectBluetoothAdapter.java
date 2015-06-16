package org.envirocar.app.view.preferences.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import org.envirocar.app.R;

import java.util.Collection;

/**
 * @author dewall
 */
public class SelectBluetoothAdapter extends ArrayAdapter<BluetoothDevice>{

    private Context mContext;
    private int mLayoutResource;
    private BluetoothDevice[] mData;

    /**
     * Constructor.
     *
     * @param context   the context of the current scope.
     * @param resource  the resource id of the layout of a single row.
     * @param devices   the array of bluetooth devices to show entries for.
     */
    public SelectBluetoothAdapter(Context context, int resource, BluetoothDevice[] devices) {
        super(context, resource, devices);
        this.mContext = context;
        this.mLayoutResource = resource;
        this.mData = devices;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BluetoothDeviceHolder holder = null;

        if(convertView == null){
            // First time inflating the view.
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(mLayoutResource, parent, false);

            // Create a holder and set the checked textview accordingly.
            holder = new BluetoothDeviceHolder();
            holder.mTextView = (CheckedTextView) convertView.findViewById(R.id
                    .bluetooth_selection_preference_list_entry_text);

            // Add the tag to the holder class.
            convertView.setTag(holder);
        } else {
            // Get the holder view from the previous inflated view.
            holder = (BluetoothDeviceHolder) convertView.getTag();
        }

        BluetoothDevice device = mData[position];
        holder.mTextView.setText(device.getName() + " (" + device.getAddress() + ")");

        return convertView;
    }



    /**
     * Holder class holding the relevant views.
     */
    private static class BluetoothDeviceHolder {
        CheckedTextView mTextView;
    }
}

