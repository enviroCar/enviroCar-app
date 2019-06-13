package org.envirocar.core.dao;

import org.envirocar.core.entity.TrackStatistics;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.UnauthorizedException;

import rx.Observable;

public interface TrackStatisticsDAO {
    TrackStatistics getTrackStatistics(String track)
            throws DataRetrievalFailureException, UnauthorizedException;

    Observable<TrackStatistics> getTrackStatisticsObservable(String track);

    TrackStatistics getTrackStatisticsByPhenomenon(String track, String phenomenon)
            throws DataRetrievalFailureException, UnauthorizedException;

    Observable<TrackStatistics> getTrackStatisticsByPhenomenonObservable(String track, String phenomenon);
}
