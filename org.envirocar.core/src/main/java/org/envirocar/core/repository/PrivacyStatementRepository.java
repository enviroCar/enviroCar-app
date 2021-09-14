/**
 * Copyright (C) 2013 - 2021 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.core.repository;

import org.envirocar.core.entity.PrivacyStatement;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;

import java.util.List;

import io.reactivex.Observable;


/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface PrivacyStatementRepository {

    PrivacyStatement getPrivacyStatement(String id) throws DataRetrievalFailureException, NotConnectedException;

    Observable<PrivacyStatement> getPrivacyStatementObservable(String id);

    List<PrivacyStatement> getPrivacyStatements() throws DataRetrievalFailureException, NotConnectedException;

    Observable<List<PrivacyStatement>> getPrivacyStatementsObservable();

}
