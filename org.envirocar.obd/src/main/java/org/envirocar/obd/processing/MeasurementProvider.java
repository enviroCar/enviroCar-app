package org.envirocar.obd.processing;

import android.location.Location;

import org.envirocar.core.entity.Measurement;
import org.envirocar.obd.commands.response.DataResponse;

import rx.Observable;

public interface MeasurementProvider {

    Observable<Measurement> measurements(long samplingRate);

    void consider(DataResponse dr);

    void newLocation(Location mLocation);
}
