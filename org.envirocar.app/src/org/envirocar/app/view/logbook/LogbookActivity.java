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
package org.envirocar.app.view.logbook;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.view.utils.ECAnimationUtils;
import org.envirocar.core.UserManager;
import org.envirocar.core.entity.Fueling;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.BaseInjectorActivity;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.DAOProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.BindView;
import butterknife.OnClick;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class LogbookActivity extends BaseInjectorActivity implements LogbookUiListener {
    private static final Logger LOG = Logger.getLogger(LogbookActivity.class);

    @Inject
    protected CarPreferenceHandler carHandler;
    @Inject
    protected DAOProvider daoProvider;
    @Inject
    protected UserManager userManager;

    @BindView(R.id.activity_logbook_toolbar)
    protected Toolbar toolbar;
    @BindView(R.id.activity_logbook_header)
    protected View headerView;
    @BindView(R.id.activity_logbook_toolbar_new_fueling_fab)
    protected View newFuelingFab;
    @BindView(R.id.activity_logbook_toolbar_fuelinglist)
    protected ListView fuelingList;
    @BindView(R.id.overlay)
    protected View overlayView;

    @BindView(R.id.layout_general_info_background)
    protected View infoBackground;
    @BindView(R.id.layout_general_info_background_img)
    protected ImageView infoBackgroundImg;
    @BindView(R.id.layout_general_info_background_firstline)
    protected TextView infoBackgroundFirst;
    @BindView(R.id.layout_general_info_background_secondline)
    protected TextView infoBackgroundSecond;

//    @BindView(R.id.activity_logbook_not_logged_in)
//    protected View notLoggedInView;
//    @BindView(R.id.activity_logbook_no_fuelings_info_view)
//    protected View noFuelingsView;

    protected LogbookListAdapter fuelingListAdapter;
    protected final List<Fueling> fuelings = new ArrayList<Fueling>();

    private LogbookAddFuelingFragment addFuelingFragment;
    private final CompositeSubscription subscription = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LOG.info("onCreate()");
        super.onCreate(savedInstanceState);

        // First, set the content view.
        setContentView(R.layout.activity_logbook);

        // Inject the Views.
        ButterKnife.bind(this);

        // Initializes the Toolbar.
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Logbook");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fuelingListAdapter = new LogbookListAdapter(this, fuelings);
        fuelingList.setAdapter(fuelingListAdapter);

        fuelingList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long
                    id) {
                final Fueling fueling = fuelings.get(position);
                new MaterialDialog.Builder(LogbookActivity.this)
                        .title(R.string.logbook_dialog_delete_fueling_header)
                        .content(R.string.logbook_dialog_delete_fueling_content)
                        .positiveText(R.string.menu_delete)
                        .negativeText(R.string.cancel)
                        .onPositive((materialDialog, dialogAction) -> deleteFueling(fueling))
                        .show();
                return false;
            }
        });

        // When the user is logged in, then download its fuelings. Otherwise, show a "not logged
        // in" notification.
        if (userManager.isLoggedIn()) {
            downloadFuelings();
        } else {
            LOG.info("User is not logged in.");
            headerView.setVisibility(View.GONE);
            newFuelingFab.setVisibility(View.GONE);
            showNotLoggedInInfo();
        }
    }

    @Override
    protected void onDestroy() {
        LOG.info("onDestroy()");
        super.onDestroy();
        if (!subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription.clear();
        }
    }

    @Override
    public void onBackPressed() {
        LOG.info("onBackPressed()");
        if(addFuelingFragment != null && addFuelingFragment.isVisible()){
            addFuelingFragment.closeThisFragment();
            LOG.info("AddFuelingCard was visible. Closing this card...");
            return;
        }
        super.onBackPressed();
    }

    @OnClick(R.id.activity_logbook_toolbar_new_fueling_fab)
    protected void onClickNewFuelingFAB() {
        if(carHandler.hasCars()) {
            // Click on the fab should first hide the fab and then open the AddFuelingFragment
            showAddFuelingCard();
        } else {
            showSnackbarInfo(R.string.logbook_background_no_cars_second);
        }
    }

    @Override
    public void onHideAddFuelingCard() {
        hideAddFuelingCard();
    }

    @Override
    public void onFuelingUploaded(Fueling fueling) {
        LOG.info("onFuelingUploaded()");
        if (!this.fuelings.contains(fueling)) {
            fuelings.add(fueling);
            Collections.sort(fuelings);
            fuelingListAdapter.notifyDataSetChanged();

            // Hide the NoFuelingsView if it is visible.
            if (!fuelings.isEmpty() && infoBackground.getVisibility() == View.VISIBLE) {
                ECAnimationUtils.animateHideView(LogbookActivity.this,
                        infoBackground, R.anim.fade_out);
            }
        }
    }

    /**
     * Downloads the fuelings
     */
    private void downloadFuelings() {
        LOG.info("downloadFuelings()");
        subscription.add(daoProvider.getFuelingDAO().getFuelingsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Fueling>>() {
                    @Override
                    public void onCompleted() {
                        LOG.info("Download of fuelings completed");

                        if (fuelings.isEmpty()) {
                            showNoFuelingsInfo();
                        } else {
                            infoBackground.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);
                    }

                    @Override
                    public void onNext(List<Fueling> result) {
                        // Add all remote fuelings
                        fuelings.addAll(result);

                        // Sort the list of fuelings
                        Collections.sort(fuelings);

                        // Redraw everything
                        fuelingListAdapter.notifyDataSetChanged();
                    }
                }));
    }

    /**
     * Deletes a given fueling locally as well as from the enviroCar server.
     *
     * @param fueling the fueling to delete.
     */
    private void deleteFueling(final Fueling fueling) {
        subscription.add(daoProvider.getFuelingDAO()
                .deleteFuelingObservable(fueling)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Void>() {
                    @Override
                    public void onCompleted() {
                        LOG.info(String.format("Successfully deleted fueling -> [%s]",
                                fueling.getRemoteID()));

                        // Remove the fueling from the local list.
                        fuelings.remove(fueling);
                        if (fuelings.isEmpty()) {
                            showNoFuelingsInfo();
                        }
                        fuelingListAdapter.notifyDataSetChanged();

                        // Show a notification about the success.
                        showSnackbarInfo(R.string.logbook_deletion_success_tmp);
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.warn(e.getMessage(), e);
                        if (e instanceof NotConnectedException) {
                            showSnackbarInfo(R.string.logbook_error_communication);
                        } else if (e instanceof UnauthorizedException) {
                            showSnackbarInfo(R.string.logbook_error_unauthorized);
                        }
                    }

                    @Override
                    public void onNext(Void aVoid) {
                        // Nothing to do
                    }
                }));
    }

    private void showNoFuelingsInfo() {
        showInfoBackground(R.drawable.img_logbook,
                R.string.logbook_background_no_fuelings_first,
                R.string.logbook_background_no_fuelings_second);
    }

    private void showNoCarsInfo() {
        showInfoBackground(R.drawable.img_car,
                R.string.logbook_background_no_cars_first,
                R.string.logbook_background_no_cars_second);
    }

    private void showNotLoggedInInfo() {
        showInfoBackground(R.drawable.img_logged_out,
                R.string.logbook_background_not_logged_in_first,
                R.string.logbook_background_no_fuelings_second);
    }

    private void showInfoBackground(int imgResource, int firstLine, int secondLine) {
        LOG.info("showInfoBackground()");
        infoBackgroundImg.setImageResource(imgResource);
        infoBackgroundFirst.setText(firstLine);
        infoBackgroundSecond.setText(secondLine);
        ECAnimationUtils.animateShowView(this, infoBackground, R.anim.fade_in);
    }

    /**
     * Shows the AddFuelingCard
     */
    private void showAddFuelingCard() {
        LOG.info("showAddFuelingCard()");
        ECAnimationUtils.animateShowView(this, overlayView, R.anim.fade_in);
        ECAnimationUtils.animateHideView(this, newFuelingFab, R.anim.fade_out);
        this.addFuelingFragment = new LogbookAddFuelingFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.activity_logbook_container, this.addFuelingFragment)
                .commit();
        LOG.info("AddFuelingCard should now be visible");




    }

    /**
     * Hides the AddFuelingCard
     */
    private void hideAddFuelingCard() {
        LOG.info("hideAddFuelingCard()");
        getSupportFragmentManager()
                .beginTransaction()
                .remove(addFuelingFragment)
                .commit();
        addFuelingFragment = null;
        ECAnimationUtils.animateShowView(LogbookActivity.this, newFuelingFab, R.anim.fade_in);
    }

    private void showSnackbarInfo(int resourceID) {
        Snackbar.make(newFuelingFab, resourceID, Snackbar.LENGTH_LONG).show();
    }
}
