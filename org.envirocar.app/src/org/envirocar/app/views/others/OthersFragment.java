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
package org.envirocar.app.views.others;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.BuildConfig;
import org.envirocar.app.R;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.handler.TrackDAOHandler;
import org.envirocar.app.handler.preferences.UserPreferenceHandler;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.recording.RecordingService;
import org.envirocar.app.services.autoconnect.AutoRecordingService;
import org.envirocar.app.views.logbook.LogbookActivity;
import org.envirocar.app.views.settings.SettingsActivity;
import org.envirocar.app.views.utils.DialogUtils;
import org.envirocar.core.entity.User;
import org.envirocar.core.logging.Logger;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * A simple {@link Fragment} subclass.
 */
public class OthersFragment extends BaseInjectorFragment {
    private static final Logger LOGGER = Logger .getLogger(OthersFragment.class);

    @Inject
    protected UserPreferenceHandler mUserManager;
    @Inject
    protected TrackDAOHandler mTrackDAOHandler;

    @BindView(R.id.othersLogOut)
    protected LinearLayout othersLogOut;
    @BindView(R.id.othersLogOutDivider)
    protected View othersLogOutDivider;

    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    private final Scheduler.Worker mBackgroundWorker = Schedulers.newThread().createWorker();

    private int REQUEST_PERMISSIONS_REQUEST_CODE = 101;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_others, container, false);

        ButterKnife.bind(this,view);

        if(mUserManager.isLoggedIn()){
            othersLogOut.setVisibility(View.VISIBLE);
            othersLogOutDivider.setVisibility(View.VISIBLE);
        }
        else{
            othersLogOut.setVisibility(View.GONE);
            othersLogOutDivider.setVisibility(View.GONE);
        }

        return view;
    }


    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }


    @OnClick(R.id.othersLogBook)
    protected void onLogBookClicked() {
        Intent intent = new Intent(getActivity(), LogbookActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.othersSettings)
    protected void onSettingsClicked() {
        Intent intent = new Intent(getActivity(), SettingsActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.othersHelp)
    protected void onHelpClicked() {
        Intent intent = new Intent(getActivity(), HelpActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.othersReportIssue)
    protected void onReportIssueClicked() {
        if(checkPermissions()){
            //access granted
            Intent intent = new Intent(getActivity(), SendLogFileActivity.class);
            startActivity(intent);
        }else{
            requestPermissions();
        }
    }

    @OnClick(R.id.othersRateUs)
    protected void onRateUsClicked() {
        final String appPackageName = "org.envirocar.app"; // getPackageName() from Context or Activity object
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    @OnClick(R.id.othersLogOut)
    protected void onLogOutClicked() {

        new MaterialDialog.Builder(getActivity())
                .title(getString(R.string.menu_logout_envirocar_title))
                .positiveText(getString(R.string.menu_logout_envirocar_positive))
                .negativeText(getString(R.string.menu_logout_envirocar_negative))
                .content(getString(R.string.menu_logout_envirocar_content))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        logOut();
                    }
                })
                .show();
    }

    @OnClick(R.id.othersCloseEnviroCar)
    protected void onCloseEnviroCarClicked() {
        new MaterialDialog.Builder(getActivity())
                .title(getString(R.string.menu_close_envirocar_title))
                .positiveText(getString(R.string.menu_close_envirocar_positive))
                .negativeText(getString(R.string.cancel))
                .content(getString(R.string.menu_close_envirocar_content))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        shutdownEnviroCar();
                    }
                })
                .show();

    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            LOGGER.debug("Requesting File Permission. Displaying permission rationale to provide additional context.");

            DialogUtils.createDefaultDialogBuilder(getContext(),
                    R.string.request_storage_permission_title,
                    R.drawable.others_settings,
                    R.string.permission_rationale_file)
                    .positiveText(R.string.ok)
                    .onPositive((dialog, which) -> {
                        // Request permission
                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_PERMISSIONS_REQUEST_CODE);
                    })
                    .show();


        } else {
            LOGGER.debug("Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }


    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        //LOG.debug("onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
               // LOG.debug("User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LOGGER.debug("Permission granted, updates requested, starting the logging procedure");
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, view -> {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",
                                    BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });
            }
        }
    }


    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                getActivity().findViewById(R.id.navigation),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }


    private void shutdownEnviroCar() {
        AutoRecordingService.stopService(getActivity());
        RecordingService.stopService(getActivity());

        mMainThreadWorker.schedule(() -> {
            System.runFinalizersOnExit(true);
            System.exit(0);
        }, 750, TimeUnit.MILLISECONDS);
    }

    private void logOut() {
        if (mUserManager.isLoggedIn()) {
            final MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.activity_login_logout_progress_dialog_title)
                    .content(R.string.activity_login_logout_progress_dialog_content)
                    .progress(true, 0)
                    .cancelable(false)
                    .build();
            dialog.show();

            User user = mUserManager.getUser();

            mBackgroundWorker.schedule(() -> {
                // Log out the user
                mUserManager.logOut();

                // Finally, delete all tracks that are associated to the previous user.
                mTrackDAOHandler.deleteAllRemoteTracksLocally();
                // Close the dialog.
                dialog.dismiss();

                //remove pref count statistics
                PreferencesHandler.resetTrackCounts(getActivity());

                mMainThreadWorker.schedule(() -> {
                    // Show a snackbar that indicates the finished logout
                    Snackbar.make(getActivity().findViewById(R.id.navigation),
                            String.format(getString(R.string.goodbye_message), user
                                    .getUsername()),
                            Snackbar.LENGTH_LONG).show();
                    //disable logout button
                    othersLogOut.setVisibility(View.GONE);
                    othersLogOutDivider.setVisibility(View.GONE);
                });

            });

        }
    }

}
