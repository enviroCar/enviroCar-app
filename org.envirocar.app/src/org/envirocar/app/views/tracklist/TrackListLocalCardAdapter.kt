package org.envirocar.app.views.tracklist

import android.view.LayoutInflater
import android.view.ViewGroup
import org.envirocar.app.databinding.FragmentTracklistCardlayoutLocalBinding
import org.envirocar.core.entity.Track

class TrackListLocalCardAdapter(
    tracks: MutableList<Track>,
    callback: OnTrackInteractionCallback
) : AbstractTrackListCardAdapter<AbstractTrackListCardAdapter.LocalTrackCardViewHolder>(
    tracks,
    callback
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocalTrackCardViewHolder {
        val binding = FragmentTracklistCardlayoutLocalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LocalTrackCardViewHolder(binding)
    }
}
