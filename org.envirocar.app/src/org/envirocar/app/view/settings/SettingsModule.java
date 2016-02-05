package org.envirocar.app.view.settings;

import org.envirocar.app.BaseApplicationModule;

import dagger.Module;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Module(
        injects = {
                GeneralSettingsFragment.class,
                OBDSettingsFragment.class,
                OtherSettingsFragment.class
        },
        addsTo = BaseApplicationModule.class,
        library = true,
        complete = false
)
public class SettingsModule {
}
