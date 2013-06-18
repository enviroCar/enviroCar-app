package car.io.application;

import java.io.UnsupportedEncodingException;

import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class RestClient {
	
	private static final String BASE_URL = "http://giv-car.uni-muenster.de:8080/stable/rest/";
	
	private static AsyncHttpClient client = new AsyncHttpClient();
	
	public static void downloadTracks(String user, JsonHttpResponseHandler handler){
		client.get(BASE_URL+"users/"+user+"/tracks", handler); //TODO use pagination
	}
	
	public static void downloadTrack(String url, JsonHttpResponseHandler handler){
		client.get(url, handler);
	}
	
	public static void createSensor(String jsonObj, String user, String token, AsyncHttpResponseHandler handler){
		client.addHeader("Content-Type", "application/json");
		client.addHeader("X-User", user);
		client.addHeader("X-Token", token);
		
		StringEntity se = null;
		try {
		  se = new StringEntity(jsonObj);
		} catch (UnsupportedEncodingException e) {
		  e.printStackTrace();
		  return;
		}
		se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

		client.post(null, BASE_URL+"sensors", se, "application/json", handler);
		

		//client.post(BASE_URL+"sensors", new RequestParams("",jsonObj), handler);
	}

}
