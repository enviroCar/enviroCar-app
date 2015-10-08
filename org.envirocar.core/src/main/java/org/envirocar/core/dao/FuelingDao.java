package org.envirocar.core.dao;


import org.envirocar.core.entity.Fueling;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;

import java.util.List;

import rx.Observable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface FuelingDAO {

    List<Fueling> getFuelings() throws NotConnectedException, UnauthorizedException,
            DataRetrievalFailureException;

    Observable<List<Fueling>> getFuelingsObservable();

    void createFueling(Fueling fueling) throws NotConnectedException,
            ResourceConflictException, UnauthorizedException;
}
