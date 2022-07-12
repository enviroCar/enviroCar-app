/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
package org.envirocar.app.views.login;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityOptionsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.databinding.ActivitySigninBinding;
import org.envirocar.app.exception.LoginException;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.agreement.AgreementManager;
import org.envirocar.app.handler.preferences.UserPreferenceHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.views.BaseMainActivity;
import org.envirocar.app.views.obdselection.OBDSelectionActivity;
import org.envirocar.app.views.utils.DialogUtils;
import org.envirocar.core.utils.rx.Optional;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserImpl;

import javax.inject.Inject;
import java.util.function.Consumer;


import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.Observable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;


/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class SigninActivity extends BaseInjectorActivity {
    private static final Logger LOG = Logger.getLogger(SigninActivity.class);
    private ImageView signInBackground;
    private TextView registerButton;
    private AppCompatEditText loginPasswordInput;


    public static void startActivity(Context context) {
        Intent intent = new Intent(context, SigninActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

    // Inject Dependencies
    @Inject
    protected UserPreferenceHandler userHandler;
    @Inject
    protected DAOProvider daoProvider;
    @Inject
    protected AgreementManager agreementManager;

    // Injected Views

    protected EditText usernameEditText;

    protected EditText passwordEditText;

    protected ImageView logoImageView;
    protected CardView activitySigninLoginButton;
    private Disposable loginSubscription;
    private static Drawable errorPassword;
    private static Drawable errorUsername;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    private ActivitySigninBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySigninBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        usernameEditText = binding.activitySigninUsernameInput;
        passwordEditText = binding.activityLoginPasswordInput;
        logoImageView = binding.activityLoginLogo;
        activitySigninLoginButton = binding.activitySigninLoginButton;
        activitySigninLoginButton.setOnClickListener(this::onLoginClicked);
        signInBackground = binding.signinBackground;
        signInBackground.setOnClickListener(this::closeKeyboard);

        registerButton = binding.activitySigninRegisterButton;
        registerButton.setOnClickListener(this::onSwitchToRegisterClicked);

        loginPasswordInput = binding.activityLoginPasswordInput;
        loginPasswordInput.setOnEditorActionListener(this::implicitSubmit);

        getWindow().setNavigationBarColor(getResources().getColor(R.color.cario_color_primary_dark));

        // inject the views

        errorPassword = getResources().getDrawable(R.drawable.ic_error_red_24dp);
        errorPassword.setBounds(-70,0,0, errorPassword.getIntrinsicHeight());

        errorUsername = getResources().getDrawable(R.drawable.ic_error_red_24dp);
        errorUsername.setBounds(0, 0, errorUsername.getIntrinsicWidth(), errorUsername.getIntrinsicHeight());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (loginSubscription != null && !loginSubscription.isDisposed()) {
            loginSubscription.dispose();
        }
    }


    protected void closeKeyboard(View view){

        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    protected void onSwitchToRegisterClicked(View view) {
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this, logoImageView, "imageMain");

        // start Signup activity.
        SignupActivity.startActivity(this);
    }

    protected boolean implicitSubmit(TextView var1, int var2, KeyEvent var3) {
        View logInSubmit;
        logInSubmit = findViewById(R.id.activity_signin_login_button);
        logInSubmit.performClick();
        return true;
    }


    protected void onLoginClicked(View view) {
        LOG.info("Clicked on the login button");
        View focusView = null;

        // Reset errors
        this.usernameEditText.setError(null);
        this.passwordEditText.setError(null);

        // Store values at the time of the login attempt
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        // check for valid password
        if (password == null || password.isEmpty() || password.equals("")) {
            this.passwordEditText.setError(getString(R.string.error_field_required), errorPassword);
            focusView = this.passwordEditText;
        }

        // check for valid username
        if (username == null || username.isEmpty() || username.equals("")) {
            this.usernameEditText.setError(getString(R.string.error_field_required), errorUsername);
            focusView = this.usernameEditText;
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
            imm.hideSoftInputFromWindow(this.passwordEditText.getWindowToken(), 0);

            // Try to login
            this.login(username, password);
        }
    }

    private void login(String username, String password) {
        // Create a dialog indicating the log in process.
        this.loginSubscription = userHandler.logIn(username, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableCompletableObserver() {
                    private AlertDialog dialog;

                    @Override
                    protected void onStart() {
                        if(checkNetworkConnection()) {
                            dialog = DialogUtils.createProgressBarDialogBuilder(SigninActivity.this,
                                            R.string.activity_login_logging_in_dialog_title,
                                            R.drawable.ic_baseline_login_24,
                                            (String) null)
                                    .setCancelable(false)
                                    .show();
                        }
                    }

                    @Override
                    public void onComplete() {
                        if(checkNetworkConnection())
                            dialog.dismiss();
                        Intent intent = new Intent(getBaseContext(), BaseMainActivity.class);
                        startActivity(intent);
                    }

                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onError(Throwable e) {
                        if(checkNetworkConnection())
                            dialog.dismiss();
                        if (e instanceof LoginException) {
                            switch (((LoginException) e).getType()) {
                                case USERNAME_OR_PASSWORD_INCORRECT:
                                    usernameEditText.setError(getString(R.string.error_invalid_credentials), errorUsername);
                                    passwordEditText.setError(getString(R.string.error_invalid_credentials), errorPassword);
                                    break;
                                case MAIL_NOT_CONFIREMED:
                                    // show alert dialog
                                    new MaterialAlertDialogBuilder(SigninActivity.this,R.style.MaterialDialog)
                                            .setTitle(R.string.login_mail_not_confirmed_dialog_title)
                                            .setMessage(R.string.login_mail_not_confirmed_dialog_content)
                                            .setIcon(R.drawable.ic_baseline_email_24)
                                            .setCancelable(true)
                                            .setPositiveButton(R.string.ok,null)
                                            .show();
                                    break;
                                case UNABLE_TO_COMMUNICATE_WITH_SERVER:
                                    passwordEditText.setError(getString(R.string.error_host_not_found), errorPassword);
                                    break;
                                case TERMS_NOT_ACCEPTED:
                                    LOG.info("User has not accepted the latest terms of use");
                                    agreementManager.initializeTermsOfUseAcceptanceWorkflow(username, password, SigninActivity.this, BaseMainActivity.class, tou -> {
                                        if (tou.isEmpty()) {
                                            passwordEditText.setError(getString(R.string.error_terms_not_acceppted), errorPassword);
                                            userHandler.logOut();
                                        } else {
                                            if (userHandler.isLoggedIn()) {
                                                Intent intent = new Intent(getBaseContext(), BaseMainActivity.class);
                                                startActivity(intent);
                                            }
                                        }
                                    });

                                    break;
                                default:
                                    passwordEditText.setError(getString(R.string.logbook_invalid_input), errorPassword);
                                    break;
                            }
                        } else if (!checkNetworkConnection()) {
                            Snackbar.make(findViewById(R.id.activity_signin_login_button),
                                    getString(R.string.error_not_connected_to_network), Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private boolean checkNetworkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        return false;
    }
}