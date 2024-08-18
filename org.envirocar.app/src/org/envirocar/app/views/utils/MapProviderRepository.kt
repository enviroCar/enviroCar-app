package org.envirocar.app.views.utils

import android.app.Application
import org.envirocar.app.handler.ApplicationSettings
import org.envirocar.map.MapProvider
import org.envirocar.map.model.AttributionSettings
import org.envirocar.map.model.LogoSettings

class MapProviderRepository(private val applicationContext: Application) {

    private var attribution: AttributionSettings = AttributionSettings.default()
    private var logo: LogoSettings = LogoSettings.default()

    // NOTE: The named arguments in default constructor cannot be used as an alternative due to issues with Java interoperability.
    constructor(
        applicationContext: Application,
        attribution: AttributionSettings = AttributionSettings.default(),
        logo: LogoSettings = LogoSettings.default()
    ) : this(applicationContext) {
        this.attribution = attribution
        this.logo = logo
    }

    val value: MapProvider
        get() = ApplicationSettings.getMapProvider(applicationContext).let {
            when {
                it.contains(PROVIDER_MAPLIBRE) -> Class.forName("org.envirocar.map.provider.maplibre.MapLibreMapProvider")
                    .getConstructor(
                        String::class.java,
                        AttributionSettings::class.java,
                        LogoSettings::class.java
                    )
                    .newInstance(
                        ApplicationSettings.getMapLibreStyle(applicationContext),
                        attribution,
                        logo
                    ) as MapProvider

                it.contains(PROVIDER_MAPBOX) -> Class.forName("org.envirocar.map.provider.mapbox.MapboxMapProvider")
                    .getConstructor(
                        String::class.java,
                        AttributionSettings::class.java,
                        LogoSettings::class.java
                    )
                    .newInstance(
                        ApplicationSettings.getMapboxStyle(applicationContext),
                        attribution,
                        logo
                    ) as MapProvider

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
