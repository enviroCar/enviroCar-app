package car.io.datatransfer;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.AbstractClientConnAdapter;
import org.json.JSONObject;

import car.io.activity.MainActivity;
import car.io.application.ECApplication;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class HttpClient {

	private static final String TAG = "HttpClient";

	public static void SendHttpPost(String url, JSONObject jsonObjSend, String xToken, String xUser) {

		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpPost httpPostRequest = new HttpPost(url);

			StringEntity se = new StringEntity(jsonObjSend.toString());
			Log.d(TAG + "SE", jsonObjSend.toString());

			// Set HTTP parameters
			httpPostRequest.setEntity(se);
			httpPostRequest.setHeader("Content-type", "application/json");
			httpPostRequest.setHeader("X-Token", xToken);
			httpPostRequest.setHeader("X-User", xUser);

			long timeSubtrahend = System.currentTimeMillis();
			HttpResponse response = (HttpResponse) httpclient.execute(httpPostRequest);
			Log.i(TAG, String.format("HTTP response time: [%s ms]", (System.currentTimeMillis() - timeSubtrahend)));
			String statusCode = String.valueOf(response.getStatusLine().getStatusCode());
			String reasonPhrase = response.getStatusLine().getReasonPhrase();
			Log.d(TAG, String.format("%s: %s", statusCode, reasonPhrase));
			
			if (statusCode != "xyz"){ // TODO replace with 201
				ECApplication eca = MainActivity.application.getInstance();
				Context context = eca.getApplicationContext();
				
				CharSequence text = String.format("%s: %s", statusCode, reasonPhrase);
				int duration = Toast.LENGTH_LONG;

				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
			}

		} catch (Exception e) {
			Log.e(TAG, "Error occured while sending JSON file to server.");
			e.printStackTrace();
		}
	}

}
