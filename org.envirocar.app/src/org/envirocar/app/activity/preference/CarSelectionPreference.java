/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.activity.preference;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.envirocar.app.injection.Injector;
import org.envirocar.app.R;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.application.CarManager;
import org.envirocar.app.application.ContextInternetAccessProvider;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.dao.DAOProvider;
import org.envirocar.app.dao.exception.NotConnectedException;
import org.envirocar.app.dao.exception.SensorRetrievalException;
import org.envirocar.app.dao.exception.UnauthorizedException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.Car.FuelType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

public class CarSelectionPreference extends DialogPreference {

    public static final String SENSOR_TYPE = "car";
    private static final Logger logger = Logger.getLogger(CarSelectionPreference.class);
    private static final String DEFAULT_VALUE = "null";
    protected String carModel;
    protected String carManufacturer;
    protected String carConstructionYear;
    protected String carFuelType;
    protected String carEngineDisplacement;
    protected List<Car> sensors;

    // Injected variables
    @Inject
    protected UserManager mUserManager;
    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected CarManager mCarManager;

    //
    private Car car;
    private EditText modelEditText;
    private EditText manufacturerEditText;
    private EditText constructionYearEditText;
    private Spinner sensorSpinner;
    private ProgressBar sensorDlProgress;
    private Button sensorRetryButton;
    //	private ScrollView garageForm;
    private RadioButton gasolineRadioButton;
    private RadioButton dieselRadioButton;
    private EditText engineDisplacementEditText;
    private TableLayout selectedCarDetails;
    private View registerButton;
    private ProgressBar carRegisterProgress;
    private TextView carRegisterStatusText;
    private View carRegisterForm;

    public CarSelectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        ((Injector) context.getApplicationContext()).injectObjects(this);

        setDialogLayoutResource(R.layout.car_selection_main);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);
    }

    public static String serializeCar(Car car) {
        ObjectOutputStream oos = null;
        Base64OutputStream b64 = null;
        try {
            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(byteArrayOut);
            oos.writeObject(car);
            oos.flush();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            b64 = new Base64OutputStream(out, Base64.DEFAULT);
            b64.write(byteArrayOut.toByteArray());
            b64.flush();
            b64.close();
            out.flush();
            out.close();

            String result = new String(out.toByteArray());
            return result;
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        } finally {
            if (oos != null)
                try {
                    b64.close();
                    oos.close();
                } catch (IOException e) {
                    logger.warn(e.getMessage(), e);
                }
        }
        return null;
    }

    public static Car instantiateCar(String object) {
        if (object == null) return null;

        ObjectInputStream ois = null;
        try {
            Base64InputStream b64 = new Base64InputStream(new ByteArrayInputStream(object.getBytes()), Base64.DEFAULT);
            ois = new ObjectInputStream(b64);
            Car car = (Car) ois.readObject();
            return car;
        } catch (StreamCorruptedException e) {
            logger.warn(e.getMessage(), e);
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            logger.warn(e.getMessage(), e);
        } finally {
            if (ois != null)
                try {
                    ois.close();
                } catch (IOException e) {
                    logger.warn(e.getMessage(), e);
                }
        }
        return null;
    }

    @Override
    protected void onBindDialogView(View view) {
        setupUIItems(view);

        getCarList();
    }

    private void setupUIItems(View rootView) {
        //TODO !fancy! search for sensors
        LinearLayout selectCarView = (LinearLayout) rootView.findViewById(R.id.car_selection_select_car);
        LinearLayout registerCarView = (LinearLayout) rootView.findViewById(R.id.car_selection_register_car);

        selectedCarDetails = (TableLayout) selectCarView.findViewById(R.id.selected_car_details);

        setupCarRegistrationItems(registerCarView);

        sensorSpinner = (Spinner) selectCarView.findViewById(R.id.dashboard_current_sensor_spinner);
        sensorSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            private boolean firstSelect = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                if (!firstSelect && pos > 0) {
                    updateCurrentSensor((Car) parent.getItemAtPosition(pos));
                } else {
                    firstSelect = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                logger.info("no change detected");
            }
        });
        sensorDlProgress = (ProgressBar) selectCarView.findViewById(R.id.sensor_dl_progress);
        sensorRetryButton = (Button) selectCarView.findViewById(R.id.retrybutton);
        sensorRetryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                getCarList();
            }
        });

