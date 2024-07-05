package org.envirocar.app.views.tracklist

import android.view.LayoutInflater
import android.view.ViewGroup
import org.envirocar.app.databinding.FragmentTracklistCardlayoutRemoteBinding
import org.envirocar.core.entity.Track

class TrackListRemoteCardAdapter(
    tracks: MutableList<Track>,
    callback: OnTrackInteractionCallback
) : AbstractTrackListCardAdapter<AbstractTrackListCardAdapter.RemoteTrackCardViewHolder>(
    tracks,
    callback
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemoteTrackCardViewHolder {
        val binding = FragmentTracklistCardlayoutRemoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RemoteTrackCardViewHolder(binding)
    }
}
