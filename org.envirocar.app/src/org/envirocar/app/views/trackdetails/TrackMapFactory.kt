package org.envirocar.app.views.trackdetails

import android.animation.ArgbEvaluator
import org.envirocar.app.R
import org.envirocar.core.entity.Measurement
import org.envirocar.core.entity.Track
import org.envirocar.map.camera.CameraUpdateFactory
import org.envirocar.map.model.Marker
import org.envirocar.map.model.Point
import org.envirocar.map.model.Polyline
import kotlin.math.max

class TrackMapFactory(track: Track) {

    private val measurements = when {
        track.measurements == null -> null
        track.measurements.isEmpty() -> null
        else -> track.measurements
    }

    private val bounds = measurements?.let {
        val latitudeMin = measurements.minOf { it.latitude }
        val latitudeMax = measurements.maxOf { it.latitude }
        val longitudeMin = measurements.minOf { it.longitude }
        val longitudeMax = measurements.maxOf { it.longitude }
        val latitudeRatio = max((latitudeMax - latitudeMin) / 10.0, 0.01)
        val longitudeRatio = max((longitudeMax - longitudeMin) / 10.0, 0.01)
        listOf(
            Point(latitudeMin - latitudeRatio, longitudeMin - longitudeRatio),
            Point(latitudeMax + latitudeRatio, longitudeMax + longitudeRatio)
        )
    }

    val cameraUpdateBasedOnBounds = bounds?.run { CameraUpdateFactory.newCameraUpdateBasedOnBounds(bounds, 50.0F) }

    val startMarker = measurements?.run {
        Marker.Builder(Point(first().latitude, first().longitude))
            .withDrawable(R.drawable.start_marker)
            .build()
    }

    val stopMarker = measurements?.run {
        Marker.Builder(Point(last().latitude, last().longitude))
            .withDrawable(R.drawable.stop_marker)
            .build()
    }

    val polyline = measurements?.run {
        Polyline.Builder(map { Point(it.latitude, it.longitude) })
            .withWidth(POLYLINE_WIDTH)
            .withColor(POLYLINE_COLOR)
            .build()
    }

    fun getGradientPolyline(key: Measurement.PropertyKey) = measurements?.run {
        when {
            size > 2 -> {
                val values = measurements.map { if (it.hasProperty(key)) it.getProperty(key).toFloat() else 0.0F }
                val min = if (key == Measurement.PropertyKey.SPEED) 0.0 else values.min()
                val max = values.max()
                val evaluator = ArgbEvaluator()
                val colors = values.map { evaluator.evaluate(it / max, POLYLINE_COLORS_MIN, POLYLINE_COLORS_MAX) as Int }
                Polyline.Builder(map { Point(it.latitude, it.longitude) })
                    .withWidth(POLYLINE_WIDTH)
                    .withColors(colors)
                    .build()
            }
            else -> {
                Polyline.Builder(map { Point(it.latitude, it.longitude) })
                    .withWidth(POLYLINE_WIDTH)
                    .withColor(POLYLINE_COLOR)
                    .build()
            }
        }
    }

    companion object {
        private const val POLYLINE_WIDTH = 4.0F
        private const val POLYLINE_COLOR = 0xFF0065A0.toInt()
        private const val POLYLINE_COLORS_MIN = 0xFF00FF00.toInt()
        private const val POLYLINE_COLORS_MAX = 0xFFFF0000.toInt()
    }
}
