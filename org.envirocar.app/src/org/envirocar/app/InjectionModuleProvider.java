package org.envirocar.app;

import java.util.List;

/**
 * @author dewall
 */
public interface InjectionModuleProvider {
    /**
     * Returns a list of modules to be added to the ObjectGraph.
     *
     * @return a list of modules
     */
    List<Object> getInjectionModules();
}
