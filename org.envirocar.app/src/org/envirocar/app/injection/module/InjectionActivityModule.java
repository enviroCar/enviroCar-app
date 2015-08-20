package org.envirocar.app.injection.module;


import android.app.Activity;
import android.content.Context;

import org.envirocar.app.BaseMainActivity;
import org.envirocar.app.activity.ListTracksFragment;
import org.envirocar.app.activity.LogbookFragment;
import org.envirocar.app.view.LoginFragment;
import org.envirocar.app.view.RegisterFragment;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.activity.StartStopButtonUtil;
import org.envirocar.app.application.CarPreferenceHandler;
import org.envirocar.app.application.TermsOfUseManager;
import org.envirocar.app.application.UploadManager;
import org.envirocar.app.view.dashboard.RealDashboardFragment;
import org.envirocar.app.view.dashboard.DashboardMainFragment;
import org.envirocar.app.injection.InjectionActivityScope;
import org.envirocar.app.view.preferences.Tempomat;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        injects = {
                BaseMainActivity.class,
                TermsOfUseManager.class,
                CarPreferenceHandler.class,
                ListTracksFragment.class,
                LogbookFragment.class,
                LoginFragment.class,
                RegisterFragment.class,
                SettingsActivity.class,
                StartStopButtonUtil.class,
                RealDashboardFragment.class,
                UploadManager.class,
                Tempomat.class,
                DashboardMainFragment.class
        },
        addsTo = InjectionApplicationModule.class,
        library = true,
        complete = false
)
public class InjectionActivityModule {

    private Activity mActivity;

    /**
     * Constructor
     *
     * @param activity the activity of this scope.
     */
    public InjectionActivityModule(Activity activity) {
        this.mActivity = activity;
    }


    @Provides
    public Activity provideActivity() {
        return mActivity;
    }

    @Provides
    @InjectionActivityScope
    public Context provideContext() {
        return mActivity;
    }

    @Provides
    @Singleton
    public RealDashboardFragment provideRealDashboardFragment(){
        return new RealDashboardFragment();
    }

}
