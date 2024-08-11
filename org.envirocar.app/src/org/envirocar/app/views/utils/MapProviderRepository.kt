package org.envirocar.app.views.utils

import android.app.Application
import org.envirocar.app.handler.ApplicationSettings
import org.envirocar.map.MapProvider

class MapProviderRepository(private val applicationContext: Application) {

    val value: MapProvider
        get() = ApplicationSettings.getMapProvider(applicationContext).let {
            when {
                it.contains(PROVIDER_MAPLIBRE) -> Class.forName("org.envirocar.map.provider.maplibre.MapLibreMapProvider")
                    .getConstructor(String::class.java)
                    .newInstance(ApplicationSettings.getMapLibreStyle(applicationContext)) as MapProvider
                it.contains(PROVIDER_MAPBOX) -> Class.forName("org.envirocar.map.provider.mapbox.MapboxMapProvider")
                    .getConstructor(String::class.java)
                    .newInstance(ApplicationSettings.getMapboxStyle(applicationContext)) as MapProvider
                else -> error("Unknown Class<T: MapProvider>: $it")
            }
        }



    companion object {
        @Volatile
        private var instance: MapProviderRepository? = null

        operator fun invoke(applicationContext: Application) = instance ?: synchronized(this) {
            instance ?: MapProviderRepository(applicationContext).also { instance = it }
        }

        const val PROVIDER_MAPLIBRE = "MapLibre"
        const val PROVIDER_MAPBOX = "Mapbox"
    }
}
