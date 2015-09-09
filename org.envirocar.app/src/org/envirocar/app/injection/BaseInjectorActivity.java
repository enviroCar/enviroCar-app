package org.envirocar.app.injection;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.common.base.Preconditions;
import com.squareup.otto.Bus;

import org.envirocar.app.logging.Logger;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.ObjectGraph;

/**
 * @author dewall
 */
public abstract class BaseInjectorActivity extends AppCompatActivity implements Injector,
        InjectionModuleProvider {
    private static final Logger LOGGER = Logger.getLogger(BaseInjectorActivity.class);

    private ObjectGraph mObjectGraph;

    // Injected variables.
    @Inject
    protected Bus mBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LOGGER.debug("onCreate()");
        super.onCreate(savedInstanceState);

        mObjectGraph = ((Injector) getApplicationContext()).getObjectGraph().plus
                (getInjectionModules().toArray());

        // Inject all variables in this object.
        injectObjects(this);
    }

    @Override
    protected void onStart(){
        super.onStart();
        LOGGER.info("onStart()");

        // Register on the bus.
        mBus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LOGGER.info("onStop()");

        // Unregister from the bus.
        mBus.unregister(this);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return mObjectGraph;
    }

    @Override
    public List<Object> getInjectionModules() {
        return new ArrayList<>();
    }

    @Override
    public void injectObjects(Object instance) {
        Preconditions.checkNotNull(instance, "Cannot inject into Null objects.");
        Preconditions.checkNotNull(mObjectGraph, "The ObjectGraph must be initialized before use.");
        mObjectGraph.inject(instance);
    }
}
