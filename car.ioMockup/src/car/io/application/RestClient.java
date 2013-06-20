package car.io.application;

import java.io.UnsupportedEncodingException;

import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

public class RestClient {
	
	private static AsyncHttpClient client = new AsyncHttpClient();
	
	public static void downloadTracks(String user, String token, JsonHttpResponseHandler handler){
		client.addHeader("X-User", user);
		client.addHeader("X-Token", token);
		client.get(ECApplication.BASE_URL+"/users/"+user+"/tracks", handler); //TODO use pagination
	}
	
	public static void downloadTrack(String user, String token, String id, JsonHttpResponseHandler handler){
		client.addHeader("X-User", user);
		client.addHeader("X-Token", token);
		client.get(ECApplication.BASE_URL+"/tracks/"+id, handler);
	}
	
	public static boolean createSensor(String jsonObj, String user, String token, AsyncHttpResponseHandler handler){
		client.addHeader("Content-Type", "application/json");
		client.addHeader("X-User", user);
		client.addHeader("X-Token", token);
		
		try {
			StringEntity se = new StringEntity(jsonObj);
			se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
			client.post(null, ECApplication.BASE_URL+"/sensors", se, "application/json", handler);		  
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static void downloadSensors(JsonHttpResponseHandler handler){
		client.get(ECApplication.BASE_URL+"/sensors", handler);
	}
}
