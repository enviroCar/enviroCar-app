package org.envirocar.app.views.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;

import org.envirocar.app.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class OnBoardingFragmentThird extends Fragment {
    @BindView(R.id.onBoardingHead)
    TextView head;
    @BindView(R.id.onBoardingText)
    TextView onBoardingText;
    @BindView(R.id.onBoardingDesc)
    TextView desc;

    @BindView(R.id.onBoardingAnim)
    LottieAnimationView lottieAnimationView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_on_boarding_layout,container,false);
        ButterKnife.bind(this,view);

        head.setText("Analyse");
        onBoardingText.setText("your track data");
        desc.setText("and find the Driving efficiency, Car running cost and many more");

        lottieAnimationView.setAnimation(R.raw.analysis);

        return view;
    }
}
