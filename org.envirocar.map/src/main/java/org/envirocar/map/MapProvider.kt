/**
 * Copyright (C) 2013 - 2026 the enviroCar community
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

package org.envirocar.map

import android.content.Context
import android.view.View

/**
 * [MapProvider]
 * -------------
 * [MapProvider] allows to initialize a [MapView] with a specific provider.
 * Currently available providers are:
 * * `MapboxMapProvider`
 * * `MapLibreMapProvider`
 *
 * Each provider may require additional setup during compilation.
 * Please refer to the module's documentation for more information.
 */
interface MapProvider {

    /**
     * Returns the underlying [View] implementation for the specified map provider.
     * A fresh instance of the [View] is created if it doesn't exist.
     */
    fun getView(context: Context): View

    /**
     * Returns the underlying [MapController] implementation for the specified map provider.
     * The [getView] method must be called before this method.
     */
    fun getController(): MapController
}
