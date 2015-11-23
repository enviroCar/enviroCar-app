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

import android.content.Context;

import org.envirocar.core.entity.Track;
import org.envirocar.core.injection.Injector;

import javax.inject.Inject;

/**
 *
 */
public class LazyLoadingStrategyImpl implements LazyLoadingStrategy {

    @Inject
    protected DbAdapter mDBAdapter;

    /**
     * Constructor.
     *
     * @param context the context of this scope.
     */
    public LazyLoadingStrategyImpl(Context context) {
        ((Injector) context).injectObjects(this);
    }

    @Override
	public void lazyLoadMeasurements(Track track) {
		mDBAdapter.loadMeasurements(track);
		track.setLazyMeasurements(false);
	}

}
