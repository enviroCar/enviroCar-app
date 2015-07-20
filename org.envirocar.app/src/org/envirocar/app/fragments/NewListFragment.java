package org.envirocar.app.fragments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.envirocar.app.R;
import org.envirocar.app.activity.ListTracksFragment;
import org.envirocar.app.application.TermsOfUseManager;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.dao.DAOProvider;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.Track;
import org.envirocar.app.view.preferences.TrackRecyclerViewAdapter;
import org.envirocar.app.view.trackdetails.TrackDetailsActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * @author dewall
 */
public class NewListFragment extends BaseInjectorFragment {
    private static final Logger LOGGER = Logger.getLogger(ListTracksFragment.class);

    @Inject
    protected UserManager mUserManager;
    @Inject
    protected DbAdapter mDBAdapter;
    @Inject
    protected TermsOfUseManager mTermsOfUseManager;
    @Inject
    protected DAOProvider mDAOProvider;


    @InjectView(R.id.fragment_tracklist_recycler_view)
    protected RecyclerView mRecyclerView;
    protected RecyclerView.Adapter mRecyclerViewAdapter;
    protected RecyclerView.LayoutManager mRecylcerViewLayoutManager;

    private List<Track> mTrackList = Collections.synchronizedList(new ArrayList<Track>());

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_tracklist, null);

        ButterKnife.inject(this, view);
        mRecyclerView.setHasFixedSize(true);

        mRecylcerViewLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mRecylcerViewLayoutManager);

        mRecyclerViewAdapter = new TrackRecyclerViewAdapter(mTrackList);
        mRecyclerView.setAdapter(mRecyclerViewAdapter);

        mRecyclerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), TrackDetailsActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        new RemoteDownloadTracksTask().execute();
    }



    private final class RemoteDownloadTracksTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Thread.currentThread().setName("TrackList-TrackRetriever" + Thread.currentThread()
                    .getId());


            //fetch db tracks (local+remote)
            List<Track> tracks = mDBAdapter.getAllTracks(true);
            for (Track t : tracks) {
                mTrackList.add(t);
            }

            Collections.sort(mTrackList);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRecyclerViewAdapter.notifyDataSetChanged();
                }
            });


//            if (mUserManager.isLoggedIn()) {
//                setProgressStatusText(R.string.fetching_tracks_remote);
//                downloadTracks();
//            } else {
//                updateStatusLayout();
//            }

            return null;
        }
    }
}
