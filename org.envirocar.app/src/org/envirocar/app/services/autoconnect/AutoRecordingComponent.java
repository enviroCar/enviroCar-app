package org.envirocar.app.services.autoconnect;

import dagger.Subcomponent;

@AutoRecordingScope
@Subcomponent(modules = {AutoRecordingModule.class})
public interface AutoRecordingComponent {

    void inject(AutoRecordingService service);

    void inject(OBDAutoRecordingStrategy autoRecordingStrategy);

    void inject(GPSAutoRecordingStrategy autoRecordingStrategy);

}
