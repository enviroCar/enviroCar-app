package org.envirocar.app;

import android.app.Activity;
import android.content.Context;

import org.envirocar.app.injection.Injector;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.Track;

import javax.inject.Inject;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * @author de Wall
 */
public class TrackHandler {
    private static final Logger LOGGER = Logger.getLogger(TrackHandler.class);
    private static final String TRACK_MODE = "trackMode";

    @Inject
    protected Activity mActivity;
    @Inject
    protected DbAdapter mDBAdapter;

    /**
     * Constructor.
     *
     * @param context the context of the activity's scope.
     */
    public TrackHandler(Context context) {
        ((Injector) context).injectObjects(this);
    }

    public void finishTrack() {
        final Track track = mDBAdapter.finishCurrentTrack();
        // TODO check
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (track != null) {
                    if (track.getLastMeasurement() == null) {
                        Crouton.makeText(mActivity, R.string.track_finished_no_measurements, Style.ALERT).show();
                    } else {
                        String text = mActivity.getString(R.string.track_finished).concat(track
                                .getName());
                        Crouton.makeText(mActivity, text, Style.INFO).show();
                    }
                } else {
                    Crouton.makeText(mActivity, R.string.track_finishing_failed, Style.ALERT).show();
                }
            }
        });
    }


}
