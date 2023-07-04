package de.fh.muenster.locationprivacytoolkit.processors.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocationDAO {
    @Query("SELECT * FROM roomLocation WHERE isExample = :isExample")
    fun getAll(isExample: Boolean = false): List<RoomLocation>

    @Query("SELECT * FROM roomLocation WHERE isExample = :isExample AND time >= :fromTimestamp AND time <= :toTimestamp")
    fun getAll(
        isExample: Boolean = false,
        fromTimestamp: Long,
        toTimestamp: Long
    ): List<RoomLocation>

    @Query("SELECT * FROM roomLocation WHERE isExample = :isExample AND time == :atTimestamp LIMIT 1")
    fun get(
        isExample: Boolean = false,
        atTimestamp: Long
    ): RoomLocation

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(location: RoomLocation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg locations: RoomLocation)

    @Delete
    fun delete(location: RoomLocation)

    @Delete
    fun deleteAll(vararg locations: RoomLocation)

    @Query("DELETE FROM roomLocation WHERE isExample = true")
    fun deleteExampleLocations()
}