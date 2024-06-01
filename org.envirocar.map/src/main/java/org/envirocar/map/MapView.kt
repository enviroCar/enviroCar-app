package org.envirocar.map

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * [MapView]
 * ---------
 * The [MapView] may be used to display a map inside the view hierarchy.
 */
class MapView : FrameLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    private lateinit var instance: MapProvider

    /**
     * Initializes the instance with the specified [MapProvider].
     * Returns the associated [MapController] to allow interaction.
     *
     * @param mapProvider The [MapProvider] to use for the [MapView] instance.
     * @return The [MapController] associated with the [MapView] instance.
     */
    fun getController(mapProvider: MapProvider): MapController {
        if (!::instance.isInitialized) {
            instance = mapProvider
            addView(
                mapProvider.getView(context).apply {
                    layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT
                    )
                }
            )
        } else if (instance != mapProvider) {
            error("MapView is already initialized with a different MapProvider.")
        }
        return mapProvider.getController()
    }
}
