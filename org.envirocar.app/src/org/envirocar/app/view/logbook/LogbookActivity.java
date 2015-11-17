package org.envirocar.app.view.logbook;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.view.utils.ECAnimationUtils;
import org.envirocar.core.UserManager;
import org.envirocar.core.entity.Car;
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
import butterknife.InjectView;
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

    @InjectView(R.id.activity_logbook_toolbar)
    protected Toolbar toolbar;
    @InjectView(R.id.activity_logbook_header)
    protected View headerView;
    @InjectView(R.id.activity_logbook_toolbar_new_fueling_fab)
    protected View newFuelingFab;
    @InjectView(R.id.activity_logbook_toolbar_fuelinglist)
    protected ListView fuelingList;

    @InjectView(R.id.activity_logbook_not_logged_in)
    protected View notLoggedInView;
    @InjectView(R.id.activity_logbook_no_fuelings_info_view)
    protected View noFuelingsView;

    protected LogbookListAdapter fuelingListAdapter;
    protected final List<Fueling> fuelings = new ArrayList<Fueling>();

    private LogbookAddFuelingFragment addFuelingFragment;
    private final CompositeSubscription subscription = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // First, set the content view.
        setContentView(R.layout.activity_logbook);

        // Inject the Views.
        ButterKnife.inject(this);

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
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                deleteFueling(fueling);
                            }
                        })
                        .show();
                return false;
            }
        });

        // When the user is logged in, then download its fuelings. Otherwise, show a "not logged
        // in" notification.
        if (userManager.isLoggedIn()) {
            downloadFuelings();
            notLoggedInView.setVisibility(View.GONE);
        } else {
            headerView.setVisibility(View.GONE);
            newFuelingFab.setVisibility(View.GONE);
            notLoggedInView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription.clear();
        }
    }

    @OnClick(R.id.activity_logbook_toolbar_new_fueling_fab)
    protected void onClickNewFuelingFAB() {
        // Click on the fab should first hide the fab and then open the AddFuelingFragment
        showAddFuelingCard();
    }

    @Override
    public void onHideAddFuelingCard() {
        hideAddFuelingCard();
    }

    @Override
    public void onFuelingUploaded(Fueling fueling) {
        if (!this.fuelings.contains(fueling)) {
            fuelings.add(fueling);
            Collections.sort(fuelings);
            fuelingListAdapter.notifyDataSetChanged();

            // Hide the NoFuelingsView if it is visible.
            if (!fuelings.isEmpty() && noFuelingsView.getVisibility() == View.VISIBLE) {
                ECAnimationUtils.animateHideView(LogbookActivity.this,
                        noFuelingsView, R.anim.fade_out);
            }
        }
    }

    /**
     * Downloads the fuelings
     */
    private void downloadFuelings() {
        subscription.add(daoProvider.getFuelingDAO().getFuelingsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Fueling>>() {
                    @Override
                    public void onCompleted() {
                        LOG.info("Download of fuelings completed");

                        if (fuelings.isEmpty()) {
                            noFuelingsView.setVisibility(View.VISIBLE);
                        } else {
                            noFuelingsView.setVisibility(View.GONE);
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
                            ECAnimationUtils.animateShowView(LogbookActivity.this,
                                    noFuelingsView, R.anim.fade_in);
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

    /**
     * Shows the AddFuelingCard
     */
    private void showAddFuelingCard() {
        ECAnimationUtils.animateHideView(this, newFuelingFab, R.anim.fade_out, () -> {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.activity_logbook_container,
                            (addFuelingFragment = new LogbookAddFuelingFragment()))
                    .commit();
        });
    }

    /**
     * Hides the AddFuelingCard
     */
    private void hideAddFuelingCard() {
        getSupportFragmentManager()
                .beginTransaction()
                .remove(addFuelingFragment)
                .commit();
        addFuelingFragment = null;
        ECAnimationUtils.animateShowView(LogbookActivity.this, newFuelingFab, R.anim.fade_in);
    }

    private void showSnackbarInfo(int resourceID) {
        Snackbar.make(toolbar, resourceID, Snackbar.LENGTH_LONG).show();
    }

    private void showSnackbarInfo(String info) {
        Snackbar.make(toolbar, info, Snackbar.LENGTH_LONG).show();
    }

    private class CarSpinnerAdapter extends ArrayAdapter<Car> {
        /**
         * Constructor.
         *
         * @param context  the context of the current scope
         * @param resource the resource id
         * @param objects  the car objects to show
         */
        public CarSpinnerAdapter(Context context, int resource, List<Car> objects) {
            super(context, resource, objects);
        }
    }
}
