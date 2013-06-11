package car.io.application;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

public class RestClient {
	
	private static final String BASE_URL = "http://giv-car.uni-muenster.de:8080/stable/rest/";
	
	private static AsyncHttpClient client = new AsyncHttpClient();
	
	public static void downloadTracks(String user, JsonHttpResponseHandler handler){
		client.get(BASE_URL+"users/"+user+"/tracks", handler); //TODO use pagination
	}
	
	public static void downloadTrack(String url, JsonHttpResponseHandler handler){
		client.get(url, handler);
	}

}
