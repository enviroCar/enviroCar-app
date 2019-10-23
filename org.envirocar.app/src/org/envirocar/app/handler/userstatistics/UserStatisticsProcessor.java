package org.envirocar.app.handler.userstatistics;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.common.base.MoreObjects;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.TrackDAOHandler;
import org.envirocar.app.handler.preferences.AbstractCachable;
import org.envirocar.core.UserManager;
import org.envirocar.core.dao.TrackDAO;
import org.envirocar.core.entity.Track;
import org.envirocar.core.events.NewUserSettingsEvent;
import org.envirocar.core.events.TrackDeletedEvent;
import org.envirocar.core.events.TrackFinishedEvent;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.statistics.TrackStatisticsProvider;
import org.envirocar.storage.EnviroCarDB;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


/**
 * @author dewall
 */
public class UserStatisticsProcessor extends AbstractCachable<UserStatisticsProcessor.UserStatisticsHolder> {
    private static final Logger LOG = Logger.getLogger(UserStatisticsProcessor.class);

    private static final String USER_STATISTICS_PREFS = "userStatisticsPrefs";
    private static final String PREF_KEY_USERNAME = "username";
    private static final String PREF_KEY_NUMTRACKS = "numTracks";
    private static final String PREF_KEY_TOTALDISTANCE = "totalDistance";
    private static final String PREF_KEY_TOTALDURATION = "totalDuration";


    protected static final class UserStatisticsHolder {
        private String username;

        private int numTracks;
        private long totalDuration;
        private double totalDistance;

        /**
         * Constructor.
         *
         * @param numTracks
         * @param totalDuration
         * @param totalDistance
         */
        public UserStatisticsHolder(String username, int numTracks, long totalDuration, double totalDistance) {
            this.username = username;
            this.numTracks = numTracks;
            this.totalDuration = totalDuration;
            this.totalDistance = totalDistance;
        }

        private boolean isLoggedIn() {
            return username != null && !username.equals("");
        }

        @NonNull
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("Username", username)
                    .add("numTracks", numTracks)
                    .add("totalDuration", totalDuration)
                    .add("totalDistance", totalDistance)
                    .toString();
        }
    }

    private final Context context;
    private final UserManager userManager;
    private final TrackDAOHandler trackDAOHandler;
    private final TrackDAO trackDAO;
    private final EnviroCarDB enviroCarDB;
    private final Bus bus;

    /**
     * Default constructor.
     */
    @Inject
    public UserStatisticsProcessor(@InjectApplicationScope Context context, UserManager userManager, DAOProvider daoProvider, TrackDAOHandler trackDAOHandler, EnviroCarDB enviroCarDB, Bus bus) {
        super(context, USER_STATISTICS_PREFS);
        this.context = context;
        this.userManager = userManager;
        this.trackDAOHandler = trackDAOHandler;
        this.enviroCarDB = enviroCarDB;
        this.trackDAO = daoProvider.getTrackDAO();
        this.bus = bus;
        this.bus.register(this);
    }

    @Override
    protected UserStatisticsHolder readFromCache(SharedPreferences prefs) {
        String username = prefs.getString(PREF_KEY_USERNAME, null);
        int numTracks = prefs.getInt(PREF_KEY_NUMTRACKS, 0);
        double totalDistance = Double.parseDouble(prefs.getString(PREF_KEY_TOTALDISTANCE, "0.0"));
        long totalDuration = prefs.getLong(PREF_KEY_TOTALDURATION, 0L);
        return new UserStatisticsHolder(username, numTracks, totalDuration, totalDistance);
    }

    @Override
    protected void writeToCache(UserStatisticsHolder s, SharedPreferences prefs) {
        SharedPreferences.Editor e = getSharedPreferences().edit();
        e.putString(PREF_KEY_USERNAME, s.username);
        e.putInt(PREF_KEY_NUMTRACKS, s.numTracks);
        e.putString(PREF_KEY_TOTALDISTANCE, String.valueOf(s.totalDistance));
        e.putLong(PREF_KEY_TOTALDURATION, s.totalDuration);
        e.commit();
    }

    @Subscribe
    public void onNewUserSettingsEvent(NewUserSettingsEvent event) {
        LOG.info(String.format("New User statistics received %s. Updating statistics.", event.toString()));
        UserStatisticsHolder s = readFromCache();
        if (event.mIsLoggedIn == s.isLoggedIn()) {
            return;
        }

        // reset statistics cache
        this.resetStatistics(event.mIsLoggedIn ? event.mUser.getUsername() : null);

        enviroCarDB.getAllLocalTracks(true)
                .compose(listObservable -> {
                    if (event.mIsLoggedIn)
                        return listObservable.mergeWith(trackDAO.getTrackIdsObservable(5000, 1));
                    return listObservable;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(tracks -> Observable.fromIterable(tracks))
                .map(this::updateStatistics)
                .debounce(1000, TimeUnit.MILLISECONDS)
                .subscribe(this::onNextStatistics, LOG::error);
    }

    @Subscribe
    public void onTrackFinishedEvent(TrackFinishedEvent event) {
        Observable.just(event.mTrack)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(this::updateStatistics)
                .subscribe(this::onNextStatistics, LOG::error);
    }

    @Subscribe
    public void onTrackDeletedEvent(TrackDeletedEvent event){

    }

    @Produce
    public UserStatisticsUpdateEvent produceUserStatisticsUpdateEvent() {
        UserStatisticsHolder s = readFromCache();
        return new UserStatisticsUpdateEvent(s.numTracks, s.totalDistance, s.totalDuration);
    }

    private void resetStatistics(String username) {
        UserStatisticsHolder s = new UserStatisticsHolder(username, 0, 0, 0);
        this.writeToCache(s);
        bus.post(new UserStatisticsUpdateEvent(s.numTracks, s.totalDistance, s.totalDuration));
    }

    private UserStatisticsHolder updateStatistics(Track track) {
        UserStatisticsHolder holder = readFromCache();

        if (track.getLength() != null) {
            holder.totalDistance += track.getLength();
        } else {
            holder.totalDistance += ((TrackStatisticsProvider) track).getDistanceOfTrack();
        }

        holder.totalDuration += track.getDurationMillis();
        holder.numTracks++;
        writeToCache(holder);
        return holder;
    }

    private void onAddTrackToStatistics(Track track){

    }

    private void onNextStatistics(UserStatisticsHolder o) {
        LOG.info("Computed new user statistics: " + o.toString());
        // Write statistics to cache and throw an event.
        this.bus.post(new UserStatisticsUpdateEvent(o.numTracks, o.totalDistance, o.totalDuration));
    }


}
