package org.envirocar.app.views.onboarding;


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import org.envirocar.app.R;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.core.UserManager;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class OnboardingFragment4 extends Fragment {

    public interface OBButtonInterface {
        void signInButtonPressed();
        void signUpButtonPressed();
        void skipButtonPressed();
    }

    protected OBButtonInterface obButtonInterface;

    @Inject
    protected UserHandler userHandler;

    @BindView(R.id.onboarding_sign_in)
    protected Button signIn;

    @BindView(R.id.onboarding_sign_up)
    protected Button signUp;

    @BindView(R.id.onboarding_skip)
    protected Button skip;

    @BindView(R.id.imageView3)
    protected ImageView background;

    public OnboardingFragment4() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            obButtonInterface = (OBButtonInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OBButtonInterface");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.onboarding_4, container, false);
        ButterKnife.bind(this, view);

        SharedPreferences prefs = getContext().getSharedPreferences("userPrefs", MODE_PRIVATE);
        // If there is no user signed in, show the sign in and sign up buttons
        // Else show a button to continue to the dashboard
        if (!(prefs.contains("username") && prefs.contains("token"))) {
            signUp.setText("Sign Up");
            signUp.setVisibility(View.VISIBLE);
            skip.setVisibility(View.VISIBLE);

            signIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    obButtonInterface.signInButtonPressed();
                }
            });
        } else {
            signIn.setText("Continue to Dashboard");
            signUp.setVisibility(View.INVISIBLE);
            skip.setVisibility(View.INVISIBLE);

            signIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    obButtonInterface.skipButtonPressed();
                }
            });
        }

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                obButtonInterface.signUpButtonPressed();
            }
        });

        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                obButtonInterface.skipButtonPressed();
            }
        });
        return view;
    }

    public void setBackgroundVisibility(float alpha) {
        background.setAlpha(alpha);
    }
}
