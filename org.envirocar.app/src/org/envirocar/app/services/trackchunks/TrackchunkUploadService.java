package org.envirocar.app.services.trackchunks;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.handler.TrackUploadHandler;
import org.envirocar.app.injection.BaseInjectorService;
import org.envirocar.core.EnviroCarDB;

import javax.inject.Inject;

public class TrackchunkUploadService extends BaseInjectorService {

    @Inject
    protected EnviroCarDB enviroCarDB;
    @Inject
    protected TrackUploadHandler trackUploadHandler;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {

    }
}
