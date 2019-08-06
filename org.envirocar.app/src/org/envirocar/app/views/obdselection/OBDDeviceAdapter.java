/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.envirocar.app.R;

import java.util.List;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.BindView;

/**
 * @author dewall
 */
public class OBDDeviceAdapter extends RecyclerView.Adapter<OBDDeviceAdapter.OBDViewHolder> {

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
    private Context context;
    private final OnOBDListActionCallback mCallback;

    private BluetoothDevice mSelectedBluetoothDevice;
    private AppCompatRadioButton mSelectedRadioButton;
    List<BluetoothDevice> pairedDevices;

    /**
     * Constructor.
     *
     * @param context    the context of the current scope.
     * @param pairedList is this a list showing the paired elements?
     */
    public OBDDeviceAdapter(Context context, boolean pairedList) {
        this.context = context;
        mIsPairedList = pairedList;
        mCallback = null;
    }

    /**
     * Constructor.
     *
     * @param context    The context of the current scope.
     * @param pairedList
     */
    public OBDDeviceAdapter(Context context, boolean pairedList, OnOBDListActionCallback
            callback) {
        this.context = context;
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
    public OBDDeviceAdapter(Context context, boolean pairedList, OnOBDListActionCallback
            callback, BluetoothDevice defaultDevice) {
        this(context, pairedList, callback);
        mSelectedBluetoothDevice = defaultDevice;
    }

    public void addAll(List<BluetoothDevice> pairedDevices){
        this.pairedDevices = pairedDevices;
    }

    public void add(BluetoothDevice device){
        pairedDevices.add(device);
        notifyDataSetChanged();
    }

    public void remove(BluetoothDevice device){
        pairedDevices.remove(device);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OBDViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_obd_selection_layout_paired_list_entry, parent, false);

        return new OBDViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull OBDViewHolder holder, int position) {
        final BluetoothDevice device = pairedDevices.get(position);
        holder.mTextView.setText(device.getName());
        holder.mRadioButton.setChecked(false);

        // If there exists an already selected bluetooth device and the device of this entry
        // matches the selected device, then set it to checked.
        if (mSelectedBluetoothDevice != null) {
            if (mSelectedBluetoothDevice.getName().equals(device.getName())) {
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
                notifyDataSetChanged();
            }

            mSelectedRadioButton = holder.mRadioButton;
            mSelectedRadioButton.setChecked(true);
            mSelectedBluetoothDevice = device;

            // Callback.
            mCallback.onOBDDeviceSelected(device);
        });

        holder.obdSelectionLayout.setOnClickListener((parent, view1, id) -> {

            // Set toolbar style
            Toolbar toolbar1 = contentView.findViewById(R.id
                    .bluetooth_selection_preference_pairing_dialog_toolbar);
            toolbar1.setTitle(R.string.bluetooth_pairing_preference_toolbar_title);
            toolbar1.setNavigationIcon(R.drawable.ic_bluetooth_white_24dp);
            toolbar1.setTitleTextColor(getActivity().getResources().getColor(R.color
                    .white_cario));

            // Set text view
            TextView textview = contentView.findViewById(R.id
                    .bluetooth_selection_preference_pairing_dialog_text);
            textview.setText(String.format(getString(
                    R.string.obd_selection_dialog_pairing_content_template), device.getName()));

            // Create the Dialog
            new AlertDialog.Builder(getActivity())
                    .setView(contentView)
                    .setPositiveButton(R.string.obd_selection_dialog_pairing_title,
                            (dialog, which) -> {
                                // If this button is clicked, pair with the given device
                                view1.setClickable(false);
                                pairDevice(device, view1);
                            })
                    .setNegativeButton(R.string.cancel, null) // Nothing to do on cancel
                    .create()
                    .show();
        });
    }

    public Boolean isEmpty(){
        return pairedDevices.isEmpty();
    }

    public void clear(){
        pairedDevices.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return pairedDevices.size();
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
        notifyDataSetChanged();
    }

    /**
     * Checks if a given device is already in this adapter.
     *
     * @param device the device to check if it is in the adapter.
     * @return true if the device is in this adapter.
     */
    public boolean contains(BluetoothDevice device) {
        for (int i = 0, size = pairedDevices.size(); i != size; i++) {
            if (pairedDevices.get(i).equals(device))
                return true;
        }
        return false;
    }

    /**
     * View holder class holding all views of a single row of the adapter.
     */
    static class OBDViewHolder extends RecyclerView.ViewHolder {

        public final View mContentView;

        // All the views of a row to lookup for.
        @BindView(R.id.obd_selection_layout)
        protected LinearLayout obdSelectionLayout;
        @BindView(R.id.activity_obd_selection_layout_paired_list_entry_image)
        protected ImageView mImageView;
        @BindView(R.id.activity_obd_selection_layout_paired_list_entry_text)
        protected TextView mTextView;
        @BindView(R.id.activity_obd_selection_layout_paired_list_entry_delete)
        protected ImageButton mDeleteButton;
        @BindView(R.id.activity_obd_selection_layout_paired_list_entry_radio)
        protected AppCompatRadioButton mRadioButton;

        /**
         * Constructor.
         *
         * @param content the parent view of the listrow.
         */
        OBDViewHolder(View content) {
            super(content);
            this.mContentView = content;
            // Inject the annotated views.
            ButterKnife.bind(this, content);
        }
    }
}
