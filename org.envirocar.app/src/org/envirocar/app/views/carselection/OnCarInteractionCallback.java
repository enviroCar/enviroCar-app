package org.envirocar.app.views.carselection;

import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Vehicles;

public interface OnCarInteractionCallback {

    String resolveFuelType(String power_source_id);

    void addAndRegisterCar(Vehicles vehicle);
}
