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
package org.envirocar.app.views.obdselection;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.envirocar.app.R;
import org.envirocar.app.databinding.ActivityObdSelectionLayoutPairedListEntryBinding;

/**
 * @author dewall
 */
public class OBDDeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

    /**
     * callback interface for the selection callback.
     */
    protected interface OnOBDListActionCallback {
        /**
         * Called when a device has been selected as OBD device.
         *
         * @param device the selected bluetooth device.
         */
        void onOBDDeviceSelected(BluetoothDevice device);

        /**
         * Called when a device should be deleted.
         *
         * @param device the device to delete.
         */
        void onDeleteOBDDevice(BluetoothDevice device);
    }

    private final boolean mIsPairedList;
    private final OnOBDListActionCallback mCallback;

    private BluetoothDevice mSelectedBluetoothDevice;
    private AppCompatRadioButton mSelectedRadioButton;

    /**
     * Constructor.
     *
     * @param context    the context of the current scope.
     * @param pairedList is this a list showing the paired elements?
     */
    public OBDDeviceListAdapter(Context context, boolean pairedList) {
        super(context, -1);
        mIsPairedList = pairedList;
        mCallback = null;
    }

    /**
     * Constructor.
     *
     * @param context    The context of the current scope.
     * @param pairedList
     */
    public OBDDeviceListAdapter(Context context, boolean pairedList, OnOBDListActionCallback
            callback) {
        super(context, -1);
        mIsPairedList = pairedList;
        mCallback = callback;
    }

    /**
     * Constructor.
     *
     * @param context       The context of the current scope.
     * @param pairedList    true if this should show a radio button.
     * @param defaultDevice The selected
     */
    public OBDDeviceListAdapter(Context context, boolean pairedList, OnOBDListActionCallback
            callback, BluetoothDevice defaultDevice) {
        this(context, pairedList, callback);
        mSelectedBluetoothDevice = defaultDevice;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the item from the given position
        final BluetoothDevice device = getItem(position);

        ViewHolder holder;
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            final ActivityObdSelectionLayoutPairedListEntryBinding binding = ActivityObdSelectionLayoutPairedListEntryBinding
                    .inflate(LayoutInflater.from(getContext()), parent, false);
            convertView = binding.getRoot();
            holder = new ViewHolder(binding);

            // if this list is used for a paired list then we show an addition radio button for
            // the selection
            if (mIsPairedList) {
                holder.mRadioButton.setVisibility(View.VISIBLE);
                holder.mDeleteButton.setVisibility(View.VISIBLE);
            }

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            holder.mTextView.setText(device.getName());
        }
        holder.mRadioButton.setChecked(false);

        // If there exists an already selected bluetooth device and the device of this entry
        // matches the selected device, then set it to checked.
        if (mSelectedBluetoothDevice != null) {
            if (mSelectedBluetoothDevice.getAddress().equals(device.getAddress())) {
                mSelectedRadioButton = holder.mRadioButton;
                mSelectedRadioButton.setChecked(true);
                mSelectedBluetoothDevice = device;
            }
        }

        holder.mDeleteButton.setOnClickListener(v -> mCallback.onDeleteOBDDevice(device));

        // Set the radiobutton on click listener.
        holder.mRadioButton.setOnClickListener(v -> {
            // When the clicked radio button corresponds to the bluetooth device that is
            // already selected, then do nothing.
            if (mSelectedBluetoothDevice != null && mSelectedBluetoothDevice.getAddress()
                    .equals(device.getAddress()))
                return;

            if (mSelectedRadioButton != null) {
                mSelectedRadioButton.setChecked(false);
                // Bug. This needs to happen.. dont know why exactly.
                notifyDataSetInvalidated();
            }

            mSelectedRadioButton = holder.mRadioButton;
            mSelectedRadioButton.setChecked(true);
            mSelectedBluetoothDevice = device;

            // Callback.
            mCallback.onOBDDeviceSelected(device);
        });

        return convertView;
    }

    @Override
    public BluetoothDevice getItem(int position) {
        return super.getItem(position);
    }

    /**
     * Updates the selected Bluetooth device and checks the new one.
     *
     * @param device the selected Bluetooth device to check.
     */
    public void setSelectedBluetoothDevice(BluetoothDevice device) {
        // If this is not a list of paired devices or the input device is null, then return.
        if(device == null || !mIsPairedList)
            return;

        // If there is any other bluetooth device selected, then uncheck it first...
        if(mSelectedRadioButton != null){
            mSelectedRadioButton.setChecked(false);
            mSelectedRadioButton = null;
            mSelectedBluetoothDevice = null;
        }

        // and set the new bluetooth device.
        mSelectedBluetoothDevice = device;
        notifyDataSetInvalidated();
    }

    /**
     * Checks if a given device is already in this adapter.
     *
     * @param device the device to check if it is in the adapter.
     * @return true if the device is in this adapter.
     */
    public boolean contains(BluetoothDevice device) {
        for (int i = 0, size = getCount(); i != size; i++) {
            if (getItem(i).equals(device))
                return true;
        }
        return false;
    }

    /**
     * View holder class holding all views of a single row of the adapter.
     */
    static class ViewHolder {

        public final View mContentView;

        protected ImageView mImageView;
        protected TextView mTextView;
        protected ImageButton mDeleteButton;
        protected AppCompatRadioButton mRadioButton;

        /**
         * Constructor.
         *
         * @param binding the binding of the layout.
         */
        ViewHolder(ActivityObdSelectionLayoutPairedListEntryBinding binding) {
            mContentView = binding.getRoot();
            mImageView = binding.activityObdSelectionLayoutPairedListEntryImage;
            mTextView = binding.activityObdSelectionLayoutPairedListEntryText;
            mDeleteButton = binding.activityObdSelectionLayoutPairedListEntryDelete;
            mRadioButton = binding.activityObdSelectionLayoutPairedListEntryRadio;
        }
    }
}
