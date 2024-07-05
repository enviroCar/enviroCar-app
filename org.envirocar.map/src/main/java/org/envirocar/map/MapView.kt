package org.envirocar.map

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import org.envirocar.map.camera.CameraUpdateFactory
import org.envirocar.map.model.Point


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

    private val lock = Any()
    private lateinit var instance: MapProvider
    private lateinit var listener: OnTouchListener

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            if (listener != null) listener.onTouch(this, event)
        }
        return super.dispatchTouchEvent(event)
    }

    override fun setOnTouchListener(listener: OnTouchListener?) {
        this.listener = listener!!
    }

    /**
     * Initializes the instance with the specified [MapProvider].
     *
     * @param mapProvider The [MapProvider] to use for the [MapView] instance.
     * @return The [MapController] associated with the [MapView] instance.
     */
    fun getController(mapProvider: MapProvider): MapController = synchronized(lock) {
        if (!::instance.isInitialized) {
            instance = mapProvider
            addView(
                instance.getView(context).apply {
                    visibility = View.INVISIBLE
                    layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT
                    )
                }
            )
            // Restore default camera state independent of the provider.
            with(instance.getController()) {
                listOf(
                    CameraUpdateFactory.newCameraUpdateBasedOnPoint(Point(CAMERA_POINT_LATITUDE_DEFAULT, CAMERA_POINT_LONGITUDE_DEFAULT)),
                    CameraUpdateFactory.newCameraUpdateBearing(CAMERA_BEARING_DEFAULT),
                    CameraUpdateFactory.newCameraUpdateTilt(CAMERA_TILT_DEFAULT),
                    CameraUpdateFactory.newCameraUpdateZoom(CAMERA_ZOOM_DEFAULT)
                ).forEach {
                    notifyCameraUpdate(it)
                }
            }
        }
        return instance.getController()
    }

    companion object {
        internal const val CAMERA_POINT_LATITUDE_DEFAULT = 52.5163
        internal const val CAMERA_POINT_LONGITUDE_DEFAULT = 13.3777
        internal const val CAMERA_BEARING_DEFAULT = 0.0F
        internal const val CAMERA_TILT_DEFAULT = 0.0F
        internal const val CAMERA_ZOOM_DEFAULT = 15.0F
    }
}
