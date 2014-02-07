package org.envirocar.app.activity;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.envirocar.app.R;
import org.envirocar.app.application.CarManager;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.model.Car.FuelType;
import org.envirocar.app.activity.*;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;

import com.actionbarsherlock.app.SherlockFragment;
	
	public class LogbookFragment extends SherlockFragment implements OnClickListener, android.view.View.OnClickListener{
		//Used Parameter 
		private EditText Volume,Unit1,Cost,Currency,Unit2,Distance;
		private Button btn;
		
		//Date and Time
		Calendar c = Calendar.getInstance();
		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
		String formattedDate = df.format(c.getTime());
		
		
		public View onCreateView(android.view.LayoutInflater inflater,
				android.view.ViewGroup container,
				android.os.Bundle savedInstanceState) {
			super.onCreateView(inflater, container, savedInstanceState);
			
			setHasOptionsMenu(true);

			View v = inflater.inflate(R.layout.logbook_layout, null);
			//Text- and EditViews
			Volume=(EditText)v.findViewById(R.id.editTextVolume);
			Unit1=(EditText)v.findViewById(R.id.editTextUnit1);
			Cost=(EditText)v.findViewById(R.id.editTextCost);
			Currency=(EditText)v.findViewById(R.id.editTextCurrency);
			Distance=(EditText)v.findViewById(R.id.editTextDistance);
			Unit2=(EditText)v.findViewById(R.id.editTextUnit2);
			btn=(Button)v.findViewById(R.id.button1);
			btn.setOnClickListener(new Button.OnClickListener()
			{
			//Order to create JSON-Object by pushing the Button
				public void onClick(View v)
				{
					createJSONFile();
				}
			});
	
			return v;
		};
			
		//Creating a JSON-Object
		void createJSONFile()
		{
	        
			try
			{
				String Username = UserManager.instance().getUser().getUsername();
				String carmodel = CarManager.instance().getCar().getModel();
				FuelType fueltype = CarManager.instance().getCar().getFuelType();
				
				JSONObject object=new JSONObject();
				object.put("Name:", Username );
				object.put("DateTime:", formattedDate);
				object.put("Car:", carmodel);
				object.put("Fueltype:",fueltype);
				object.put("Volume:", Volume.getText());
				object.put("VolumeUnit:", Unit1.getText());
				object.put("Cost:", Cost.getText());
				object.put("Currency:", Currency.getText());
				object.put("Distance:", Distance.getText());
				object.put("DistanceUnit:", Unit2.getText());
				String str=object.toString();
				//Sending the JSON-Object
				new MyAsyncTask().execute(str);
					//Log-Entry to LogCat (for Testing)
					Log.i("JsonString :", str);
				 
			}
		catch (Exception je)
			{
			//Exception if user is not logged in, or has not chosen a carmodel
			Toast.makeText(LogbookFragment.this.getActivity(), "Please check if you're logged in,have chosen a car or filled in all input fields.", Toast.LENGTH_LONG).show();
			}
		}
			
		private class MyAsyncTask extends AsyncTask<String, Integer, Double>{
			 
			@Override
			protected Double doInBackground(String... params) {
			// TODO Auto-generated method stub
			postData(params[0]);
			return null;
			}
			 
			protected void onPostExecute(Double result){
			Toast.makeText(LogbookFragment.this.getActivity(), "Data sent", Toast.LENGTH_LONG).show();
			}
			
			 
			public void postData(String valueIWantToSend) {
			// Create a new HttpClient and Post Header. Also HTTP-Adress from Server
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost("http://ows.dev.52north.org/enviroCar/receiver.php");
			 
			try {
			// Where to add the JSON-Object (watch reciever.php also)
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("myHttpData", valueIWantToSend));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			 
			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);
			 
			} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			} catch (IOException e) {
			// TODO Auto-generated catch block
			}
			}
			 
			}
	 
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				
			}

		@Override
		public void onClick(DialogInterface arg0, int arg1) {
			// TODO Auto-generated method stub
			
		}
		
	}	
	