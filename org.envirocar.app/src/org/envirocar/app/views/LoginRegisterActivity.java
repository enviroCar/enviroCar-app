/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.cardview.widget.CardView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.text.TextUtils;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.TermsOfUseManager;
import org.envirocar.app.handler.TrackDAOHandler;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserImpl;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.logging.Logger;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class LoginRegisterActivity extends BaseInjectorActivity {
    private static final Logger LOG = Logger.getLogger(LoginRegisterActivity.class);

    @BindView(R.id.activity_login_toolbar)
    protected Toolbar mToolbar;
    @BindView(R.id.activity_login_exp_toolbar)
    protected Toolbar mExpToolbar;
    
    @BindView(R.id.activity_account_login_card_layout)
    protected ConstraintLayout mLoginLayout;
    @BindView(R.id.activity_account_login_card_username_text)
    protected EditText mLoginUsername;
    @BindView(R.id.activity_account_login_card_password_text)
    protected EditText mLoginPassword;

    @BindView(R.id.activity_account_register_card_layout)
    protected ConstraintLayout mRegisterLayout;
    @BindView(R.id.activity_account_register_email_input)
    protected EditText mRegisterEmail;
    @BindView(R.id.activity_account_register_username_input)
    protected EditText mRegisterUsername;
    @BindView(R.id.activity_account_register_password_input)
    protected EditText mRegisterPassword;
    @BindView(R.id.activity_account_register_password2_input)
    protected EditText mRegisterPassword2;

    @Inject
    protected UserHandler mUserManager;
    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected TermsOfUseManager mTermsOfUseManager;
    @Inject
    protected TrackDAOHandler mTrackDAOHandler;

    private final Scheduler.Worker mMainThreadWorker = AndroidSchedulers
            .mainThread().createWorker();
    private final Scheduler.Worker mBackgroundWorker = Schedulers
            .newThread().createWorker();

    private Subscription mLoginSubscription;
    private Subscription mRegisterSubscription;
    private Subscription mTermsOfUseSubscription;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);

        // Inject the Views.
        ButterKnife.bind(this);

        // Initializes the Toolbar.
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Account");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        //expandExpToolbarToHalfScreen();
        if(intent.getStringExtra("from").equalsIgnoreCase("login")){
            slideInLoginCard();
        }else{
            slideInRegisterCard();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)  onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mRegisterLayout != null && mRegisterLayout.getVisibility() == View.VISIBLE) {
            animateViewTransition(mRegisterLayout, R.anim.translate_slide_out_right_card, true);
            animateViewTransition(mLoginLayout, R.anim.translate_slide_in_left_card, false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // If a login process is in progress, then
        // unsubscribe the subscription and finish the thread.
        if (mLoginSubscription != null && !mLoginSubscription.isUnsubscribed()) {
            mLoginSubscription.unsubscribe();
            mLoginSubscription = null;
        }
        // same for the registration process.
        if (mRegisterSubscription != null && mRegisterSubscription.isUnsubscribed()) {
            mRegisterSubscription.unsubscribe();
            mRegisterSubscription = null;
        }
    }

    @OnClick(R.id.activity_account_login_card_login_button)
    protected void onLoginButtonClicked() {
        // Reset errors.
        mLoginUsername.setError(null);
        mLoginPassword.setError(null);

        // Store values at the time of the login attempt.
        String username = mLoginUsername.getText().toString();
        String password = mLoginPassword.getText().toString();

        View focusView = null;

        // Check for a valid password.
        if (password == null || password.isEmpty() || password.equals("")) {
            mLoginPassword.setError(getString(R.string.error_field_required));
            focusView = mLoginPassword;
        }

        // Check if the password is too short.
        else if (password.length() < 6) {
            mLoginPassword.setError(getString(R.string.error_invalid_password));
            focusView = mLoginPassword;
        }

        // Check for a valid username.
        if (username == null || username.isEmpty() || username.equals("")) {
            mLoginUsername.setError(getString(R.string.error_field_required));
            focusView = mLoginUsername;
        }

        if (focusView != null) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        }
        // If the input values are valid, then try to login.
        else {
            // hide the keyboard
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mLoginPassword.getWindowToken(), 0);

            // Create a dialog indicating the log in process.
            final MaterialDialog dialog = new MaterialDialog.Builder(LoginRegisterActivity.this)
                    .title(R.string.activity_login_logging_in_dialog_title)
                    .progress(true, 0)
                    .cancelable(false)
                    .show();

            mLoginSubscription = mBackgroundWorker.schedule(() -> {
                mUserManager.logIn(username, password, new UserHandler.LoginCallback() {
                    @Override
                    public void onSuccess(User user) {
                        dialog.dismiss();
                        // Successfully logged in.
                        mMainThreadWorker.schedule(() -> {
                            // If any error occurs, then set the focus on the error.
                            if (user == null) {
                                if (mLoginUsername.getError() != null)
                                    mLoginUsername.requestFocus();
                                else
                                    mLoginPassword.requestFocus();
                                return;
                            }

                            // First, show a snackbar.
                            Snackbar.make(mExpToolbar,
                                    String.format(getResources().getString(
                                            R.string.welcome_message), user.getUsername()),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                            finish();
                            // Then ask for terms of use acceptance.
                           // askForTermsOfUseAcceptance();
                        });
                    }

                    @Override
                    public void onPasswordIncorrect(String password) {
                        dialog.dismiss();
                        mMainThreadWorker.schedule(() ->
                                mLoginPassword.setError(
                                        getString(R.string.error_incorrect_password)));
                    }

                    @Override
                    public void onUnableToCommunicateServer() {
                        dialog.dismiss();
                        mMainThreadWorker.schedule(() ->
                                mLoginPassword.setError(
                                        getString(R.string.error_host_not_found)));
                    }
                });
            });
        }
    }

    private void askForTermsOfUseAcceptance() {
        // Unsubscribe before issueing a new request.
        if(mTermsOfUseSubscription != null && !mTermsOfUseSubscription.isUnsubscribed())
            mTermsOfUseSubscription.unsubscribe();

        mTermsOfUseSubscription = mTermsOfUseManager.verifyTermsOfUse(LoginRegisterActivity.this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<TermsOfUse>() {
                    @Override
                    public void onStart() {
                        LOG.info("onStart() verifying terms of use");
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("onCompleted() verifying terms of use");
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.warn(e.getMessage(), e);
                    }

                    @Override
                    public void onNext(TermsOfUse termsOfUse) {
                        LOG.info(String.format(
                                "User has accepted the terms of use -> [%s]",
                                termsOfUse.getIssuedDate()));
                    }
                });
    }

    @OnClick(R.id.activity_account_register_button)
    protected void onRegisterAccountButtonClicked() {
        mRegisterUsername.setError(null);
        mRegisterEmail.setError(null);
        mRegisterPassword.setError(null);
        mRegisterPassword2.setError(null);

        // We do not want to have dublicate registration processes.
        if (mRegisterSubscription != null && !mRegisterSubscription.isUnsubscribed())
            return;

        // Get all the values of the edittexts
        final String username = mRegisterUsername.getText().toString();
        final String email = mRegisterEmail.getText().toString();
        final String password = mRegisterPassword.getText().toString();
        final String password2 = mRegisterPassword2.getText().toString();

        View focusView = null;
        // Check for valid passwords.
        if (password == null || password.isEmpty() || password.equals("")) {
            mRegisterPassword.setError(getString(R.string.error_field_required));
            focusView = mRegisterPassword;
        } else if (mRegisterPassword.length() < 6) {
            mRegisterPassword.setError(getString(R.string.error_invalid_password));
            focusView = mRegisterPassword;
        }

        // check if the password confirm is empty
        if (password2 == null || password2.isEmpty() || password2.equals("")) {
            mRegisterPassword2.setError(getString(R.string.error_field_required));
            focusView = mRegisterPassword2;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mRegisterEmail.setError(getString(R.string.error_field_required));
            focusView = mRegisterEmail;
        } else if (!email.matches("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\" +
                ".[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$")) {
            mRegisterEmail.setError(getString(R.string.error_invalid_email));
            focusView = mRegisterEmail;
        }

        // check for valid username
        if (username == null || username.isEmpty() || username.equals("")) {
            mRegisterUsername.setError(getString(R.string.error_field_required));
            focusView = mRegisterUsername;
        } else if (username.length() < 6) {
            mRegisterUsername.setError(getString(R.string.error_invalid_username));
            focusView = mRegisterUsername;
        }

        // check if passwords match
        if (!password.equals(password2)) {
            mRegisterPassword2.setError(getString(R.string.error_passwords_not_matching));
            focusView = mRegisterPassword2;
        }

        // Check if an error occured.
        if (focusView != null) {
            // There was an error; don't attempt register and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            //hide the keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mRegisterPassword.getWindowToken(), 0);

            // TODO
            //            mRegisterStatusMessageView.setText(R.string.register_progress_signing_in);

            // Show a progress spinner, and kick off a pground task to
            // perform the user register attempt.
            final MaterialDialog dialog = new MaterialDialog.Builder(LoginRegisterActivity.this)
                    .title(R.string.register_progress_signing_in)
                    .progress(true, 0)
                    .cancelable(false)
                    .show();

            mBackgroundWorker.schedule(() -> {
                try {
                    User newUser = new UserImpl(username, password);
                    newUser.setMail(email);
                    mDAOProvider.getUserDAO().createUser(newUser);

                    // Successfully created the user
                    mMainThreadWorker.schedule(() -> {
                        // Set the new user as the logged in user.
                        mUserManager.setUser(newUser);

                        // Update the view, i.e., hide the registration card and show the profile
                        // page.

                        // Dismiss the progress dialog.
                        dialog.dismiss();

                        // Show a snackbar containing a welcome message.
                        Snackbar.make(mExpToolbar, String.format(
                                getResources().getString(R.string.welcome_message),
                                username), Snackbar.LENGTH_LONG).show();
                    });
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    finish();
                   // askForTermsOfUseAcceptance();
                } catch (ResourceConflictException e) {
                    LOG.warn(e.getMessage(), e);

                    // Show an error. // TODO show error in a separate error text view.
                    mMainThreadWorker.schedule(() -> {
                        mRegisterUsername.setError(getString(
                                R.string.error_username_already_in_use));
                        mRegisterEmail.setError(getString(
                                R.string.error_email_already_in_use));
                        mRegisterUsername.requestFocus();
                    });

                    // Dismuss the progress dialog.
                    dialog.dismiss();
                } catch (DataUpdateFailureException e) {
                    LOG.warn(e.getMessage(), e);

                    // Show an error.
                    mMainThreadWorker.schedule(() -> {
                        mRegisterUsername.setError(getString(R.string.error_host_not_found));
                        mRegisterUsername.requestFocus();
                    });

                    // Dismuss the progress dialog.
                    dialog.dismiss();
                }
            });
        }
    }

    /**
     * OnClick annotated function that gets invoked when the register button on the login card
     * gets clicked.
     */
    @OnClick(R.id.activity_account_login_card_register_button)
    protected void onRegisterButtonClicked() {
        // When the register button was clicked, then replace the login card with the
        // registration card.
        animateViewTransition(mLoginLayout, R.anim.translate_slide_out_left_card, true);
        animateViewTransition(mRegisterLayout, R.anim.translate_slide_in_right_card, false);
    }

    @OnClick(R.id.activity_account_register_card_signin_button)
    protected void onSignInButtonClicked() {
        // When the register button was clicked, then replace the login card with the
        // registration card.
        animateViewTransition(mRegisterLayout, R.anim.translate_slide_out_right_card, true);
        animateViewTransition(mLoginLayout, R.anim.translate_slide_in_left_card, false);
    }

    /**
     * Applies an animation on the given view.
     *
     * @param view         the view to apply the animation on.
     * @param animResource the animation resource.
     * @param hide         should the view be hid?
     */
    private void animateViewTransition(final View view, int animResource, boolean hide) {
        Animation animation = AnimationUtils.loadAnimation(this, animResource);
        if (hide) {
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    // nothing to do..
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    // nothing to do..
                }
            });
            view.startAnimation(animation);
        } else {
            view.setVisibility(View.VISIBLE);
            view.startAnimation(animation);
        }
    }

    private void slideInLoginCard() {
        Animation animation = AnimationUtils.loadAnimation(this,
                R.anim.translate_in_bottom_login_card);
        mLoginLayout.setVisibility(View.VISIBLE);
        mLoginLayout.startAnimation(animation);
    }

    private void slideInRegisterCard() {
        Animation animation = AnimationUtils.loadAnimation(this,
                R.anim.translate_in_bottom_login_card);
        mRegisterLayout.setVisibility(View.VISIBLE);
        mRegisterLayout.startAnimation(animation);
    }

    /**
     * Expands the expanding toolbar to the a specific amount of the screensize.
     */
    private void expandExpToolbarToHalfScreen() {
        mExpToolbar.setVisibility(View.VISIBLE);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int height = size.y;

        ValueAnimator animator = createSlideAnimator(0, height / 3);
        animator.setDuration(600);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                // nothing to do..
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.start();
    }


    /**
     * Constructs and returns a ValueAnimator that animates between int values.
     *
     * @param start start value
     * @param end   end value
     * @return the ValueAnimator that animates the desired animation.
     */
    private ValueAnimator createSlideAnimator(int start, int end) {

        ValueAnimator animator = ValueAnimator.ofInt(start, end);

        animator.addUpdateListener(animation -> {
            int value = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = mExpToolbar.getLayoutParams();
            layoutParams.height = value;
            mExpToolbar.setLayoutParams(layoutParams);
        });

        return animator;
    }
}
