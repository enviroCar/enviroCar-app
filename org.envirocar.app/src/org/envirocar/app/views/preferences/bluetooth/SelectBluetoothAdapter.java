/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import org.envirocar.app.R;

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
            holder.mTextView = convertView.findViewById(R.id
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

