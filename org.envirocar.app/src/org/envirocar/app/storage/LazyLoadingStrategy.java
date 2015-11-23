/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.storage;

import org.envirocar.core.entity.Track;

/**
 * An interface for providing a strategy to lazy load memory consuming
 * resources.
 *
 */
public interface LazyLoadingStrategy {

	/**
	 * an implementation shall load all measurements
	 * for the given track. after succesful loading,
	 * {@link Track#setLazyMeasurements(boolean)} with
	 * false shall be set.
	 * 
	 * @param track the track
	 */
	void lazyLoadMeasurements(Track track);

}
