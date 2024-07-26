package org.envirocar.map.provider.mapbox

import android.content.Context
import com.mapbox.maps.MapView
import org.envirocar.map.MapController
import org.envirocar.map.MapProvider

/**
 * [MapboxMapProvider]
 * -------------------
 * [Mapbox](https://www.mapbox.com) based implementation for [MapProvider].
 *
 * Following options are available to be configured:
 *
 * @param style
 * The style for the map to be loaded from a specified URI or from a JSON represented as [String].
 * The URI can be one of the following forms:
 * * `mapbox://`
 * * `http://`
 * * `https://`
 * * `asset://`
 * * `file://`
 * The default style is `mapbox://styles/mapbox/streets-v12`.
 */
class MapboxMapProvider(
    private val style: String = DEFAULT_STYLE
) : MapProvider {
    private lateinit var viewInstance: MapView
    private lateinit var controllerInstance: MapboxMapController

    override fun getView(context: Context): MapView {
        if (!::viewInstance.isInitialized) {
            viewInstance = MapView(context).apply {
                mapboxMap.loadStyle(style)
            }
        }
        return viewInstance
    }

    override fun getController(): MapController {
        if (!::viewInstance.isInitialized) {
            error("MapboxMapProvider is not initialized.")
        }
        if (!::controllerInstance.isInitialized) {
            controllerInstance = MapboxMapController(viewInstance)
        }
        return controllerInstance
    }

    companion object {
        const val DEFAULT_STYLE = "mapbox://styles/mapbox/streets-v12"
    }
}
