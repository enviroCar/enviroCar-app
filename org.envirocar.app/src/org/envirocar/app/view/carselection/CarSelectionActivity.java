/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.view.carselection;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;


import org.envirocar.app.R;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.remote.DAOProvider;
import org.envirocar.app.view.utils.ECAnimationUtils;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.CarImpl;
import org.envirocar.core.injection.BaseInjectorActivity;
import org.envirocar.core.logging.Logger;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public class CarSelectionActivity extends BaseInjectorActivity {
    private static final Logger LOG = Logger.getLogger(CarSelectionActivity.class);

    private static final int DURATION_SHEET_ANIMATION = 350;

    @InjectView(R.id.activity_car_selection_layout_content)
    protected View mContentView;
    @InjectView(R.id.activity_car_selection_layout_toolbar)
    protected Toolbar mToolbar;
    @InjectView(R.id.activity_car_selection_layout_exptoolbar)
    protected Toolbar mExpToolbar;

    @InjectView(R.id.overlay)
    protected View mOverlay;
    //    @InjectView(R.id.activity_car_selection_new_car_sheet)
    //    protected View mSheetView;

    @InjectView(R.id.activity_car_selection_new_car_card)
    protected View mNewCarCard;
    @InjectView(R.id.activity_car_selection_new_car_fab)
    protected FloatingActionButton mFab;

    @InjectView(R.id.activity_car_selection_layout_carlist)
    protected ListView mCarListView;

    // Views of the sheet view used to add a new car type.
    @InjectView(R.id.activity_car_selection_model_input_layout)
    protected TextInputLayout mModelTextLayout;
    @InjectView(R.id.activity_car_selection_manufacturer_edit_text)
    protected AutoCompleteTextView mManufacturerTextView;
    @InjectView(R.id.activity_car_selection_model_edit_text)
    protected AutoCompleteTextView mModelTextView;
    @InjectView(R.id.activity_car_selection_year_edit_text)
    protected AutoCompleteTextView mYearTextView;
    @InjectView(R.id.activity_car_selection_engine_edit_text)
    protected AutoCompleteTextView mEngineTextView;
    @InjectView(R.id.activity_car_selection_new_car_card_add_button)
    protected Button mAddCarButton;

    @InjectView(R.id.activity_car_selection_new_car_card_radio_group)
    protected RadioGroup mRadioGroup;
    @InjectView(R.id.activity_car_selection_new_car_card_radio_gasoline)
    protected RadioButton mRadioGasoline;
    @InjectView(R.id.activity_car_selection_new_car_card_radio_diesel)
    protected RadioButton mDieselGasoline;

    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected CarPreferenceHandler mCarManager;

    private Set<Car> mCars = new HashSet<>();
    private Set<String> mManufacturerNames;
    private Map<String, Set<String>> mCarToModelMap = new ConcurrentHashMap<>();
    private Map<String, Set<String>> mModelToYear = new ConcurrentHashMap<>();
    private Map<String, Set<String>> mModelToCCM = new ConcurrentHashMap<>();

    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    private Subscription mSensoreSubscription;

    private CarSelectionListAdapter mCarListAdapter;

    private AutoCompleteArrayAdapter mManufacturerNameAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the content view of this activity.
        setContentView(R.layout.activity_car_selection_layout);

        // Inject all annotated views.
        ButterKnife.inject(this);

        // Set the toolbar as default actionbar.
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize the manufacturer names and its textview adapter
        mManufacturerNames = new HashSet<String>(Arrays.asList(getResources()
                .getStringArray(R.array.car_types)));
        mManufacturerNameAdapter = new AutoCompleteArrayAdapter(CarSelectionActivity.this,
                android.R.layout.simple_dropdown_item_1line, mManufacturerNames.toArray(
                new String[mManufacturerNames.size()]));

        // Set the adapter.
        mManufacturerTextView.setAdapter(mManufacturerNameAdapter);
        mManufacturerTextView.setOnClickListener(v ->
                mManufacturerTextView.showDropDown());

        // Init the text watcher that are responsible to update the edit text views.
        initTextWatcher();

        // Init the hide keyboard listener. These always hide the keyboard once an item has been
        // selected in the autocomplete list.
        setupHideKeyboardListener();
        setupListView();
        dispatchRemoteSensors();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSensoreSubscription != null)
            mSensoreSubscription.unsubscribe();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // click on the home button in the toolbar.
        if (item.getItemId() == android.R.id.home) {
            // If the sheet view is visible, then only close the sheet view.
            // Otherwise, close the activity.
            if (!closeAddCarCard()) {
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // if the sheet view was not visible.
        if (!closeAddCarCard()) {
            // call the super method.
            super.onBackPressed();
        }
    }

    // Set the onClick listener for the FloatingActionButton. When triggered, the sheet view
    // gets shown.
    @OnClick(R.id.activity_car_selection_new_car_fab)
    public void onClickNewCarButton() {
        showAddCarCard();
    }

    /**
     * Add car button onClick listener. When clicked, it tries to find out if the car already
     * exists. If this is the case, then it adds the car to the list of selected cars. If not,
     * then it selects
     */
    @OnClick(R.id.activity_car_selection_new_car_card_add_button)
    public void onClickAddCarButton() {
        // TODO Check views.
        String manufacturer = mManufacturerTextView.getText().toString();
        String model = mModelTextView.getText().toString();
        String yearString = mYearTextView.getText().toString();
        String engineString = mEngineTextView.getText().toString();
        Car.FuelType fuelType = mRadioGasoline.isChecked() ? Car.FuelType.GASOLINE : Car
                .FuelType.DIESEL;

        View focusView = null;

        //First check all input forms for empty strings
        if (engineString == null || engineString.isEmpty()) {
            mEngineTextView.setError("Cannot be empty");
            focusView = mEngineTextView;
        }
        if (yearString == null || yearString.isEmpty()) {
            mYearTextView.setError("Cannot be empty");
            focusView = mYearTextView;
        }
        if (model == null || model.isEmpty()) {
            mModelTextView.setError("Cannot be empty");
            focusView = mModelTextView;
        }
        if (manufacturer == null || manufacturer.isEmpty()) {
            mManufacturerTextView.setError("Cannot be empty");
            focusView = mManufacturerTextView;
        }

        // if any of the input forms contained empty values, then set the focus to the last one set.
        if (focusView != null) {
            focusView.requestFocus();
            return;
        }


        int year = Integer.parseInt(mYearTextView.getText().toString());
        int engine = Integer.parseInt(mEngineTextView.getText().toString());
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        // Check the values of engine and year for validity.
        if (engine < 500 || engine > 5000) {
            mEngineTextView.setError("Invalid value");
            focusView = mEngineTextView;
        }
        if (year < 1990 || year > currentYear) {
            mYearTextView.setError("Invalid value");
            focusView = mYearTextView;
        }

        // if tengine or year have invalid values, then request the focus.
        if (focusView != null) {
            focusView.requestFocus();
            return;
        }

        Car selectedCar = null;
        if (mManufacturerNames.contains(manufacturer)
                && mCarToModelMap.get(manufacturer) != null
                && mCarToModelMap.get(manufacturer).contains(model)
                && mModelToCCM.get(model) != null
                && mModelToCCM.get(model).contains("" + engine)
                && mModelToYear.get(model) != null
                && mModelToYear.get(model).contains("" + year)) {
            for (Car car : mCars) {
                if (car.getManufacturer().equals(manufacturer)
                        && car.getModel().equals(model)
                        && car.getConstructionYear() == year
                        && car.getEngineDisplacement() == engine
                        && car.getFuelType() == fuelType) {
                    selectedCar = car;
                }
            }
        }

        if (selectedCar == null) {
            selectedCar = new CarImpl(manufacturer, model, fuelType, year, engine);
            mCarManager.registerCarAtServer(selectedCar);
        }

        // When the car has been successfully inserted in the listadapter, then update
        // the list adapter.
        if (mCarManager.addCar(selectedCar)) {
            // Add the car to the adapter and close the sheet view.
            mCarListAdapter.addCarItem(selectedCar);
            closeAddCarCard();

            // Schedule a show snackbar runnable when the sheet animation has been finished.
            new Handler().postDelayed(() -> showSnackbar("Car successfully created!"),
                    DURATION_SHEET_ANIMATION);
            resetEditTexts();
        }
        // Otherwise, when the list already contained the specific car type, then show a
        // snackbar.
        else {
            showSnackbar("Car is already in the list");
        }
    }

    /**
     * Shows the card view for the addition cars.
     *
     * @return true if the card view was not shown.
     */
    private boolean showAddCarCard() {
        // If the card view is not visible...
        if (!mNewCarCard.isShown()) {
            // Get the height of the display
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int height = size.y;

            // expand the toolbar.
            ECAnimationUtils.expandView(mExpToolbar, height / 3);
            // Start an animation that shows the card view.
            ECAnimationUtils.animateShowView(this, mNewCarCard,
                    R.anim.translate_in_bottom_login_card);
            ECAnimationUtils.animateHideView(this, mFab, R.anim.fade_out);
            return true;
        }

        // this card was already visible. Therefore, return false.
        return false;
    }

    /**
     * Closes the sheet view if shown.
     *
     * @return true if the sheet view as visible and has been
     */
    private boolean closeAddCarCard() {
        // If the card view is visible.
        if (mNewCarCard.isShown()) {
            // start an animation that hides the card view.
            ECAnimationUtils.animateHideView(this, mNewCarCard, R.anim.translate_out_bottom_card,
                    // When the animation is finished, show the FAB
                    () -> ECAnimationUtils.animateShowView(
                            CarSelectionActivity.this, mFab, R.anim.fade_in));
            ECAnimationUtils.compressView(mExpToolbar, 1);

            return true;
        }
        // the card view was not visible. Therefore, return false.
        return false;
    }

    private void setupListView() {
        Car selectedCar = mCarManager.getCar();
        List<Car> usedCars = mCarManager.getDeserialzedCars();
        mCarListAdapter = new CarSelectionListAdapter(this, selectedCar, usedCars, new
                CarSelectionListAdapter
                        .OnCarListActionCallback() {

                    @Override
                    public void onSelectCar(Car car) {
                        mCarManager.setCar(car);
                        showSnackbar(String.format("%s %s selected as my car",
                                car.getManufacturer(), car.getModel()));
                    }

                    @Override
                    public void onDeleteCar(Car car) {
                        LOG.info(String.format("onDeleteCar(%s %s %s %s)", car.getManufacturer
                                (), car
                                .getModel(), "" + car.getConstructionYear(), "" + car
                                .getEngineDisplacement()));

                        // If the car has been removed successfully...
                        if (mCarManager.removeCar(car)) {
                            // then remove it from the list and show a snackbar.
                            mCarListAdapter.removeCarItem(car);
                            showSnackbar(String.format("%s %s has been deleted!", car
                                    .getManufacturer(), car.getModel()));
                        }
                    }
                });
        mCarListView.setAdapter(mCarListAdapter);
    }

    /**
     * Setups the listener to hide the keyboards.
     */
    private void setupHideKeyboardListener() {
        mManufacturerTextView.setOnItemClickListener((parent, view, position, id) -> {
            InputMethodManager in = (InputMethodManager) getSystemService(Context
                    .INPUT_METHOD_SERVICE);
            in.hideSoftInputFromWindow(mManufacturerTextView.getWindowToken(), 0);
        });

        mModelTextView.setOnItemClickListener((parent, view, position, id) -> {
            InputMethodManager in = (InputMethodManager) getSystemService(Context
                    .INPUT_METHOD_SERVICE);
            in.hideSoftInputFromWindow(mModelTextView.getWindowToken(), 0);
        });

        mYearTextView.setOnItemClickListener((parent, view, position, id) -> {
            InputMethodManager in = (InputMethodManager) getSystemService(Context
                    .INPUT_METHOD_SERVICE);
            in.hideSoftInputFromWindow(mYearTextView.getWindowToken(), 0);
        });

        mEngineTextView.setOnItemClickListener((parent, view, position, id) -> {
            InputMethodManager in = (InputMethodManager) getSystemService(Context
                    .INPUT_METHOD_SERVICE);
            in.hideSoftInputFromWindow(mEngineTextView.getWindowToken(), 0);
        });
    }

    private void dispatchRemoteSensors() {
        mSensoreSubscription =
                mDAOProvider.getSensorDAO()
                        .getAllCarsObservable()
                        .onBackpressureBuffer(10000)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .subscribe(new Observer<List<Car>>() {
                            @Override
                            public void onCompleted() {
                                mMainThreadWorker.schedule(() -> {
                                    Toast.makeText(CarSelectionActivity.this, "Received! " +
                                            mCars.size(), Toast.LENGTH_SHORT).show();
                                    mManufacturerNameAdapter = new AutoCompleteArrayAdapter(
                                            CarSelectionActivity.this,
                                            android.R.layout.simple_dropdown_item_1line,
                                            mManufacturerNames.toArray(
                                                    new String[mManufacturerNames.size()]));
                                    mManufacturerTextView.setAdapter(mManufacturerNameAdapter);
                                    mSensoreSubscription.unsubscribe();
                                });
                            }

                            @Override
                            public void onError(Throwable e) {
                                LOG.error(e.getMessage(), e);
                                mMainThreadWorker.schedule(() ->
                                        Toast.makeText(CarSelectionActivity.this, "ERROR!", Toast
                                                .LENGTH_SHORT).show());
                            }

                            @Override
                            public void onNext(List<Car> cars) {
                                for (Car car : cars) {
                                    if (car != null)
                                        addCarToAutocompleteList(car);
                                }
                            }
                        });
    }


    /**
     * Resets the edittexts to empty strings.
     */
    private void resetEditTexts() {
        mManufacturerTextView.setText("");
        mModelTextView.setText("");
        mYearTextView.setText("");
        mEngineTextView.setText("");
    }

    /**
     * Inserts the attributes of the car
     *
     * @param car
     */
    private void addCarToAutocompleteList(Car car) {
        mCars.add(car);
        String manufacturer = car.getManufacturer();
        String model = car.getModel();
        mManufacturerNames.add(manufacturer);

        if (!mCarToModelMap.containsKey(manufacturer))
            mCarToModelMap.put(manufacturer, new HashSet<>());
        mCarToModelMap.get(manufacturer).add(model);

        if (!mModelToYear.containsKey(model))
            mModelToYear.put(model, new HashSet<>());
        mModelToYear.get(model).add(Integer.toString(car.getConstructionYear()));

        if (!mModelToCCM.containsKey(model))
            mModelToCCM.put(model, new HashSet<>());
        mModelToCCM.get(model).add(Integer.toString(car.getEngineDisplacement()));
    }

    private void initTextWatcher() {
        mManufacturerTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Nothing to do..
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Nothing to do..
            }

            @Override
            public void afterTextChanged(Editable s) {
                Set<String> modelTypes = mCarToModelMap.get(s.toString());

                mModelTextView.setText("");
                mModelTextView.setAdapter(null);
                mModelTextView.dismissDropDown();

                mYearTextView.setText("");
                mYearTextView.setAdapter(null);
                mYearTextView.dismissDropDown();

                mEngineTextView.setText("");
                mEngineTextView.setAdapter(null);
                mEngineTextView.dismissDropDown();

                if (modelTypes != null && modelTypes.size() > 0) {
                    AutoCompleteArrayAdapter adapter = new AutoCompleteArrayAdapter(
                            CarSelectionActivity.this,
                            android.R.layout.simple_dropdown_item_1line,
                            modelTypes.toArray(new String[modelTypes.size()]));

                    mModelTextView.setAdapter(adapter);
                    mModelTextView.showDropDown();
                }
            }
        });

        mModelTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // nothing to do...
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // nothing to do...
            }

            @Override
            public void afterTextChanged(Editable s) {
                String model = s.toString();
                Set<String> modelYear = mModelToYear.get(model);
                if (modelYear != null && modelYear.size() > 0) {
                    AutoCompleteArrayAdapter adapter = new AutoCompleteArrayAdapter(
                            CarSelectionActivity.this,
                            android.R.layout.simple_dropdown_item_1line,
                            modelYear.toArray(new String[modelYear.size()]));

                    mYearTextView.setAdapter(adapter);
                    mYearTextView.showDropDown();
                }

                Set<String> engine = mModelToCCM.get(model);
                if (engine != null && modelYear.size() > 0) {
                    AutoCompleteArrayAdapter adapter = new AutoCompleteArrayAdapter(
                            CarSelectionActivity.this,
                            android.R.layout.simple_dropdown_item_1line,
                            engine.toArray(new String[engine.size()]));

                    mEngineTextView.setAdapter(adapter);
                }
            }
        });

        mYearTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mEngineTextView.showDropDown();
            }
        });

    }

    /**
     * Creates and shows a snackbar
     *
     * @param msg the message that is gonna shown by the snackbar.
     */
    private void showSnackbar(String msg) {
        Snackbar.make(mFab, msg, Snackbar.LENGTH_LONG).show();
    }


    /**
     * Array adapter for the automatic completion of the AutoCompleteTextView. The intention of
     * this class is to limit the number of visibile suggestions to a bounded number.
     */
    private static class AutoCompleteArrayAdapter extends ArrayAdapter<String> {

        /**
         * Constructor.
         *
         * @param context  the context of the current scope.
         * @param resource the layout resource
         * @param objects  the auto complete suggestions to show.
         */
        public AutoCompleteArrayAdapter(Context context, int resource, String[] objects) {
            super(context, resource, objects);
        }


        @Override
        public int getCount() {
            // We only want to show a maximum of 2 suggestions.
            return Math.min(2, super.getCount());
        }
    }
}
