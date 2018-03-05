package org.envirocar.app.view.carselection;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.jakewharton.rxbinding.support.v7.widget.RxToolbar;

import org.envirocar.app.R;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.view.utils.ECAnimationUtils;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.CarImpl;
import org.envirocar.core.injection.BaseInjectorFragment;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.DAOProvider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class CarSelectionAddCarFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(CarSelectionAddCarFragment.class);

    @InjectView(R.id.activity_car_selection_newcar_toolbar)
    protected Toolbar toolbar;
    @InjectView(R.id.activity_car_selection_newcar_toolbar_exp)
    protected View toolbarExp;
    @InjectView(R.id.activity_car_selection_newcar_content_view)
    protected View contentView;
    @InjectView(R.id.activity_car_selection_newcar_download_layout)
    protected View downloadView;

    @InjectView(R.id.activity_car_selection_newcar_manufacturer)
    protected TextView manufacturerText;
    @InjectView(R.id.activity_car_selection_newcar_manufacturer_spinner)
    protected Spinner manufacturerSpinner;

    @InjectView(R.id.activity_car_selection_newcar_model)
    protected TextView modelText;
    @InjectView(R.id.activity_car_selection_newcar_model_spinner)
    protected Spinner modelSpinner;

    @InjectView(R.id.activity_car_selection_newcar_year)
    protected TextView yearText;
    @InjectView(R.id.activity_car_selection_newcar_year_spinner)
    protected Spinner yearSpinner;

    @InjectView(R.id.activity_car_selection_newcar_engine)
    protected TextView engineText;
    @InjectView(R.id.activity_car_selection_newcar_engine_spinner)
    protected Spinner engineSpinner;

    @InjectView(R.id.activity_car_selection_newcar_radio_group)
    protected RadioGroup fuelTypeRadioGroup;
    @InjectView(R.id.activity_car_selection_newcar_radio_group_gasoline)
    protected RadioButton gasolineRadio;
    @InjectView(R.id.activity_car_selection_newcar_radio_group_diesel)
    protected RadioButton dieselRadio;

    @Inject
    protected DAOProvider daoProvider;
    @Inject
    protected CarPreferenceHandler carManager;

    private Subscription sensorsSubscription;
    private Subscription createCarSubscription;
    private Scheduler.Worker mainThreadWorker = AndroidSchedulers.mainThread().createWorker();

    private Set<Car> mCars = new HashSet<>();
    private Set<String> mManufacturerNames = new HashSet<>();
    private Map<String, Set<String>> mCarToModelMap = new ConcurrentHashMap<>();
    private Map<String, Set<String>> mModelToYear = new ConcurrentHashMap<>();
    private Map<Pair<String, String>, Set<String>> mModelToCCM = new ConcurrentHashMap<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(
                R.layout.activity_car_selection_newcar_fragment, container, false);
        ButterKnife.inject(this, view);

        // Get the display size in pixels
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        // Set the dropdown width of the spinner to half of the display pixel width.
        manufacturerSpinner.setDropDownWidth(width / 2);
        modelSpinner.setDropDownWidth(width / 2);
        yearSpinner.setDropDownWidth(width / 2);
        engineSpinner.setDropDownWidth(width / 2);

        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        toolbar.inflateMenu(R.menu.menu_logbook_add_fueling);
        toolbar.setNavigationOnClickListener(v -> {hideKeyboard(v); closeThisFragment(); });


        // initially we set the toolbar exp to gone
        toolbar.setVisibility(View.GONE);
        toolbarExp.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
        downloadView.setVisibility(View.INVISIBLE);

        createCarSubscription = RxToolbar.itemClicks(toolbar)
                .filter(continueWhenFormIsCorrect())
                .map(createCarFromForm())
                .filter(continueWhenCarHasCorrectValues())
                .map(checkCarAlreadyExist())
                .subscribe(new Subscriber<Car>() {
                    @Override
                    public void onCompleted() {
                        LOG.info("onCompleted car");
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.warn(e.getMessage(), e);
                    }

                    @Override
                    public void onNext(Car car) {
                        LOG.info("car added");
                        ((CarSelectionUiListener) getActivity()).onCarAdded(car);
                        hideKeyboard(getView());
                        closeThisFragment();
                    }
                });


        dispatchRemoteSensors();

        initFocusChangedListener();
        initTextWatcher();
        return view;
    }

    @Override
    public void onResume() {
        LOG.info("onResume()");
        super.onResume();
        ECAnimationUtils.animateShowView(getContext(), toolbar,
                R.anim.translate_slide_in_top_fragment);
        ECAnimationUtils.animateShowView(getContext(), toolbarExp,
                R.anim.translate_slide_in_top_fragment);
        ECAnimationUtils.animateShowView(getContext(), contentView,
                R.anim.translate_slide_in_bottom_fragment);
    }

    @Override
    public void onDestroy() {
        LOG.info("onDestroy()");

        if (sensorsSubscription != null && !sensorsSubscription.isUnsubscribed()) {
            sensorsSubscription.unsubscribe();
        }
        if (createCarSubscription != null && !createCarSubscription.isUnsubscribed()) {
            createCarSubscription.unsubscribe();
        }

        super.onDestroy();
    }


    /**
     * Add car button onClick listener. When clicked, it tries to find out if the car already
     * exists. If this is the case, then it adds the car to the list of selected cars. If not,
     * then it selects
     */
    private Func1<MenuItem, Boolean> continueWhenFormIsCorrect() {
        return menuItem -> {
            // First, reset the form
            manufacturerText.setError(null);
            modelText.setError(null);
            yearText.setError(null);
            engineText.setError(null);

            View focusView = null;

            //First check all input forms for empty strings
            if (engineText.getText().length() == 0) {
                engineText.setError("Cannot be empty");
                focusView = engineText;
            }
            if (yearText.getText().length() == 0) {
                yearText.setError("Cannot be empty");
                focusView = yearText;
            }
            if (modelText.getText().length() == 0) {
                modelText.setError("Cannot be empty");
                focusView = modelText;
            }
            if (manufacturerText.getText().length() == 0) {
                manufacturerText.setError("Cannot be empty");
                focusView = manufacturerText;
            }

            // if any of the input forms contained empty values, then set the focus to the
            // last one set.
            if (focusView != null) {
                LOG.info("Some input fields were empty");
                focusView.requestFocus();
                return false;
            } else {
                return true;
            }
        };
    }

    private <T> Func1<T, Car> createCarFromForm() {
        return t -> {
            // Get the values
            String manufacturer = manufacturerText.getText().toString();
            String model = modelText.getText().toString();
            String yearString = yearText.getText().toString();
            String engineString = engineText.getText().toString();
            Car.FuelType fuelType = gasolineRadio.isChecked() ?
                    Car.FuelType.GASOLINE : Car.FuelType.DIESEL;

            // create the car
            return new CarImpl(manufacturer, model, fuelType,
                    Integer.parseInt(yearString), Integer.parseInt(engineString));
        };
    }

    private Func1<Car, Boolean> continueWhenCarHasCorrectValues() {
        return car -> {
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            View focusView = null;

            // Check the values of engine and year for validity.
            if (car.getEngineDisplacement() < 500 || car.getEngineDisplacement() > 5000) {
                engineText.setError("Invalid value");
                focusView = engineText;
            }
            if (car.getConstructionYear() < 1990 || car.getConstructionYear() > currentYear) {
                yearText.setError("Invalid value");
                focusView = yearText;
            }

            // if tengine or year have invalid values, then request the focus.
            if (focusView != null) {
                focusView.requestFocus();
                return false;
            }

            return true;
        };
    }

    private Func1<Car, Car> checkCarAlreadyExist() {
        return car -> {
            String manu = car.getManufacturer();
            String model = car.getModel();
            String year = "" + car.getConstructionYear();
            String engine = "" + car.getEngineDisplacement();
            Pair<String, String> modelYear = new Pair<>(model, year);

            Car selectedCar = null;
            if (mManufacturerNames.contains(manu)
                    && mCarToModelMap.get(manu) != null
                    && mCarToModelMap.get(manu).contains(model)
                    && mModelToYear.get(model) != null
                    && mModelToYear.get(model).contains(year)
                    && mModelToCCM.get(modelYear) != null
                    && mModelToCCM.get(modelYear).contains(engine)) {
                for (Car other : mCars) {
                    if (other.getManufacturer().equals(manu)
                            && other.getModel().equals(model)
                            && other.getConstructionYear() == car.getConstructionYear()
                            && other.getEngineDisplacement() == car.getEngineDisplacement()
                            && other.getFuelType() == car.getFuelType()) {
                        selectedCar = other;
                        break;
                    }
                }
            }

            if (selectedCar == null) {
                LOG.info("New Car type. Register car at server.");
                carManager.registerCarAtServer(car);
                return car;
            } else {
                LOG.info(String.format("Car already existed -> [%s]", selectedCar.getId()));
                return selectedCar;
            }
        };
    }

    private void dispatchRemoteSensors() {
        sensorsSubscription = daoProvider.getSensorDAO()
                .getAllCarsObservable()
                .onBackpressureBuffer(10000)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Subscriber<List<Car>>() {
                    @Override
                    public void onStart() {
                        LOG.info("onStart() download sensors");
                        downloadView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("onCompleted(): cars successfully downloaded.");

                        mainThreadWorker.schedule(() -> {
                            // Update the manufactuerers in
                            updateSpinner(mManufacturerNames, manufacturerSpinner);

                            // Set the initial selection of the manufacturer to NO SELECTION
                            manufacturerSpinner.setSelection(Adapter.NO_SELECTION, true);

                            // Initialize the spinner.
                            initSpinner();
                            unsubscribe();

                            downloadView.setVisibility(View.INVISIBLE);
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);
                        mainThreadWorker.schedule(() -> {
                            downloadView.setVisibility(View.INVISIBLE);
                        });
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

    private void initTextWatcher() {
        manufacturerText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Nothing to do..
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // nothing to do..
            }

            @Override
            public void afterTextChanged(Editable s) {
                manufacturerText.setError(null);

                modelText.setText("");
                yearText.setText("");
                engineText.setText("");

                modelSpinner.setAdapter(null);
                yearSpinner.setAdapter(null);
                engineSpinner.setAdapter(null);
            }
        });

        modelText.addTextChangedListener(new TextWatcher() {
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
                modelText.setError(null);

                yearText.setText("");
                engineText.setText("");
            }
        });

        yearText.addTextChangedListener(new TextWatcher() {
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
                yearText.setError(null);

                engineText.setText("");
            }
        });

        engineText.addTextChangedListener(new TextWatcher() {
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
                engineText.setError(null);
            }
        });
    }

    private void initFocusChangedListener() {
        manufacturerText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String manufacturer = manufacturerText.getText().toString();
                updateModelViews(manufacturer);
            }
        });

        modelText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String model = modelText.getText().toString();
                updateYearView(model);
            }
        });

        yearText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String year = yearText.getText().toString();
                String model = modelText.getText().toString();
                Pair<String, String> modelYear = new Pair<>(model, year);

                updateEngineView(modelYear);
            }
        });

        engineText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                checkFuelingType();
            }
        });
    }

    private void initSpinner() {
        manufacturerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String manufacturer = parent.getItemAtPosition(position).toString();
                LOG.info(String.format("manufactuererSpinner.onItemSelected(%s)", manufacturer));

                // update the manufacturer textview.
                manufacturerText.setText(manufacturer);
                ((TextView) view).setText(null);

                // update the model views
                updateModelViews(manufacturer);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String model = parent.getItemAtPosition(position).toString();
                LOG.info(String.format("modelSpinner.onItemSelected(%s)", model));

                modelText.setText(model);
                ((TextView) view).setText(null);

                updateYearView(model);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        yearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String year = parent.getItemAtPosition(position).toString();
                LOG.info(String.format("yearSpinner.onItemSelected(%s)", year));

                yearText.setText(year);
                ((TextView) view).setText(null);

                updateEngineView(new Pair<>(modelText.getText().toString(), year));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        engineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String engine = parent.getItemAtPosition(position).toString();
                LOG.info(String.format("engineSpinner.onItemSelected(%s)", engine));

                engineText.setText(engine);
                ((TextView) view).setText(null);

                checkFuelingType();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void checkFuelingType() {
        String manufacturer = manufacturerText.getText().toString();
        String model = modelText.getText().toString();
        String yearString = yearText.getText().toString();
        String engineString = engineText.getText().toString();
        Pair<String, String> modelYear = new Pair<>(model, yearString);

        Car selectedCar = null;
        if (mManufacturerNames.contains(manufacturer)
                && mCarToModelMap.get(manufacturer) != null
                && mCarToModelMap.get(manufacturer).contains(model)
                && mModelToYear.get(model) != null
                && mModelToYear.get(model).contains(yearString)
                && mModelToCCM.get(modelYear) != null
                && mModelToCCM.get(modelYear).contains(engineString)) {
            for (Car other : mCars) {
                if (other.getManufacturer() == null ||
                        other.getModel() == null ||
                        other.getConstructionYear() == 0 ||
                        other.getEngineDisplacement() == 0 ||
                        other.getFuelType() == null) {
                    continue;
                }
                if (other.getManufacturer().equals(manufacturer)
                        && other.getModel().equals(model)
                        && other.getConstructionYear() == Integer.parseInt(yearString)
                        && other.getEngineDisplacement() == Integer.parseInt(engineString)) {
                    selectedCar = other;
                    break;
                }
            }
        }

        if (selectedCar != null &&
                selectedCar.getFuelType() != null &&
                selectedCar.getFuelType() == Car.FuelType.DIESEL) {
            dieselRadio.setChecked(true);
        } else {
            gasolineRadio.setChecked(true);
        }
    }

    private void updateManufacturerViews() {
        if (!mManufacturerNames.isEmpty()) {
            updateSpinner(mManufacturerNames, manufacturerSpinner);
        } else {
            modelSpinner.setAdapter(null);
        }
    }

    private void updateModelViews(String manufacturer) {
        if (mCarToModelMap.containsKey(manufacturer)) {
            updateSpinner(mCarToModelMap.get(manufacturer), modelSpinner);
        } else {
            modelSpinner.setAdapter(null);
        }
    }

    private void updateYearView(String model) {
        if (mModelToYear.containsKey(model)) {
            updateSpinner(mModelToYear.get(model), yearSpinner);
        } else {
            yearSpinner.setAdapter(null);
        }
    }

    private void updateEngineView(Pair<String, String> model) {
        if (mModelToCCM.containsKey(model)) {
            updateSpinner(mModelToCCM.get(model), engineSpinner);
        } else {
            engineSpinner.setAdapter(null);
        }
    }

    private void updateAutoComplete(Set<String> toSet, TextView textView) {
        List<String> list = new ArrayList<>();
        list.addAll(toSet);
        Collections.sort(list);

//        ArrayAdapter<String>
    }

    private void updateSpinner(Set<String> toSet, Spinner spinner) {
        List<String> list = new ArrayList<>();
        list.addAll(toSet);
        Collections.sort(list);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getActivity(),
                R.layout.activity_car_selection_newcar_spinner_item,
                list.toArray(new String[list.size()]));

        spinner.setAdapter(adapter);
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
        String year = "" + car.getConstructionYear();

        if (!mManufacturerNames.contains(manufacturer))
            mManufacturerNames.add(manufacturer);

        if (!mCarToModelMap.containsKey(manufacturer))
            mCarToModelMap.put(manufacturer, new HashSet<>());
        mCarToModelMap.get(manufacturer).add(model);

        if (!mModelToYear.containsKey(model))
            mModelToYear.put(model, new HashSet<>());
        mModelToYear.get(model).add(Integer.toString(car.getConstructionYear()));

        Pair<String, String> modelYearPair = new Pair<>(model, year);
        if (!mModelToCCM.containsKey(modelYearPair))
            mModelToCCM.put(modelYearPair, new HashSet<>());
        mModelToCCM.get(modelYearPair).add(Integer.toString(car.getEngineDisplacement()));
    }

    public void closeThisFragment() {
        // ^^
        ECAnimationUtils.animateHideView(getContext(),
                ((CarSelectionActivity) getActivity()).overlayView, R.anim.fade_out);
        ECAnimationUtils.animateHideView(getContext(), R.anim
                .translate_slide_out_top_fragment, toolbar, toolbarExp);
        ECAnimationUtils.animateHideView(getContext(), contentView, R.anim
                .translate_slide_out_bottom, new Action0() {
            @Override
            public void call() {
                ((CarSelectionUiListener) getActivity()).onHideAddCarFragment();
            }
        });
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
