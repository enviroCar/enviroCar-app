package car.io.activitys;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import car.io.R;

import com.actionbarsherlock.app.SherlockFragment;

public class ListMeasurementsFragment extends SherlockFragment {

	public View onCreateView(android.view.LayoutInflater inflater,
			android.view.ViewGroup container,
			android.os.Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.list_tracks_layout, null);
		ExpandableListView elv = (ExpandableListView) v.findViewById(R.id.list);
		elv.setAdapter(new TracksListAdapter());
		return v;
	};

	public class TracksListAdapter extends BaseExpandableListAdapter {

		private String[] groups = { "People Names", "Dog Names", "Cat Names",
				"Fish Names" };

		private String[][] children = {
				{ "Arnold", "Barry", "Chuck", "David" },
				{ "Ace", "Bandit", "Cha-Cha", "Deuce" },
				{ "Fluffy", "Snuggles" }, { "Goldy", "Bubbles" } };

		@Override
		public int getGroupCount() {
			return groups.length;
		}

		@Override
		public int getChildrenCount(int i) {
			return 1;
		}

		@Override
		public Object getGroup(int i) {
			return groups[i];
		}

		@Override
		public Object getChild(int i, int i1) {
			return children[i];
		}

		@Override
		public long getGroupId(int i) {
			return i;
		}

		@Override
		public long getChildId(int i, int i1) {
			return i1;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public View getGroupView(int i, boolean b, View view,
				ViewGroup viewGroup) {
			if (view == null) {
				View groupRow = ViewGroup.inflate(getActivity(),
						R.layout.list_tracks_group_layout, null);
				TextView textView = (TextView) groupRow
						.findViewById(R.id.track_name_textview);
				textView.setText(getGroup(i).toString());
				return groupRow;
			}
			return view;
		}

		@Override
		public View getChildView(int i, int i1, boolean b, View view,
				ViewGroup viewGroup) {
			if(view == null) {
				View row = ViewGroup.inflate(getActivity(),
						R.layout.list_tracks_item_layout, null);
				
				
				row.setClickable(false);
				
				return row;
			}
			return view;
		}

		@Override
		public boolean isChildSelectable(int i, int i1) {
			return true;
		}

	}

}

