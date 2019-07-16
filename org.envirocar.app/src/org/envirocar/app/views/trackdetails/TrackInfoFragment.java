package org.envirocar.app.views.trackdetails;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.envirocar.app.R;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.main.MainActivityComponent;
import org.envirocar.app.main.MainActivityModule;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;
import org.envirocar.core.trackprocessing.TrackStatisticsProvider;
import org.envirocar.core.utils.CarUtils;
import org.envirocar.storage.EnviroCarDB;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rx.schedulers.Schedulers;

public class TrackInfoFragment extends BaseInjectorFragment {

    private static final DecimalFormat DECIMAL_FORMATTER_TWO_DIGITS = new DecimalFormat("#.##");
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();
    private static final DateFormat UTC_DATE_FORMATTER = new SimpleDateFormat("HH:mm:ss", Locale
            .ENGLISH);

    static {
        UTC_DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Inject
    protected EnviroCarDB mEnvirocarDB;

    @BindView(R.id.activity_track_details_attr_begin_value)
    protected TextView mBeginText;
    @BindView(R.id.activity_track_details_attr_end_value)
    protected TextView mEndText;
    @BindView(R.id.activity_track_details_attr_car_value)
    protected TextView mCarText;
    @BindView(R.id.activity_track_details_attr_emission_value)
    protected TextView mEmissionText;
    @BindView(R.id.activity_track_details_attr_consumption_value)
    protected TextView mConsumptionText;
    @BindView(R.id.consumption_container)
    protected RelativeLayout mConsumptionContainer;
    @BindView(R.id.co2_container)
    protected RelativeLayout mCo2Container;
    @BindView(R.id.descriptionTv)
    protected TextView descriptionTv;
    protected Track track;
    protected Unbinder unbinder;


    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        MainActivityComponent mainActivityComponent =  baseApplicationComponent.plus(new MainActivityModule(getActivity()));
        mainActivityComponent.inject(this);
    }

    @Nullable
    @Override
    public android.view.View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final Bundle args = getArguments();
        int mTrackID = args.getInt("track");
        Track.TrackId trackid = new Track.TrackId(mTrackID);
        track = mEnvirocarDB.getTrack(trackid)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .first();
        View rootView = inflater.inflate(R.layout.activity_track_details_attributes, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        initViewValues();
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void initViewValues() {
        try {
            mCarText.setText(CarUtils.carToStringWithLinebreak(track.getCar()));
            mBeginText.setText(DATE_FORMAT.format(new Date(track.getStartTime())));
            mEndText.setText(DATE_FORMAT.format(new Date(track.getEndTime())));

            // show consumption and emission either when the fuel type of the track's car is
            // gasoline or the beta setting has been enabled.
            if(!track.hasProperty(Measurement.PropertyKey.SPEED)){
                mConsumptionContainer.setVisibility(View.GONE);
                mCo2Container.setVisibility(View.GONE);
                descriptionTv.setText(R.string.gps_track_details);
            }
            else if (track.getCar().getFuelType() == Car.FuelType.GASOLINE ||
                    PreferencesHandler.isDieselConsumptionEnabled(getContext())) {
                mEmissionText.setText(DECIMAL_FORMATTER_TWO_DIGITS.format(
                        ((TrackStatisticsProvider) track).getGramsPerKm()) + " g/km");
                mConsumptionText.setText(
                        String.format("%s l/h\n%s l/100 km",
                                DECIMAL_FORMATTER_TWO_DIGITS.format(
                                        ((TrackStatisticsProvider) track)
                                                .getFuelConsumptionPerHour()),

                                DECIMAL_FORMATTER_TWO_DIGITS.format(
                                        ((TrackStatisticsProvider) track).getLiterPerHundredKm())));
            } else {
                mEmissionText.setText(R.string.track_list_details_diesel_not_supported);
                mConsumptionText.setText(R.string.track_list_details_diesel_not_supported);
                mEmissionText.setTextColor(Color.RED);
                mConsumptionText.setTextColor(Color.RED);
            }

        } catch (FuelConsumptionException e) {
            e.printStackTrace();
        } catch (NoMeasurementsException e) {
            e.printStackTrace();
        } catch (UnsupportedFuelTypeException e) {
            e.printStackTrace();
        }
    }

    String convertMillisToDate(){
        try {
            long timeInMillis = track.getDuration();
            long diffSeconds = timeInMillis / 1000 % 60;
            long diffMinutes = timeInMillis / (60 * 1000) % 60;
            long diffHours = timeInMillis / (60 * 60 * 1000) % 24;
            long diffDays = timeInMillis / (24 * 60 * 60 * 1000);
            StringBuilder stringBuilder = new StringBuilder();
            if (diffDays != 0) {
                stringBuilder.append(diffDays);
                stringBuilder.append("d ");
                if (diffHours > 1) {
                    stringBuilder.append(diffHours);
                    stringBuilder.append("h");
                }
            } else {
                if (diffHours != 0) {
                    stringBuilder.append(diffHours);
                    if (diffMinutes != 0) {
                        stringBuilder.append(":");
                        stringBuilder.append(new DecimalFormat("00").format(diffMinutes));
                    }
                    stringBuilder.append(" h");
                } else {
                    if (diffMinutes != 0) {
                        stringBuilder.append(diffMinutes);
                        if (diffSeconds != 0) {
                            stringBuilder.append(":");
                            stringBuilder.append(new DecimalFormat("00").format(diffSeconds));
                        }
                        stringBuilder.append(" m");
                    } else {
                        if (diffSeconds != 0) {
                            stringBuilder.append(diffSeconds);
                            stringBuilder.append(" s");
                        } else {
                            stringBuilder.append("No Tracks");
                        }
                    }
                }

            }
            return stringBuilder.toString();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
