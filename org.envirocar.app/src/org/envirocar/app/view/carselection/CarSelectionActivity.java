package org.envirocar.app.view.carselection;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.envirocar.app.R;
import org.envirocar.app.application.CarPreferenceHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.dao.DAOProvider;
import org.envirocar.app.model.dao.exception.SensorRetrievalException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public class CarSelectionActivity extends BaseInjectorActivity {
    private static final Logger LOGGER = Logger.getLogger(CarSelectionActivity.class);

    private static final int DURATION_SHEET_ANIMATION = 350;


    @InjectView(R.id.activity_car_selection_layout_content)
    protected View mContentView;
    @InjectView(R.id.activity_car_selection_layout_toolbar)
    protected Toolbar mToolbar;

    @InjectView(R.id.overlay)
    protected View mOverlay;
    @InjectView(R.id.activity_car_selection_new_car_sheet)
    protected View mSheetView;
    @InjectView(R.id.fab)
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
    @InjectView(R.id.activity_car_selection_add_car_button)
    protected Button mAddCarButton;


    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected CarPreferenceHandler mCarManager;

    private Set<Car> mCars = Sets.newHashSet();
    private Set<String> mManufacturerNames;
    private Map<String, Set<String>> mCarToModelMap = Maps.newConcurrentMap();
    private Map<String, Set<String>> mModelToYear = Maps.newConcurrentMap();
    private Map<String, Set<String>> mModelToCCM = Maps.newConcurrentMap();

    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    private Subscription mSensoreSubscription;

    private CarListAdapter mCarListAdapter;

    private AutoCompleteArrayAdapter mManufacturerNameAdapter;
    private SupportAnimator mSupportAnimatorReverse;

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


        // Set the onClick listener for the FloatingActionButton. When triggered, the sheet view
        // gets shown.
        mFab.setOnClickListener(v -> animateButton(mFab));
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
            if (!closeSheetView()) {
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // if the sheet view was not visible.
        if (!closeSheetView()) {
            // call the super method.
            super.onBackPressed();
        }
    }

    /**
     * Closes the sheet view if shown.
     *
     * @return true if the sheet view as visible and has been
     */
    private boolean closeSheetView() {
        // If the sheet view is visible.
        if (mSheetView.isShown()) {
            // and there exist a reverse animation.
            if (mSupportAnimatorReverse != null) {
                // Start the animaton.
                mSupportAnimatorReverse.start();
                mSupportAnimatorReverse = null;
            } else {
                // Otherwise, simply reverse the visibility.
                mSheetView.setVisibility(View.INVISIBLE);
                mFab.setVisibility(View.VISIBLE);
            }
            return true;
        }
        // the sheet view was not visible. Therefore, return false.
        return false;
    }

    private void setupListView() {
        Car selectedCar = mCarManager.getCar();
        List<Car> usedCars = mCarManager.getDeserialzedCars();
        mCarListAdapter = new CarListAdapter(this, selectedCar, usedCars, new CarListAdapter
                .OnCarListActionCallback() {

            @Override
            public void onSelectCar(Car car) {
                mCarManager.setCar(car);
                showSnackbar(String.format("%s %s selected as my car",
                        car.getManufacturer(), car.getModel()));
            }

            @Override
            public void onDeleteCar(Car car) {
                LOGGER.info(String.format("onDeleteCar(%s %s %s %s)", car.getManufacturer(), car
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

        mAddCarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Check views.
                String manufacturer = mManufacturerTextView.getText().toString();
                String model = mModelTextView.getText().toString();
                int year = Integer.parseInt(mYearTextView.getText().toString());
                int engine = Integer.parseInt(mEngineTextView.getText().toString());

                Car selectedCar = null;
                if (mManufacturerNames.contains(manufacturer)
                        && mCarToModelMap.get(manufacturer).contains(model)
                        && mModelToCCM.get(model).contains("" + engine)
                        && mModelToYear.get(model).contains("" + year)) {
                    for (Car car : mCars) {
                        if (car.getManufacturer().equals(manufacturer)
                                && car.getModel().equals(model)
                                && car.getConstructionYear() == year
                                && car.getEngineDisplacement() == engine) {
                            selectedCar = car;
                        }
                    }
                }

                if (selectedCar == null) {
                    selectedCar = new Car(Car.FuelType.GASOLINE, manufacturer, model, null, year,
                            engine);
                    mCarManager.registerCarAtServer(selectedCar);
                } else {
//                    Toast.makeText(CarSelectionActivity.this, "YEA found", Toast.LENGTH_LONG).show();
                }

                // When the car has been successfully inserted in the listadapter, then update
                // the list adapter.
                if (mCarManager.addCar(selectedCar)) {
                    // Add the car to the adapter and close the sheet view.
                    mCarListAdapter.addCarItem(selectedCar);
                    closeSheetView();

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
        });
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
        try {
            mSensoreSubscription = AppObservable.bindActivity(this,
                    mDAOProvider.getSensorDAO().getSensorObservable()
                            .onBackpressureBuffer(10000)
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.computation()))
                    .subscribe(new Observer<Car>() {

                        @Override
                        public void onCompleted() {
                            mMainThreadWorker.schedule(() -> {
                                Toast.makeText(CarSelectionActivity.this, "Received! " +
                                        mManufacturerNames.size(), Toast.LENGTH_SHORT).show();
                                mManufacturerNameAdapter = new AutoCompleteArrayAdapter(
                                        CarSelectionActivity.this,
                                        android.R.layout.simple_dropdown_item_1line,
                                        mManufacturerNames.toArray(
                                                new String[mManufacturerNames.size()]));
                                mManufacturerTextView.setAdapter(mManufacturerNameAdapter);
                            });
                        }

                        @Override
                        public void onError(Throwable e) {
                            LOGGER.error("Error!", e);
                            mMainThreadWorker.schedule(() ->
                                    Toast.makeText(CarSelectionActivity.this, "ERROR!", Toast
                                            .LENGTH_SHORT).show());
                        }

                        @Override
                        public void onNext(Car car) {
                            addCarToAutocompleteList(car);
                        }
                    });
        } catch (SensorRetrievalException e) {
            e.printStackTrace();
        }
    }

    private void animateButton(final FloatingActionButton fab) {

        //        fab.animate()
        //                .translationXBy(0.5f)
        //                .translationYBy(-0.5f)
        //                .translationX(-mSheetView.getWidth()/2)
        //                .translationY(-mSheetView.getHeight()/2)
        //                .setDuration(300)
        //                .setListener(new AnimatorListenerAdapter() {
        //                    @Override
        //                    public void onAnimationEnd(Animator animation) {
        //                        super.onAnimationEnd(animation);
        startSheetAnimation((int) fab.getX(), (int) fab.getY(), fab);
        //                    }
        //                });
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

    private void startSheetAnimation(int cx, int cy, final FloatingActionButton fab) {
        float finalRadius = (float) Math.sqrt(Math.pow(mSheetView.getWidth(), 2)
                + Math.pow(mSheetView.getHeight(), 2));
        Log.e("centerX", "x=" + cx + " y=" + cy);
        int margin = ((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin;
        SupportAnimator animator = ViewAnimationUtils.createCircularReveal(
                mSheetView,
                cx + fab.getWidth() / 2,
                cy - fab.getHeight() / 2 - margin / 2, 0,
                finalRadius);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(DURATION_SHEET_ANIMATION);
        animator.addListener(new SupportAnimator.AnimatorListener() {
            @Override
            public void onAnimationStart() {
                mOverlay.setVisibility(View.VISIBLE);
                fab.setVisibility(View.INVISIBLE);
                mSheetView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd() {
                // Nothind to do
            }

            @Override
            public void onAnimationCancel() {
                // Nothind to do
            }

            @Override
            public void onAnimationRepeat() {
                // Nothing to do
            }
        });

        mSupportAnimatorReverse = animator.reverse();
        mSupportAnimatorReverse.setDuration(DURATION_SHEET_ANIMATION);
        mSupportAnimatorReverse.addListener(new SupportAnimator.AnimatorListener() {
            @Override
            public void onAnimationStart() {
                // Nothing to do
            }

            @Override
            public void onAnimationEnd() {
                mOverlay.setVisibility(View.INVISIBLE);
                fab.setVisibility(View.VISIBLE);
                mSheetView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel() {
                // Nothing to do
            }

            @Override
            public void onAnimationRepeat() {
                // Nothing to do
            }
        });
        animator.start();
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
