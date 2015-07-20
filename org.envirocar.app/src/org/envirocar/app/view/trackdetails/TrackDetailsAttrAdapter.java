package org.envirocar.app.view.trackdetails;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.collect.Maps;

import org.envirocar.app.R;
import org.envirocar.app.storage.Track;

import java.util.List;
import java.util.Map;

/**
 * @author dewall
 */
public class TrackDetailsAttrAdapter extends ArrayAdapter<Track> {


    private final LayoutInflater mInflater;


    public TrackDetailsAttrAdapter(Context mContext, int layoutResourceId, List<Track> data) {
        super(mContext, layoutResourceId, data);

        mInflater = LayoutInflater.from(getContext());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Track track = getItem(position);

        ViewHolder holder;

        if (convertView == null) {
//            convertView = LayoutInflater.from(getContext()).inflate()
            convertView = mInflater.inflate(R.layout.activity_track_details_list_entry, parent,
                    false);
            holder = new ViewHolder();
        }

        ViewGroup viewGroup = (ViewGroup) convertView;

//        viewGroup.addView(inflater.inflate(R.id));

        return convertView;
    }

    private RowViewHolder inflateViewFor(ViewGroup parent) {
        RowViewHolder holder = new RowViewHolder();
        holder.mView = mInflater.inflate(R.layout
                .activity_track_details_list_element, parent, false);
        holder.mImageView = (ImageView) holder.mView.findViewById(R.id
                .activity_track_details_list_element_image);
        holder.mNameView = (TextView) holder.mView.findViewById(R.id
                .activity_track_details_list_element_name);
        holder.mValueView = (TextView) holder.mView.findViewById(R.id
                .activity_track_details_list_element_value);
        return holder;
    }

    private static class RowViewHolder {
        protected View mView;
        protected ImageView mImageView;
        protected TextView mNameView;
        protected TextView mValueView;
    }

    private static class ViewHolder {
        protected Map<String, RowViewHolder> mAttributeViewHolderMap = Maps.newConcurrentMap();
    }

    private enum TrackAttributes {
        CONSUMPTION
    }
}
