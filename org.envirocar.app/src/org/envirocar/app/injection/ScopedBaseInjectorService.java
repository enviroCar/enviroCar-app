package org.envirocar.app.injection;

import org.envirocar.app.BaseApplicationComponent;

/**
 * @author dewall
 */
public abstract class ScopedBaseInjectorService extends BaseInjectorService {

    @Override
    public void onCreate() {
        super.onCreate();
        setupServiceComponent();
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        // nothing to do
    }

    protected abstract void setupServiceComponent();
}