//		rootView.findViewById(R.id.mygaragelayout).requestFocus();
//		rootView.findViewById(R.id.mygaragelayout).requestFocusFromTouch();
    }

    private void setupCarRegistrationItems(View view) {
        modelEditText = (EditText) view.findViewById(R.id.addCarToGarage_car_model);
        manufacturerEditText = (EditText) view.findViewById(R.id.addCarToGarage_car_manufacturer);
        constructionYearEditText = (EditText) view.findViewById(R.id.addCarToGarage_car_constructionYear);
        engineDisplacementEditText = (EditText) view.findViewById(R.id.addCarToGarage_car_engineDisplacement);

        TextWatcher textWatcher = new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                carModel = modelEditText.getText().toString();
                carManufacturer = manufacturerEditText.getText().toString();
                carConstructionYear = constructionYearEditText.getText()
                        .toString();
                carEngineDisplacement = engineDisplacementEditText.getText().toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

        };

        modelEditText.addTextChangedListener(textWatcher);
        manufacturerEditText.addTextChangedListener(textWatcher);
        constructionYearEditText.addTextChangedListener(textWatcher);
        engineDisplacementEditText.addTextChangedListener(textWatcher);

        OnClickListener listener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                carFuelType = resolveFuelTypeFromCheckbox(v.getId());
                logger.info(carFuelType);
            }
        };

        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radiogroup_fueltype);
        carFuelType = resolveFuelTypeFromCheckbox(radioGroup.getCheckedRadioButtonId());

        gasolineRadioButton = (RadioButton) view.findViewById(R.id.radio_gasoline);
        gasolineRadioButton.setOnClickListener(listener);
        dieselRadioButton = (RadioButton) view.findViewById(R.id.radio_diesel);
        dieselRadioButton.setOnClickListener(listener);

        carRegisterProgress = (ProgressBar) view.findViewById(R.id.car_register_progress);
        carRegisterStatusText = (TextView) view.findViewById(R.id.car_register_status_text);
        carRegisterForm = view.findViewById(R.id.car_register_form);

        registerButton = view.findViewById(R.id.register_car_button);
        registerButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        registerSensorAtServer(SENSOR_TYPE, carManufacturer,
                                carModel, carConstructionYear, carFuelType, carEngineDisplacement);
                    }
                });
    }

    /**
     * Shows the progress UI and hides the register form.
     */
    private void changeProgress(final boolean show, String statusText) {
        registerButton.setEnabled(!show && statusText == null);
        carRegisterForm.setVisibility(show || statusText != null ? View.INVISIBLE : View.VISIBLE);
        carRegisterForm.invalidate();

        if (statusText != null) {
            carRegisterStatusText.setText(statusText);
            carRegisterStatusText.setVisibility(View.VISIBLE);
            carRegisterProgress.setVisibility(View.GONE);
        } else {
            carRegisterProgress.setIndeterminate(true);
            carRegisterProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Register a new sensor (car) at the server
     *
     * @param sensorType
     * @param carManufacturer     Car manufacturer
     * @param carModel            Car model
     * @param carConstructionYear Construction year of the car
     * @param carFuelType         Fuel type of the car
     */
    private void registerSensorAtServer(final String sensorType,
                                        final String carManufacturer, final String carModel,
                                        final String carConstructionYear, final String carFuelType,
                                        final String carEngineDisplacement) {

        try {
            checkEmpty(sensorType, carManufacturer, carModel, carConstructionYear,
                    carConstructionYear, carFuelType, carEngineDisplacement);
        } catch (Exception e) {
            //TODO i18n
            Toast.makeText(getContext(), "Not all values were defined.", Toast.LENGTH_SHORT).show();
            return;
        }

        changeProgress(true, null);


        if (new ContextInternetAccessProvider(getContext()).isConnected() && mUserManager.isLoggedIn
                ()) {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    Car tmpCar = new Car(Car.resolveFuelType(carFuelType), carManufacturer, carModel, null,
                            Integer.parseInt(carConstructionYear), Integer.parseInt(carEngineDisplacement));

                    try {
                        String sensorId = mDAOProvider.getSensorDAO().saveSensor(tmpCar);

                        //put the sensor id into shared preferences
                        tmpCar.setId(sensorId);
                        car = tmpCar;
                        persistCar();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                changeProgress(false, String.format("%s %s", car.toString(), resolveSuccesString()));
                                submitDialogClosure();
                            }
                        });

                    } catch (final NotConnectedException e1) {
                        logger.warn(e1.getMessage());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), "Server Error: " + e1.getMessage(), Toast.LENGTH_SHORT).show();
                                changeProgress(false, e1.getMessage());
                            }
                        });
                    } catch (final UnauthorizedException e1) {
                        logger.warn(e1.getMessage());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), "Server Error: " + e1.getMessage(), Toast.LENGTH_SHORT).show();
                                changeProgress(false, e1.getMessage());
                            }
                        });
                    }

                    return null;
                }

            }.execute();

        } else {
            createTemporaryCar();
        }

    }

    protected void runOnUiThread(Runnable runnable) {
        if (getContext() instanceof Activity) {
            ((Activity) getContext()).runOnUiThread(runnable);
        }
    }

    private String resolveSuccesString() {
        try {
            return getContext().getResources().getString(R.string.register_car_success);
        } catch (RuntimeException e) {
        }
        return "succesfully registered.";
    }

    private void createTemporaryCar() {
        String uuid = UUID.randomUUID().toString();
        String sensorId = Car.TEMPORARY_SENSOR_ID.concat(uuid.substring(0, uuid.length() - Car.TEMPORARY_SENSOR_ID.length()));

        int year = Integer.parseInt(carConstructionYear);
        car = new Car(Car.resolveFuelType(carFuelType), carManufacturer,
                carModel, sensorId, year,
                Integer.parseInt(carEngineDisplacement));

        persistCar();

        Toast.makeText(getContext(), getContext().getString(R.string.creating_temp_car), Toast.LENGTH_SHORT).show();
        changeProgress(false, getContext().getString(R.string.creating_temp_car));

        submitDialogClosure();
    }

    private void submitDialogClosure() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Dialog dialog = getDialog();
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        }, 2000);
    }

    private void checkEmpty(String... values) throws Exception {
        for (String string : values) {
            if (string == null || string.isEmpty()) {
                throw new Exception("Empty value!");
            }
        }
    }

    /**
     * Get the fuel type form the checkbox
     *
     * @param resid
     * @return
     */
    private String resolveFuelTypeFromCheckbox(int resid) {
        switch (resid) {
            case R.id.radio_diesel:
                return FuelType.DIESEL.toString();
            case R.id.radio_gasoline:
                return FuelType.GASOLINE.toString();
        }
        return "none";
    }

    protected void retrieveSensorsFromDao() throws SensorRetrievalException {
        sensors = mDAOProvider.getSensorDAO().getAllSensors();

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                SensorAdapter adapter = new SensorAdapter();
                sensorSpinner.setAdapter(adapter);
                int index = adapter.getInitialSelectedItem();
                sensorSpinner.setSelection(index);
                if (index > 0) {
                    updateCurrentSensor(car);
                }
            }
        });
    }

    public void getCarList() {

        sensorDlProgress.setVisibility(View.VISIBLE);
        sensorSpinner.setVisibility(View.GONE);
        sensorRetryButton.setVisibility(View.GONE);

        sensors = new ArrayList<Car>();

        try {
            new SensorRetrievalTask().execute();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            Toast.makeText(getContext(), "Could not retrieve cars from server", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * This method updates the attributes of the current sensor (=car)
     */
    private void updateCurrentSensor(Car car) {
        this.car = car;

        if (car != null) {
            ((TextView) selectedCarDetails.findViewById(R.id.car_constructionYear_value)).setText(car.getConstructionYear() + "");
            ((TextView) selectedCarDetails.findViewById(R.id.car_engineDisplacement_value)).setText(car.getEngineDisplacement() + "");
            ((TextView) selectedCarDetails.findViewById(R.id.car_manufacturer_value)).setText(car.getManufacturer());
            ((TextView) selectedCarDetails.findViewById(R.id.car_model_value)).setText(car.getModel());
            selectedCarDetails.setVisibility(View.VISIBLE);
        } else {
            selectedCarDetails.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            //this fixes issue #166
            persistCar();
        }
    }

    private void persistCar() {
        mCarManager.setCar(car);
        persistString(serializeCar(car));
        getSharedPreferences().edit().putInt(SettingsActivity.CAR_HASH_CODE, car.hashCode()).commit();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setSummary(car.toString());
            }
        });
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            car = instantiateCar(this.getPersistedString(DEFAULT_VALUE));
        }

        if (car != null) {
            setSummary(car.toString());
        } else {
            setSummary(R.string.please_select);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        // Check whether this Preference is persistent (continually saved)
        if (isPersistent()) {
            // No need to save instance state since it's persistent, use superclass state
            return superState;
        }

        // Create instance of custom BaseSavedState
        final SavedState myState = new SavedState(superState);
        // Set the state's value with the class member that holds current setting value
        myState.car = car;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // Check whether we saved the state in onSaveInstanceState
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state);
            return;
        }

        // Cast state to custom BaseSavedState and pass to superclass
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());

    }

    public void updateUIOnAfterSensorRetrieval() {
        if (sensors.isEmpty()) {
            logger.warn("Got no cars neither from server nor from cache.");
            //TODO show warning that no cars were found i18n
            Toast.makeText(getContext(), "Could not retrieve cars from server or local cache", Toast.LENGTH_SHORT).show();
        }

        sensorDlProgress.setVisibility(View.GONE);
        sensorSpinner.setVisibility(View.VISIBLE);

		/*
		 * workaround for getDialog() returning null in every lifecycle method
		 */
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    public static class SavedState extends BaseSavedState {
        // Standard creator object using an instance of this class
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
        // Member that holds the setting's value
        // Change this data type to match the type saved by your Preference
        Car car;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            car = (Car) source.readSerializable();  // Change this to read the appropriate data type
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeSerializable(car);  // Change this to write the appropriate data type
        }
    }

    private class SensorRetrievalTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            try {
                retrieveSensorsFromDao();
            } catch (SensorRetrievalException e) {
                logger.warn(e.getMessage(), e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            updateUIOnAfterSensorRetrieval();
        }

    }

    private class SensorAdapter extends BaseAdapter implements SpinnerAdapter {

        @Override
        public int getCount() {
            return sensors.size() + 1;
        }

        public int getInitialSelectedItem() {
            if (car != null) {
                int index = 1;
                for (Car c : sensors) {
                    if (c.equals(car)) {
                        return index;
                    }
                    index++;
                }
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return sensors.get(position - 1);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            TextView text;
            if (position == 0) {
                text = new TextView(parent.getContext());
                text.setText(getContext().getString(R.string.please_select));
            } else {
                text = new TextView(parent.getContext());
                text.setText(((Car) getItem(position)).toString());
            }

            return text;
        }

    }


}
