package car.io.application;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

public class RestClient {
	
	private static final String BASE_URL = "http://giv-car.uni-muenster.de:8080/stable/rest/";
	
	private static AsyncHttpClient client = new AsyncHttpClient();
	
	public static void downloadTracks(JsonHttpResponseHandler handler){
		client.get(BASE_URL+"tracks?limit=10", handler); //TODO: remove download limit
	}
	
	public static void downloadTrack(String url, JsonHttpResponseHandler handler){
		client.get(url, handler);
	}

}
