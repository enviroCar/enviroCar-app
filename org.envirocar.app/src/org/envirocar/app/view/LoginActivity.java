package org.envirocar.app.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.app.TrackHandler;
import org.envirocar.app.application.TermsOfUseManager;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.views.TypefaceEC;
import org.envirocar.core.dao.TrackDAO;
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserImpl;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.BaseInjectorActivity;
import org.envirocar.core.logging.Logger;
import org.envirocar.app.injection.DAOProvider;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public class LoginActivity extends BaseInjectorActivity {
    private static final Logger LOG = Logger.getLogger(LoginActivity.class);

    @InjectView(R.id.activity_login_toolbar)
    protected Toolbar mToolbar;
    @InjectView(R.id.activity_login_exp_toolbar)
    protected Toolbar mExpToolbar;

    @InjectView(R.id.activity_login_exp_toolbar_content)
    protected View mExpToolbarContent;
    @InjectView(R.id.activity_login_account_image)
    protected ImageView mAccountImage;
    @InjectView(R.id.activity_login_account_name)
    protected TextView mAccountName;
    @InjectView(R.id.activity_account_exp_toolbar_tracknumber)
    protected TextView mGlobalTrackNumber;
    @InjectView(R.id.activity_account_exp_toolbar_local_tracknumber)
    protected TextView mLocalTrackNumber;
    @InjectView(R.id.activity_account_exp_toolbar_remote_tracknumber)
    protected TextView mRemoteTrackNumber;

    @InjectView(R.id.activity_login_card)
    protected CardView mLoginCard;
    @InjectView(R.id.activity_account_login_card_username_text)
    protected EditText mLoginUsername;
    @InjectView(R.id.activity_account_login_card_password_text)
    protected EditText mLoginPassword;

    @InjectView(R.id.activity_register_card)
    protected CardView mRegisterCard;
    @InjectView(R.id.activity_account_register_email_input)
    protected EditText mRegisterEmail;
    @InjectView(R.id.activity_account_register_username_input)
    protected EditText mRegisterUsername;
    @InjectView(R.id.activity_account_register_password_input)
    protected EditText mRegisterPassword;
    @InjectView(R.id.activity_account_register_password2_input)
    protected EditText mRegisterPassword2;

    @InjectView(R.id.activity_account_statistics_no_statistics_info)
    protected View mNoStatisticsInfo;
    @InjectView(R.id.activity_account_statistics_listview)
    protected ListView mStatisticsListView;
    @InjectView(R.id.activity_account_statistics_progress)
    protected View mStatisticsProgressView;

    @Inject
    protected UserManager mUserManager;
    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected TermsOfUseManager mTermsOfUseManager;
    @Inject
    protected TrackHandler mTrackHandler;


    private final Scheduler.Worker mMainThreadWorker = AndroidSchedulers
            .mainThread().createWorker();
    private final Scheduler.Worker mBackgroundWorker = Schedulers
            .newThread().createWorker();

    private Subscription mLoginSubscription;
    private Subscription mRegisterSubscription;
    private Subscription mStatisticsDownloadSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        // Inject the Views.
        ButterKnife.inject(this);

        TypefaceEC.applyCustomFont((ViewGroup) mAccountName.getParent(), TypefaceEC.Raleway(this));

        // Initializes the Toolbar.
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Account");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mLoginCard.setVisibility(View.GONE);
        mStatisticsListView.setVisibility(View.GONE);
        mExpToolbarContent.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (mRegisterCard.getVisibility() == View.VISIBLE) {
                animateViewTransition(mRegisterCard, R.anim.translate_slide_out_right_card, true);
                animateViewTransition(mLoginCard, R.anim.translate_slide_in_left_card, false);
            } else {
                finish();
            }
        } else if (item.getTitle().equals("Logout")) {
            logOut();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Logout");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (mRegisterCard.getVisibility() == View.VISIBLE) {
            animateViewTransition(mRegisterCard, R.anim.translate_slide_out_right_card, true);
            animateViewTransition(mLoginCard, R.anim.translate_slide_in_left_card, false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (mExpToolbar.getVisibility() == View.GONE)
            mMainThreadWorker.schedule(
                    () -> {
                        if (!mUserManager.isLoggedIn()) {
                            slideInLoginCard();
                        }
                        expandExpToolbarToHalfScreen();
                    }, 300, TimeUnit.MILLISECONDS);
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
            final MaterialDialog dialog = new MaterialDialog.Builder(LoginActivity.this)
                    .title("Logging in...")
                    .progress(true, 0)
                    .cancelable(false)
                    .show();

            mLoginSubscription = mBackgroundWorker.schedule(() -> {
                mUserManager.logIn(username, password, new UserManager.LoginCallback() {
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

                            // TODO: update the UI
                            updateView(true);

                            // Then ask for terms of use acceptance.
                            mTermsOfUseManager.askForTermsOfUseAcceptance(user,
                                    LoginActivity.this, null);
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

            // Show a progress spinner, and kick off a background task to
            // perform the user register attempt.
            final MaterialDialog dialog = new MaterialDialog.Builder(LoginActivity.this)
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
                        mTermsOfUseManager.askForTermsOfUseAcceptance(
                                newUser, LoginActivity.this, null);

                        // Update the view, i.e., hide the registration card and show the profile
                        // page.
                        updateView(true);

                        // Dismiss the progress dialog.
                        dialog.dismiss();

                        // Show a snackbar containing a welcome message.
                        Snackbar.make(mExpToolbar, String.format(
                                getResources().getString(R.string.welcome_message),
                                username), Snackbar.LENGTH_LONG).show();
                    });
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

    private void logOut() {
        if (mUserManager.isLoggedIn()) {
            User user = mUserManager.getUser();
            // Log out the user
            mUserManager.logOut();

            // Show a snackbar that indicates the finished logout
            Snackbar.make(mExpToolbarContent,
                    String.format(getString(R.string.goodbye_message), user.getUsername()),
                    Snackbar.LENGTH_LONG).show();

            // hide the content of the list view and finally delete the adapter.
            animateHideView(mStatisticsListView, R.anim.fade_out,
                    () -> mStatisticsListView.setAdapter(null));

            // hide the content of the exp toolbar and finally slide in the login card.
            animateHideView(mExpToolbarContent, R.anim.fade_out, () -> slideInLoginCard());

            // hide the no statistics info if it is visible.
            if (mNoStatisticsInfo.getVisibility() == View.VISIBLE) {
                animateHideView(mNoStatisticsInfo, R.anim.fade_out, null);
            }

            // hide the no statistics info if it is visible.
            if (mStatisticsProgressView.getVisibility() == View.VISIBLE) {
                animateHideView(mStatisticsProgressView, R.anim.fade_out, null);
                if (mStatisticsDownloadSubscription != null &&
                        !mStatisticsDownloadSubscription.isUnsubscribed()) {
                    mStatisticsDownloadSubscription.unsubscribe();
                    mStatisticsDownloadSubscription = null;
                }
            }

            mTrackHandler.deleteAllRemoteTracksLocally();
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
        animateViewTransition(mLoginCard, R.anim.translate_slide_out_left_card, true);
        animateViewTransition(mRegisterCard, R.anim.translate_slide_in_right_card, false);
    }

    private void updateView(boolean isLoggedIn) {
        if (isLoggedIn) {
            // First, show all user informations.
            final User user = mUserManager.getUser();
            mAccountName.setText(user.getUsername());

            // Animate the fade in progress of the Exp Toolbar content.
            if (mExpToolbarContent.getVisibility() != View.VISIBLE)
                animateViewTransition(mExpToolbarContent, R.anim
                        .fade_in, false);

            // If the login card is visible, then slide it out.
            if (mLoginCard.getVisibility() == View.VISIBLE) {
                slideOutLoginCard();
            }
            // If the register card is visible, then slide it out.
            if (mRegisterCard.getVisibility() == View.VISIBLE) {
                slideOutRegisterCard();
            }
            // If the statistics progess view is not visible, then fade it in.
            if (mStatisticsProgressView.getVisibility() != View.VISIBLE) {
                animateViewTransition(mStatisticsProgressView, R.anim.fade_in, false);
            }

            // Update the Gravatar image.
            mUserManager.getGravatarBitmapObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bitmap -> {
                        if (mAccountImage != null && mAccountImage.getVisibility() == View.VISIBLE)
                            mAccountImage.setImageBitmap(bitmap);
                    });

            // Update the new values of the exp toolbar content.
            mBackgroundWorker.schedule(() -> {
                try {
                    final TrackDAO trackDAO = mDAOProvider.getTrackDAO();
                    final int totalTrackCount = trackDAO.getTotalTrackCount();
                    final int userTrackCount = trackDAO.getUserTrackCount();

                    String.format("%s (%s)", userTrackCount, totalTrackCount);
                    mMainThreadWorker.schedule(() -> {
                        mGlobalTrackNumber.setText(Integer.toString(totalTrackCount));
                        mRemoteTrackNumber.setText(Integer.toString(userTrackCount));
                    });
                } catch (NotConnectedException e) {
                    e.printStackTrace();
                } catch (DataRetrievalFailureException e) {
                    e.printStackTrace();
                } catch (UnauthorizedException e) {
                    e.printStackTrace();
                }
            });

            Observable.just(true)
                    .map(aBoolean -> {
                        try {
                            return mDAOProvider
                                    .getUserStatisticsDAO()
                                    .getUserStatistics(user)
                                    .getStatistics();
                        } catch (UnauthorizedException e) {
                            LOG.warn("The user is unauthorized to access this endpoint.", e);
                        } catch (DataRetrievalFailureException e) {
                            LOG.warn("Error while trying to retrive user statistics.", e);
                            mMainThreadWorker.schedule(() ->
                                    animateHideView(mStatisticsProgressView, R.anim.fade_out,
                                            () -> animateViewTransition(mNoStatisticsInfo, R
                                                    .anim.fade_in, false)));
                        }
                        return null;
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(statistics -> {
                        if (statistics == null || statistics.isEmpty()) {
                            animateHideView(mStatisticsProgressView, R.anim.fade_out,
                                    () -> animateViewTransition(mNoStatisticsInfo,
                                            R.anim.fade_in, false));
                        } else {
                            mStatisticsListView.setAdapter(new UserStatisticsAdapter
                                    (LoginActivity.this,
                                            new ArrayList<>(statistics.values())));
                            animateHideView(mStatisticsProgressView, R.anim.fade_out,
                                    () -> animateViewTransition(mStatisticsListView,
                                            R.anim.fade_in, false));
                        }
                    });
        }
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

    private void animateHideView(View view, int animResource, Action0 action) {
        Animation animation = AnimationUtils.loadAnimation(this, animResource);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // nothing to do..
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
                if (action != null) {
                    action.call();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // nothing to do..
            }
        });
        view.startAnimation(animation);
    }

    private void slideInLoginCard() {
        Animation animation = AnimationUtils.loadAnimation(this,
                R.anim.translate_in_bottom_login_card);
        mLoginCard.setVisibility(View.VISIBLE);
        mLoginCard.startAnimation(animation);
    }


    /**
     * Animtes the hiding process by sliding the login card out at the bottom.
     */
    private void slideOutLoginCard() {
        animateViewTransition(mLoginCard, R.anim.translate_out_bottom_card, true);
    }

    /**
     * Animtes the hiding process by sliding the register card out at the bottom.
     */
    private void slideOutRegisterCard() {
        animateViewTransition(mRegisterCard, R.anim.translate_out_bottom_card, true);
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

        ValueAnimator animator = createSlideAnimator(0, (int) height / 3);
        animator.setDuration(600);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                // nothing to do..
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                updateView(mUserManager.isLoggedIn());
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

    //    private void collapseExpToolbar() {
    //        int finalHeight = mExpToolbar.getHeight();
    //
    //        // Create a new slide animator
    //        ValueAnimator animator = createSlideAnimator(finalHeight, 0);
    //        animator.addListener(new Animator.AnimatorListener() {
    //            @Override
    //            public void onAnimationStart(Animator animation) {
    //                // nothing to do..
    //            }
    //
    //            @Override
    //            public void onAnimationEnd(Animator animation) {
    //                mExpToolbar.setVisibility(View.GONE);
    //            }
    //
    //            @Override
    //            public void onAnimationCancel(Animator animation) {
    //                // nothing to do..
    //            }
    //
    //            @Override
    //            public void onAnimationRepeat(Animator animation) {
    //                // nothing to do..
    //            }
    //        });
    //        animator.start();
    //    }

    //    private void expandExpToolbar() {
    //        mExpToolbar.setVisibility(View.VISIBLE);
    //
    //        final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec
    // .UNSPECIFIED);
    //        final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec
    // .UNSPECIFIED);
    //
    //        mExpToolbar.measure(widthSpec, heightSpec);
    //
    //        ValueAnimator animator = createSlideAnimator(0, mExpToolbar.getMeasuredHeight());
    //        animator.start();
    //    }
}
