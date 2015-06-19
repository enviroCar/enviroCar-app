/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */

package org.envirocar.app.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.R;
import org.envirocar.app.application.ContextInternetAccessProvider;
import org.envirocar.app.application.TermsOfUseManager;
import org.envirocar.app.application.TrackUploadFinishedHandler;
import org.envirocar.app.application.UploadManager;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.dao.DAOProvider;
import org.envirocar.app.dao.DAOProvider.AsyncExecutionWithCallback;
import org.envirocar.app.dao.TrackDAO;
import org.envirocar.app.dao.exception.DAOException;
import org.envirocar.app.dao.exception.NotConnectedException;
import org.envirocar.app.exception.FuelConsumptionException;
import org.envirocar.app.exception.MeasurementsException;
import org.envirocar.app.exception.ServerException;
import org.envirocar.app.json.TrackWithoutMeasurementsException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.model.TermsOfUseInstance;
import org.envirocar.app.model.User;
import org.envirocar.app.network.WPSClient;
import org.envirocar.app.network.WPSClient.ResultCallback;
import org.envirocar.app.protocol.algorithm.UnsupportedFuelTypeException;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.RemoteTrack;
import org.envirocar.app.storage.Track;
import org.envirocar.app.util.NamedThreadFactory;
import org.envirocar.app.util.Util;
import org.envirocar.app.views.TypefaceEC;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * List Fragement that displays local and remote tracks.
 */
public class ListTracksFragment extends BaseInjectorFragment {

    // Measurements and tracks

    protected static final Logger logger = Logger.getLogger(ListTracksFragment.class);
    @Inject
    protected UserManager mUserManager;
    @Inject
    protected DbAdapter mDBAdapter;
    @Inject
    protected TermsOfUseManager mTermsOfUseManager;
    @Inject
    protected DAOProvider mDAOProvider;


    private List<Track> tracksList;
    private TracksListAdapter trackListAdapter;

    // UI Elements
    private ExpandableListView trackListView;
    private int itemSelect;
    private Menu menu;
    private TextView statusText;
    private View statusProgressBar;
    private AtomicInteger remoteTrackCount = new AtomicInteger(-1);


