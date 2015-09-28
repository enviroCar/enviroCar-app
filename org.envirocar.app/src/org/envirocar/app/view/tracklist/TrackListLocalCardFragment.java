package org.envirocar.app.view.tracklist;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorFragment;

/**
 * @author dewall
 */
public class TrackListLocalCardFragment extends BaseInjectorFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        Toast.makeText(getActivity(), "YEA", Toast.LENGTH_SHORT).show();
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
