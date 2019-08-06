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

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.envirocar.app.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.BindView;

/**
 * @author dewall
 */
public class OBDDeviceAdapter extends RecyclerView.Adapter<OBDDeviceAdapter.OBDViewHolder> {

    private final String SELECTED_COLOR = "#0166A0";
    private final String UNSELECTED_COLOR = "#757575";

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

        void createDialog(BluetoothDevice device, View view);
    }

    private final boolean mIsPairedList;
    private Context context;
    private final OnOBDListActionCallback mCallback;

    private BluetoothDevice mSelectedBluetoothDevice;
    private ImageView mSelectedImageView;
    List<BluetoothDevice> pairedDevices = new ArrayList<>();

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
        holder.mTextViewAdd.setText(device.getAddress());
        holder.mImageView.setImageTintList(ColorStateList.valueOf(Color.parseColor(UNSELECTED_COLOR)));

        // If there exists an already selected bluetooth device and the device of this entry
        // matches the selected device, then set it to checked.
        if (mSelectedBluetoothDevice != null) {
            if (mSelectedBluetoothDevice.getName().equals(device.getName())) {
                mSelectedImageView = holder.mImageView;
                mSelectedImageView.setImageTintList(ColorStateList.valueOf(Color.parseColor(SELECTED_COLOR)));
                mSelectedBluetoothDevice = device;
            }
        }

        holder.obdSelectionLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mIsPairedList)
                    mCallback.createDialog(device, holder.mContentView);
                else{
                    if (mSelectedBluetoothDevice != null && mSelectedBluetoothDevice.getAddress()
                            .equals(device.getAddress()))
                        return;

                    if (mSelectedImageView != null) {
                        mSelectedImageView.setImageTintList(ColorStateList.valueOf(Color.parseColor(SELECTED_COLOR)));
                        // Bug. This needs to happen.. dont know why exactly.
                        notifyDataSetChanged();
                    }

                    mSelectedImageView = holder.mImageView;
                    mSelectedImageView.setImageTintList(ColorStateList.valueOf(Color.parseColor(SELECTED_COLOR)));
                    mSelectedBluetoothDevice = device;

                    // Callback.
                    mCallback.onOBDDeviceSelected(device);
                }
            }
        });

        holder.obdSelectionLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(mIsPairedList)
                    mCallback.onDeleteOBDDevice(device);
                return false;
            }
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
        if(mSelectedImageView != null){
            mSelectedImageView.setImageTintList(ColorStateList.valueOf(Color.parseColor("#757575")));
            mSelectedImageView = null;
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
        protected ConstraintLayout obdSelectionLayout;
        @BindView(R.id.activity_obd_selection_layout_paired_list_entry_image)
        protected ImageView mImageView;
        @BindView(R.id.activity_obd_selection_layout_paired_list_entry_text)
        protected TextView mTextView;
        @BindView(R.id.activity_obd_selection_layout_paired_list_entry_text_add)
        protected TextView mTextViewAdd;

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
