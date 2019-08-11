package org.envirocar.app.views.trackdetails;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.envirocar.app.R;

import java.util.List;

public class TrackStatisticsAdapter extends RecyclerView.Adapter<TrackStatisticsAdapter.ViewHolder> {
    private List<TrackStatisticsDataHolder> trackStatisticsDataHolderList;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView phenomena, trackMax, trackAvg, userAvg, globalAvg;
        public ImageView image;

        public ViewHolder(View view) {
            super(view);
            phenomena =  view.findViewById(R.id.card_title);
            trackAvg = view.findViewById(R.id.average);
            trackMax = view.findViewById(R.id.maximum);
            userAvg = view.findViewById(R.id.userAverage);
            globalAvg = view.findViewById(R.id.globalAverage);
            image = view.findViewById(R.id.imgId);
        }
    }

    public TrackStatisticsAdapter(List<TrackStatisticsDataHolder> trackStatisticsDataHolderList) {
        this.trackStatisticsDataHolderList = trackStatisticsDataHolderList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.track_details_analysis_card, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TrackStatisticsDataHolder temp = trackStatisticsDataHolderList.get(position);
        holder.phenomena.setText(temp.getPhenomena());
        holder.trackAvg.setText(temp.getTrackAvg() + temp.getUnit());
        holder.trackMax.setText(temp.getTrackMax() + temp.getUnit());
        holder.image.setImageResource(temp.getResImg());
        if (temp.getDisplayUserAndGlobalAvg()) {
            holder.globalAvg.setVisibility(View.VISIBLE);
            holder.userAvg.setVisibility(View.VISIBLE);
            holder.globalAvg.setText("The Community Average: " + temp.getGlobalAvg() + temp.getUnit());
            holder.userAvg.setText("Your Average: " + temp.getUserAvg() + temp.getUnit());
        } else {
            holder.globalAvg.setVisibility(View.GONE);
            holder.userAvg.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return trackStatisticsDataHolderList.size();
    }
}
