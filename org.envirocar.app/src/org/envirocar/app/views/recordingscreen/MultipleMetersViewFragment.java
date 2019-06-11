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
package org.envirocar.app.views.recordingscreen;


import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.events.recording.RecordingNewMeasurementEvent;
import org.envirocar.core.logging.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 */
public class MultipleMetersViewFragment extends Fragment {
    private static final Logger LOGGER = Logger.getLogger(MultipleMetersViewFragment.class);

    @BindView(R.id.speedText)
    protected TextView speedText;
    @BindView(R.id.rpmText)
    protected TextView rpmText;
    @BindView(R.id.intakePressureText)
    protected TextView intakePressureText;
    @BindView(R.id.intakeTempText)
    protected TextView intakeTempText;
    @BindView(R.id.engineLoadText)
    protected TextView engineLoadText;
    @BindView(R.id.throttlePositionText)
    protected TextView throttlePositionText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View contentView = inflater.inflate(R.layout.fragment_multiple_meters_view, container, false);

        // Inject all dashboard-related views.
        ButterKnife.bind(this, contentView);

        return contentView;
    }


    @Subscribe
    public void onReceiveNewMeasurementEvent(RecordingNewMeasurementEvent event) {
        LOGGER.debug(String.format("Received event: %s", event.toString()));
        Measurement measurement = event.mMeasurement;
        if(measurement.hasProperty(Measurement.PropertyKey.SPEED)){
            speedText.setText(measurement.getProperty(Measurement.PropertyKey.SPEED) + " Km/h");
        }else{
            speedText.setText("No Data");
        }

        if(measurement.hasProperty(Measurement.PropertyKey.RPM)){
            rpmText.setText( measurement.getProperty(Measurement.PropertyKey.RPM) + " rpm");
        }else{
            rpmText.setText("No Data");
        }

        if(measurement.hasProperty(Measurement.PropertyKey.INTAKE_PRESSURE)){
            intakePressureText.setText(measurement.getProperty(Measurement.PropertyKey.INTAKE_PRESSURE) + " kPa");
        }else{
            intakePressureText.setText("No Data");
        }

        if(measurement.hasProperty(Measurement.PropertyKey.INTAKE_TEMPERATURE)){
            intakeTempText.setText(measurement.getProperty(Measurement.PropertyKey.INTAKE_TEMPERATURE) + " F");
        }else{
            intakeTempText.setText("No Data");
        }


        if(measurement.hasProperty(Measurement.PropertyKey.ENGINE_LOAD)){
            engineLoadText.setText(measurement.getProperty(Measurement.PropertyKey.ENGINE_LOAD) + " kJ");
        }else{
            engineLoadText.setText("No Data");
        }

        if(measurement.hasProperty(Measurement.PropertyKey.THROTTLE_POSITON)){
            throttlePositionText.setText(measurement.getProperty(Measurement.PropertyKey.THROTTLE_POSITON) + " units");
        }else{
            throttlePositionText.setText("No Data");
        }

    }

}