    public View onCreateView(android.view.LayoutInflater inflater,
                             android.view.ViewGroup container,
                             android.os.Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setHasOptionsMenu(true);

        View v = inflater.inflate(R.layout.list_tracks_layout, null);

        trackListView = (ExpandableListView) v.findViewById(R.id.list);

        statusProgressBar = v.findViewById(R.id.list_tracks_status_progress);
        statusText = (TextView) v.findViewById(R.id.list_tracks_status_text);

        setProgressStatusText(R.string.fetching_tracks);

        trackListView.setEmptyView(v.findViewById(R.id.empty));

        registerForContextMenu(trackListView);

        trackListView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                itemSelect = ExpandableListView.getPackedPositionGroup(id);
                logger.info(String.valueOf("Selected item: " + itemSelect));
                return false;
            }

        });

        return v;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        logger.info("Create view ListTracksFragment");
        super.onViewCreated(view, savedInstanceState);
        trackListView.setGroupIndicator(getResources().getDrawable(R.drawable.group_indicator));
        trackListView.setChildDivider(getResources().getDrawable(
                android.R.color.transparent));

        tracksList = new ArrayList<Track>();

        if (trackListAdapter == null) {
            trackListAdapter = new TracksListAdapter();
            trackListView.setAdapter(trackListAdapter);
        }

        startTracksRetrieval();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_tracks, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        this.menu = menu;
    }

    private void updateUsabilityOfMenuItems() {
        if (menu == null) return;

        if (!isAdded()) return;

        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Menu theMenu = menu;

                if (theMenu == null) {
                    return;
                }

                if (mDBAdapter.getAllLocalTracks().size() > 0) {
                    setItemEnabled(theMenu.findItem(R.id.menu_delete_all), true);
                    setItemEnabled(theMenu.findItem(R.id.menu_upload), mUserManager.isLoggedIn());
                } else {
                    setItemEnabled(theMenu.findItem(R.id.menu_upload), false);
                    setItemEnabled(theMenu.findItem(R.id.menu_delete_all), false);
                }
            }

            private void setItemEnabled(MenuItem item,
                                        boolean b) {
                if (item != null) {
                    item.setEnabled(b);
                }
            }
        });
    }

    /**
     * Method to remove all tracks of the logged in user from the listview and from the internal database.
     * Tracks which are locally on the device, are not removed.
     */
    public void clearRemoteTracks() {
        //remove tracks in a safe way
        Iterator<Track> trackIterator = tracksList.iterator();

        while (trackIterator.hasNext()) {
            Track track = (Track) trackIterator.next();
            if (!track.isLocalTrack()) {
                trackIterator.remove();
            }
        }
        mDBAdapter.deleteAllRemoteTracks();
        updateTrackListView();
    }

    public void notifyDataSetChanged(Track track) {
        updateUsabilityOfMenuItems();
        updateTrackListView();
    }

    /**
     * Edit all tracks
     */
    @Override
    public boolean onOptionsItemSelected(
            MenuItem item) {
        switch (item.getItemId()) {

            //Upload all tracks

            case R.id.menu_upload:
                if (mUserManager.isLoggedIn()) {
                    startTrackUpload(true, null);
                } else {
                    Crouton.showText(getActivity(), R.string.hint_login_first, Style.INFO);
                }
                return true;

            //Delete all tracks

            case R.id.menu_delete_all:
                mDBAdapter.deleteAllLocalTracks();
                Crouton.makeText(getActivity(), R.string.not_yet_supported, Style.CONFIRM).show();
                return true;

        }
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        final Track track = tracksList.get(itemSelect);
        if (track.isLocalTrack()) {
            inflater.inflate(R.menu.context_item, menu);
        } else {
            inflater.inflate(R.menu.context_item_remote, menu);
        }
    }

    /**
     * Change one item
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final Track track;
        synchronized (this) {
            track = tracksList.get(itemSelect);
        }
        switch (item.getItemId()) {

            // Edit the trackname
            case R.id.editName:
                if (track.isLocalTrack()) {
                    logger.info("editing track: " + itemSelect);
                    final EditText input = new EditText(getActivity());
                    new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.editTrack)).setMessage(getString(R.string.enterTrackName)).setView(input).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String value = input.getText().toString();
                            logger.info("New name: " + value.toString());
                            track.setName(value);
                            mDBAdapter.updateTrack(track);
                            tracksList.get(itemSelect).setName(value);
                            updateTrackListView();
                            Crouton.showText(getActivity(), getString(R.string.nameChanged), Style.INFO);
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Do nothing.
                        }
                    }).show();
                } else {
                    Crouton.showText(getActivity(), R.string.not_possible_for_remote, Style.INFO);
                }
                return true;

            // Edit the track description
            case R.id.editDescription:
                if (track.isLocalTrack()) {
                    logger.info("editing track: " + itemSelect);
                    final EditText input2 = new EditText(getActivity());
                    new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.editTrack)).setMessage(getString(R.string.enterTrackDescription)).setView(input2).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String value = input2.getText().toString();
                            logger.info("New description: " + value.toString());
                            track.setDescription(value);
                            mDBAdapter.updateTrack(track);
                            trackListView.collapseGroup(itemSelect);
                            updateTrackListView();
                            trackListAdapter.updateTrackChildView(track);
                            Crouton.showText(getActivity(), getString(R.string.descriptionChanged), Style.INFO);
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Do nothing.
                        }
                    }).show();
                } else {
                    Crouton.showText(getActivity(), R.string.not_possible_for_remote, Style.INFO);
                }
                return true;

            // Show that track in the map
            case R.id.startMap:
                logger.info("Show in Map");
                logger.info(Environment.getExternalStorageDirectory().toString());
                File f = new File(Environment.getExternalStorageDirectory() + "/Android");
                if (f.isDirectory()) {
//				if (track.isLazyLoadingMeasurements()) {
//					dbAdapter.loadMeasurements(track);
//				}
                    List<Measurement> measurements = track.getMeasurements();
                    logger.info("Count of measurements in the track: " + String.valueOf(measurements.size()));
                    String[] trackCoordinates = extractCoordinates(measurements);

                    if (trackCoordinates.length != 0) {
                        logger.info(String.valueOf(trackCoordinates.length));
                        Intent intent = new Intent(getActivity().getApplicationContext(), org.envirocar.app.activity.Map.class);
                        Bundle bundle = new Bundle();
                        bundle.putStringArray("coordinates", trackCoordinates);
                        intent.putExtras(bundle);
                        startActivity(intent);
                    } else {
                        Crouton.showText(getActivity(), getString(R.string.trackContainsNoCoordinates), Style.INFO);
                    }

                } else {
                    Crouton.showText(getActivity(), getString(R.string.noSdCard), Style.INFO);
                }

                return true;

            // Delete only selected track
            case R.id.deleteTrack:
            /*
             * we need to check the database if the track might have
			 * transisted to a remote track due to uploading
			 */
                Track dbRefTrack = mDBAdapter.getTrack(track.getTrackId(), true);
                if (dbRefTrack.isLocalTrack()) {
                    logger.info("deleting item: " + itemSelect);
                    mDBAdapter.deleteTrack(track.getTrackId());
                    Crouton.showText(getActivity(), getString(R.string.trackDeleted), Style.INFO);
                    tracksList.remove(itemSelect);
                    updateTrackListView();
                } else {
                    createRemoteDeleteDialog(track, (RemoteTrack) dbRefTrack);
                }
                return true;

            // Share track
            case R.id.shareTrack:
                try {
//				if (track.isLazyLoadingMeasurements()) {
//					dbAdapter.loadMeasurements(track);
//				}
                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("application/json");
                    Uri shareBody = Uri.fromFile(Util.saveTrackAndReturnFile(track, isObfuscationEnabled()).getFile());
                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "EnviroCar Track " + track.getName());
                    sharingIntent.putExtra(android.content.Intent.EXTRA_STREAM, shareBody);
                    startActivity(Intent.createChooser(sharingIntent, "Share via"));
                } catch (JSONException e) {
                    logger.warn(e.getMessage(), e);
                    Crouton.showText(getActivity(), R.string.error_json, Style.ALERT);
                } catch (IOException e) {
                    logger.warn(e.getMessage(), e);
                    Crouton.showText(getActivity(), R.string.error_io, Style.ALERT);
                } catch (TrackWithoutMeasurementsException e) {
                    logger.warn(e.getMessage(), e);
                    if (isObfuscationEnabled()) {
                        Crouton.showText(getActivity(), R.string.uploading_track_no_measurements_after_obfuscation_long, Style.ALERT);
                    } else {
                        Crouton.showText(getActivity(), R.string.uploading_track_no_measurements_after_obfuscation_long, Style.ALERT);
                    }
                }
                return true;

            // Upload track
            case R.id.uploadTrack:
                if (mUserManager.isLoggedIn()) {
//				if (track.isLazyLoadingMeasurements()) {
//					dbAdapter.loadMeasurements(track);
//				}
                    startTrackUpload(false, track);
                } else {
                    Crouton.showText(getActivity(), R.string.hint_login_first, Style.INFO);
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public boolean isObfuscationEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        return prefs.getBoolean(SettingsActivity.OBFUSCATE_POSITION, false);
    }

    /**
     * starts the uploading mechanism. It asserts the Terms of Use
     * acceptance state.
     *
     * @param all   if all local tracks should be uploaded
     * @param track a single track to upload
     */
    private void startTrackUpload(final boolean all, final Track track) {
        final User user = mUserManager.getUser();
        boolean verified = false;
        try {
            verified = mTermsOfUseManager.verifyTermsUseOfVersion(user.getTouVersion());
        } catch (ServerException e) {
            logger.warn(e.getMessage(), e);
            Crouton.makeText(getActivity(), getString(R.string.server_error_please_try_later), Style.ALERT).show();
            return;
        }
        if (!verified) {

            final TermsOfUseInstance current;
            try {
                current = mTermsOfUseManager.getCurrentTermsOfUse();
            } catch (ServerException e) {
                logger.warn("This should never happen!", e);
                return;
            }

            DialogUtil.createTermsOfUseDialog(current,
                    user.getTouVersion() == null, new DialogUtil.PositiveNegativeCallback() {

                        @Override
                        public void negative() {
                            logger.info("User did not accept the ToU.");
                            Crouton.makeText(getActivity(), getString(R.string.terms_of_use_info), Style.ALERT).show();
                        }

                        @Override
                        public void positive() {
                            mTermsOfUseManager.userAcceptedTermsOfUse(user, current.getIssuedDate());
                            uploadTracks(all, track);
                        }

                    }, getActivity());
        } else {
            uploadTracks(all, track);
        }

    }

    /**
     * executes the actual track uploading
     *
     * @param all   if all local tracks should be uploaded
     * @param track a single track to upload
     */
    private void uploadTracks(boolean all, Track track) {

        TrackUploadFinishedHandler callback = new TrackUploadFinishedHandler() {
            @Override
            public void onSuccessfulUpload(Track track) {
                trackListAdapter.updateTrackGroupView(track);
                remoteTrackCount.getAndIncrement();
                updateStatusLayout();
            }
        };

        if (all) {
            new UploadManager(getActivity()).uploadAllTracks(callback);
        } else {
            new UploadManager(getActivity()).uploadSingleTrack(track, callback);
        }
    }

    /**
     * @param track      the track object as used in the list adapter
     * @param dbRefTrack the database reference, representing the most up-to-date version
     *                   of the track (might have transisted from local to remote due to upload)
     */
    private void createRemoteDeleteDialog(final Track track, final RemoteTrack dbRefTrack) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.deleteRemoteTrackQuestion)
                .setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                final TrackDAO dao = mDAOProvider.getTrackDAO();

                                DAOProvider.async(new AsyncExecutionWithCallback<Void>() {

                                    @Override
                                    public Void execute()
                                            throws DAOException {
                                        dao.deleteTrack(dbRefTrack.getRemoteID());
                                        return null;
                                    }

                                    @Override
                                    public Void onResult(Void result,
                                                         boolean fail, Exception ex) {
                                        if (!fail) {
                                            mDBAdapter.deleteTrack(track.getTrackId());
                                            removeRemoteTrackFromView(track);
                                        } else {
                                            logger.warn(ex.getMessage(), ex);
                                        }
                                        return null;
                                    }

                                });

                            }
                        })
                .setNegativeButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // do nothing
                            }
                        });
        builder.create().show();
    }

    private void removeRemoteTrackFromView(final Track track) {
        if (tracksList.remove(track)) {
            updateTrackListView();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Crouton.showText(
                            getActivity(),
                            getString(R.string.remoteTrackDeleted),
                            Style.INFO);
                }
            });
        }
    }

    /**
     * Returns an StringArray of coordinates for the mpa
     *
     * @param measurements arraylist with all measurements
     * @return string array with coordinates
     */
    private String[] extractCoordinates(List<Measurement> measurements) {
        ArrayList<String> coordinates = new ArrayList<String>();

        for (Measurement measurement : measurements) {
            String lat = String.valueOf(measurement.getLatitude());
            String lon = String.valueOf(measurement.getLongitude());
            coordinates.add(lat);
            coordinates.add(lon);
        }
        return coordinates.toArray(new String[coordinates.size()]);
    }


    protected void updateStatusLayout() {
        if (!isAdded()) return;

        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                statusProgressBar.setVisibility(View.INVISIBLE);

                statusText.setText(getResources().getString(R.string.track_list_count_text,
                        resolveTrackCount(false),
                        resolveTrackCount(true), createRemoteTrackCountString()));
            }
        });

    }

    protected String createRemoteTrackCountString() {
        if (remoteTrackCount.get() < 0) {
            return "?";
        }
        if (remoteTrackCount.get() < 100) {
            return Integer.toString(remoteTrackCount.get());
        }
        return "100+";
    }

    private int resolveTrackCount(boolean remote) {
        int result = 0;

        for (Track t : tracksList) {
            if (t.isRemoteTrack() && remote) {
                result++;
            } else if (t.isLocalTrack() && !remote) {
                result++;
            }
        }

        return result;
    }

    protected void setProgressStatusText(int resId) {
        if (isAdded()) {
            final String str = getString(resId);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusText.setText(str);
                    TypefaceEC.applyCustomFont(statusText,
                            TypefaceEC.Newscycle(getActivity()));
                }
            });
        }
    }

    protected void updateTrackListView() {
        if (!isAdded()) return;

        synchronized (this) {
            Collections.sort(tracksList);
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                trackListAdapter.notifyDataSetChanged();
            }
        });
    }


    /**
     * Download remote tracks from the server and include them in the track list
     */
    @SuppressLint("NewApi")
    private void startTracksRetrieval() {


        new RemoteDownloadTracksTask().execute();
//        Util.execute(task);

    }

    private void downloadTracks() {

        if (!(new ContextInternetAccessProvider(getActivity()).isConnected())) {
            updateStatusLayout();
            return;
        }

        resolveTotalRemoteTrackCount(new AsyncExecutionWithCallback<Void>() {

            @Override
            public Void onResult(Void result, boolean fail, Exception exception) {
                downloadTracks(5, 1);
                return result;
            }

            @Override
            public Void execute() throws DAOException {
                return null;
            }
        });

    }

    private void resolveTotalRemoteTrackCount(final AsyncExecutionWithCallback<?> callback) {
        DAOProvider.async(new AsyncExecutionWithCallback<Integer>() {

            @Override
            public Integer execute() throws DAOException {
                return mDAOProvider.getTrackDAO().getUserTrackCount();
            }

            @Override
            public Integer onResult(Integer result, boolean fail,
                                    Exception e) {
                if (!fail) {
                    remoteTrackCount.set(result.intValue());
                } else {
                    logger.warn(e.getMessage(), e);
                }
                callback.onResult(null, false, null);
                return null;
            }
        });
    }

    private void downloadTracks(final int limit, final int page) {
        if (!mUserManager.isLoggedIn()) {
            logger.info("cancelling download of tracks: not logged in.");
        }

        DAOProvider.async(new AsyncExecutionWithCallback<List<String>>() {

            @Override
            public List<String> execute() throws DAOException {
                return mDAOProvider.getTrackDAO().getTrackIds(limit, page);
            }

            @Override
            public List<String> onResult(List<String> trackIdList, boolean fail,
                                         Exception exception) {
                if (!fail) {
                    if (trackIdList.size() == 0) {
                        updateStatusLayout();
                    }

                    Set<String> localRemoteIds = new HashSet<String>();
                    synchronized (ListTracksFragment.this) {
                        for (Track t : tracksList) {
                            if (t.isRemoteTrack()) {
                                localRemoteIds.add(((RemoteTrack) t).getRemoteID());
                            }
                        }
                    }

                    logger.info("found " + localRemoteIds.size() + " local tracks which have remoteIds.");

                    List<String> tracksToDownload = new ArrayList<String>();
                    for (int i = 0; i < trackIdList.size(); i++) {

                        String remoteId = trackIdList.get(i);
                        // check if track is listed. if, continue
                        if (localRemoteIds.contains(remoteId)) {
                            logger.info("Skipping track with remoteId " + remoteId);
                        } else {
                            tracksToDownload.add(remoteId);
                        }
                    }

                    if (!tracksToDownload.isEmpty()) {
                        logger.info("Starting download of " + tracksToDownload.size() + " tracks");
                    }

                    for (String trackId : tracksToDownload) {
                        Track t;
                        try {
                            /*
                             * we do not need async, we are still in the outer AsyncTask
							 */
                            t = mDAOProvider.getTrackDAO().getTrack(trackId);
                            mDBAdapter.insertTrack(t, true);

                            synchronized (ListTracksFragment.this) {
                                tracksList.add(t);
                            }

                            updateTrackListView();
                        } catch (NotConnectedException e) {
                            logger.warn(e.getMessage(), e);
                        }
                    }

                    updateStatusLayout();
                    updateUsabilityOfMenuItems();
                } else {
                    logger.warn("Could not retrieve the track ids: " + exception.getMessage(), exception);
                }
                return null;
            }

//			private synchronized void afterOneTrack() {
//				
//				View empty = getView().findViewById(android.R.id.empty);
//				if (empty != null) {
//					empty.setVisibility(View.GONE);
//				}
//				
//				if (--unprocessedTrackCount == 0) {
//					logger.info("Finished fetching tracks.");
//					updateStatusLayout();
//					updateTrackListView();
//					updateUsabilityOfMenuItems();
//				}
//			}
        });
    }


    /**
     * This method requests the current fuel price of a given fuel type from a
     * WPS that caches prices from export.benzinpreis-aktuell.de, calculates the
     * total fuel price using the fuel consumption and length of track and sets
     * the text of the respective <code>Textview</code>.
     *
     * @param fuelCostView The <code>Textview</code> the fuel price will be written to.
     * @param twoDForm     Used for rounding the fuel price.
     * @param t            the track
     */
    private void getEstimatedFuelCost(final TextView fuelCostView,
                                      final DecimalFormat twoDForm, Track t) {

        WPSClient.calculateFuelCosts(t, new ResultCallback<Double>() {

            @Override
            public void onResultAvailable(Double result) {
                if (result.equals(Double.NaN)) {
                    fuelCostView.setText(R.string.error_calculating_fuel_costs);

                    fuelCostView.setTextColor(Color.RED);

                } else {
                    fuelCostView.setText(twoDForm.format(result)
                            + " " + getActivity().getString(R.string.euro_sign));
                }
            }
        });

    }

    private class TracksListAdapter extends BaseExpandableListAdapter {

        private static final int GROUP_VIEW_BASE_ID = 10000000;
        private static final int CHILD_VIEW_BASE_ID = 10001000;
        private ExecutorService lazyLoader = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("MeasurementLazyLoader"));
        private java.util.Map<Track, View> trackToGroupViewMap;
        private Map<Track, View> trackToChildViewMap;

        public TracksListAdapter() {
            trackToGroupViewMap = new HashMap<Track, View>();
            trackToChildViewMap = new HashMap<Track, View>();
        }


        @Override
        public int getGroupCount() {
            return tracksList.size();
        }

        @Override
        public int getChildrenCount(int i) {
            return 1;
        }

        @Override
        public Object getGroup(int i) {
            return tracksList.get(i);
        }

        @Override
        public Object getChild(int i, int i1) {
            return tracksList.get(i);
        }

        @Override
        public long getGroupId(int i) {
            return i;
        }

        @Override
        public long getChildId(int i, int i1) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public void updateTrackGroupView(final Track t) {
            View groupRow = trackToGroupViewMap.get(t);

            if (groupRow == null) {
                /*
                 * fallback, unknown object id, but could be in the db
				 */
                for (Track tmp : trackToGroupViewMap.keySet()) {
                    if (tmp.getTrackId().equals(t.getTrackId())) {
                        groupRow = trackToGroupViewMap.get(tmp);
                        break;
                    }
                }
            }

            final View groupToAdjut = groupRow;

            if (groupRow != null && isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setTrackTypeImage(mDBAdapter.getTrack(t.getTrackId(), true), groupToAdjut);
                        groupToAdjut.invalidate();
                    }
                });
            }
        }

        public void updateTrackChildView(final Track t) {
            View childView = trackToChildViewMap.get(t);

            if (childView == null) {
                /*
                 * fallback, unknown object id, but could be in the db
				 */
                for (Track tmp : trackToChildViewMap.keySet()) {
                    if (tmp.getTrackId().equals(t.getTrackId())) {
                        childView = trackToChildViewMap.get(tmp);
                        break;
                    }
                }
            }

            final View viewoAdjust = childView;

            if (viewoAdjust != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setTrackChildFields(t, viewoAdjust);
                        viewoAdjust.invalidate();
                    }
                });
            }
        }

        @Override
        public View getGroupView(int i, boolean b, View view,
                                 ViewGroup viewGroup) {
            Track t;
            synchronized (ListTracksFragment.this) {
                t = tracksList.get(i);
                if (!trackToGroupViewMap.containsKey(t)) {
                    View groupRow = ViewGroup.inflate(getActivity(), R.layout.list_tracks_group_layout, null);
                    setTrackTypeImage(t, groupRow);

                    groupRow.setId(GROUP_VIEW_BASE_ID + i);
                    TypefaceEC.applyCustomFont((ViewGroup) groupRow,
                            TypefaceEC.Newscycle(getActivity()));

                    trackToGroupViewMap.put(t, groupRow);
                }
            }

            return trackToGroupViewMap.get(t);
        }

        private void setTrackTypeImage(Track t, View groupRow) {
            TextView textView = (TextView) groupRow.findViewById(R.id.track_name_textview);
            textView.setText(t.getName());

            ImageView imageView = (ImageView) groupRow.findViewById(R.id.track_icon_view);
            if (t.isLocalTrack()) {
                imageView.setImageDrawable(getResources().getDrawable(R.drawable.mobile));
            } else {
                imageView.setImageDrawable(getResources().getDrawable(R.drawable.server));
            }
        }

        @Override
        public View getChildView(int position, int i1, boolean b, View view, ViewGroup viewGroup) {
            final Track t;
            synchronized (ListTracksFragment.this) {
                t = tracksList.get(position);
                if (!trackToChildViewMap.containsKey(t)) {
                    final View row = ViewGroup.inflate(getActivity(),
                            R.layout.list_tracks_item_layout, null);

                    setTrackChildFields(t, row);

                    if (t.isLazyLoadingMeasurements()) {
                        lazyLoader.submit(new Runnable() {
                            @Override
                            public void run() {
                                t.getMeasurements();
                                getActivity().runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        setTrackChildFields(t, row);
                                    }
                                });

                            }
                        });
                    }

                    row.setId(CHILD_VIEW_BASE_ID + position + i1);
                    TypefaceEC.applyCustomFont((ViewGroup) row,
                            TypefaceEC.Newscycle(getActivity()));
                    trackToChildViewMap.put(t, row);
                }
            }

            return trackToChildViewMap.get(t);
        }

        private void setTrackChildFields(Track t, View childView) {
            TextView startView = (TextView) childView
                    .findViewById(R.id.track_details_start_textview);
            TextView endView = (TextView) childView
                    .findViewById(R.id.track_details_end_textview);
            TextView lengthView = (TextView) childView
                    .findViewById(R.id.track_details_length_textview);
            TextView carView = (TextView) childView
                    .findViewById(R.id.track_details_car_textview);
            TextView durationView = (TextView) childView
                    .findViewById(R.id.track_details_duration_textview);
            TextView co2View = (TextView) childView
                    .findViewById(R.id.track_details_co2_textview);
            TextView consumptionView = (TextView) childView
                    .findViewById(R.id.track_details_consumption_textview);
            TextView fuelCostView = (TextView) childView
                    .findViewById(R.id.track_details_fuel_cost_textview);
            TextView descriptionView = (TextView) childView.findViewById(R.id.track_details_description_textview);

            Car car = t.getCar();
            carView.setText(car.toString());
            descriptionView.setText(t.getDescription());
            if (t.isLazyLoadingMeasurements()) {
                String loading = "loading...";
                setTextViewContent(startView, loading);
                setTextViewContent(endView, loading);
                setTextViewContent(lengthView, loading);
                setTextViewContent(durationView, loading);
                setTextViewContent(co2View, loading);
                setTextViewContent(consumptionView, loading);
            } else {
                try {
                    DateFormat sdf = DateFormat.getDateTimeInstance();
                    DecimalFormat twoDForm = new DecimalFormat("#.##");
                    DateFormat dfDuration = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
                    dfDuration.setTimeZone(TimeZone.getTimeZone("UTC"));
                    startView.setText(sdf.format(t.getStartTime()));
                    endView.setText(sdf.format(t.getEndTime()));
                    Date durationMillis = new Date(t.getDurationInMillis());
                    durationView.setText(dfDuration.format(durationMillis));
                    if (!PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getBoolean(SettingsActivity.IMPERIAL_UNIT, false)) {
                        lengthView.setText(twoDForm.format(t.getLengthOfTrack()) + " km");
                    } else {
                        lengthView.setText(twoDForm.format(t.getLengthOfTrack() / 1.6) + " miles");
                    }

                    try {
                        double consumption = t.getFuelConsumptionPerHour();
                        double literOn100km = t.getLiterPerHundredKm();
                        co2View.setText(twoDForm.format(t.getGramsPerKm()) + " g/km");
                        consumptionView.setText(twoDForm.format(consumption) + " l/h (" + twoDForm.format(literOn100km) + " l/100 km)");
                        if (fuelCostView.getText() == null || fuelCostView.getText().equals("")) {
                            fuelCostView.setText(R.string.calculating);
                            getEstimatedFuelCost(fuelCostView, twoDForm, t);
                        }
                    } catch (UnsupportedFuelTypeException e) {
                        logger.warn(e.getMessage());
                    } catch (MeasurementsException e) {
                        logger.warn(e.getMessage());
                    } catch (FuelConsumptionException e) {
                        logger.warn(e.getMessage());
                    }

                } catch (MeasurementsException e) {
                    logger.warn(e.getMessage(), e);
                }
            }

        }

        private void setTextViewContent(TextView tv, String string) {
            tv.setText(string);
        }

        @Override
        public boolean isChildSelectable(int i, int i1) {
            return false;
        }

    }

    /**
     *
     */
    private final class RemoteDownloadTracksTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Thread.currentThread().setName("TrackList-TrackRetriever" + Thread.currentThread().getId());

            setProgressStatusText(R.string.fetching_tracks_local);

            //fetch db tracks (local+remote)
            List<Track> tracks = mDBAdapter.getAllTracks(true);
            synchronized (ListTracksFragment.this) {
                for (Track t : tracks) {
                    tracksList.add(t);
                }
            }


            updateTrackListView();

            logger.info("Number of tracks in the List: " + tracksList.size());

            if (mUserManager.isLoggedIn()) {
                setProgressStatusText(R.string.fetching_tracks_remote);
                downloadTracks();
            } else {
                updateStatusLayout();
            }

            return null;
        }
    }


}
