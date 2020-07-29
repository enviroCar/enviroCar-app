package org.envirocar.app.views.dashboard;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.envirocar.app.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ObdContent2Fragment extends Fragment {

    @BindView(R.id.obdHelpHead)
    TextView obdHelpHead;
    @BindView(R.id.obdHelpIamge)
    ImageView imageView;
    @BindView(R.id.obdHelpText)
    TextView obdHelpText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View contentView = inflater.inflate(R.layout.obd_help_layout, container, false);
        ButterKnife.bind(this, contentView);

        Drawable image = getActivity().getDrawable(R.drawable.plugdesignfinal);
        imageView.setImageDrawable(image);
        obdHelpHead.setText("Plug");
        obdHelpText.setText(getText(R.string.obd_help_content2));
        return contentView;
    }
}
