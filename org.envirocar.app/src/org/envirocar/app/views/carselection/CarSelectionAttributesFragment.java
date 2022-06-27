/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
package org.envirocar.app.views.carselection;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jakewharton.rxbinding3.widget.RxTextView;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.core.entity.Manufacturers;
import org.envirocar.core.entity.Vehicles;
import org.envirocar.core.logging.Logger;
import org.envirocar.storage.EnviroCarVehicleDB;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class CarSelectionAttributesFragment extends BaseInjectorFragment {

    @BindView(R.id.fragment_attributes_manufacturer_input)
    protected AutoCompleteTextView manufactureEditText;
    @BindView(R.id.fragment_attributes_model_input)
    protected AutoCompleteTextView modelEditText;
    @BindView(R.id.fragment_attributes_year_input)
    protected AutoCompleteTextView yearEditText;
    @BindView(R.id.fragment_attributes_fueltype_input)
    protected AutoCompleteTextView fuelTypeSelection;
    @BindView(R.id.fragment_attributes_displacement_input)
    protected EditText displacementEditText;
    @BindView(R.id.fragment_attributes_weight_input)
    protected EditText weightEditText;
    @BindView(R.id.fragment_attributes_utility_input)
    protected AutoCompleteTextView utilityTypeSelection;

    @BindView(R.id.fragment_car_search_button_text)
    protected TextView searchButton;

    @Inject
    EnviroCarVehicleDB enviroCarVehicleDB;
    private static final Logger LOG = Logger.getLogger(CarSelectionAttributesFragment.class);
    private Set<String> mManufacturerNames = new HashSet<>();
    private Map<String, Set<String>> mCarToModelMap = new ConcurrentHashMap<>();
    private Map<Pair<String, String>, Set<String>> mManufactureModelToYear = new ConcurrentHashMap<>();
    private Scheduler.Worker mainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    private CompositeDisposable disposable = new CompositeDisposable();
    private static final int ERROR_DEBOUNCE_TIME = 750;
    private List<Manufacturers> manufacturersList;
    private static Drawable error;

    CarSelectionAttributesFragment(List<Manufacturers> manufacturersList) {
        this.manufacturersList = manufacturersList;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_car_selection_attributes, container, false);
        ButterKnife.bind(this, view);
        fetchManufactures();
        initFocusChangedListener();
        initManufacturerTextChangeListener();
        error = getResources().getDrawable(R.drawable.ic_error_red_24dp);
        error.setBounds(-50, 0, 0, error.getIntrinsicHeight());
        manufactureEditText.setOnItemClickListener((parent, view1, position, id) -> requestNextTextFieldFocus(manufactureEditText));
        modelEditText.setOnItemClickListener((parent, view12, position, id) -> requestNextTextFieldFocus(modelEditText));
        yearEditText.setOnItemClickListener((parent, view13, position, id) -> requestNextTextFieldFocus(yearEditText));

        List<String> fuelTypes = Arrays.asList(getContext().getString(R.string.fuel_type_gasoline), getContext().getString(R.string.fuel_type_diesel),
                getContext().getString(R.string.fuel_type_electric), getContext().getString(R.string.fuel_type_gas), getContext().getString(R.string.fuel_type_hybrid));

        ArrayAdapter<String> fuelTypesAdapter = new ArrayAdapter<>(
            getContext(),
            R.layout.activity_car_selection_newcar_fueltype_item,
            fuelTypes);
        fuelTypeSelection.setAdapter(fuelTypesAdapter);
        fuelTypeSelection.setText(fuelTypesAdapter.getItem(0).toString(), false);

        String[] utilityTypes = new String[]{getContext().getString(R.string.car_selection_private_vehicle), getContext().getString(R.string.car_selection_utility_car), getContext().getString(R.string.car_selection_taxi)};

        ArrayAdapter<String> utilityTypesAdapter = new ArrayAdapter<>(
            getContext(),
            R.layout.activity_car_selection_newcar_fueltype_item,
            utilityTypes);
        utilityTypeSelection.setAdapter(utilityTypesAdapter);
        utilityTypeSelection.setText(utilityTypesAdapter.getItem(0).toString(), false);

        return view;
    }

    @Override
    public void onDestroy() {
        disposable.dispose();
        super.onDestroy();
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @OnTextChanged(value = R.id.fragment_attributes_manufacturer_input, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    protected void onManufacturerChanged() {
        manufactureEditText.setError(null);
        modelEditText.setText("");
        yearEditText.setText("");
    }

    @OnTextChanged(value = R.id.fragment_attributes_model_input, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    protected void onModelChanged() {
        modelEditText.setError(null);
        yearEditText.setText("");
    }

    @OnTextChanged(value = R.id.fragment_attributes_year_input, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    protected void onYearChanged() {
        yearEditText.setError(null);
    }

    @OnClick(R.id.fragment_car_search_button)
    void searchButtonClick() {
        String manufacturer = manufactureEditText.getText().toString().trim();
        String model = modelEditText.getText().toString().trim();
        String year = yearEditText.getText().toString().trim();
        View focusView = null;
        if (manufacturer.isEmpty()) {
            manufactureEditText.setError(getString(R.string.car_selection_error_empty_input), error);
            focusView = manufactureEditText;
        }

        if (model.isEmpty()) {
            modelEditText.setError(getString(R.string.car_selection_error_select_from_list), error);
            focusView = modelEditText;
        }

        if (year.isEmpty()) {
            yearEditText.setError(getString(R.string.car_selection_error_select_from_list), error);
            focusView = yearEditText;
        }

        //focus on last and stop searching and return
        if (focusView != null) {
            focusView.requestFocus();
            return;
        }

        // also stop searching if already error becuase of values not in list
        if (manufactureEditText.getError() != null || modelEditText.getError() != null || yearEditText.getError() != null) {
            return;
        }

        if (hasStoredVehicle()) {
            // launch the selection fragment
            Single<List<Vehicles>> vehicle = enviroCarVehicleDB.vehicleDAO().getVehicleAttributeType(manufacturer, model, year);
                vehicle.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribeWith(new DisposableSingleObserver<List<Vehicles>>() {
                    @Override
                    public void onSuccess(List<Vehicles> vehiclesList) {
                        CarListFragment carListFragment = new CarListFragment(vehiclesList);
                        carListFragment.show(getFragmentManager(), carListFragment.getTag());
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i("fetchError", "" + e.getMessage());
                    }
                });
        } else {
            // create new car without selection
            Vehicles vehicle = createVehicleFromFields();
            if (vehicle != null) {
                ((CarSelectionActivity) getActivity()).registerCar(vehicle);
            }
        }
        
    }


    private Vehicles createVehicleFromFields() {
        String manufacturer = manufactureEditText.getText().toString().trim();
        String model = modelEditText.getText().toString().trim();
        String year = yearEditText.getText().toString().trim();
        String fuelType = fuelTypeSelection.getText().toString().trim();
        String displacement = displacementEditText.getText().toString().trim();
        String vehicleType = utilityTypeSelection.getText().toString().trim();
        String weight = weightEditText.getText().toString().trim();

        View focusView = null;
        if (fuelType.isEmpty()) {
            fuelTypeSelection.setError(getString(R.string.car_selection_error_empty_input), error);
            focusView = fuelTypeSelection;
        }

        if (displacement.isEmpty()) {
            displacementEditText.setError(getString(R.string.car_selection_error_select_from_list), error);
            focusView = displacementEditText;
        }

        if (vehicleType.equals(getContext().getString(R.string.car_selection_private_vehicle))) {
            vehicleType = getEnglishString(R.string.car_selection_private_vehicle);
        } else if (vehicleType.equals(getContext().getString(R.string.car_selection_utility_car))) {
            vehicleType = getEnglishString(R.string.car_selection_utility_car);
        } else if (vehicleType.equals(getContext().getString(R.string.car_selection_taxi))) {
            vehicleType = getEnglishString(R.string.car_selection_taxi);
        }

        if (weight.length() == 0) {
            weight = null;
        }


        // focus on error and return null
        if (focusView != null) {
            focusView.requestFocus();
            return null;
        }

        Vehicles vehicle = new Vehicles();
        vehicle.setManufacturer(manufacturer);
        vehicle.setCommerical_name(model);
        vehicle.setAllotment_date("01.01." + year);
        
        vehicle.setEngine_capacity(displacement);
        vehicle.setPower_source_id(getFuelTypeId(fuelType));

        vehicle.setWeight(weight);
        vehicle.setVehicleType(vehicleType);

        return vehicle;
    }

    private String getFuelTypeId(String id) {
        String fuel = null;
        if (id.equalsIgnoreCase(getContext().getString(R.string.fuel_type_gasoline)))
            fuel = "01";
        else if (id.equalsIgnoreCase(getContext().getString(R.string.fuel_type_diesel)))
            fuel = "02";
        else if (id.equalsIgnoreCase(getContext().getString(R.string.fuel_type_electric)))
            fuel = "04";
        else if (id.equalsIgnoreCase(getContext().getString(R.string.fuel_type_gas)))
            fuel = "05";

        return fuel;
    }

    @NonNull
    protected String getEnglishString(int res) {
        Configuration configuration = getEnglishConfiguration();
    
        return getContext().createConfigurationContext(configuration).getResources().getString(res);
    }
    
    @NonNull
    private Configuration getEnglishConfiguration() {
        Configuration configuration = new Configuration(getContext().getResources().getConfiguration());
        configuration.setLocale(new Locale("en"));
        return configuration;
    }

    private void fetchManufactures() {
        if (manufacturersList != null) {
            for (Manufacturers manufacturers : manufacturersList) {
                mManufacturerNames.add(manufacturers.getName());
            }
        }
        updateManufacturerView();
    }

    private void fetchVehicles(String manufacturersName) {
        Observable<List<Vehicles>> getManufacturersVehicles = enviroCarVehicleDB.vehicleDAO().getManufacturerVehicles(manufacturersName);
        getManufacturersVehicles.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<List<Vehicles>>() {
                    @Override
                    public void onNext(List<Vehicles> vehiclesList) {
                        for (Vehicles vehicles : vehiclesList) {
                            addCarToAutocompleteList(vehicles);
                        }
                        updateModelView(manufacturersName);
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.info("vehicleFetch():",e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void addCarToAutocompleteList(Vehicles vehicle) {
        mManufacturerNames.add(vehicle.getManufacturer());

        if (!mCarToModelMap.containsKey(vehicle.getManufacturer()))
            mCarToModelMap.put(vehicle.getManufacturer(), new HashSet<>());
        mCarToModelMap.get(vehicle.getManufacturer()).add(vehicle.getCommerical_name());
        Pair<String, String> manufactureModel = new Pair<>(vehicle.getManufacturer(), vehicle.getCommerical_name());
        if (!mManufactureModelToYear.containsKey(manufactureModel))
            mManufactureModelToYear.put(manufactureModel, new HashSet<>());
        int year = ((CarSelectionActivity) getActivity()).convertDateToInt(vehicle.getAllotment_date());
        String yearString = Integer.toString(year);
        mManufactureModelToYear.get(manufactureModel).add(yearString);
    }

    private void updateManufacturerView() {
        if (!mManufacturerNames.isEmpty()) {
            manufactureEditText.setAdapter(((CarSelectionActivity) getActivity()).sortedAdapter(getContext(), mManufacturerNames));
        } else {
            manufactureEditText.setAdapter(null);
            searchButton.setText("Add new car");
        }
    }

    private void updateModelView(String manufacturer) {
        if (mCarToModelMap.containsKey(manufacturer)) {
            modelEditText.setAdapter(((CarSelectionActivity) getActivity()).sortedAdapter(getContext(), mCarToModelMap.get(manufacturer)));
        } else {
            modelEditText.setAdapter(null);
            searchButton.setText("Add new car");
        }
    }

    private void updateYearView(Pair<String, String> manufactureModel) {
        if (mManufactureModelToYear.containsKey(manufactureModel)) {
            yearEditText.setAdapter(((CarSelectionActivity) getActivity()).sortedAdapter(getContext(), mManufactureModelToYear.get(manufactureModel)));
        } else {
            yearEditText.setAdapter(null);
            searchButton.setText("Add new car");
        }
    }

    private void initFocusChangedListener() {
        manufactureEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String manufacturer = manufactureEditText.getText().toString();
                fetchVehicles(manufacturer);
            }
        });
        modelEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String model = modelEditText.getText().toString();
                String manufacture = manufactureEditText.getText().toString();
                updateYearView(new Pair<>(manufacture, model));
            }
        });
    }

    private void requestNextTextFieldFocus(TextView textField) {
        try {
            TextView nextField = (TextView) textField.focusSearch(View.FOCUS_DOWN);
            nextField.requestFocus();
        } catch (Exception e) {
            LOG.warn("Unable to find next field or to request focus to next field.");
        }
        hideKeyboard(textField);
    }

    private void initManufacturerTextChangeListener() {
        disposable.add(RxTextView.textChanges(manufactureEditText)
                .skipInitialValue()
                .debounce(ERROR_DEBOUNCE_TIME, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(t -> t.toString())
                .subscribe(manufacture -> {
                    // ListAdapter adapter = manufactureEditText.getAdapter();
                    // int flag = 0;
                    // for (int i = 0; i < adapter.getCount(); i++) {
                    //     if (adapter.getItem(i).toString().compareTo(manufacture) == 0) {
                    //         flag = 1;
                    //         break;
                    //     }
                    // }
                    // if (flag == 0) {
                    //     manufactureEditText.setError(getString(R.string.car_selection_error_select_from_list), error);
                    //     manufactureEditText.requestFocus();
                    // } else {
                    //     manufactureEditText.setError(null);
                    // }
                    updateSearchButtonState();
                    manufactureEditText.setError(null);
                }));

        disposable.add(RxTextView.textChanges(modelEditText)
                .skipInitialValue()
                .debounce(ERROR_DEBOUNCE_TIME, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(t -> t.toString())
                .subscribe(model -> {
                    try {
                        // ListAdapter adapter = modelEditText.getAdapter();
                        // int flag = 0;
                        // for (int i = 0; i < adapter.getCount(); i++) {
                        //     if (adapter.getItem(i).toString().compareTo(model) == 0) {
                        //         flag = 1;
                        //         break;
                        //     }
                        // }

                        // if (flag == 0) {
                        //     modelEditText.setError(getString(R.string.car_selection_error_select_from_list), error);
                        //     modelEditText.requestFocus();
                        // } else {
                        //     modelEditText.setError(null);
                        // }
                        updateSearchButtonState();
                        modelEditText.setError(null);
                    } catch (Exception e) {
                    }
                }));

        disposable.add(RxTextView.textChanges(yearEditText)
                .skipInitialValue()
                .debounce(ERROR_DEBOUNCE_TIME, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(t -> t.toString())
                .subscribe(year -> {
                    try {
                        // ListAdapter adapter = yearEditText.getAdapter();
                        // int flag = 0;
                        // for (int i = 0; i < adapter.getCount(); i++) {
                        //     if (adapter.getItem(i).toString().compareTo(year) == 0) {
                        //         flag = 1;
                        //         break;
                        //     }
                        // }

                        // if (flag == 0) {
                        //     yearEditText.setError(getString(R.string.car_selection_error_select_from_list), error);
                        //     yearEditText.requestFocus();
                        // } else {
                        //     yearEditText.setError(null);
                        // }
                        updateSearchButtonState();
                        yearEditText.setError(null);
                    } catch (Exception e) {
                    }
                }));
    }

    private void updateSearchButtonState() {
        if (hasStoredVehicle()) {
            searchButton.setText(R.string.car_selection_search);
        } else {
            searchButton.setText(R.string.car_selection_add_new_car);
        }
    }

    private boolean hasStoredVehicle() {
        String manufacturer = manufactureEditText.getText().toString().trim();
        String model = modelEditText.getText().toString().trim();
        String year = yearEditText.getText().toString().trim();

        // check manufacturer
        boolean found = checkAdapterContainsEntry(manufactureEditText.getAdapter(), manufacturer);
        if (!found) {
            return false;
        }

        // check model
        found = checkAdapterContainsEntry(modelEditText.getAdapter(), model);
        if (!found) {
            return false;
        }

        // check year
        found = checkAdapterContainsEntry(yearEditText.getAdapter(), year);

        return found;
    }

    private boolean checkAdapterContainsEntry(ListAdapter adapter, String text) {
        if (adapter == null) {
            return false;
        }
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(text)) {
                return true;
            }
        }
        return false;
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
