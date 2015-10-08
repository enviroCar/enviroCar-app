/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
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
	 * {@link Track#setLazyLoadingMeasurements(boolean)} with
	 * false shall be set.
	 * 
	 * @param track the track
	 */
	void lazyLoadMeasurements(Track track);

}
