package car.io.activity;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import car.io.R;
import car.io.views.TYPEFACE;

import com.actionbarsherlock.app.SherlockFragment;

public class ListFriends extends SherlockFragment {

	public View onCreateView(android.view.LayoutInflater inflater,
			android.view.ViewGroup container,
			android.os.Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.friends_list, null);
		ExpandableListView elv = (ExpandableListView) v
				.findViewById(R.id.friends_list);
		elv.setAdapter(new FriendsListAdapter());
		elv.setGroupIndicator(getResources().getDrawable(
				R.drawable.list_indicator));
		elv.setChildDivider(getResources().getDrawable(
				android.R.color.transparent));

		return v;
	};

	public class FriendsListAdapter extends BaseExpandableListAdapter {

		private String[] groups = { "Max Mustermann", "Petra Musterfrau",
				"Dr. Achim Musterdoktor", "Prof. Luise Musterprofessorin" };

		private String[][] children = {
				{ "BMW 535d", "0 Nachrichten", "Umweltsünder" },
				{ "VW Golf III", "1 Nachricht", "Durchschnittlich" },
				{ "MB W221", "2 Nachrichten", "Bleifuß" },
				{ "BMW 114i", "0 Nachrichten", "Sehr umweltbewusst" }, };

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
			return children[i][i1];
		}

		@Override
		public long getGroupId(int i) {
			return i;
		}

		@Override
		public long getChildId(int i, int i1) {
			return i;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public View getGroupView(int i, boolean b, View view,
				ViewGroup viewGroup) {
			if (view == null || view.getId() != 10000000 + i) {
				View groupRow = ViewGroup.inflate(getActivity(),
						R.layout.list_tracks_group_layout, null);
				TextView textView = (TextView) groupRow
						.findViewById(R.id.track_name_textview);
				textView.setText(getGroup(i).toString());
				Button button = (Button) groupRow
						.findViewById(R.id.track_name_go_to_map);
				button.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
//						Intent intent = new Intent(getActivity()
//								.getApplicationContext(), Map.class);
//						startActivity(intent);
//						Log.i("bla", "bla");

					}
				});
				groupRow.setId(10000000 + i);
				TYPEFACE.applyCustomFont((ViewGroup) groupRow,
						TYPEFACE.Newscycle(getActivity()));
				return groupRow;
			}
			return view;
		}

		@Override
		public View getChildView(int i, int i1, boolean b, View view,
				ViewGroup viewGroup) {
			if (view == null || view.getId() != 10000100 + i + i1) {
				View row = ViewGroup.inflate(getActivity(),
						R.layout.friends_item, null);
				TextView car = (TextView) row
						.findViewById(R.id.textViewCar);
				TextView message = (TextView) row
						.findViewById(R.id.textViewMessages);
				TextView performance = (TextView) row
						.findViewById(R.id.textViewPerformance);

				car.setText(getChild(i, 0).toString());
				message.setText(getChild(i, 1).toString());
				performance.setText(getChild(i, 2).toString());

				row.setId(10000100 + i + i1);
				TYPEFACE.applyCustomFont((ViewGroup) row,
						TYPEFACE.Newscycle(getActivity()));
				return row;
			}
			return view;
		}

		@Override
		public boolean isChildSelectable(int i, int i1) {
			return false;
		}

	}

}