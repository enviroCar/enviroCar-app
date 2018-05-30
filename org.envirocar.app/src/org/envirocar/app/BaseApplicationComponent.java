package org.envirocar.app;

import com.squareup.sqlbrite.SqlBrite;

import org.envirocar.app.activity.StartStopButtonUtil;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.TrackRecordingHandler;
import org.envirocar.app.services.OBDConnectionService;
import org.envirocar.app.services.SystemStartupService;
import org.envirocar.app.services.TrackUploadService;
import org.envirocar.app.view.LoginActivity;
import org.envirocar.app.view.carselection.CarSelectionActivity;
import org.envirocar.app.view.carselection.CarSelectionAddCarFragment;
import org.envirocar.app.view.logbook.LogbookActivity;
import org.envirocar.app.view.logbook.LogbookAddFuelingFragment;
import org.envirocar.app.view.obdselection.OBDSelectionActivity;
import org.envirocar.app.view.obdselection.OBDSelectionFragment;
import org.envirocar.app.view.preferences.BluetoothDiscoveryIntervalPreference;
import org.envirocar.app.view.preferences.BluetoothPairingPreference;
import org.envirocar.app.view.preferences.SelectBluetoothPreference;
import org.envirocar.app.view.preferences.Tempomat;
import org.envirocar.app.view.settings.OBDSettingsFragment;
import org.envirocar.app.view.settings.SettingsActivity;
import org.envirocar.app.view.trackdetails.TrackDetailsActivity;
import org.envirocar.app.view.trackdetails.TrackStatisticsActivity;
import org.envirocar.remote.dao.CacheAnnouncementsDAO;
import org.envirocar.remote.dao.CacheCarDAO;
import org.envirocar.remote.dao.CacheFuelingDAO;
import org.envirocar.remote.dao.CacheTermsOfUseDAO;
import org.envirocar.remote.dao.CacheTrackDAO;
import org.envirocar.remote.dao.CacheUserDAO;
import org.envirocar.remote.dao.RemoteAnnouncementsDAO;
import org.envirocar.remote.dao.RemoteCarDAO;
import org.envirocar.remote.dao.RemoteFuelingDAO;
import org.envirocar.remote.dao.RemoteTermsOfUseDAO;
import org.envirocar.remote.dao.RemoteTrackDAO;
import org.envirocar.remote.dao.RemoteUserDAO;
import org.envirocar.remote.dao.RemoteUserStatisticsDAO;

import javax.inject.Singleton;

import dagger.Component;
import retrofit.Retrofit;

/**
 * @author Sai Krishna
 */
@Singleton
@Component(
        modules = BaseApplicationModule.class
)
public interface BaseApplicationComponent {

    void inject(BaseApplication baseApplication);
    void inject(StartStopButtonUtil startStopButtonUtil);
    void inject(TrackRecordingHandler trackRecordingHandler);
    void inject(OBDConnectionService obdConnectionService);
    void inject(SystemStartupService systemStartupService);
    void inject(TrackUploadService trackUploadService);
    void inject(CarSelectionActivity carSelectionActivity);
    void inject(CarSelectionAddCarFragment carSelectionAddCarFragment);
    void inject(LogbookActivity logbookActivity);
    void inject(LogbookAddFuelingFragment logbookAddFuelingFragment);
    void inject(OBDSelectionActivity obdSelectionActivity);
    void inject(OBDSelectionFragment obdSelectionFragment);
    void inject(BluetoothDiscoveryIntervalPreference bluetoothDiscoveryIntervalPreference);
    void inject(BluetoothPairingPreference bluetoothPairingPreference);
    void inject(SelectBluetoothPreference selectBluetoothPreference);
    void inject(Tempomat tempomat);
    void inject(OBDSettingsFragment obdSettingsFragment);
    void inject(SettingsActivity settingsActivity);
    void inject(TrackDetailsActivity trackDetailsActivity);
    void inject(TrackStatisticsActivity trackStatisticsActivity);
    void inject(LoginActivity loginActivity);
    void inject(DAOProvider daoProvider);

    MainActivityComponent plus(MainActivityModule mainActivityModule);


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
    CacheTermsOfUseDAO getCacheTermsOfUseDAO();
    RemoteAnnouncementsDAO getRemoteAnnouncementsDAO();
    CacheAnnouncementsDAO getCacheAnnouncementsDAO();
    Retrofit provideRetrofit();
    SqlBrite provideSqlBrite();


}
