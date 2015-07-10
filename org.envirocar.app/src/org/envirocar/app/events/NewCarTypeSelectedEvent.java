package org.envirocar.app.events;

import com.google.common.base.MoreObjects;

import org.envirocar.app.model.Car;

/**
 * @author dewall
 */
public class NewCarTypeSelectedEvent {

    public final Car mCar;

    /**
     * Constructor.
     *
     * @param mCar the instance of the currently selected car.
     */
    public NewCarTypeSelectedEvent(Car mCar) {
        this.mCar = mCar;
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("Car", mCar)
                .toString();
    }
}
