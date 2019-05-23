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
import org.envirocar.core.events.NewMeasurementEvent;
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
    public void onReceiveNewMeasurementEvent(NewMeasurementEvent event) {
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
