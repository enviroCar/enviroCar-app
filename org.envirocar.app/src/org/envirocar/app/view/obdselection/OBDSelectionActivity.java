package org.envirocar.app.view.obdselection;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Switch;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.bluetooth.BluetoothHandler;
import org.envirocar.app.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.injection.BaseInjectorActivity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * @dewall
 */
public class OBDSelectionActivity extends BaseInjectorActivity implements
        OBDSelectionFragment.ShowSnackbarListener {
    private static final Logger LOGGER = Logger.getLogger(OBDSelectionActivity.class);

    @Inject
    protected BluetoothHandler mBluetoothHandler;

    @InjectView(R.id.activity_obd_selection_layout_toolbar)
    protected Toolbar mToolbar;
    @InjectView(R.id.activity_obd_selection_layout_enablebt_switch)
    protected Switch mSwitch;
    @InjectView(R.id.activity_obd_selection_layout_enablebt_text)
    protected TextView mEnableBTText;

    protected Fragment mOBDSelectionFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the content view of this activity.
        setContentView(R.layout.activity_obd_selection_layout);

        // Inject all annotated views.
        ButterKnife.inject(this);

        // Set the toolbar as default actionbar.
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("OBD-Device Selection");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Instantiate the fragment
        if (mOBDSelectionFragment == null)
            mOBDSelectionFragment = new OBDSelectionFragment();

        // And set the fragment in the layout container.
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.activity_obd_selection_layout_container, mOBDSelectionFragment)
                .commit();

        // Setup the bluetooth toolbar
        setupBluetoothSwitch();
    }

    private void setupBluetoothSwitch() {
        boolean isBTEnabled = mBluetoothHandler.isBluetoothEnabled();
        mEnableBTText.setText(isBTEnabled ? "Bluetooth On" : "Bluetooth Off");
        mSwitch.setChecked(mBluetoothHandler.isBluetoothEnabled());

        mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                //                mSwitch.setChecked(false);
                mBluetoothHandler.enableBluetooth(OBDSelectionActivity.this);
            } else {
                mBluetoothHandler.disableBluetooth(OBDSelectionActivity.this);
            }
        });
    }

    @Subscribe
    public void onReceiveBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        LOGGER.info(String.format("Received event: %s", event.toString()));

        if (event.isBluetoothEnabled) {
            mEnableBTText.setText("Bluetooth On");
            mSwitch.setChecked(true);
        } else {
            mEnableBTText.setText("Bluetooth Off");
            mSwitch.setChecked(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // click on the home button in the toolbar.
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Shows a snackbar with a given text.
     *
     * @param text the text to show in the snackbar.
     */
    @Override
    public void showSnackbar(String text) {
        Snackbar.make(mToolbar, text, Snackbar.LENGTH_LONG).show();
    }

    private void setSwitchState(float value) {
        try {
            Class[] args = {Float.TYPE};
            Method m = Switch.class.getDeclaredMethod("setThumbPosition", args);
            m.setAccessible(true);
            m.invoke(mSwitch, value);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
