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
package org.envirocar.app.views.carselection;

import android.content.Context;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;
import org.envirocar.app.handler.preferences.UserPreferenceHandler;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.core.entity.Car;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.core.logging.Logger;
import org.envirocar.app.handler.DAOProvider;

import java.util.ArrayList;
import java.util.HashSet;
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
public class CarSelectionActivity extends BaseInjectorActivity implements CarSelectionUiListener {
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
                            showSnackbar(String.format(
                                    getString(R.string.car_selection_car_deleted_tmp),
                                    car.getManufacturer(), car.getModel()));
                        }
                        // then remove it from the list and show a snackbar.
                        mCarListAdapter.removeCarItem(car);
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

            // Check the total Cars count after adding, if only 1 car then set it.
            int count = mCarListAdapter.getCount();
            if(count == 1) {
                mCarManager.setCar(car);
            }
        } else {
            showSnackbar(String.format(getString(R.string.car_selection_already_in_list_tmp),
                    car.getManufacturer(), car.getModel()));
        }
        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
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
