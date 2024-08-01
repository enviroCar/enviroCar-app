package org.envirocar.app.views.utils

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.envirocar.app.BuildConfig
import org.envirocar.map.MapProvider
import org.envirocar.map.provider.mapbox.MapboxMapProvider
import org.envirocar.map.provider.maplibre.MapLibreMapProvider

class MapProviderRepository(context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val sharedPreferences = context.getSharedPreferences(
        this::class.qualifiedName,
        Context.MODE_PRIVATE
    )

    private val mapLibreStyle: String
        get() = sharedPreferences.getString(
            KEY_MAP_PROVIDER_MAPLIBRE_STYLE,
            DEFAULT_VALUE_MAP_PROVIDER_MAPLIBRE_STYLE
        ) ?: DEFAULT_VALUE_MAP_PROVIDER_MAPLIBRE_STYLE

    private val mapboxStyle: String
        get() = sharedPreferences.getString(
            KEY_MAP_PROVIDER_MAPBOX_STYLE,
            DEFAULT_VALUE_MAP_PROVIDER_MAPBOX_STYLE
        ) ?: DEFAULT_VALUE_MAP_PROVIDER_MAPBOX_STYLE

    val value: MapProvider
        get() = sharedPreferences.getString(KEY_MAP_PROVIDER, DEFAULT_VALUE_MAP_PROVIDER).let {
            when (it) {
                MapLibreMapProvider::class.qualifiedName -> MapLibreMapProvider(mapLibreStyle)
                MapboxMapProvider::class.qualifiedName -> MapboxMapProvider(mapboxStyle)
                else -> error("Unknown map provider: $it")
            }
        }

    fun setMapProvider(value: Class<out MapProvider>) {
        sharedPreferences.edit().putString(KEY_MAP_PROVIDER, value.name).apply()
    }

    fun setMapLibreStyle(value: String) {
        sharedPreferences.edit().putString(KEY_MAP_PROVIDER_MAPLIBRE_STYLE, value).apply()
    }

    fun setMapboxStyle(value: String) {
        sharedPreferences.edit().putString(KEY_MAP_PROVIDER_MAPBOX_STYLE, value).apply()
    }

    companion object {
        @Volatile
        private var instance: MapProviderRepository? = null

        operator fun invoke(context: Context) = instance ?: synchronized(this) {
            instance ?: MapProviderRepository(context).also { instance = it }
        }

        private const val KEY_MAP_PROVIDER = "MAP_PROVIDER"
        private const val KEY_MAP_PROVIDER_MAPLIBRE_STYLE = "MAPLIBRE_STYLE"
        private const val KEY_MAP_PROVIDER_MAPBOX_STYLE = "MAPBOX_STYLE"

        private val DEFAULT_VALUE_MAP_PROVIDER = MapLibreMapProvider::class.qualifiedName
        private const val DEFAULT_VALUE_MAP_PROVIDER_MAPLIBRE_STYLE = "https://api.maptiler.com/maps/basic/style.json?key=${BuildConfig.MAPTILER_API_KEY}"
        private const val DEFAULT_VALUE_MAP_PROVIDER_MAPBOX_STYLE = MapboxMapProvider.DEFAULT_STYLE
    }
}
