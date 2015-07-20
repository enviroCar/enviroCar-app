//package org.envirocar.app.view.trackdetails;
//
//import android.content.Intent;
//import android.graphics.Rect;
//import android.os.Bundle;
//import android.support.design.widget.CollapsingToolbarLayout;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.app.ActivityOptionsCompat;
//import android.support.v4.widget.NestedScrollView;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.AbsListView;
//import android.widget.ArrayAdapter;
//import android.widget.LinearLayout;
//import android.widget.ListView;
//import android.widget.TextView;
//
//import com.mapbox.mapboxsdk.geometry.BoundingBox;
//import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
//import com.mapbox.mapboxsdk.views.MapView;
//
//import org.envirocar.app.R;
//import org.envirocar.app.injection.BaseInjectorActivity;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import butterknife.ButterKnife;
//import butterknife.InjectView;
//
///**
// * @author dewall
// */
//public class TrackDetailsActivity extends BaseInjectorActivity {
//
//    private static final String EXTRA_TITLE = "org.envirocar.app.extraTitle";
////    @InjectView(R.id.collapsing_toolbar)
////    private CollapsingToolbarLayout mCollapsingToolbarLayout;
//
//    public static void navigate(AppCompatActivity activity, View transition, int trackID) {
//        Intent intent = new Intent(activity, TrackDetailsActivity.class);
////        intent.putExtra()
//
//        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation
//                (activity, transition, EXTRA_TITLE);
//        ActivityCompat.startActivity(activity, intent, options.toBundle());
//    }
//
//    private int lastTopValueAssigned = 0;
//
////    @InjectView(R.id.activity_track_details_list_header_map)
////    protected MapView mMapView;
////    @InjectView(R.id.activity_track_details_sticky)
////    protected LinearLayout mStickyView;
////    @InjectView(R.id.activity_track_details_list)
////    protected ListView mListView;
////    @InjectView(R.id.activity_track_details_list_toolbar)
////    protected Toolbar mToolbar;
//
//    private View mStickySpacer;
//
////    @Override
////    protected void onCreate(Bundle savedInstanceState) {
////        super.onCreate(savedInstanceState);
////        setContentView(R.layout.activity_track_details_layout3);
////
////        ButterKnife.inject(this);
////
////        // First, inflate the header of the view consisting of the mapview and the sticky view.
////        inflateHeader();
////
////        // Initialize the map view with an OSM tile provider.
////        initMapView();
////
////        setSupportActionBar(mToolbar);
////        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
////
////        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
////            @Override
////            public void onScrollStateChanged(AbsListView view, int scrollState) {
////                // nothing to do
////            }
////
////            @Override
////            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
////                                 int totalItemCount) {
////                if (mListView.getFirstVisiblePosition() == 0) {
////                    View firstChild = mListView.getChildAt(0);
////                    int topY = 0;
////                    if (firstChild != null) {
////                        topY = firstChild.getTop();
////                    }
////
////                    int heroTopY = mStickySpacer.getTop();
////                    mStickyView.setY(Math.max(0, heroTopY + topY));
////
////                    /* Set the image to scroll half of the amount that of ListView */
////                    mMapView.setY(topY * 0.5f);
////                }
////            }
////        });
////
////
////        List<String> modelList = new ArrayList<>();
////        for (int i = 0; i < 22; i++) {
////            modelList.add("item " + i);
////        }
////
////        ArrayAdapter adapter = new ArrayAdapter(this, R.layout.list_row, modelList);
////        mListView.setAdapter(adapter);
////    }
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_track_details_layout2);
//
//
//        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//
//        String itemTitle = "title";
//        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
//        collapsingToolbarLayout.setTitle(itemTitle);
//        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color
//                .transparent));
//
//        TextView title = (TextView) findViewById(R.id.title);
//        title.setText(itemTitle);
//    }
//
//    private void parallaxImage(View view) {
//        Rect rect = new Rect();
//        view.getLocalVisibleRect(rect);
//        if (lastTopValueAssigned != rect.top) {
//            lastTopValueAssigned = rect.top;
//            view.setY((float) (rect.top / 2.0));
//        }
//    }
//
////    private void inflateHeader() {
////        LayoutInflater inflater = getLayoutInflater();
////        View listHeader = inflater.inflate(R.layout.activity_track_details_list_header, null);
////        mStickySpacer = listHeader.findViewById(R.id
////                .activity_track_details_list_header_sticky_placeholder);
////
////        mListView.addHeaderView(listHeader);
////    }
//
////    /**
////     * Initializes the MapView, its base layers and settings.
////     */
////    private void initMapView() {
////        // Set the openstreetmap tile layer as baselayer of the map.
////        WebSourceTileLayer source = new WebSourceTileLayer("openstreetmap", "http://tile" +
////                ".openstreetmap.org/{z}/{x}/{y}.png");
////        source.setName("OpenStreetMap")
////                .setAttribution("OpenStreetMap Contributors")
////                .setMinimumZoomLevel(1)
////                .setMaximumZoomLevel(18);
////        mMapView.setTileSource(source);
////
////        // set the bounding box and min and max zoom level accordingly.
////        BoundingBox box = source.getBoundingBox();
////        mMapView.setScrollableAreaLimit(box);
////        mMapView.setMinZoomLevel(mMapView.getTileProvider().getMinimumZoomLevel());
////        mMapView.setMaxZoomLevel(mMapView.getTileProvider().getMaximumZoomLevel());
////        mMapView.setCenter(mMapView.getTileProvider().getCenterCoordinate());
////        mMapView.setZoom(0);
////    }
//
//    @Override
//    public List<Object> getInjectionModules() {
//        return new ArrayList<>();
//    }
//
//}
