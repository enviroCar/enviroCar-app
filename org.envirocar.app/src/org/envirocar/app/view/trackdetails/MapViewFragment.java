package org.envirocar.app.view.trackdetails;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;

import org.envirocar.app.R;
import org.envirocar.core.entity.Track;

/**
 * A placeholder fragment containing a simple view.
 */
public class MapViewFragment extends DialogFragment {
    Track track;
    Activity activity;
    WebSourceTileLayer source;
    protected MapView mapView;

    public void setTrack(Track track) {

        this.track = track;
    }

    public MapViewFragment() {
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setStyle(2,STYLE_NO_FRAME);
        View rootView = inflater.inflate(R.layout.fragment_map_view2,container,false);
        setRetainInstance(true);
        mapView= (MapView) rootView.findViewById(R.id.fragment_track_details_header_map);


        TrackDetailsActivity trackDetailsActivity=new TrackDetailsActivity();
        try{
            track = ((TrackInterface) activity).getTrackLocal();
            source=((TrackInterface) activity).getSourceLocal();

        }catch (ClassCastException cce){
            Log.d("error",cce.getMessage());


        }
        trackDetailsActivity.initMapView(mapView,source);
        trackDetailsActivity.initTrackPath(track, mapView);
        return rootView;


    }
    public static MapViewFragment newInstance(Track track) {
        MapViewFragment d = new MapViewFragment();
        d.setTrack(track);

        return d;
    }
}
