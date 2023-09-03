package de.fh.muenster.locationprivacytoolkit.processors

import android.content.Context
import android.location.*
import android.util.Log
import de.fh.muenster.locationprivacytoolkit.R
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig
import de.fh.muenster.locationprivacytoolkit.processors.utils.DistanceFormat
import de.fh.muenster.locationprivacytoolkit.processors.utils.LocationProcessorUserInterface
import gov.nasa.worldwind.geom.LatLon
import gov.nasa.worldwind.geom.LatLon.rhumbEndPosition
import gov.nasa.worldwind.globes.Earth

/**
 * The AccuracyProcessor changes the accuracy of a location.
 */
class AccuracyProcessor(context: Context) : AbstractInternalLocationProcessor(context) {

    override val config = LocationPrivacyConfig.Accuracy
    override val titleId = R.string.accuracyTitle
    override val subtitleId = R.string.accuracySubtitle
    override val descriptionId = R.string.accuracyDescription
    override val userInterface = LocationProcessorUserInterface.Slider
    override val values = arrayOf(5000, 1000, 500, 100, 0)

    override fun formatLabel(value: Int): String = DistanceFormat.formatDistance(value)

    /**
     * The location will be moved to a random point around the actual
     * point and the `accuracy` metadata will be changed as well
     */
    override fun manipulateLocation(location: Location, config: Int): Location {
        if (location.accuracy >= config) {
            return location
        }

        // TODO: translate config to actual desired accuracy in meters
        val randomDirection = (0..359).random()
        val randomDistance = (0..config).random()

        Log.d("distance", randomDistance.toString())

        val radDistance = randomDistance / Earth.WGS84_EQUATORIAL_RADIUS


        val loc = LatLon.fromDegrees(location.latitude, location.longitude)
        val pos = rhumbEndPosition(loc, Math.toRadians(randomDirection.toDouble()), radDistance)

        val transformedLocation = Location(location)
        transformedLocation.longitude = pos.longitude.degrees
        transformedLocation.latitude = pos.latitude.degrees
        transformedLocation.accuracy = config.toFloat()

        return transformedLocation
    }
}
