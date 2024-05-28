package org.envirocar.map

import org.envirocar.map.camera.CameraUpdate
import org.envirocar.map.model.Marker
import org.envirocar.map.model.Polyline

/**
 * [MapController]
 * ---------------
 * [MapController] provides various methods to interact with the visible [MapView].
 *
 * ```
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     val mapView: MapView = findViewById(R.id.mapView)
 *     val mapController: MapController = mapView.getController(...)
 *     /* Interact with the [MapView] using the [MapController]. */
 * }
 * ```
 *
 * @see MapView
 * @see CameraUpdate
 * @see Marker
 * @see Polyline
 */
interface MapController {

    /** Sets the minimum zoom level. */
    fun setMinZoom(minZoom: Float)

    /** Sets the maximum zoom level. */
    fun setMaxZoom(maxZoom: Float)

    /** Notifies the [MapView] about a [CameraUpdate]. */
    fun notifyCameraUpdate(cameraUpdate: CameraUpdate)

    /** Adds a [Marker] to the [MapView]. */
    fun addMarker(marker: Marker)

    /** Adds a [Polyline] to the [MapView]. */
    fun addPolyline(polyline: Polyline)

    /** Removes a [Marker] from the [MapView]. */
    fun removeMarker(marker: Marker)

    /** Removes a [Polyline] from the [MapView]. */
    fun removePolyline(polyline: Polyline)

}
