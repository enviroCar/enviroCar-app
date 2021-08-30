/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.login;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.jakewharton.rxbinding3.widget.RxCompoundButton;
import com.jakewharton.rxbinding3.widget.RxTextView;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.agreement.AgreementManager;
import org.envirocar.app.handler.preferences.UserPreferenceHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.views.utils.DialogUtils;
import org.envirocar.core.ContextInternetAccessProvider;
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserImpl;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.logging.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
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

    private static final String EMAIL_REGEX = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
    private static final String PASSWORD_REGEX = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9]).{6,}$";
    private static final String USERNAME_REGEX = "^[A-Za-z0-9_-]{6,}$";
    private static final int CHECK_FORM_DELAY = 750;
    private static Drawable errorPassword;
    private static Drawable errorUsername;

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
    @BindView(R.id.activity_signup_ps_checkbox)
    protected CheckBox psCheckbox;
    @BindView(R.id.activity_signup_ps_text)
    protected TextView psText;

    private final Scheduler.Worker mainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    private final Scheduler.Worker backgroundWorker = Schedulers.newThread().createWorker();
    private Disposable registerSubscription;
    private AlertDialog dialog;


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

        errorPassword = getResources().getDrawable(R.drawable.ic_error_red_24dp);
        errorPassword.setBounds(-70,0,0, errorPassword.getIntrinsicHeight());

        errorUsername = getResources().getDrawable(R.drawable.ic_error_red_24dp);
        errorUsername.setBounds(0, 0, errorUsername.getIntrinsicWidth(), errorUsername.getIntrinsicHeight());

        // make terms of use and privacy statement clickable
        this.makeClickableTextLinks();
        observeFormInputs();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registerSubscription != null && !registerSubscription.isDisposed()) {
            registerSubscription.dispose();
        }
    }

    @OnClick(R.id.imageView)
    protected void closeKeyboard(){
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    @OnClick(R.id.activity_signup_login_button)
    protected void onSwitchToRegister() {
        Intent intent = new Intent(this, SigninActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.activity_signup_register_button)
    protected void onRegisterAccountButtonClicked() {

        // We do not want to have dublicate registration processes.
        if (this.registerSubscription != null && !this.registerSubscription.isDisposed()) {
            return;
        }

        // Get all the values of the edittexts
        final String username = usernameEditText.getText().toString().trim();
        final String email = emailEditText.getText().toString().trim();
        final String password1 = password1EditText.getText().toString();
        final String password2 = password2EditText.getText().toString();

        // check if tou and privacy statement have been accepted.
        View focusView = null;
        if (!touCheckbox.isChecked()) {
            touCheckbox.setError("some error");
            focusView = touCheckbox;
        }
        if (!psCheckbox.isChecked()) {
            psCheckbox.setError("some error");
            focusView = psCheckbox;
        }

        if (!checkPasswordMatch(password1, password2)) {
            focusView = password2EditText;
        }
        if (!checkPasswordValidity(password1)) {
            focusView = password1EditText;
        }
        if (!checkEmailValidity(email)) {
            focusView = emailEditText;
        }
        if (!checkUsernameValidity(username)) {
            focusView = usernameEditText;
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
            imm.hideSoftInputFromWindow(password1EditText.getWindowToken(), 0);

            // finally submit registration
            this.register(username, email, password1);
        }
    }

    private void register(String username, String email, String password) {

        if(new ContextInternetAccessProvider(getApplicationContext()).isConnected()) {
            dialog = DialogUtils.createProgressBarDialogBuilder(SignupActivity.this,
                R.string.register_progress_signing_in,
                R.drawable.ic_baseline_login_24,
                (String) null)
                .setCancelable(false)
                .show();
        }

        registerSubscription = backgroundWorker.schedule(() -> {
            try {
                if(new ContextInternetAccessProvider(getApplicationContext()).isConnected()) {

                    User newUser = new UserImpl(username, password);
                    newUser.setMail(email);
                    daoProvider.getUserDAO().createUser(newUser);
                    // Successfully created the getUserStatistic
                    mainThreadWorker.schedule(() -> {
                        // Dismiss the progress dialog.
                        if(new ContextInternetAccessProvider(getApplicationContext()).isConnected())
                        dialog.dismiss();

                        new MaterialAlertDialogBuilder(SignupActivity.this, R.style.MaterialDialog)
                                .setTitle(R.string.register_success_dialog_title)
                                .setMessage(R.string.register_success_dialog_content)
                                .setIcon(R.drawable.ic_baseline_login_24)
                                .setCancelable(false)
                                .setOnCancelListener(dialog1 -> {
                                    LOG.info("canceled");
                                    finish();
                                })
                                .setPositiveButton(R.string.ok, (a, b) -> {
                                    LOG.info("onPositive");
                                    finish();
                                })
                                .show();
                    });
                }else{
                    if(new ContextInternetAccessProvider(getApplicationContext()).isConnected())
                    dialog.dismiss();
                    showSnackbar(getString(R.string.error_not_connected_to_network));
                }
            } catch (ResourceConflictException e) {
                LOG.warn(e.getMessage(), e);

                // Show an error. // TODO show error in a separate error text view.
                final ResourceConflictException.ConflictType reason = e.getConflictType();
                mainThreadWorker.schedule(() -> {
                    if (e.getConflictType() == ResourceConflictException.ConflictType.USERNAME) {
                        usernameEditText.setError(getString(
                                R.string.error_username_already_in_use),errorUsername);
                        usernameEditText.requestFocus();
                    } else if (e.getConflictType() == ResourceConflictException.ConflictType.MAIL) {
                        emailEditText.setError(getString(R.string.error_email_already_in_use),errorUsername);
                        emailEditText.requestFocus();
                    }
                });

                // Dismuss the progress dialog.
                dialog.dismiss();
            } catch (DataUpdateFailureException e) {
                LOG.warn(e.getMessage(), e);

                // Show an error.
                mainThreadWorker.schedule(() -> {
                    usernameEditText.setError(getString(R.string.error_host_not_found),errorUsername);
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
                    LOG.info("Redirecting to Terms and Conditions webpage ");
                    showTermsOfUseBrowser();
                }),
                new Pair<>(getString(R.string.privacy_statement), v -> {
                    LOG.info("Redirecting to Privacy Policy webpage");
                    showPrivacyStatementBrowser();
                })
        );

        for (TextView textView : Arrays.asList(touText, psText)) {
            SpannableString string = new SpannableString(textView.getText());
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

                int start = textView.getText().toString().indexOf(link.first);
                if (start > 0) {
                    string.setSpan(span, start, start + link.first.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setText(string, TextView.BufferType.SPANNABLE);
        }
    }

    private void showTermsOfUseBrowser() {
        if (Locale.getDefault().getLanguage() == "en") {
            String TOUwebpage = "https://envirocar.org/TermsOfUse.html?lng=en"; //TOU-->TermsOfUse
            Intent TOUintent = new Intent(Intent.ACTION_VIEW);

            LOG.info("Redirected to the Terms and Conditions webpage (English)");
            //To handle ActivityNotFoundException ,there may be zero activities to handle the Intent.
            if (TOUintent.resolveActivity(getPackageManager()) != null) {
                TOUintent.setData(Uri.parse(TOUwebpage));
                startActivity(TOUintent);
            }
        } else{
            String TOUwebpage = "https://envirocar.org/TermsOfUse.html?lng=de"; //TOU-->TermsOfUse
            Intent TOUintent = new Intent(Intent.ACTION_VIEW);

            LOG.info("Redirected to the Terms and Conditions webpage (German)");
            //To handle ActivityNotFoundException ,there may be zero activities to handle the Intent.
            if (TOUintent.resolveActivity(getPackageManager()) != null) {
                TOUintent.setData(Uri.parse(TOUwebpage));
                startActivity(TOUintent);
            }
        }

    }
    private void showPrivacyStatementBrowser() {
        if (Locale.getDefault().getLanguage() == "en") {
            String condWebpage = "https://envirocar.org/conditions.html?lng=en"; //Cond-->Condition
            Intent condintent = new Intent(Intent.ACTION_VIEW);
            LOG.info("Redirected to Privacy Policy webpage(English)");
            if (condintent.resolveActivity(getPackageManager()) != null) {
                condintent.setData(Uri.parse(condWebpage));
                startActivity(condintent);
            }
        }
        else{
            String condWebpage = "https://envirocar.org/conditions.html?lng=de"; //Cond-->Condition
            Intent condintent = new Intent(Intent.ACTION_VIEW);
            LOG.info("Redirected to Privacy Policy webpage(German)");
            if (condintent.resolveActivity(getPackageManager()) != null) {
                condintent.setData(Uri.parse(condWebpage));
                startActivity(condintent);
            }
        }
    }

    /**
     * Checks for a valid username
     */
    private boolean checkUsernameValidity(String username) {
        // reset error text
        usernameEditText.setError(null);

        boolean isValidUsername = true;
        if (username == null || username.isEmpty() || username.equals("")) {
            usernameEditText.setError(getString(R.string.error_field_required),errorUsername);
            isValidUsername = false;
        } else if (username.length() < 6) {
            usernameEditText.setError(getString(R.string.error_invalid_username),errorUsername);
            isValidUsername = false;
        } else if (!Pattern.matches(USERNAME_REGEX,username)) {
            usernameEditText.setError(getString(R.string.error_username_contain_special),errorUsername);
            isValidUsername = false;
        }
        return isValidUsername;
    }

    /**
     * Check for a valid email address.
     */
    private boolean checkEmailValidity(String email) {
        boolean isValidEmail = true;
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError(getString(R.string.error_field_required),errorUsername);
            isValidEmail = false;
        } else if (!Pattern.matches(EMAIL_REGEX, email)) {
            emailEditText.setError(getString(R.string.error_invalid_email),errorUsername);
            isValidEmail = false;
        }
        return isValidEmail;
    }

    /**
     * Checks for a valid password
     */
    private boolean checkPasswordValidity(String password) {
        boolean isValidPassword = true;
        if (password == null || password.isEmpty() || password.equals("")) {
            password1EditText.setError(getString(R.string.error_field_required), errorPassword);
            isValidPassword = false;
        } else if (password.length() < 6) {
            password1EditText.setError(getString(R.string.error_invalid_password), errorPassword);
            isValidPassword = false;
        } else if (!Pattern.matches(PASSWORD_REGEX, password)) {
            password1EditText.setError(getString(R.string.error_field_weak_password), errorPassword);
            isValidPassword = false;
        } else {
            final String password2 = password2EditText.getText().toString().trim();
            if (!password2.equals("") && !password2.isEmpty() && password2 != null) {
                checkPasswordMatch(password, password2);
            }else {
                password2EditText.setError(getString(R.string.error_field_required), errorPassword);
            }
        }
        return isValidPassword;
    }

    /**
     * Check for a valid match between the two passwords
     */
    private boolean checkConfirmPasswordValidity(String password2) {
        boolean isValidMatch = true;
        if (password2 == null || password2.isEmpty() || password2.equals("")) {
            password2EditText.setError(getString(R.string.error_field_required), errorPassword);
            isValidMatch = false;
        } else {
            final String password1 = password1EditText.getText().toString().trim();
            if (!password1.equals("") && !password1.isEmpty() && password1 != null) {
                isValidMatch = checkPasswordMatch(password1, password2);
            }
        }
        return isValidMatch;
    }

    /**
     * check if passwords match
     */
    private boolean checkPasswordMatch(String password, String password2) {
        boolean isValidMatch = password.equals(password2);
        if (!isValidMatch) {
            password1EditText.setError(getString(R.string.error_passwords_not_matching), errorPassword);
            password2EditText.setError(getString(R.string.error_passwords_not_matching), errorPassword);
        } else {
            password1EditText.setError(null);
            password2EditText.setError(null);
        }
        return isValidMatch;
    }

    private void showSnackbar(String info) {
        Snackbar.make(findViewById(R.id.activity_signup_register_button), info, Snackbar.LENGTH_LONG).show();
    }

    private void observeFormInputs() {
        RxTextView.textChanges(usernameEditText)
                .skipInitialValue()
                .debounce(CHECK_FORM_DELAY, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(CharSequence::toString)
                .subscribe(this::checkUsernameValidity, LOG::error);

        RxTextView.textChanges(emailEditText).
                skipInitialValue()
                .debounce(CHECK_FORM_DELAY, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(CharSequence::toString)
                .subscribe(this::checkEmailValidity, LOG::error);

        RxTextView.textChanges(password1EditText)
                .skipInitialValue()
                .debounce(CHECK_FORM_DELAY, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(CharSequence::toString)
                .subscribe(this::checkPasswordValidity, LOG::error);

        RxTextView.textChanges(password2EditText)
                .skipInitialValue()
                .debounce(CHECK_FORM_DELAY, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(CharSequence::toString)
                .subscribe(this::checkConfirmPasswordValidity, LOG::error);

        RxCompoundButton.checkedChanges(touCheckbox)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(b -> touCheckbox.setError(null), LOG::error);

        RxCompoundButton.checkedChanges(psCheckbox)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(b -> psCheckbox.setError(null), LOG::error);
    }
}
