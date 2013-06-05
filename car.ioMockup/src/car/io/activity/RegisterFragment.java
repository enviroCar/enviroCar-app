package car.io.activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
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

import com.actionbarsherlock.app.SherlockFragment;

/**
 * Activity which displays a register screen to the user, offering registration as
 * well.
 */
public class RegisterFragment extends SherlockFragment {

	/**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_EMAIL = "com.example.android.authenticatordemo.extra.EMAIL";

	/**
	 * Keep track of the register task to ensure we can cancel it if requested.
	 */
	private UserRegisterTask mAuthTask = null;

	// Values for email and password at the time of the register attempt.
	private String mEmail;
	private String mPassword;

	// UI references.
	private EditText mEmailView;
	private EditText mPasswordView;
	private View mRegisterFormView;
	private View mRegisterStatusView;
	private TextView mRegisterStatusMessageView;

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
//				super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.register_layout, null);
		
		mEmailView = (EditText) view.findViewById(R.id.email);


		mPasswordView = (EditText) view.findViewById(R.id.password);
		mPasswordView
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
		mRegisterStatusMessageView = (TextView) view.findViewById(R.id.register_status_message);

		view.findViewById(R.id.sign_in_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptRegister();
					}
				});
		return view;
	}

	/**
	 * Attempts to sign in or register the account specified by the register form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual register attempt is made.
	 */
	public void attemptRegister() {
		if (mAuthTask != null) {
			return;
		}

		// Reset errors.
		mEmailView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the register attempt.
		mEmail = mEmailView.getText().toString();
		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		} else if (mPassword.length() < 4) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
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

		if (cancel) {
			// There was an error; don't attempt register and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user register attempt.
			mRegisterStatusMessageView.setText(R.string.register_progress_signing_in);
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
							mRegisterStatusView.setVisibility(show ? View.VISIBLE
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
	 * Represents an asynchronous register/registration task used to authenticate
	 * the user.
	 */
	public class UserRegisterTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {
			return authenticateHttp(mEmail,mPassword);
			
		}

		@Override
		protected void onPostExecute(final Boolean success) {
			mAuthTask = null;
			showProgress(false);

			if (success) {
				getActivity().finish();
			} else {
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
	public void createUser(String user, String token, String mail, String groups){

		JSONObject requestJson = new JSONObject();
		try {
			requestJson.put("name", user);
			requestJson.put("token", token);
			requestJson.put("mail", mail);
			requestJson.put("groups", groups);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DefaultHttpClient httpClient = new DefaultHttpClient();

		HttpResponse response;
		try {
			HttpPost postRequest = new HttpPost("http://giv-car.uni-muenster.de:8080/stable/rest/users");

			StringEntity input = new StringEntity(requestJson.toString(), HTTP.UTF_8);
			input.setContentType("application/json");
			postRequest.setEntity(input);
					
			response = httpClient.execute(postRequest);

			if (response.getStatusLine().getStatusCode() != 201) {
				// TODO Throw error
			} else {
				// TODO Report to user if successfull and redirect
				BufferedReader br = new BufferedReader(new InputStreamReader(
						(response.getEntity().getContent())));

				String output;
				System.out.println("Output from Server .... \n");
				while ((output = br.readLine()) != null) {
					System.out.println(output);
				}
			}
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ClientProtocolException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			httpClient.getConnectionManager().shutdown();
		}
	}
	
	

	/*
	 * Method used for authentication (e.g. at registerscreen to verify user
	 * credentials
	 */
	public boolean authenticateHttp(String user, String token) {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		try {
			HttpGet httpget = new HttpGet(
					"http://giv-car.uni-muenster.de:8080/stable/rest/users"
							+ user);
			httpget.addHeader(new BasicHeader("X-User", user));
			httpget.addHeader(new BasicHeader("X-Token", token));
			HttpResponse response = httpclient.execute(httpget);

			int status = response.getStatusLine().getStatusCode();
			if (status != HttpStatus.SC_OK) {
				return false;
			} else {
				// TODO Set Sharedprefs or similar
				return true;

			}

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			httpclient.getConnectionManager().shutdown();
		}
		return false;
	}
}
