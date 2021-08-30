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

import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Switch;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.logging.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @dewall
 */
public class OBDSelectionActivity extends BaseInjectorActivity implements
        OBDSelectionFragment.ShowSnackbarListener {
    private static final Logger LOGGER = Logger.getLogger(OBDSelectionActivity.class);

    @Inject
    protected BluetoothHandler mBluetoothHandler;

    @BindView(R.id.activity_obd_selection_layout_toolbar)
    protected Toolbar mToolbar;
    @BindView(R.id.activity_obd_selection_layout_enablebt_switch)
    protected Switch mSwitch;
    @BindView(R.id.activity_obd_selection_layout_enablebt_text)
    protected TextView mEnableBTText;

    protected Fragment mOBDSelectionFragment;


    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the content view of this activity.
        setContentView(R.layout.activity_obd_selection_layout);

        // Inject all annotated views.
        ButterKnife.bind(this);

        // Set the toolbar as default actionbar.
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.obd_selection_title);
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
        mEnableBTText.setText(isBTEnabled ?
                R.string.obd_selection_bluetooth_on :
                R.string.obd_selection_bluetooth_off);

        mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                //  If Bluetooth is allowed then mSwitch checked
                mBluetoothHandler.enableBluetooth(OBDSelectionActivity.this);
                mSwitch.setChecked(true);
            } else {
                mBluetoothHandler.disableBluetooth(OBDSelectionActivity.this);
                mSwitch.setChecked(false);
            }
        });
    }

    @Subscribe
    public void onReceiveBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        LOGGER.info(String.format("Received event: %s", event.toString()));

        if (event.isBluetoothEnabled) {
            mEnableBTText.setText(R.string.obd_selection_bluetooth_on);
            mSwitch.setChecked(true);
        } else {
            mEnableBTText.setText(R.string.obd_selection_bluetooth_off);
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
