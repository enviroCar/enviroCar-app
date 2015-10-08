package org.envirocar.core.dao;


import org.envirocar.core.entity.Car;
import org.envirocar.core.exception.DataCreationFailureException;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;

import java.util.List;

import rx.Observable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface CarDAO {

    List<Car> getAllCars() throws NotConnectedException, DataRetrievalFailureException;

    Observable<List<Car>> getAllCarsObservable();

    String createCar(Car car) throws NotConnectedException, DataCreationFailureException,
            UnauthorizedException;
}
