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
package org.envirocar.app.views.login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Pair;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.agreement.AgreementManager;
import org.envirocar.app.handler.preferences.UserPreferenceHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserImpl;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.logging.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * @author dewall
 */
public class SignupActivity extends BaseInjectorActivity {
    private static final Logger LOG = Logger.getLogger(SignupActivity.class);

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, SignupActivity.class);
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
    @BindView(R.id.activity_signup_username_input)
    protected EditText usernameEditText;
    @BindView(R.id.activity_signup_email_input)
    protected EditText emailEditText;
    @BindView(R.id.activity_signup_password_1)
    protected EditText password1EditText;
    @BindView(R.id.activity_signup_password_2)
    protected EditText password2EditText;
    @BindView(R.id.activity_signup_tou_checkbox)
    protected CheckBox touCheckbox;
    @BindView(R.id.activity_signup_tou_text)
    protected TextView touText;

    private final Scheduler.Worker mainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    private final Scheduler.Worker backgroundWorker = Schedulers.newThread().createWorker();
    private Disposable registerSubscription;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        getWindow().setNavigationBarColor(getResources().getColor(R.color.cario_color_primary_dark));

        // inject the views
        ButterKnife.bind(this);

        // make terms of use and privacy statement clickable
        this.makeClickableTextLinks();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registerSubscription != null && !registerSubscription.isDisposed()) {
            registerSubscription.dispose();
        }
    }

    @OnClick(R.id.activity_signup_login_button)
    protected void onSwitchToRegister() {
        Intent intent = new Intent(this, SigninActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.activity_signup_register_button)
    protected void onRegisterAccountButtonClicked() {
        // reset errors
        this.usernameEditText.setError(null);
        this.emailEditText.setError(null);
        this.password1EditText.setError(null);
        this.password2EditText.setError(null);
        this.touCheckbox.setError(null);

        // We do not want to have dublicate registration processes.
        if (this.registerSubscription != null && !this.registerSubscription.isDisposed()) {
            return;
        }

        // Get all the values of the edittexts
        final String username = usernameEditText.getText().toString().trim();
        final String email = emailEditText.getText().toString().trim();
        final String password = password1EditText.getText().toString();
        final String password2 = password2EditText.getText().toString();

        View focusView = null;

        // check for valid passwords
        if (password == null || password.isEmpty() || password.equals("")) {
            password1EditText.setError(getString(R.string.error_field_required));
            focusView = this.password1EditText;
        } else if (password.length() < 6) {
            password1EditText.setError(getString(R.string.error_invalid_password));
            focusView = this.password1EditText;
        } else if (isStrongPassword(password)==false)  {
            password1EditText.setError(getString(R.string.error_field_weak_password));
            focusView=this.password1EditText;
        }

            // check if the password confirm is empty
            if (password2 == null || password2.isEmpty() || password2.equals("")) {
                password2EditText.setError(getString(R.string.error_field_required));
                focusView = password2EditText;
            }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError(getString(R.string.error_field_required));
            focusView = emailEditText;
        } else if (!email.matches("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\" +
                ".[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$")) {
            emailEditText.setError(getString(R.string.error_invalid_email));
            focusView = emailEditText;
        }

        // check for valid username
        if (username == null || username.isEmpty() || username.equals("")) {
            usernameEditText.setError(getString(R.string.error_field_required));
            focusView = usernameEditText;
        } else if (username.length() < 6) {
            usernameEditText.setError(getString(R.string.error_invalid_username));
            focusView = usernameEditText;
        }

        // check if passwords match
        if (!password.equals(password2)) {
            usernameEditText.setError(getString(R.string.error_passwords_not_matching));
            focusView = usernameEditText;
        }

        // check if tou and privacy statement have been accepted.
        if (!touCheckbox.isChecked()) {
            touCheckbox.setError("some error");
            focusView = touCheckbox;
        }
//        if (!mAcceptPrivacyCheckbox.isChecked()) {
//            mAcceptPrivacyCheckbox.setError("some error");
//        }

        // Check if an error occured.
        if (focusView != null) {
            // There was an error; don't attempt register and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            //hide the keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(password1EditText.getWindowToken(), 0);

            // finally submit registration
            this.register(username, email, password);
        }
    }

    private void register(String username, String email, String password) {
        final MaterialDialog dialog = new MaterialDialog.Builder(SignupActivity.this)
                .title(R.string.register_progress_signing_in)
                .progress(true, 0)
                .cancelable(false)
                .show();

        registerSubscription = backgroundWorker.schedule(() -> {
            try {
                User newUser = new UserImpl(username, password);
                newUser.setMail(email);
                daoProvider.getUserDAO().createUser(newUser);

                // Successfully created the getUserStatistic
                mainThreadWorker.schedule(() -> {
                    // Dismiss the progress dialog.
                    dialog.dismiss();

                    final MaterialDialog d = new MaterialDialog.Builder(SignupActivity.this)
                            .title(R.string.register_success_dialog_title)
                            .content(R.string.register_success_dialog_content)
                            .cancelable(false)
                            .positiveText(R.string.ok)
                            .cancelListener(dialog1 -> {
                                LOG.info("canceled");
                                finish();
                            })
                            .onAny((a, b) -> {
                                LOG.info("onPositive");
                                finish();
                            })
                            .show();
                });
            } catch (ResourceConflictException e) {
                LOG.warn(e.getMessage(), e);

                // Show an error. // TODO show error in a separate error text view.
                final ResourceConflictException.ConflictType reason = e.getConflictType();
                mainThreadWorker.schedule(() -> {
                    if (e.getConflictType() == ResourceConflictException.ConflictType.USERNAME) {
                        usernameEditText.setError(getString(
                                R.string.error_username_already_in_use));
                        usernameEditText.requestFocus();
                    } else if (e.getConflictType() == ResourceConflictException.ConflictType.MAIL) {
                        emailEditText.setError(getString(R.string.error_email_already_in_use));
                        emailEditText.requestFocus();
                    }
                });

                // Dismuss the progress dialog.
                dialog.dismiss();
            } catch (DataUpdateFailureException e) {
                LOG.warn(e.getMessage(), e);

                // Show an error.
                mainThreadWorker.schedule(() -> {
                    usernameEditText.setError(getString(R.string.error_host_not_found));
                    usernameEditText.requestFocus();
                });

                // Dismiss the progress dialog.
                dialog.dismiss();
            }
        });
    }

    private void makeClickableTextLinks() {

        List<Pair<String, View.OnClickListener>> clickableStrings = Arrays.asList(
                new Pair<>(getString(R.string.terms_and_conditions), v -> {
                    LOG.info("Terms and Conditions clicked. Showing dialog");
                    showTermsOfUseDialog();
                }),
                new Pair<>(getString(R.string.privacy_statement), v -> {
                    LOG.info("Privacy Policy clicked. Showing dialog");
                    showPrivacyStatementDialog();
                })
        );

        SpannableString string = new SpannableString(touText.getText());
        for (Pair<String, View.OnClickListener> link : clickableStrings) {
            ClickableSpan span = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Selection.setSelection((Spannable) ((TextView) widget).getText(), 0);
                    widget.invalidate();
                    link.second.onClick(widget);
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setColor(getResources().getColor(R.color.green_dark_cario));
                }
            };

            int start = touText.getText().toString().indexOf(link.first);
            if (start > 0) {
                string.setSpan(span, start, start + link.first.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        touText.setMovementMethod(LinkMovementMethod.getInstance());
        touText.setText(string, TextView.BufferType.SPANNABLE);
    }

    private void showTermsOfUseDialog() {
        LOG.info("Show Terms of Use Dialog");
        agreementManager.showLatestTermsOfUseDialogObservable(this)
                .subscribe(tou -> LOG.info("Closed Dialog"));
    }

    private void showPrivacyStatementDialog() {
        LOG.info("Show Privacy Statement dialog");
        agreementManager.showLatestPrivacyStatementDialogObservable(this)
                .subscribe(ps -> LOG.info("Closed Dialog"));
    }

    public static boolean isStrongPassword(String password) {
        return Pattern.matches("^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{6,}$", password);

    }
}
