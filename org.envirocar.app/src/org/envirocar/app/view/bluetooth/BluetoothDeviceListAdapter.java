package org.envirocar.app.view.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.envirocar.app.R;

/**
 * @author dewall
 */
public class BluetoothDeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

    /**
     *
     */
    public interface BluetoothDeviceListButtonListener{
        void onButtonClicked(BluetoothDevice device);
    }

//    private final BluetoothDeviceListButtonListener mListener;

    /**
     * Constructor.
     *
     * @param context  The context of the current scope.
     * @param resource The resource
     */
    public BluetoothDeviceListAdapter(Context context, int resource, boolean pairedList,
                                      BluetoothDeviceListButtonListener listener) {
        super(context, resource);

        // Set the listener
//        this.mListener = listener;

        // TODO first integrate preference fragment, then include this
        // Inject ourself.
//        ((Injector) context).injectObjects(this);


    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Get the item from the given poosition
        final BluetoothDevice device = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout
                    .bluetooth_selection_preference_devices_list_entry, parent, false);
        }

        // Lookup views
        ImageView imageVied = (ImageView) convertView.findViewById(R.id
                .bluetooth_selection_preference_device_list_entry_image);
        TextView labelView = (TextView) convertView.findViewById(R.id
                .bluetooth_selection_preference_device_list_entry_text);
//        ImageButton imageButton = (ImageButton) convertView.findViewById(R.id
//                .bluetooth_selection_preference_device_list_entry_button);

        labelView.setText(device.getName());

//        imageButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                mListener.onButtonClicked(device);
//            }
//        });

        return convertView;
    }

    @Override
    public BluetoothDevice getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public void add(BluetoothDevice object) {
        super.add(object);
        notifyDataSetChanged();
    }

    @Override
    public void remove(BluetoothDevice object) {
        super.remove(object);
        notifyDataSetChanged();
    }

    /**
     * Checks whether this ArrayAdapter already contains an given {@link BluetoothDevice}.
     *
     * @param object the given BluetoothObject
     * @return returns true if this array adapter contains the object.
     */
    public boolean contains(BluetoothDevice object) {
        return getPosition(object) >= 0;
    }
}
