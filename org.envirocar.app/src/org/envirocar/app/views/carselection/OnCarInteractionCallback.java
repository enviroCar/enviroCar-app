package org.envirocar.app.views.carselection;

import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Vehicles;

public interface OnCarInteractionCallback {

    //create car using vehicle entity i.e Vehicle to Car entity
   Car createCar(Vehicles vehicle);

    String resolveFuelType(String power_source_id);

    void addAndRegisterCar(Car car);
}
