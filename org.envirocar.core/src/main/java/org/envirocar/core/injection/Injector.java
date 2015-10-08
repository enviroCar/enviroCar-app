package org.envirocar.core.injection;

import dagger.ObjectGraph;

/**
 * @author dewall
 */
public interface Injector {
    /**
     * Gets the objectgraph of the implemented class.
     *
     * @return the objectgraph
     */
    public ObjectGraph getObjectGraph();

    /**
     * Injects a target object using this object's object graph.
     *
     * @param instance
     *            the target object
     */
    public void injectObjects(Object instance);
}