// package car.io.activitys;
//
// import java.io.IOException;
// import java.util.ArrayList;
//
// import org.apache.http.client.ClientProtocolException;
//
// import android.content.Intent;
// import android.database.DataSetObserver;
// import android.os.Bundle;
// import android.util.Log;
// import android.view.LayoutInflater;
// import android.view.View;
// import android.view.ViewGroup;
// import android.widget.AdapterView;
// import android.widget.AdapterView.OnItemClickListener;
// import android.widget.ArrayAdapter;
// import android.widget.BaseExpandableListAdapter;
// import android.widget.ExpandableListAdapter;
// import android.widget.ListView;
// import android.widget.TextView;
// import car.io.R;
// import car.io.adapter.DbAdapter;
// import car.io.adapter.DbAdapterLocal;
// import car.io.adapter.Measurement;
// import car.io.adapter.MeasurementAdapter;
// import car.io.adapter.MeasurementAdapterLocal;
// import car.io.exception.LocationInvalidException;
//
// import com.actionbarsherlock.app.SherlockListFragment;
//
// public class ListMeasurementsFragment extends ExpandableListFragment {
//
// // DB Adapter for SQLite
//
// private DbAdapter dbAdapter;
//
// // MeasurementAdapter for server connection
//
// private MeasurementAdapter measurementAdapter;
//
// // List with all measurements
//
// private ArrayList<Measurement> allMeasurementsList;
//
// private String[] measurements_short;
// private final int DELETE_ALL = 3;
// private final int UPLOAD_ALL = 4;
//
// /*
// * (non-Javadoc)
// *
// * @see android.app.Activity#onCreate(android.os.Bundle)
// */
// @Override
// public View onCreateView(LayoutInflater inflater, ViewGroup container,
// Bundle savedInstanceState) {
//
// return super.onCreateView(inflater, container, savedInstanceState);
//
// }
//
// @Override
// public void onViewCreated(View view, Bundle savedInstanceState) {
// initDbAdapter();
//
// fillListWithMeasurements();
//
// measurementAdapter = new MeasurementAdapterLocal();
// super.onViewCreated(view, savedInstanceState);
// }
// /*
// * (non-Javadoc)
// *
// * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
// */
// // TODO machen
// /*
// * @Override public boolean onCreateOptionsMenu(Menu menu) { menu.add(0,
// * DELETE_ALL, 0, "Delete All"); menu.add(0, UPLOAD_ALL, 0, "Upload All");
// * return super.onCreateOptionsMenu(menu); }
// */
// /*
// * (non-Javadoc)
// *
// * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
// */
// //TODO MEnü
// /*
// @Override
// public boolean onOptionsItemSelected(MenuItem item) {
// switch (item.getItemId()) {
// case DELETE_ALL:
// dbAdapter.deleteAllMeasurements();
// fillListWithMeasurements();
// return true;
// case UPLOAD_ALL:
// try {
// uploadAllMeasurements();
// dbAdapter.deleteAllMeasurements();
// fillListWithMeasurements();
// } catch (ClientProtocolException e) {
// Toast.makeText(getApplicationContext(), "Upload failed",
// Toast.LENGTH_SHORT).show();
// Log.e("obd2", "upload failed");
// e.printStackTrace();
// } catch (IOException e) {
// Toast.makeText(getApplicationContext(), "Upload failed",
// Toast.LENGTH_SHORT).show();
// Log.e("obd2", "upload failed2");
// e.printStackTrace();
// }
//
// return true;
// }
//
// return super.onOptionsItemSelected(item);
// }
// */
// /**
// * Uploads all local measurements to the server
// *
// * @throws NoServerResponseException
// * @throws IOException
// * @throws ClientProtocolException
// */
//
// private void uploadAllMeasurements() throws ClientProtocolException,
// IOException {
//
// for (Measurement measurement : allMeasurementsList) {
//
// Log.e("obd2", measurement.toString());
//
// measurementAdapter.uploadMeasurement(measurement);
// }
//
// }
//
// /**
// * Helper method that fills the ListActivity's list with all measurement ids
// */
// private void fillListWithMeasurements() {
//
// allMeasurementsList = dbAdapter.getAllMeasurements();
//
// // String List for the list itself
//
// measurements_short = new String[allMeasurementsList.size()];
//
// for (int i = 0; i < measurements_short.length; i++) {
// measurements_short[i] = "Own Measurement "
// + allMeasurementsList.get(i).getId();
// }
//
// if (allMeasurementsList != null) {
//
// // Generate List
//
// //setListAdapter(new
// ArrayAdapter<String>(getActivity(),R.layout.listmeasurements,
// measurements_short));
// setListAdapter((ExpandableListAdapter) new BaseExpandableListAdapter() {
//
// private String[] groups = { "People Names", "Dog Names", "Cat Names",
// "Fish Names" };
//
// private String[][] children = {
// { "Arnold", "Barry", "Chuck", "David" },
// { "Ace", "Bandit", "Cha-Cha", "Deuce" },
// { "Fluffy", "Snuggles" },
// { "Goldy", "Bubbles" }
// };
//
// @Override
// public int getGroupCount() {
// return groups.length;
// }
//
// @Override
// public int getChildrenCount(int i) {
// return children[i].length;
// }
//
// @Override
// public Object getGroup(int i) {
// return groups[i];
// }
//
// @Override
// public Object getChild(int i, int i1) {
// return children[i][i1];
// }
//
// @Override
// public long getGroupId(int i) {
// return i;
// }
//
// @Override
// public long getChildId(int i, int i1) {
// return i1;
// }
//
// @Override
// public boolean hasStableIds() {
// return true;
// }
//
// @Override
// public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
// TextView textView = new
// TextView(ListMeasurementsFragment.this.getActivity());
// textView.setText(getGroup(i).toString());
// return textView;
// }
//
// @Override
// public View getChildView(int i, int i1, boolean b, View view, ViewGroup
// viewGroup) {
// TextView textView = new
// TextView(ListMeasurementsFragment.this.getActivity());
// textView.setText(getChild(i, i1).toString());
// return textView;
// }
//
// @Override
// public boolean isChildSelectable(int i, int i1) {
// return true;
// }
//
// });
// ListView lv = getExpandableListView();
// lv.setTextFilterEnabled(true);
//
// // Start the MeasurementDisplay for the selected entry
//
// lv.setOnItemClickListener(new OnItemClickListener() {
//
// public void onItemClick(AdapterView<?> parent, View view,
// int position, long id) {
//
// String selectedId = ((TextView) view).getText().toString();
//
// String[] split = selectedId.split(" ");
//
// selectedId = split[split.length - 1];
//
// for (Measurement measurement : allMeasurementsList) {
//
// if (Integer.valueOf(selectedId) == measurement.getId()) {
// //TODO measurementsdisplay
//
// Intent i;
//
// // Start the intent and put the selected id into the
// // bundle
// i = new Intent(getActivity(),
// MeasurementDisplay.class);
// Bundle bundle = new Bundle();
// bundle.putString("id",
// String.valueOf(measurement.getId()));
// i.putExtras(bundle);
// startActivity(i);
//
// }
// }
// }
// });
//
// }
//
// }
//
// /**
// * Helper Method to open DB Connection
// */
// private void initDbAdapter() {
// dbAdapter = new DbAdapterLocal(getActivity());
// dbAdapter.open();
// try {
// Measurement m = new Measurement(51.7f, 7.4f);
// dbAdapter.insertMeasurement(m);
// } catch (LocationInvalidException e) {
// // TODO Auto-generated catch block
// e.printStackTrace();
// }
// }
//
//
//
// }
