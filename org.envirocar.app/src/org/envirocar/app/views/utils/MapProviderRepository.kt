package org.envirocar.app.views.utils

import android.app.Application
import org.envirocar.app.handler.ApplicationSettings
import org.envirocar.map.MapProvider
import org.envirocar.map.provider.mapbox.MapboxMapProvider
import org.envirocar.map.provider.maplibre.MapLibreMapProvider

class MapProviderRepository(private val applicationContext: Application) {

    val value: MapProvider get() = ApplicationSettings.getMapProvider(applicationContext).let {
        when (it) {
            MapLibreMapProvider::class.qualifiedName -> MapLibreMapProvider(ApplicationSettings.getMapLibreStyle(applicationContext))
            MapboxMapProvider::class.qualifiedName -> MapboxMapProvider(ApplicationSettings.getMapboxStyle(applicationContext))
            else -> error("Unknown Class<T: MapProvider>: $it")
        }
    }

    companion object {
        @Volatile
        private var instance: MapProviderRepository? = null

        operator fun invoke(applicationContext: Application) = instance ?: synchronized(this) {
            instance ?: MapProviderRepository(applicationContext).also { instance = it }
        }
    }
}
