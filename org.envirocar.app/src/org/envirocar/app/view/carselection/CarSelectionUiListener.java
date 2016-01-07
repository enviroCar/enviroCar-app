package org.envirocar.app.view.carselection;

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
