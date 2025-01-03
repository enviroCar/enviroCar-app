package org.envirocar.app.views.tracklist

import android.view.View
import org.envirocar.core.entity.Track

interface OnTrackInteractionCallback {
    fun onTrackDetailsClicked(track: Track, transitionView: View)
    fun onDeleteTrackClicked(track: Track)
    fun onUploadTrackClicked(track: Track)
    fun onShareTrackClicked(track: Track)
    fun onDownloadTrackClicked(track: Track, holder: AbstractTrackListCardAdapter.TrackCardViewHolder)
    fun onLongPressedTrack(track: Track)
}
