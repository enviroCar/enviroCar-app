package org.envirocar.app.view.obdselection;


import org.envirocar.app.BaseApplicationModule;

import dagger.Module;


/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Module(
        injects = {OBDSelectionFragment.class},
        addsTo = BaseApplicationModule.class,
        library = true,
        complete = false
)
public class OBDSelectionModule {
}
