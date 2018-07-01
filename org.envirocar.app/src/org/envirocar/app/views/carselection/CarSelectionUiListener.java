package org.envirocar.app.views.carselection;

import org.envirocar.core.entity.Car;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
interface CarSelectionUiListener {

    void onHideAddCarFragment();

    void onCarAdded(Car car);
}
