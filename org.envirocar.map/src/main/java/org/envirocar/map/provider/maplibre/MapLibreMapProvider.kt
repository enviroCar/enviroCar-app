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

package org.envirocar.map.provider.maplibre

import android.content.Context
import org.envirocar.map.MapController
import org.envirocar.map.MapProvider
import org.envirocar.map.model.AttributionSettings
import org.envirocar.map.model.LogoSettings
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * [MapLibreMapProvider]
 * ---------------------
 * [MapLibre](https://maplibre.org) based implementation for [MapProvider].
 *
 * Following options are available to be configured:
 *
 * @param style
 * The style for the map to be loaded from a specified URI or from a JSON represented as [String].
 * The URI can be one of the following forms:
 * * `http://`
 * * `https://`
 * * `asset://`
 * * `file://`
 * @param attribution
 * The attribution settings for the map.
 * @param logo
 * The logo settings for the map.
 */
class MapLibreMapProvider(
    private val style: String = DEFAULT_STYLE,
    private val attribution: AttributionSettings = DEFAULT_ATTRIBUTION,
    private val logo: LogoSettings = DEFAULT_LOGO
) : MapProvider {
    private lateinit var viewInstance: MapView
    private lateinit var controllerInstance: MapLibreMapController

    override fun getView(context: Context): MapView {
        if (!initailized) {
            initailized = true
            // Must be called before consuming any MapLibre API.
            MapLibre.getInstance(context)
        }
        if (!::viewInstance.isInitialized) {
            viewInstance = MapView(context).apply {
                getMapAsync {
                    it.setStyle(Style.Builder().fromUri(style))
                }
            }
        }
        return viewInstance
    }

    override fun getController(): MapController {
        if (!::viewInstance.isInitialized) {
            error("MapLibreMapProvider is not initialized.")
        }
        if (!::controllerInstance.isInitialized) {
            controllerInstance = MapLibreMapController(
                viewInstance,
                attribution,
                logo
            )
        }
        return controllerInstance
    }

    companion object {
        const val DEFAULT_STYLE = "asset://maplibre_default_style.json"
        val DEFAULT_ATTRIBUTION = AttributionSettings.default()
        val DEFAULT_LOGO = LogoSettings.default()

        @Volatile
        private var initailized = false
    }
}
