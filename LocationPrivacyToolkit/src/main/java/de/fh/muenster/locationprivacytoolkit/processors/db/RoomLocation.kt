package de.fh.muenster.locationprivacytoolkit.processors.db

import android.location.Location
import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["time", "latitude", "longitude"])
data class RoomLocation(
    @ColumnInfo(name = "time") val time: Long,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "altitude") val altitude: Double,
    @ColumnInfo(name = "accuracy") val accuracy: Float,
    @ColumnInfo(name = "speed") val speed: Float,
    @ColumnInfo(name = "bearing") val bearing: Float,
    @ColumnInfo(name = "isExample") val isExample: Boolean,
) {
    constructor(location: Location, isExample: Boolean = false) : this(
        location.time,
        location.latitude,
        location.longitude,
        location.altitude,
        location.accuracy,
        location.speed,
        location.bearing,
        isExample
    )

    val location: Location
        get() {
            return Location("").also {
                it.time = time
                it.latitude = latitude
                it.longitude = longitude
                it.altitude = altitude
                it.accuracy = accuracy
                it.speed = speed
                it.bearing = bearing
            }
        }
}