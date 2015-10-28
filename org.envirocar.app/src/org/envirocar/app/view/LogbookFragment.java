package org.envirocar.app.view;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;

import org.envirocar.core.injection.BaseInjectorFragment;

public class LogbookFragment extends BaseInjectorFragment implements OnClickListener, android.view.View
        .OnClickListener {
    @Override
    public void onClick(View v) {

    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

    }

    //
//    private static final String Default_Distance_Unit = "Settings_Distance_Unit";
//    private static final String Default_Volume_Unit = "Settings_Volume_Unit";
//    private static final String Default_Currency_Unit = "Settings_Currency_Unit";
//    //Used Parameter
//    private EditText Volume, Unit1, Cost, Currency, Unit2, Distance, Note;
//    private Button btn;
//
//    //Date and Time
////		Calendar c = Calendar.getInstance();
////		SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
////		String formattedDate = df.format(c.getTime());
//
//    // Injected Variables
//    @Inject
//    protected CarPreferenceHandler mCarManager;
//    @Inject
//    protected DAOProvider mDAOProvider;
//
//    public View onCreateView(android.view.LayoutInflater inflater,
//                             android.view.ViewGroup container,
//                             android.os.Bundle savedInstanceState) {
//        super.onCreateView(inflater, container, savedInstanceState);
//
//        setHasOptionsMenu(true);
//
//        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
//        String distanceUnit = settings.getString(Default_Distance_Unit, "km");
//        String VolumeUnit = settings.getString(Default_Volume_Unit, "Liter");
//        String CurrencyUnit = settings.getString(Default_Currency_Unit, "Euro");
//
//        View v = inflater.inflate(R.layout.logbook_layout, null);
//        //Text- and EditViews
//        Volume = (EditText) v.findViewById(R.id.editTextVolume);
//        Unit1 = (EditText) v.findViewById(R.id.editTextUnit1);
//        Unit1.setText(VolumeUnit);
//        Cost = (EditText) v.findViewById(R.id.editTextCost);
//        Currency = (EditText) v.findViewById(R.id.editTextCurrency);
//        Currency.setText(CurrencyUnit);
//        Distance = (EditText) v.findViewById(R.id.editTextDistance);
//        Unit2 = (EditText) v.findViewById(R.id.editTextUnit2);
//        Unit2.setText(distanceUnit);
//        Note = (EditText) v.findViewById(R.id.editTextNote);
//
//        btn = (Button) v.findViewById(R.id.button1);
//        btn.setOnClickListener(new Button.OnClickListener() {
//            //Order to create JSON-Object by pushing the Button
//            public void onClick(View v) {
//                getActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        btn.setEnabled(false);
//                    }
//                });
//                createJSONFile();
//            }
//        });
//
//        return v;
//    }
//
//    ;
//
//
//    //Creating a JSON-Object
//    void createJSONFile() {
//
//        try {
//            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
//            settings.edit().putString(Default_Distance_Unit, Unit2.getText().toString()).commit();
//            settings.edit().putString(Default_Volume_Unit, Unit1.getText().toString()).commit();
//            settings.edit().putString(Default_Currency_Unit, Currency.getText().toString()).commit();
//
////				String Username = UserHandler.instance().getUser().getUsername();
////				String carmodel = CarManager.instance().getCar().getModel();
////				FuelType fueltype = CarManager.instance().getCar().getFuelType();
//
//            Fueling fueling = new FuelingImpl();
//            fueling.setTime(new Date());
//
//            if (mCarManager.getCar() == null) {
//                throw new Exception("No car selected");
//            }
//
//            fueling.setCar(mCarManager.getCar());
//            if (!Note.getText().toString().isEmpty()) {
//                fueling.setComment(Note.getText().toString());
//            }
//
//            try {
//                fueling.setCost(new NumberWithUOM(Double.parseDouble(Cost.getText().toString()),
//                        Currency.getText().toString()));
//                fueling.setVolume(new NumberWithUOM(Double.parseDouble(Volume.getText().toString()),
//                        Unit1.getText().toString()));
//                fueling.setMileage(new NumberWithUOM(Double.parseDouble(Distance.getText().toString()),
//                        Unit2.getText().toString()));
//            } catch (NumberFormatException e) {
//                //TODO implement some valuable user feedback
//                throw e;
//            }
//
////				JSONObject obj1=new JSONObject();
////				obj1.put("DateTime:", formattedDate);
////				obj1.put("Note:", Note.getText());
////				JSONObject obj2=new JSONObject();
////				obj2.put("Name:", Username );
////				obj2.put("Car:", carmodel);
////				obj2.put("Fueltype:",fueltype);
////				obj1.put("User:", obj2);
////				JSONObject obj3=new JSONObject();
////				obj3.put("Volume:", Volume.getText());
////				obj3.put("VolumeUnit:", Unit1.getText());
////				obj1.put("Fuel Tank:", obj3);
////				JSONObject obj4=new JSONObject();
////				obj4.put("Cost:", Cost.getText());
////				obj4.put("Currency:", Currency.getText());
////				obj1.put("Finance:", obj4);
////				JSONObject obj5=new JSONObject();
////				obj5.put("Distance:", Distance.getText());
////				obj5.put("DistanceUnit:", Unit2.getText());
////				obj1.put("Total traveled Distance:", obj5);
////
////				String str=obj1.toString();
//            //Sending the JSON-Object
//            new MyAsyncTask().execute(fueling);
//            //Log-Entry to LogCat (for Testing)
//            Log.i("JsonString :", new FuelingEncoder().createFuelingJson(fueling).toString());
//
//        } catch (Exception je) {
//            //Exception if user is not logged in, or has not chosen a carmodel
//            makeToast("Please check if you're logged in,have chosen a car or filled in all input fields.");
//        }
//    }
//
//    public void makeToast(final String text) {
//        getActivity().runOnUiThread(new Runnable() {
//
//            @Override
//            public void run() {
//                Toast.makeText(LogbookFragment.this.getActivity(), text, Toast.LENGTH_LONG).show();
//            }
//        });
//
//    }
//
//    private class MyAsyncTask extends AsyncTask<Fueling, Integer, Double> {
//
//        @Override
//        protected Double doInBackground(Fueling... params) {
//            // TODO Auto-generated method stub
//            postData(params[0]);
//            return null;
//        }
//
//        protected void onPostExecute(Double result) {
//            makeToast("Data sent");
//        }
//
//
//        public void postData(Fueling valueIWantToSend) {
//            // Create a new HttpClient and Post Header. Also HTTP-Adress from Server
////			HttpClient httpclient = new DefaultHttpClient();
////			HttpPost httppost = new HttpPost("http://ows.dev.52north.org/enviroCar/receiver.php");
////
////			try {
////			// Where to add the JSON-Object (watch reciever.php also)
////			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
////			nameValuePairs.add(new BasicNameValuePair("myHttpData", valueIWantToSend));
////			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
////
////			// Execute HTTP Post Request
////			HttpResponse response = httpclient.execute(httppost);
////
////			} catch (ClientProtocolException e) {
////			// TODO Auto-generated catch block
////			} catch (IOException e) {
////			// TODO Auto-generated catch block
////			}
//            try {
//                mDAOProvider.getFuelingDAO().storeFueling(valueIWantToSend);
//            } catch (NotConnectedException e) {
//                makeToast("There has been an issue while communicating with the server.");
//
//            } catch (InvalidObjectStateException e) {
//                makeToast("Please check if you're logged in,have chosen a car or filled in all input fields.");
//            } catch (UnauthorizedException e) {
//                makeToast("Unauthorized");
//                e.printStackTrace();
//            }
//        }
//
//
//    }
//
//    @Override
//    public void onClick(View v) {
//        // TODO Auto-generated method stub
//
//    }
//
//
//    @Override
//    public void onClick(DialogInterface dialog, int which) {
//        // TODO Auto-generated method stub
//
//    }

}	
