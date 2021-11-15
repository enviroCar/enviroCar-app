/**
 * Copyright (C) 2013 - 2021 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.preferences.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.envirocar.app.R;

/**
 * @author dewall
 */
public class BluetoothDeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

//    private final BluetoothDeviceListButtonListener mListener;

    /**
     * Constructor.
     *
     * @param context  The context of the current scope.
     * @param resource The resource
     */
    public BluetoothDeviceListAdapter(Context context, int resource, boolean pairedList) {
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
                    .bluetooth_pairing_preference_devices_list_entry, parent, false);
        }

        // Lookup views
        ImageView imageVied = convertView.findViewById(R.id
                .bluetooth_selection_preference_device_list_entry_image);
        TextView labelView = convertView.findViewById(R.id
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
