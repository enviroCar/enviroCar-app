package org.envirocar.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.app.application.CarPreferenceHandler;
import org.envirocar.app.bluetooth.event.BluetoothDeviceSelectedEvent;
import org.envirocar.app.events.NewCarTypeSelectedEvent;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.view.carselection.CarSelectionActivity;
import org.envirocar.app.view.obdselection.OBDSelectionActivity;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * @author dewall
 */
public class StartupFragment extends BaseInjectorFragment {
    private static final Logger LOGGER = Logger.getLogger(StartupFragment.class);

    @Inject
    protected CarPreferenceHandler mCarManager;

    @InjectView(R.id.fragment_startup_obd_selection)
    protected View mOBDTypeView;

    @InjectView(R.id.fragment_startup_car_selection)
    protected View mCarTypeView;
    @InjectView(R.id.fragment_startup_car_selection_text)
    protected TextView mCarTypeTextView;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        LOGGER.info("onCreateView()");

        // First inflate the general dashboard view.
        View contentView = inflater.inflate(R.layout.fragment_startup, container, false);

        // Inject all dashboard-related views.
        ButterKnife.inject(this, contentView);

        mCarTypeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CarSelectionActivity.class);
                getActivity().startActivity(intent);
            }
        });
        mOBDTypeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), OBDSelectionActivity.class);
                getActivity().startActivity(intent);
            }
        });

        setCarTypeText(mCarManager.getCar());

        return contentView;
    }

    @Subscribe
    public void onReceiveNewCarTypeSelectedEvent(NewCarTypeSelectedEvent event) {
        LOGGER.debug(String.format("Received event: %s", event.toString()));
        setCarTypeText(event.mCar);
    }

    @Subscribe
    public void onReceiveBluetoothDeviceSelectedEvent(BluetoothDeviceSelectedEvent event){
        LOGGER.debug(String.format("Received event: %s", event.toString()));
        // TODO
    }

    /**
     * @param car
     */
    private void setCarTypeText(Car car) {
        if (car != null) {
            mCarTypeTextView.setText(String.format("%s %s\n(%s %s %s ccm)",
                    car.getManufacturer(),
                    car.getModel(),
                    car.getConstructionYear(),
                    car.getFuelType(),
                    car.getEngineDisplacement()));
        } else {
            mCarTypeTextView.setText(String.format("No Car selected"));
        }
    }
}
