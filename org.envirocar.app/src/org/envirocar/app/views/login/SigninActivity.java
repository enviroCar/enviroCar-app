package org.envirocar.app.views.login;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.core.app.ActivityOptionsCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;

import org.envirocar.app.R;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.app.handler.agreement.AgreementManager;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.core.entity.User;
import org.envirocar.core.logging.Logger;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class SigninActivity extends BaseInjectorActivity {
    private static final Logger LOG = Logger.getLogger(SigninActivity.class);

    // Inject Dependencies
    @Inject
    protected UserHandler userHandler;
    @Inject
    protected DAOProvider daoProvider;
    @Inject
    protected AgreementManager agreementManager;

    // Injected Views
    @BindView(R.id.activity_signin_username_input)
    protected EditText usernameEditText;
    @BindView(R.id.activity_login_password_input)
    protected EditText passwordEditText;
    @BindView(R.id.activity_login_logo)
    protected ImageView logoImageView;

    //
    private final Scheduler.Worker mainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    private final Scheduler.Worker backgroundWorker = Schedulers.newThread().createWorker();
    private Subscription loginSubscription;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        // inject the views
        ButterKnife.bind(this);
    }

    @OnClick(R.id.activity_signin_register_button)
    protected void onSwitchToRegisterClicked() {
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this, logoImageView, "imageMain");
        Intent in = new Intent(this, SignupActivity.class);
        startActivity(in, options.toBundle());
    }

    @OnClick(R.id.activity_signin_login_button)
    protected void onLoginClicked() {
        View focusView = null;

        // Reset errors
        this.usernameEditText.setError(null);
        this.passwordEditText.setError(null);

        // Store values at the time of the login attempt
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        // check for valid password
        if (password == null || password.isEmpty() || password.equals("")) {
            this.passwordEditText.setError(getString(R.string.error_field_required));
            focusView = this.passwordEditText;
        }

        // check for valid username
        if (username == null || username.isEmpty() || username.equals("")) {
            this.usernameEditText.setError(getString(R.string.error_field_required));
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
        final MaterialDialog dialog = new MaterialDialog.Builder(SigninActivity.this)
                .title(R.string.activity_login_logging_in_dialog_title)
                .progress(true, 0)
                .cancelable(false)
                .show();

        this.loginSubscription = backgroundWorker.schedule(() -> {
            this.userHandler.logIn(username, password, new UserHandler.LoginCallback() {
                @Override
                public void onSuccess(User user) {
                    dialog.dismiss();
                    // Successfully logged in.
                    mainThreadWorker.schedule(() -> {
                        // If any error occurs, then set the focus on the error.
                        if (user == null) {
                            if (usernameEditText.getError() != null)
                                usernameEditText.requestFocus();
                            else
                                passwordEditText.requestFocus();
                            return;
                        }

                        // First, show a snackbar.
                        Snackbar.make(logoImageView,
                                String.format(getResources().getString(
                                        R.string.welcome_message), user.getUsername()),
                                Snackbar.LENGTH_LONG)
                                .show();

                        finish();
                    });
                }

                @Override
                public void onPasswordIncorrect(String password) {
                    dialog.dismiss();
                    mainThreadWorker.schedule(() ->
                            passwordEditText.setError(
                                    getString(R.string.error_incorrect_password)));
                }

                @Override
                public void onMailNotConfirmed() {
                    dialog.dismiss();
                    mainThreadWorker.schedule(() ->
                            new MaterialDialog.Builder(SigninActivity.this)
                                    .cancelable(true)
                                    .positiveText(R.string.ok)
                                    .title(R.string.login_mail_not_confirmed_dialog_title)
                                    .content(R.string.login_mail_not_confirmed_dialog_content)
                                    .build().show());
                }

                @Override
                public void onUnableToCommunicateServer() {
                    dialog.dismiss();
                    mainThreadWorker.schedule(() ->
                            passwordEditText.setError(
                                    getString(R.string.error_host_not_found)));
                }
            });
        });
    }
}
