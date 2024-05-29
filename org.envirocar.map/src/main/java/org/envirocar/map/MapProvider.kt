package org.envirocar.map

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
 */
interface MapProvider<out T : View> {

    /**
     * Creates the visible map view.
     */
    fun create(): T
}
