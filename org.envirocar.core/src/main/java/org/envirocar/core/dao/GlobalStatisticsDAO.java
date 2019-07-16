package org.envirocar.core.dao;



import org.envirocar.core.entity.GlobalStatistics;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.UnauthorizedException;

import rx.Observable;

public interface GlobalStatisticsDAO {
    GlobalStatistics getGlobalStatistics()
            throws DataRetrievalFailureException, UnauthorizedException;

    Observable<GlobalStatistics> getGlobalStatisticsObservable();

    GlobalStatistics getGlobalStatisticsByPhenomenon(String phenomenon)
            throws DataRetrievalFailureException, UnauthorizedException;

    Observable<GlobalStatistics> getGlobalStatisticsByPhenomenonObservable(String phenomenon);
}
