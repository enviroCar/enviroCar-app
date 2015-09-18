package org.envirocar.app.view.obdselection;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.envirocar.app.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

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
        // Get the item from the given poosition
        final BluetoothDevice device = getItem(position);

        ViewHolder holder;
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout
                    .activity_obd_selection_layout_paired_list_entry, parent, false);
            holder = new ViewHolder(convertView);

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

        // All the views of a row to lookup for.
        @InjectView(R.id.activity_obd_selection_layout_paired_list_entry_image)
        protected ImageView mImageView;
        @InjectView(R.id.activity_obd_selection_layout_paired_list_entry_text)
        protected TextView mTextView;
        @InjectView(R.id.activity_obd_selection_layout_paired_list_entry_delete)
        protected ImageButton mDeleteButton;
        @InjectView(R.id.activity_obd_selection_layout_paired_list_entry_radio)
        protected AppCompatRadioButton mRadioButton;

        /**
         * Constructor.
         *
         * @param content the parent view of the listrow.
         */
        ViewHolder(View content) {
            this.mContentView = content;
            // Inject the annotated views.
            ButterKnife.inject(this, content);
        }
    }
}
