package org.envirocar.map.provider.maplibre

import android.content.Context
import org.envirocar.map.MapController
import org.envirocar.map.MapProvider
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
 */
class MapLibreMapProvider(
    private val style: String = DEFAULT_STYLE
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
            controllerInstance = MapLibreMapController(viewInstance)
        }
        return controllerInstance
    }

    companion object {
        const val DEFAULT_STYLE = "asset://maplibre_default_style.json"

        @Volatile
        private var initailized = false
    }
}
