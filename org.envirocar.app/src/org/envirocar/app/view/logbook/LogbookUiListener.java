package org.envirocar.app.view.logbook;

import org.envirocar.core.entity.Fueling;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
interface LogbookUiListener {
    void onHideAddFuelingCard();

    void onFuelingUploaded(Fueling fueling);
}
