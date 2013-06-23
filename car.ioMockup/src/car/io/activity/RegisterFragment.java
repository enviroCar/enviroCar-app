package car.io.activity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import car.io.R;
import car.io.application.ECApplication;
import car.io.application.User;
import car.io.views.TYPEFACE;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * Activity which displays a register screen to the user, offering registration
 * as well.
 */
public class RegisterFragment extends SherlockFragment {

	private static final int ERROR_GENERAL = 1;
	private static final int ERROR_NET = 2;

	/**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_EMAIL = "com.example.android.authenticatordemo.extra.EMAIL";

	/**
	 * Keep track of the register task to ensure we can cancel it if requested.
	 */
	private UserRegisterTask mAuthTask = null;

	// Values for email and password at the time of the register attempt.
	private String mUsername;
	private String mEmail;
	private String mPassword;
	private String mPasswordConfirm;
	// private String mPasswordMD5;

	// UI references.
	private EditText mUsernameView;
	private EditText mEmailView;
	private EditText mPasswordView;
	private EditText mPasswordConfirmView;
	private View mRegisterFormView;
	private View mRegisterStatusView;
	private TextView mRegisterStatusMessageView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.register_layout, null);

		mUsernameView = (EditText) view.findViewById(R.id.register_username);

		mEmailView = (EditText) view.findViewById(R.id.register_email);

		mPasswordView = (EditText) view.findViewById(R.id.register_password);
		mPasswordConfirmView = (EditText) view
				.findViewById(R.id.register_password_second);
		mPasswordConfirmView
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id,
							KeyEvent keyEvent) {
						if (id == R.id.register || id == EditorInfo.IME_NULL) {
							attemptRegister();
							return true;
						}
						return false;
					}
				});
		mRegisterFormView = view.findViewById(R.id.register_form);
		mRegisterStatusView = view.findViewById(R.id.register_status);
		mRegisterStatusMessageView = (TextView) view
				.findViewById(R.id.register_status_message);

		view.findViewById(R.id.register_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptRegister();
					}
				});
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		TYPEFACE.applyCustomFont((ViewGroup) view,
				TYPEFACE.Raleway(getActivity()));
		mUsernameView.requestFocus();
	}

	/**
	 * Attempts to sign in or register the account specified by the register
	 * form. If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual register attempt is made.
	 */
	public void attemptRegister() {
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mUsernameView.setError(null);
		mEmailView.setError(null);
		mPasswordView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the register attempt.
		mUsername = mUsernameView.getText().toString();
		mEmail = mEmailView.getText().toString();
		mPassword = mPasswordView.getText().toString();
		mPasswordConfirm = mPasswordConfirmView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// TODO fiddle around with order of checks

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

		// check if the password confirm is empty
		if (TextUtils.isEmpty(mPasswordConfirm)) {
			mPasswordConfirmView
					.setError(getString(R.string.error_field_required));
			focusView = mPasswordConfirmView;
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(mEmail)) {
			mEmailView.setError(getString(R.string.error_field_required));
			focusView = mEmailView;
			cancel = true;
		} else if (!mEmail.contains("@")) {
			mEmailView.setError(getString(R.string.error_invalid_email));
			focusView = mEmailView;
			cancel = true;
		}

		// check for valid username
		if (TextUtils.isEmpty(mUsername)) {
			mUsernameView.setError(getString(R.string.error_field_required));
			focusView = mUsernameView;
			cancel = true;
		} else if (mUsername.length() < 6) {
			mUsernameView.setError(getString(R.string.error_invalid_username));
			focusView = mUsernameView;
			cancel = true;
		}

		// check if passwords match
		if (!mPassword.equals(mPasswordConfirm)) {
			mPasswordConfirmView
					.setError(getString(R.string.error_passwords_not_matching));
			focusView = mPasswordConfirmView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt register and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user register attempt.
			mRegisterStatusMessageView
					.setText(R.string.register_progress_signing_in);
			showProgress(true);
			mAuthTask = new UserRegisterTask();
			mAuthTask.execute((Void) null);
		}
	}

	/**
	 * Shows the progress UI and hides the register form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mRegisterStatusView.setVisibility(View.VISIBLE);
			mRegisterStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mRegisterStatusView
									.setVisibility(show ? View.VISIBLE
											: View.GONE);
						}
					});

			mRegisterFormView.setVisibility(View.VISIBLE);
			mRegisterFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mRegisterFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mRegisterStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	/**
	 * Represents an asynchronous register/registration task used to
	 * authenticate the user.
	 */
	public class UserRegisterTask extends AsyncTask<Void, Void, Integer> {
		@Override
		protected Integer doInBackground(Void... params) {

			return createUser(mUsername, mPassword, mEmail);

		}

		@Override
		protected void onPostExecute(final Integer httpStatus) {
			mAuthTask = null;
			showProgress(false);

			if (httpStatus == HttpStatus.SC_CREATED) {
				// TODO greet the user or something..
				((ECApplication) getActivity().getApplication()).setUser(new User(mUsername, mPassword));
				
				Intent intent = new Intent(getActivity(), MyGarage.class);
//				intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//				intent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
				getActivity().startActivityForResult(intent, LoginActivity.REQUEST_MY_GARAGE);
				//getActivity().finish();
				
			} else if (httpStatus == HttpStatus.SC_CONFLICT) {
				// TODO look out for server changes..
			} else {
				// TODO general error
				mPasswordView
						.setError(getString(R.string.error_incorrect_password));
				mPasswordView.requestFocus();
			}
		}

		@Override
		protected void onCancelled() {
			mAuthTask = null;
			showProgress(false);
		}
	}

	/*
	 * Use this method to sign up a new user
	 */
	public int createUser(String user, String token, String mail) {

		JSONObject requestJson = new JSONObject();
		try {
			requestJson.put("name", user);
			requestJson.put("token", token);
			requestJson.put("mail", mail);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		DefaultHttpClient httpClient = new DefaultHttpClient();

		try {
			HttpPost postRequest = new HttpPost(
					ECApplication.BASE_URL+"/users");
					

			StringEntity input = new StringEntity(requestJson.toString(),
					HTTP.UTF_8);
			input.setContentType("application/json");

			postRequest.setEntity(input);
			return httpClient.execute(postRequest).getStatusLine()
					.getStatusCode();

		} catch (UnsupportedEncodingException e1) {
			// Shouldn't occur hopefully..
			e1.printStackTrace();
			return ERROR_GENERAL;
		} catch (ClientProtocolException e1) {
			e1.printStackTrace();
			return ERROR_GENERAL;
		} catch (IOException e1) {
			e1.printStackTrace();
			// probably something with the Internet..
			return ERROR_NET;
		} finally {
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			httpClient.getConnectionManager().shutdown();
		}
	}

}
