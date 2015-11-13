package org.envirocar.core.injection;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.common.base.Preconditions;
import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.ObjectGraph;

/**
 * @author dewall
 */
public abstract class BaseInjectorActivity extends AppCompatActivity implements Injector,
        InjectionModuleProvider {
    private ObjectGraph mObjectGraph;

    // Injected variables.
    @Inject
    protected Bus mBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mObjectGraph = ((Injector) getApplicationContext()).getObjectGraph().plus
                (getInjectionModules().toArray());

        // Inject all variables in this object.
        injectObjects(this);
    }

    @Override
    protected void onStart(){
        super.onStart();

        // Register on the bus.
        mBus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

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
