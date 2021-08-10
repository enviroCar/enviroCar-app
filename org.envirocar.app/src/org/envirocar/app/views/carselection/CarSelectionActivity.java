/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.carselection;

import android.content.Context;
import android.os.Bundle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;

import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;
import org.envirocar.app.handler.preferences.UserPreferenceHandler;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.core.entity.Car;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.core.entity.CarImpl;
import org.envirocar.core.entity.Vehicles;
import org.envirocar.core.logging.Logger;
import org.envirocar.app.handler.DAOProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * @author dewall
 */
public class CarSelectionActivity extends BaseInjectorActivity implements CarSelectionUiListener, CarSelectionCreation {
    private static final Logger LOG = Logger.getLogger(CarSelectionActivity.class);

    private static final int DURATION_SHEET_ANIMATION = 350;

    @BindView(R.id.activity_car_selection_layout_content)
    protected View mContentView;
    @BindView(R.id.envirocar_toolbar)
    protected Toolbar mToolbar;
    @BindView(R.id.activity_car_selection_layout_exptoolbar)
    protected Toolbar mExpToolbar;
    @BindView(R.id.actvity_car_selection_layout_loading)
    protected View loadingView;
    @BindView(R.id.overlay)
    protected View overlayView;

    @BindView(R.id.activity_car_selection_new_car_fab)
    protected FloatingActionButton mFab;

    @BindView(R.id.activity_car_selection_layout_carlist)
    protected ListView mCarListView;

    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected CarPreferenceHandler mCarManager;
    @Inject
    protected UserPreferenceHandler mUserHandler;

    @BindView(R.id.layout_general_info_background)
    protected View infoBackground;
    @BindView(R.id.layout_general_info_background_img)
    protected ImageView infoBackgroundImg;
    @BindView(R.id.layout_general_info_background_firstline)
    protected TextView infoBackgroundFirst;
    @BindView(R.id.layout_general_info_background_secondline)
    protected TextView infoBackgroundSecond;
    @BindView(R.id.activity_car_selection_header)
    protected View headerView;

    private CarSelectionAddCarFragment addCarFragment;
    private CarSelectionListAdapter mCarListAdapter;
    private Disposable loadingCarsSubscription;


    @Override
    protected void injectDependencies(BaseApplicationComponent appComponent) {
        appComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the content view of this activity.
        setContentView(R.layout.activity_car_selection_layout);

        // Inject all annotated views.
        ButterKnife.bind(this);

        // Set the toolbar as default actionbar.
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
//        getSupportActionBar().setTitle(R.string.car_selection_header);

        // If no cars present show background image.
        if (!mCarManager.hasCars()){
            showbackgroungimage();
        }

        setupListView();
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
        // if the add car fragment is visible.
        if (addCarFragment != null && addCarFragment.isVisible()) {
            addCarFragment.closeThisFragment();
        } else {
            // call the super method.
            super.onBackPressed();
        }
    }

    // Set the onClick listener for the FloatingActionButton. When triggered, the sheet view
    // gets shown.
    @OnClick(R.id.activity_car_selection_new_car_fab)
    public void onClickNewCarButton() {
        showAddCarFragment();
    }

    @Override
    protected void onDestroy() {
        LOG.info("onDestroy()");

        if (this.loadingCarsSubscription != null && !this.loadingCarsSubscription.isDisposed()) {
            this.loadingCarsSubscription.dispose();
        }

        super.onDestroy();
    }

    /**
     * Shows the card view for the addition cars.
     *
     * @return true if the card view was not shown.
     */
    private boolean showAddCarFragment() {
        if (this.addCarFragment != null && this.addCarFragment.isVisible()) {
            LOG.info("addCarFragment is already visible.");
            return false;
        }
        ECAnimationUtils.animateShowView(this, overlayView, R.anim.fade_in);
        ECAnimationUtils.animateHideView(this, mFab, R.anim.fade_out);
        this.addCarFragment = new CarSelectionAddCarFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.activity_car_selection_container, this.addCarFragment)
                .commit();


