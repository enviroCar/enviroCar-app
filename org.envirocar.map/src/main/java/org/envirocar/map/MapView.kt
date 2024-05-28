package org.envirocar.map

import android.content.Context
import android.util.AttributeSet
import android.view.View
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

    fun getController(provider: MapProvider<View>): MapController {
        // TODO: Missing implementation.
        throw NotImplementedError()
    }
}
