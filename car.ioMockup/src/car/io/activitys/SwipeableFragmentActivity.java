package car.io.activitys;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import car.io.R;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public abstract class SwipeableFragmentActivity extends
		SherlockFragmentActivity {

	private static class TabsAdapter extends FragmentPagerAdapter implements
			TabListener, ViewPager.OnPageChangeListener {

		private static class TabInfo {
			public final Class<? extends Fragment> fragmentClass;
			public final Bundle args;

			public TabInfo(Class<? extends Fragment> fragmentClass, Bundle args) {
				this.fragmentClass = fragmentClass;
				this.args = args;
			}
		}

		private final SherlockFragmentActivity mActivity;
		private final ActionBar mActionBar;

		private final ViewPager mPager;

		private List<TabInfo> mTabs = new ArrayList<TabInfo>();

		/**
		 * @param fm
		 * @param fragments
		 */
		public TabsAdapter(SherlockFragmentActivity activity, ViewPager pager) {
			super(activity.getSupportFragmentManager());
			this.mActivity = activity;
			this.mActionBar = activity.getSupportActionBar();
			this.mPager = pager;

			//mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		}

		public void addTab(CharSequence title,
				Class<? extends Fragment> fragmentClass, Bundle args) {
			final TabInfo tabInfo = new TabInfo(fragmentClass, args);

			Tab tab = mActionBar.newTab();
			tab.setText(title);
			tab.setTabListener(this);
			tab.setTag(tabInfo);

			mTabs.add(tabInfo);

			mActionBar.addTab(tab);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mTabs.size();
		}

		@Override
		public Fragment getItem(int position) {
			final TabInfo tabInfo = mTabs.get(position);
			return Fragment.instantiate(mActivity,
					tabInfo.fragmentClass.getName(), tabInfo.args);
		}
		@Override
		public CharSequence getPageTitle(int position) {
			return mTabs.get(position).args.getCharSequence("wat");
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}

		@Override
		public void onPageSelected(int position) {
			/*
			 * Select tab when user swiped
			 */
			mActionBar.setSelectedNavigationItem(position);
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			/*
			 * Slide to selected fragment when user selected tab
			 */
			TabInfo tabInfo = (TabInfo) tab.getTag();
			for (int i = 0; i < mTabs.size(); i++) {
				if (mTabs.get(i) == tabInfo) {
					mPager.setCurrentItem(i);
				}
			}
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}
	}

	protected ViewPager viewPager;

	private TabsAdapter tabsAdapter;

	/**
	 * Add a tab with a backing Fragment to the action bar
	 * 
	 * @param titleRes
	 *            A string to be used as the title for the tab
	 * @param fragmentClass
	 *            The class of the Fragment to instantiate for this tab
	 * @param args
	 *            An optional Bundle to pass along to the Fragment (may be null)
	 */
	protected void addTab(CharSequence title,
			Class<? extends Fragment> fragmentClass, Bundle args) {
		tabsAdapter.addTab(title, fragmentClass, args);
	}

	/**
	 * Add a tab with a backing Fragment to the action bar
	 * 
	 * @param titleRes
	 *            A string resource pointing to the title for the tab
	 * @param fragmentClass
	 *            The class of the Fragment to instantiate for this tab
	 * @param args
	 *            An optional Bundle to pass along to the Fragment (may be null)
	 */
	protected void addTab(int titleRes,
			Class<? extends Fragment> fragmentClass, Bundle args) {
		tabsAdapter.addTab(getString(titleRes), fragmentClass, args);
	}
	/**WARNING */
	protected void setSelectedTab(int i){
		viewPager.setCurrentItem(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
	
		viewPager = (ViewPager) findViewById(R.id.pager);

		tabsAdapter = new TabsAdapter(this, viewPager);
		viewPager.setAdapter(tabsAdapter);
		viewPager.setOnPageChangeListener(tabsAdapter);

	}
}