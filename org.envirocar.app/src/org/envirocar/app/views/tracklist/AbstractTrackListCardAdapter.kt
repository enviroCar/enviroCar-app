package org.envirocar.app.views.tracklist

import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import org.envirocar.app.R
import org.envirocar.app.databinding.FragmentTracklistCardlayoutLocalBinding
import org.envirocar.app.databinding.FragmentTracklistCardlayoutRemoteBinding
import org.envirocar.app.views.trackdetails.TrackMapFactory
import org.envirocar.app.views.utils.MapProviderRepository
import org.envirocar.core.entity.Track
import org.envirocar.core.logging.Logger
import org.envirocar.map.MapController
import org.envirocar.map.MapView
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

abstract class AbstractTrackListCardAdapter<E : AbstractTrackListCardAdapter.TrackCardViewHolder>(
    private val tracks: MutableList<Track>,
    private val callback: OnTrackInteractionCallback
) : RecyclerView.Adapter<E>() {
    private val mapControllers = mutableMapOf<String, MapController>()

    sealed class TrackCardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract val toolbar: Toolbar
        abstract val titleTextView: TextView
        abstract val contentView: View
        abstract val distance: TextView
        abstract val duration: TextView
        abstract val mapView: MapView
        abstract val invisibleMapButton: ImageButton
        abstract val cardViewLayout: LinearLayout
    }

    class LocalTrackCardViewHolder(binding: FragmentTracklistCardlayoutLocalBinding) :
        TrackCardViewHolder(binding.root) {
        override val toolbar = binding.fragmentTracklistCardlayoutToolbar
        override val titleTextView = binding.fragmentTracklistCardlayoutToolbarTitle
        override val contentView = binding.fragmentTracklistCardlayoutContent.root
        override val distance = binding.fragmentTracklistCardlayoutContent.trackDetailsAttributesHeaderDistance
        override val duration = binding.fragmentTracklistCardlayoutContent.trackDetailsAttributesHeaderDuration
        override val mapView = binding.fragmentTracklistCardlayoutContent.fragmentTracklistCardlayoutMap
        override val invisibleMapButton = binding.fragmentTracklistCardlayoutContent.fragmentTracklistCardlayoutInvisibleMapbutton
        override val cardViewLayout = binding.fragmentLayoutCardView

    }

    class RemoteTrackCardViewHolder(binding: FragmentTracklistCardlayoutRemoteBinding) :
        TrackCardViewHolder(binding.root) {
        override val toolbar = binding.fragmentTracklistCardlayoutToolbar
        override val titleTextView = binding.fragmentTracklistCardlayoutToolbarTitle
        override val contentView = binding.fragmentTracklistCardlayoutContent.root
        override val distance = binding.fragmentTracklistCardlayoutContent.trackDetailsAttributesHeaderDistance
        override val duration = binding.fragmentTracklistCardlayoutContent.trackDetailsAttributesHeaderDuration
        override val mapView = binding.fragmentTracklistCardlayoutContent.fragmentTracklistCardlayoutMap
        override val invisibleMapButton = binding.fragmentTracklistCardlayoutContent.fragmentTracklistCardlayoutInvisibleMapbutton
        override val cardViewLayout = binding.fragmentLayoutCardView
        val progressCircle = binding.fragmentTracklistCardlayoutRemoteProgressCircle
        val downloadFab = binding.fragmentTracklistCardlayoutRemoteDownloadFab
        val downloadingNotification = binding.fragmentTracklistCardlayoutRemoteDownloadingNotification
    }

    fun addTrack(track: Track) {
        tracks.add(track)
        // [MapController] will be created when the view is bound.
        notifyItemInserted(tracks.indexOf(track))
    }

    fun removeTrack(track: Track) {
        val index = tracks.indexOf(track)
        tracks.remove(track)
        mapControllers.remove(track.id)
        notifyItemRemoved(index)
    }

    fun clearTracks() {
        tracks.clear()
        mapControllers.clear()
        notifyItemRangeRemoved(0, itemCount)
    }

    override fun getItemCount() = tracks.size

    override fun onBindViewHolder(holder: E, position: Int) {
        LOG.info("onBindViewHolder()")
        bindTrackCardViewHolder(holder, tracks[position])
    }

    fun bindTrackCardViewHolder(holder: E, track: Track) {
        LOG.info("bindTrackCardViewHolder()")

        holder.titleTextView.text = track.name

        holder.distance.text = "..."
        holder.duration.text = "..."

        setupMapView(holder.mapView, track)

        try {
            val distance = String.format("%s km", DECIMAL_FORMAT.format(track.length))
            val duration = DATE_FORMAT.format(Date(track.duration))
            holder.distance.text = distance
            holder.duration.text = duration
        } catch(e: Exception) {
            LOG.warn(e.message, e)
            holder.distance.text = "0 km"
            holder.duration.text = "0:00"
        }

        if (!holder.toolbar.menu.hasVisibleItems()) {
            holder.toolbar.inflateMenu(R.menu.menu_tracklist_cardlayout)
        }
        holder.toolbar.setOnMenuItemClickListener { item ->
            LOG.info("${item.itemId} clicked for track ${track.trackID}.")
            when (item.itemId) {
                R.id.menu_tracklist_cardlayout_item_details -> callback.onTrackDetailsClicked(track, holder.mapView)
                R.id.menu_tracklist_cardlayout_item_delete -> callback.onDeleteTrackClicked(track)
                R.id.menu_tracklist_cardlayout_item_share -> callback.onShareTrackClicked(track)
                R.id.menu_tracklist_cardlayout_item_upload -> callback.onUploadTrackClicked(track)
            }
            false
        }

        holder.invisibleMapButton.setOnClickListener {
            callback.onTrackDetailsClicked(track, holder.mapView)
        }
        holder.invisibleMapButton.setOnLongClickListener {
            callback.onLongPressedTrack(track)
            true
        }
        holder.cardViewLayout.setOnLongClickListener {
            callback.onLongPressedTrack(track)
            true
        }

        when (holder) {
            is LocalTrackCardViewHolder -> {
                /* NO/OP */
            }
            is RemoteTrackCardViewHolder -> {
                holder.toolbar.menu.removeItem(R.id.menu_tracklist_cardlayout_item_upload)
                when (track.downloadState) {
                    Track.DownloadState.REMOTE -> {
                        holder.contentView.visibility = View.GONE
                        holder.progressCircle.visibility = View.VISIBLE
                        holder.downloadFab.show()
                        holder.downloadFab.setOnClickListener {
                            holder.downloadFab.setOnClickListener(null)
                            callback.onDownloadTrackClicked(track, holder)
                        }
                        holder.downloadingNotification.visibility = View.GONE
                    }
                    Track.DownloadState.DOWNLOADING -> {
                        holder.contentView.visibility = View.GONE
                        holder.progressCircle.visibility = View.VISIBLE
                        holder.progressCircle.post { holder.progressCircle.show() }
                        holder.downloadFab.show()
                        holder.downloadFab.setOnClickListener(null)
                        holder.downloadingNotification.visibility = View.VISIBLE
                    }
                    Track.DownloadState.DOWNLOADED -> {
                        holder.contentView.visibility = View.VISIBLE
                        holder.progressCircle.visibility = View.GONE
                        holder.downloadFab.hide()
                        holder.downloadFab.setOnClickListener(null)
                        holder.downloadingNotification.visibility = View.GONE
                    }
                    null -> { /* NO/OP */ }
                }
            }
            else -> error("Unknown [TrackCardViewHolder] instance.")
        }
    }

    private fun setupMapView(view: MapView, track: Track) {
        LOG.info("setupMapView()")
        mapControllers
            .getOrPut(track.id) { view.getController(MapProviderRepository(view.context).value) }
            .run {
                val factory = TrackMapFactory(track)
                factory.cameraUpdateBasedOnBounds?.let { notifyCameraUpdate(it) }
                factory.polyline?.let {
                    clearPolylines()
                    addPolyline(it)
                }
            }
    }

    val Track.id : String get() = when {
        isLocalTrack -> trackID.id.toString()
        isRemoteTrack -> remoteID.toString()
        else -> error("Unknown track type.")
    }

    companion object {
        private val LOG = Logger.getLogger(AbstractTrackListCardAdapter::class.java)
        private val DECIMAL_FORMAT = DecimalFormat("#.##")
        private val DATE_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
    }
}
