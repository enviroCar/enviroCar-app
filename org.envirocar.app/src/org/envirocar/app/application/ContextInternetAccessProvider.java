//package org.envirocar.app.application;
//
//import org.envirocar.app.model.dao.InternetAccessProvider;
//
//import android.content.Context;
//import android.net.ConnectivityManager;
//import android.net.NetworkInfo;
//
//public class ContextInternetAccessProvider implements InternetAccessProvider {
//
//private Context context;
//
//	public ContextInternetAccessProvider(Context applicationContext) {
//		this.context = applicationContext;
//	}
//
//	@Override
//	public boolean isConnected() {
//		if (context == null)
//			return false;
//
//		ConnectivityManager cm = (ConnectivityManager) context
//				.getSystemService(Context.CONNECTIVITY_SERVICE);
//		NetworkInfo netInfo = cm.getActiveNetworkInfo();
//
//		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
//			return true;
//		}
//
//		return false;
//	}
//
//}
