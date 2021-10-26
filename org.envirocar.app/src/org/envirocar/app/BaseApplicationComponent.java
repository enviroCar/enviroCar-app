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
package org.envirocar.app;


import com.squareup.sqlbrite3.SqlBrite;

import org.envirocar.app.aidl.EnviroCarDataService;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.LocationHandler;
import org.envirocar.app.handler.TrackRecordingHandler;
import org.envirocar.app.handler.userstatistics.UserStatisticsProcessor;
import org.envirocar.app.injection.components.MainActivityComponent;
import org.envirocar.app.injection.components.RecordingScreenComponent;
import org.envirocar.app.injection.modules.MainActivityModule;
import org.envirocar.app.injection.modules.RecordingScreenModule;
import org.envirocar.app.recording.RecordingComponent;
import org.envirocar.app.recording.RecordingModule;
import org.envirocar.app.services.TrackUploadService;
import org.envirocar.app.services.autoconnect.AutoRecordingComponent;
import org.envirocar.app.services.autoconnect.AutoRecordingModule;
import org.envirocar.app.services.trackchunks.TrackChunkUploaderService;
import org.envirocar.app.views.SplashScreenActivity;
import org.envirocar.app.views.carselection.CarSelectionActivity;
import org.envirocar.app.views.carselection.CarSelectionAddCarFragment;
import org.envirocar.app.views.carselection.CarSelectionAttributesFragment;
import org.envirocar.app.views.carselection.CarSelectionHsnTsnFragment;
import org.envirocar.app.views.dashboard.DashboardFragment;
import org.envirocar.app.views.logbook.LogbookActivity;
import org.envirocar.app.views.logbook.LogbookAddFuelingFragment;
import org.envirocar.app.views.login.SigninActivity;
import org.envirocar.app.views.login.SignupActivity;
import org.envirocar.app.views.obdselection.OBDSelectionActivity;
import org.envirocar.app.views.obdselection.OBDSelectionFragment;
import org.envirocar.app.views.others.OthersFragment;
import org.envirocar.app.views.others.SendLogFileActivity;
import org.envirocar.app.views.preferences.BluetoothPairingPreference;
import org.envirocar.app.views.preferences.SelectBluetoothPreference;
import org.envirocar.app.views.recordingscreen.Tempomat;
import org.envirocar.app.views.trackdetails.MapExpandedActivity;
import org.envirocar.app.views.trackdetails.TrackDetailsActivity;
import org.envirocar.app.views.trackdetails.TrackStatisticsActivity;
import org.envirocar.app.views.tracklist.TrackListLocalCardFragment;
import org.envirocar.app.views.tracklist.TrackListRemoteCardFragment;
import org.envirocar.core.UserManager;
import org.envirocar.core.interactor.GetUserStatistic;
import org.envirocar.remote.dao.CacheAnnouncementsDAO;
import org.envirocar.remote.dao.CacheCarDAO;
import org.envirocar.remote.dao.CacheFuelingDAO;
import org.envirocar.remote.dao.CachePrivacyStatementDAO;
import org.envirocar.remote.dao.CacheTermsOfUseDAO;
import org.envirocar.remote.dao.CacheTrackDAO;
import org.envirocar.remote.dao.CacheUserDAO;
import org.envirocar.remote.dao.RemoteAnnouncementsDAO;
import org.envirocar.remote.dao.RemoteCarDAO;
import org.envirocar.remote.dao.RemoteFuelingDAO;
import org.envirocar.remote.dao.RemotePrivacyStatementDAO;
import org.envirocar.remote.dao.RemoteTermsOfUseDAO;
import org.envirocar.remote.dao.RemoteTrackDAO;
import org.envirocar.remote.dao.RemoteUserDAO;
import org.envirocar.remote.dao.RemoteUserStatisticsDAO;
import org.envirocar.storage.EnviroCarVehicleDB;

import javax.inject.Singleton;

import dagger.Component;
import retrofit2.Retrofit;

@Singleton
@Component(modules = { BaseApplicationModule.class })
public interface BaseApplicationComponent {

    void inject(BaseApplication baseApplication);
    void inject(TrackRecordingHandler trackRecordingHandler);

    void inject(BluetoothPairingPreference bluetoothPairingPreference);
    void inject(SelectBluetoothPreference selectBluetoothPreference);
    void inject(Tempomat tempomat);

    void inject(MapExpandedActivity mapExpandedActivity);
    void inject(DAOProvider daoProvider);

    void inject(LocationHandler locationHandler);
    void inject(UserStatisticsProcessor statisticsProcessor);
    void inject(UserManager userManager);

    // activity injections
    void inject(SendLogFileActivity activity);
    void inject(LogbookActivity logbookActivity);
    void inject(SigninActivity loginActivity);
    void inject(SignupActivity registerActivity);
    void inject(CarSelectionActivity carSelectionActivity);
    void inject(OBDSelectionActivity obdSelectionActivity);
    void inject(TrackDetailsActivity trackDetailsActivity);
    void inject(TrackStatisticsActivity trackStatisticsActivity);
    void inject(SplashScreenActivity splashScreenActivity);

    // fragment injections
    void inject(DashboardFragment dashboardFragment);
    void inject(TrackListLocalCardFragment fragment);
    void inject(TrackListRemoteCardFragment fragment);
    void inject(CarSelectionAddCarFragment carSelectionAddCarFragment);
    void inject(LogbookAddFuelingFragment logbookAddFuelingFragment);
    void inject(OBDSelectionFragment obdSelectionFragment);
    void inject(OthersFragment othersFragment);
    void inject(CarSelectionHsnTsnFragment hsnTsnFragment);
    void inject(CarSelectionAttributesFragment attributesFragment);

    // service injections
    void inject(TrackUploadService trackUploadService);
    void inject(EnviroCarDataService enviroCarDataService);
    void inject(EnviroCarVehicleDB enviroCarVehicleDB);
    void inject(TrackChunkUploaderService trackChunkUploaderService);

    // interactors
    void inject(GetUserStatistic getUserStatistic);

    // module extensions
    MainActivityComponent plus(MainActivityModule module);
    RecordingComponent plus(RecordingModule module);
    AutoRecordingComponent plus(AutoRecordingModule module);
    RecordingScreenComponent plus(RecordingScreenModule module);

    // injection getter
    CacheCarDAO getCacheCarDAO();
    RemoteCarDAO getRemoteCarDAO();
    RemoteTrackDAO getRemoteTrackDAO();
    CacheTrackDAO getCacheTrackDAO();
    RemoteUserDAO getRemoteUserDAO();
    CacheUserDAO getCacheUserDAO();
    RemoteUserStatisticsDAO getRemoteUserStatisticsDAO();
    RemoteFuelingDAO getRemoteFuelingDAO();
    CacheFuelingDAO getCacheFuelingDAO();
    RemoteTermsOfUseDAO getRemoteTermsOfUseDAO();
    RemotePrivacyStatementDAO getRemotePrivacyStatementDAO();
    CacheTermsOfUseDAO getCacheTermsOfUseDAO();
    CachePrivacyStatementDAO getCachePrivacyStatementDao();
    RemoteAnnouncementsDAO getRemoteAnnouncementsDAO();
    CacheAnnouncementsDAO getCacheAnnouncementsDAO();
    Retrofit provideRetrofit();


}
