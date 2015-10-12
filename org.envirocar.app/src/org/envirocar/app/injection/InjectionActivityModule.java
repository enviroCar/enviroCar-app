package org.envirocar.app.injection;


import android.app.Activity;
import android.content.Context;

import org.envirocar.app.BaseMainActivity;
import org.envirocar.app.activity.StartStopButtonUtil;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.TermsOfUseManager;
import org.envirocar.app.handler.UploadManager;
import org.envirocar.app.view.LogbookFragment;
import org.envirocar.app.view.RegisterFragment;
import org.envirocar.app.view.dashboard.DashboardMainFragment;
import org.envirocar.app.view.dashboard.RealDashboardFragment;
import org.envirocar.app.view.preferences.Tempomat;
import org.envirocar.core.injection.InjectionActivityScope;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Module(
        injects = {
                BaseMainActivity.class,
                TermsOfUseManager.class,
                CarPreferenceHandler.class,
                LogbookFragment.class,
                RegisterFragment.class,
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