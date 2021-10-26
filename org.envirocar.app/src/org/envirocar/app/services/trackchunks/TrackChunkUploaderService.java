package org.envirocar.app.services.trackchunks;

import com.squareup.otto.Subscribe;
import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.injection.BaseInjectorService;
import org.envirocar.core.events.recording.RecordingNewMeasurementEvent;
import org.envirocar.core.logging.Logger;

public class TrackChunkUploaderService extends BaseInjectorService {

    private static final Logger LOG = Logger.getLogger(TrackChunkUploaderService.class);

    @Subscribe
    public void onReceiveRecordingNewMeasurementEvent(RecordingNewMeasurementEvent event) {
        LOG.info("Received event. %s", event.toString());
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent appComponent) {
        appComponent.inject(this);
    }
}
