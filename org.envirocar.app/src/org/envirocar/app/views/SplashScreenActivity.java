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
package org.envirocar.app.views;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.views.carselection.CarSelectionAddCarFragment;
import org.envirocar.core.entity.Manufacturers;
import org.envirocar.core.logging.Logger;
import org.envirocar.storage.EnviroCarVehicleDB;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.ButterKnife;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * @author dewall
 */
public class SplashScreenActivity extends BaseInjectorActivity {
    private static final Logger LOG = Logger.getLogger(SplashScreenActivity.class);
    private static final String HAS_BEEN_SEEN_KEY = "has_been_seen";
    private static final int SPLASH_SCREEN_DURATION = 1000;
    @Inject
    EnviroCarVehicleDB enviroCarVehicleDB;
    private Disposable timerDisposable;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //fetch to db intilization to prepoulate database before the database object used in CarSelection to remove delay

        Observable<List<Manufacturers>> dbInit = enviroCarVehicleDB.manufacturersDAO().getAllManufacturers();
        dbInit.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<List<Manufacturers>>() {
                    @Override
                    public void onNext(List<Manufacturers> manufacturersList1) {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

        if (savedInstanceState != null && savedInstanceState.getBoolean(HAS_BEEN_SEEN_KEY, false)) {
            startMainActivity();
            return;
        }

        setContentView(R.layout.activity_splashscreen);
        getWindow().setNavigationBarColor(getResources().getColor(R.color.cario_color_primary_dark));
        ButterKnife.bind(this);

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register a timer for starting the main activity
        timerDisposable = Completable.timer(SPLASH_SCREEN_DURATION, TimeUnit.MILLISECONDS)
                .subscribe(() -> startMainActivity());
    }

    @Override
    protected void onStop() {
        super.onStop();

        // remove the timer for starting the main activity
        if (timerDisposable != null && !timerDisposable.isDisposed()) {
            timerDisposable.dispose();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        LOG.info("onSaveInstanceState()");
        super.onSaveInstanceState(outState);
        outState.putBoolean(HAS_BEEN_SEEN_KEY, true);
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, BaseMainActivity.class);
        this.startActivity(intent);
        finish();
    }
}
