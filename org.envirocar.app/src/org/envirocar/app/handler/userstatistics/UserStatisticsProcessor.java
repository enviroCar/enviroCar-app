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
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.internal.AggregatedUserStatistic;
import org.envirocar.core.events.NewUserSettingsEvent;
import org.envirocar.core.events.TrackDeletedEvent;
import org.envirocar.core.events.TrackFinishedEvent;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.interactor.GetAggregatedUserStatistic;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.EnviroCarDB;

import javax.inject.Inject;

import io.reactivex.observers.DisposableObserver;


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
    private final Bus eventBus;
    private final GetAggregatedUserStatistic userStatisticInteractor;


    /**
     * Default constructor.
     */
    @Inject
    public UserStatisticsProcessor(@InjectApplicationScope Context context, GetAggregatedUserStatistic userStatisticInteractor, UserManager userManager, DAOProvider daoProvider, TrackDAOHandler trackDAOHandler, EnviroCarDB enviroCarDB, Bus bus) {
        super(context, USER_STATISTICS_PREFS);
        this.context = context;
        this.userManager = userManager;
        this.trackDAOHandler = trackDAOHandler;
        this.enviroCarDB = enviroCarDB;
        this.trackDAO = daoProvider.getTrackDAO();
        this.eventBus = bus;
        this.eventBus.register(this);
        this.userStatisticInteractor = userStatisticInteractor;
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
        LOG.info("New User settings received %s. Updating statistics.", event.toString());
        UserStatisticsHolder s = readFromCache();
        if (event.mIsLoggedIn == s.isLoggedIn()) {
            return;
        }

        // reset and update statistics cache
        String username = event.mIsLoggedIn ? event.mUser.getUsername() : null;
        this.resetStatistics(username);
        this.updateUserStatistics(username);
    }

    @Subscribe
    public void onTrackFinishedEvent(TrackFinishedEvent event) {
        LOG.info("Received event: %s", event.toString());
        User user = userManager.getUser();
        this.updateUserStatistics(user != null ? user.getUsername() : null);
    }

    @Subscribe
    public void onTrackDeletedEvent(TrackDeletedEvent event) {
        LOG.info("Received event: %s", event.toString());
        User user = userManager.getUser();
        this.updateUserStatistics(user != null ? user.getUsername() : null);
    }

    @Produce
    public UserStatisticsUpdateEvent produceUserStatisticsUpdateEvent() {
        UserStatisticsHolder s = readFromCache();
        return new UserStatisticsUpdateEvent(s.numTracks, s.totalDistance, s.totalDuration);
    }

    private void updateUserStatistics(String username){
        userStatisticInteractor.execute(new DisposableObserver<AggregatedUserStatistic>() {
            @Override
            public void onNext(AggregatedUserStatistic userStatistic) {
                UserStatisticsHolder holder = new UserStatisticsHolder(username,
                        userStatistic.getNumTracks(),
                        (long) userStatistic.getTotalDuration(),
                        userStatistic.getTotalDistance());
                writeToCache(holder);
                eventBus.post(new UserStatisticsUpdateEvent(holder.numTracks, holder.totalDistance, holder.totalDuration));
            }

            @Override
            public void onError(Throwable e) {
                LOG.error(e);
            }

            @Override
            public void onComplete() {

            }
        }, new GetAggregatedUserStatistic.Params(username));
    }

    private void resetStatistics(String username) {
        UserStatisticsHolder s = new UserStatisticsHolder(username, 0, 0, 0);
        this.writeToCache(s);
        eventBus.post(new UserStatisticsUpdateEvent(s.numTracks, s.totalDistance, s.totalDuration));
    }

}
