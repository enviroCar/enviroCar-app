/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.view.carselection;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.envirocar.app.R;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.app.view.utils.ECAnimationUtils;
import org.envirocar.core.entity.Car;
import org.envirocar.core.injection.BaseInjectorActivity;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.DAOProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public class CarSelectionActivity extends BaseInjectorActivity implements CarSelectionUiListener {
    private static final Logger LOG = Logger.getLogger(CarSelectionActivity.class);

    private static final int DURATION_SHEET_ANIMATION = 350;

    @InjectView(R.id.activity_car_selection_layout_content)
    protected View mContentView;
    @InjectView(R.id.activity_car_selection_layout_toolbar)
    protected Toolbar mToolbar;
    @InjectView(R.id.activity_car_selection_layout_exptoolbar)
    protected Toolbar mExpToolbar;
    @InjectView(R.id.actvity_car_selection_layout_loading)
    protected View loadingView;
    @InjectView(R.id.overlay)
    protected View overlayView;

    @InjectView(R.id.activity_car_selection_new_car_fab)
    protected FloatingActionButton mFab;

    @InjectView(R.id.activity_car_selection_layout_carlist)
    protected ListView mCarListView;

    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected CarPreferenceHandler mCarManager;
    @Inject
    protected UserHandler mUserHandler;

    private CarSelectionAddCarFragment addCarFragment;

    private Set<Car> mCars = new HashSet<>();


    private CarSelectionListAdapter mCarListAdapter;
    private AutoCompleteArrayAdapter mManufacturerNameAdapter;
    private Subscription loadingCarsSubscription;


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
        getSupportActionBar().setTitle("Select a Car");

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

        if (this.loadingCarsSubscription != null &&
                !this.loadingCarsSubscription.isUnsubscribed()) {
            this.loadingCarsSubscription.unsubscribe();
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
        ECAnimationUtils.animateHideView(this,mFab, R.anim.fade_out);
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

                        // If the car has been removed successfully...
                        if (mCarManager.removeCar(car)) {
                            // then remove it from the list and show a snackbar.
                            mCarListAdapter.removeCarItem(car);
                            showSnackbar(String.format(
                                    getString(R.string.car_selection_car_deleted_tmp),
                                    car.getManufacturer(), car.getModel()));
                        }
                    }
                });
        mCarListView.setAdapter(mCarListAdapter);

        loadingCarsSubscription = mCarManager.getAllDeserializedCars()
                .flatMap(cars -> {
                    Observable<List<Car>> carsObs = Observable.just(cars);
                    if (mUserHandler.isLoggedIn() && !mCarManager.isDownloaded()) {
                        LOG.info("Loading Cars: user has not downloaded its remote cars. " +
                                "Trying to fetch these.");
                        carsObs = carsObs.concatWith(mCarManager.downloadRemoteCarsOfUser());
                    }
                    return carsObs;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Car>>() {
                    @Override
                    public void onStart() {
                        LOG.info("onStart()");
                        loadingView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCompleted() {
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
            showSnackbar(String.format(getString(R.string.car_selection_successfully_added_tmp),
                    car.getManufacturer(), car.getModel()));
        } else {
            showSnackbar(String.format(getString(R.string.car_selection_already_in_list_tmp),
                    car.getManufacturer(), car.getModel()));
        }
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
