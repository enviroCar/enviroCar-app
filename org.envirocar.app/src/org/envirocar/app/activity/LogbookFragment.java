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
import org.json.JSONObject;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
	
	public class LogbookFragment extends SherlockFragment implements OnClickListener, android.view.View.OnClickListener{
		
		
		private static final String Default_Distance_Unit = "Settings_Distance_Unit";
		private static final String Default_Volume_Unit = "Settings_Volume_Unit";
		private static final String Default_Currency_Unit = "Settings_Currency_Unit";
		//Used Parameter 
		private EditText Volume,Unit1,Cost,Currency,Unit2,Distance,Note;
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
			
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
			String distanceUnit = settings.getString(Default_Distance_Unit, "km");
			String VolumeUnit = settings.getString(Default_Volume_Unit, "Liter");
			String CurrencyUnit = settings.getString(Default_Currency_Unit,"Euro");

			View v = inflater.inflate(R.layout.logbook_layout, null);
			//Text- and EditViews
			Volume=(EditText)v.findViewById(R.id.editTextVolume);
			Unit1=(EditText)v.findViewById(R.id.editTextUnit1);
			Unit1.setText(VolumeUnit);
			Cost=(EditText)v.findViewById(R.id.editTextCost);
			Currency=(EditText)v.findViewById(R.id.editTextCurrency);
			Currency.setText(CurrencyUnit);
			Distance=(EditText)v.findViewById(R.id.editTextDistance);
			Unit2=(EditText)v.findViewById(R.id.editTextUnit2);
			Unit2.setText(distanceUnit);
			Note=(EditText)v.findViewById(R.id.editTextNote);
			
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
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
				settings.edit().putString(Default_Distance_Unit, Unit2.getText().toString()).commit();
				settings.edit().putString(Default_Volume_Unit, Unit1.getText().toString()).commit();
				settings.edit().putString(Default_Currency_Unit, Currency.getText().toString()).commit();
				
				String Username = UserManager.instance().getUser().getUsername();
				String carmodel = CarManager.instance().getCar().getModel();
				FuelType fueltype = CarManager.instance().getCar().getFuelType();
				
				JSONObject obj1=new JSONObject();
				obj1.put("DateTime:", formattedDate);
				obj1.put("Note:", Note.getText());
				JSONObject obj2=new JSONObject();
				obj2.put("Name:", Username );
				obj2.put("Car:", carmodel);
				obj2.put("Fueltype:",fueltype);
				obj1.put("User:", obj2);
				JSONObject obj3=new JSONObject();
				obj3.put("Volume:", Volume.getText());
				obj3.put("VolumeUnit:", Unit1.getText());
				obj1.put("Fuel Tank:", obj3);
				JSONObject obj4=new JSONObject();
				obj4.put("Cost:", Cost.getText());
				obj4.put("Currency:", Currency.getText());
				obj1.put("Finance:", obj4);
				JSONObject obj5=new JSONObject();
				obj5.put("Distance:", Distance.getText());
				obj5.put("DistanceUnit:", Unit2.getText());
				obj1.put("Total traveled Distance:", obj5);
							
				String str=obj1.toString();
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
	