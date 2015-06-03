/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */

package org.envirocar.app.activity;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.envirocar.app.BaseInjectorFragment;
import org.envirocar.app.R;
import org.envirocar.app.application.TermsOfUseManager;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.dao.DAOProvider;
import org.envirocar.app.dao.exception.UnauthorizedException;
import org.envirocar.app.dao.exception.UserRetrievalException;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.User;
import org.envirocar.app.views.TypefaceEC;

import javax.inject.Inject;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginFragment extends BaseInjectorFragment {

    private static final Logger logger = Logger.getLogger(LoginFragment.class);
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // Values for email and password at the time of the login attempt.
    private String mUsername;
    private String mPassword;

    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;
    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;

    // Injected Variables
    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected UserManager mUserManager;
    @Inject
    protected TermsOfUseManager mTermsOfUseManager;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.login_layout, null);

        mUsernameView = (EditText) view.findViewById(R.id.login_username);

        mPasswordView = (EditText) view.findViewById(R.id.login_password);
        mPasswordView
                .setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int id,
                                                  KeyEvent keyEvent) {
                        if (id == R.id.login || id == EditorInfo.IME_NULL) {
                            attemptLogin();
                            return true;
                        }
                        return false;
                    }
                });
        mLoginFormView = view.findViewById(R.id.login_form);
        mLoginStatusView = view.findViewById(R.id.login_status);
        mLoginStatusMessageView = (TextView) view
                .findViewById(R.id.login_status_message);

        view.findViewById(R.id.sign_in_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        attemptLogin();
                    }
                });

        view.findViewById(R.id.not_yet_registered_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openRegisterFragment();
                    }
                });
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TypefaceEC.applyCustomFont((ViewGroup) view,
                TypefaceEC.Raleway(getActivity()));
        mUsernameView.requestFocus();
    }

    @Override
    public boolean onOptionsItemSelected(
            MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_register:
                openRegisterFragment();
                return true;

        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu,
                                    MenuInflater inflater) {
        inflater.inflate(R.menu.menu_login,
                menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        if (mAuthTask != null) {
            return;
        }

        // Store values at the time of the login attempt.
        mUsername = mUsernameView.getText().toString();
        mPassword = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (mPassword.length() < 6) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(mUsername)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // hide the keyboard
            InputMethodManager imm = (InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mPasswordView.getWindowToken(), 0);

            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);
            mAuthTask = new UserLoginTask();
            mAuthTask.execute();
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(
                    android.R.integer.config_shortAnimTime);

            mLoginStatusView.setVisibility(View.VISIBLE);
            mLoginStatusView.animate().setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginStatusView.setVisibility(show ? View.VISIBLE
                                    : View.GONE);
                        }
                    });

            mLoginFormView.setVisibility(View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginFormView.setVisibility(show ? View.GONE
                                    : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, User> {
        @Override
        protected User doInBackground(Void... params) {
            return authenticateHttp(mUsername, mPassword);
        }

        @Override
        protected void onPostExecute(final User newUser) {
            mAuthTask = null;
            showProgress(false);

            if (newUser != null) {
                mUserManager.setUser(newUser);
                Crouton.makeText(
                        getActivity(),
                        getResources().getString(R.string.welcome_message)
                                + " " + mUsername, Style.CONFIRM).show();

                mTermsOfUseManager.askForTermsOfUseAcceptance(newUser, getActivity(), null);

                getActivity().getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                DashboardFragment dashboardFragment = new DashboardFragment();
                getActivity().getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, dashboardFragment)
                        .commit();

            } else {
                if (mUsernameView.getError() != null) {
                    mUsernameView.requestFocus();
                } else {
                    mPasswordView.requestFocus();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    /**
     * Method used for authentication (e.g. at loginscreen to verify user
     * credentials
     */
    private User authenticateHttp(String user, String token) {
        User currentUser = mUserManager.getUser();

        if (currentUser == null || currentUser.getToken() == null) {
            User candidateUser = new User(user, token);
            mUserManager.setUser(candidateUser);
        }

        try {
            User result = mDAOProvider.getUserDAO().getUser(user);
            result.setToken(token);
            return result;
        } catch (UnauthorizedException e1) {
            logger.warn(e1.getMessage(), e1);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                }
            });
        } catch (UserRetrievalException e1) {
            logger.warn(e1.getMessage(), e1);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPasswordView.setError(getString(R.string.error_host_not_found));
                }
            });
        }

        mUserManager.logOut();

        return null;
    }

    private void openRegisterFragment() {
        RegisterFragment registerFragment = new RegisterFragment();
        getActivity().getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, registerFragment, "REGISTER")
                .addToBackStack(null).commit();
    }

}
