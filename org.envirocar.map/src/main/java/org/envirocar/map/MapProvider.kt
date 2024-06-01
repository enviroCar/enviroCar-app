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
 * * `OsmDroidMapProvider`
 * * `GoogleMapProvider`
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