        // this card was already visible. Therefore, return false.
        return true;
    }

    /**
     * Closes the sheet view if shown.
     *
     * @return true if the sheet view as visible and has been
     */
    private boolean closeAddCarCard() {
        if (this.addCarFragment != null && this.addCarFragment.isVisible()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(addCarFragment)
                    .commit();
            addCarFragment = null;
            ECAnimationUtils.animateShowView(this, mFab, R.anim.fade_in);
            return true;
        }
        return false;
    }

    private void setupListView() {
        Car selectedCar = mCarManager.getCar();
        List<Car> usedCars = new ArrayList<>();

        mCarListAdapter = new CarSelectionListAdapter(this, selectedCar, usedCars,
                new CarSelectionListAdapter.OnCarListActionCallback() {

                    @Override
                    public void onSelectCar(Car car) {
                        mCarManager.setCar(car);
                        showSnackbar(String.format(getString(R.string.car_selection_car_selected),
                                car.getManufacturer(), car.getModel()));
                    }

                    @Override
                    public void onDeleteCar(Car car) {
                        LOG.info(String.format("onDeleteCar(%s %s %s %s)",
                                car.getManufacturer(), car.getModel(),
                                "" + car.getConstructionYear(),
                                "" + car.getEngineDisplacement()));

                        // Create a dialog to confirm the car deletion
                        new MaterialAlertDialogBuilder(CarSelectionActivity.this, R.style.MaterialDialog)
                                .setTitle(R.string.car_deselection_dialog_delete_pairing_title)
                                .setMessage(String.format(getString(R.string.car_deselection_dialog_delete_pairing_content_template),
                                        car.getManufacturer(), car.getModel()))
                                .setIcon(R.drawable.ic_drive_eta_white_24dp)
                                .setPositiveButton(R.string.car_deselection_dialog_delete_title, (dialog, which) -> {
                                    // If the car has been removed successfully...
                                    if (mCarManager.removeCar(car)) {
                                        showSnackbar(String.format(
                                                getString(R.string.car_selection_car_deleted_tmp),
                                                car.getManufacturer(), car.getModel()));
                                        if (!mCarManager.hasCars()) {
                                            showbackgroungimage();
                                        }
                                    }
                                    // then remove it from the list and show a snackbar.
                                    mCarListAdapter.removeCarItem(car);// Nothing to do on cancel

                                })
                                .setNegativeButton(R.string.cancel,null)
                                .show();
                    }
                });
        mCarListView.setAdapter(mCarListAdapter);

        loadingCarsSubscription = mCarManager.getAllDeserializedCars()
                .flatMap(cars -> {
                    Observable<List<Car>> carsObs = Observable.just(cars);
                    if (mUserHandler.isLoggedIn() && !mCarManager.isDownloaded()) {
                        LOG.info("Loading Cars: getUserStatistic has not downloaded its remote cars. " +
                                "Trying to fetch these.");
                        carsObs = carsObs.concatWith(mCarManager.downloadRemoteCarsOfUser());
                    }
                    return carsObs;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<List<Car>>() {
                    @Override
                    public void onStart() {
                        LOG.info("onStart()");
                        loadingView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onComplete() {
                        LOG.info("onCompleted() loading of all cars");
                        loadingView.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);
                        loadingView.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onNext(List<Car> cars) {
                        LOG.info("onNext() " + cars.size());
                        for (Car car : cars) {
                            if (!usedCars.contains(car))
                                usedCars.add(car);
                        }
                        mCarListAdapter.notifyDataSetInvalidated();
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

    public void showbackgroungimage(){
        showInfoBackground(R.drawable.img_alert,
                R.string.car_selection_no_car_no_car_first,
                R.string.car_selection_no_car_no_car_second);
        headerView.setVisibility(View.GONE);

    }

    private void showInfoBackground(int imgResource, int firstLine, int secondLine) {
        LOG.info("showInfoBackground()");
        infoBackgroundImg.setImageResource(imgResource);
        infoBackgroundFirst.setText(firstLine);
        infoBackgroundSecond.setText(secondLine);
        ECAnimationUtils.animateShowView(this, infoBackground, R.anim.fade_in);
    }

    /**
     * Hides the AddCarFragment
     */
    @Override
    public void onHideAddCarFragment() {
        LOG.info("hideAddCarFragment()");
        closeAddCarCard();
    }

    @Override
    public void onCarAdded(Car car) {
        LOG.info("onCarAdded(Car)");

        if (mCarManager.addCar(car)) {
            mCarListAdapter.addCarItem(car);

            headerView.setVisibility(View.VISIBLE);
            ECAnimationUtils.animateHideView(this, infoBackground, R.anim.fade_out);

            showSnackbar(String.format(getString(R.string.car_selection_successfully_added_tmp),
                    car.getManufacturer(), car.getModel()));
        } else {
            showSnackbar(String.format(getString(R.string.car_selection_already_in_list_tmp),
                    car.getManufacturer(), car.getModel()));
        }
    }

    @Override
    public Car createCar(Vehicles vehicle) {
        // Get the car values from Vehicles entitiy
        String manufacturer = vehicle.getManufacturer();
        String model = vehicle.getCommerical_name();
        String yearString = vehicle.getAllotment_date();
        int year = convertDateToInt(yearString);
        int engine = 0;
        if (!vehicle.getEngine_capacity().isEmpty())
            engine = Integer.parseInt(vehicle.getEngine_capacity());
        Car.FuelType fuelType = getFuel(vehicle.getPower_source_id());
        if (fuelType != Car.FuelType.ELECTRIC) {
            return new CarImpl(manufacturer, model, fuelType, year, engine);
        } else {
            return new CarImpl(manufacturer, model, fuelType, year);

        }
    }

    @Override
    public Car.FuelType getFuel(String id) {
        String fuel = null;
        if (id.equals("01"))
            fuel = "gasoline";
        else if (id.equals("02"))
            fuel = "diesel";
        else if (id.equals("04"))
            fuel = "electric";
        else if (id.equals("05") || id.equals("09") || id.equals("38"))
            fuel = "gas";
        else
            fuel = "hybrid";

        return Car.FuelType.resolveFuelType(fuel);
    }

    @Override
    public void registerCar(Vehicles vehicle) {
        Car car = createCar(vehicle);
        mCarManager.registerCarAtServer(car);
        onCarAdded(car);
        closeAddCarCard();
    }

    public int convertDateToInt(String date) {
        int convertedDate = 0;
        for (int i = 6; i < date.length(); i++) {
            convertedDate = convertedDate * 10 + (date.charAt(i) - 48);
        }
        return convertedDate;
    }

    public final ArrayAdapter<String> sortedAdapter(Context context, Set<String> set) {
        String[] strings = set.toArray(new String[set.size()]);
        Arrays.sort(strings);
        return new ArrayAdapter<>(
                context,
                R.layout.activity_car_selection_newcar_fueltype_item,
                strings);
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
