package org.envirocar.map.provider.mapbox

import android.content.Context
import com.mapbox.maps.MapView
import org.envirocar.map.MapController
import org.envirocar.map.MapProvider

/**
 * [MapboxMapProvider]
 * -------------------
 * [Mapbox](https://www.mapbox.com) based implementation for [MapProvider].
 */
class MapboxMapProvider : MapProvider {
    private lateinit var viewInstance: MapView
    private lateinit var controllerInstance: MapboxMapController

    override fun getView(context: Context): MapView {
        if (!::viewInstance.isInitialized) {
            viewInstance = MapView(context)
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
}
