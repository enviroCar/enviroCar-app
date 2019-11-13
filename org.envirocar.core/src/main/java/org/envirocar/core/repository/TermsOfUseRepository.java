/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.core.repository;


import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;

import java.util.List;

import io.reactivex.Observable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface TermsOfUseRepository {

    TermsOfUse getTermsOfUse(String id) throws DataRetrievalFailureException, NotConnectedException;

    /**
     * Get an {@link Observable} which will emit the terms of use for a given id.
     *
     * @param id the id of the terms of use.
     */
    Observable<TermsOfUse> getTermsOfUseObservable(String id);

    List<TermsOfUse> getAllTermsOfUse() throws DataRetrievalFailureException, NotConnectedException;

    /**
     * Get an {@link Observable} which will emit all existing terms of use.
     */
    Observable<List<TermsOfUse>> getAllTermsOfUseObservable();
}
