//package car.io.activity;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import android.os.Bundle;
//import android.support.v4.app.Fragment;
//import android.support.v4.app.FragmentPagerAdapter;
//import android.support.v4.app.FragmentTransaction;
//import android.support.v4.view.ViewPager;
//import android.util.Log;
//import car.io.R;
//
//import com.actionbarsherlock.app.ActionBar;
//import com.actionbarsherlock.app.ActionBar.Tab;
//import com.actionbarsherlock.app.ActionBar.TabListener;
//import com.actionbarsherlock.app.SherlockFragmentActivity;
//
//public abstract class SwipeableFragmentActivity extends
//		SherlockFragmentActivity {
//
//	private static class TabsAdapter extends FragmentPagerAdapter implements
//			TabListener, ViewPager.OnPageChangeListener {
//
//		private static class TabInfo {
//			public final Class<? extends Fragment> fragmentClass;
//
//			public TabInfo(Class<? extends Fragment> fragmentClass) {
//				this.fragmentClass = fragmentClass;
//			}
//		}
//
//		private final SherlockFragmentActivity mActivity;
//		private final ActionBar mActionBar;
//
//		private final ViewPager mPager;
//
//		private List<TabInfo> mTabs = new ArrayList<TabInfo>();
//
//		/**
//		 * @param fm
//		 * @param fragments
//		 */
//		public TabsAdapter(SherlockFragmentActivity activity, ViewPager pager) {
//			super(activity.getSupportFragmentManager());
//			this.mActivity = activity;
//			this.mActionBar = activity.getSupportActionBar();
//			this.mPager = pager;
//
//		}
//
//		public void addTab(CharSequence title,
//				Class<? extends Fragment> fragmentClass) {
//			final TabInfo tabInfo = new TabInfo(fragmentClass);
//
//			Tab tab = mActionBar.newTab();
//			tab.setText(title);
//			tab.setTabListener(this);
//			tab.setTag(tabInfo);
//
//			mTabs.add(tabInfo);
//
//			mActionBar.addTab(tab);
//			notifyDataSetChanged();
//		}
//
//		@Override
//		public int getCount() {
//			return mTabs.size();
//		}
//
//		@Override
//		public Fragment getItem(int position) {
//			final TabInfo tabInfo = mTabs.get(position);
//			Fragment f = Fragment.instantiate(mActivity,
//					tabInfo.fragmentClass.getName());
//			return f;
//		}
//
//		@Override
//		public CharSequence getPageTitle(int position) {
//			return mActionBar.getTabAt(position).getText();
//		}
//
//		@Override
//		public void onPageScrolled(int arg0, float arg1, int arg2) {
//		}
//
//		@Override
//		public void onPageScrollStateChanged(int arg0) {
//		}
//
//		@Override
//		public void onPageSelected(int position) {
//			/*
//			 * Select tab when user swiped
//			 */
//			mActionBar.setSelectedNavigationItem(position);
//		}
//
//		@Override
//		public void onTabReselected(Tab tab, FragmentTransaction ft) {
//		}
//
//		@Override
//		public void onTabSelected(Tab tab, FragmentTransaction ft) {
//			/*
//			 * Slide to selected fragment when user selected tab
//			 */
//			TabInfo tabInfo = (TabInfo) tab.getTag();
//			for (int i = 0; i < mTabs.size(); i++) {
//				if (mTabs.get(i) == tabInfo) {
//					mPager.setCurrentItem(i);
//				}
//			}
//			Log.i("tabs", "onTabSelected");
//		}
//
//		@Override
//		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
//		}
//	}
//
//	protected ViewPager viewPager;
//
//	private TabsAdapter tabsAdapter;
//
//	/**
//	 * Add a tab with a backing Fragment to the action bar
//	 * 
//	 * @param titleRes
//	 *            A string to be used as the title for the tab
//	 * @param fragmentClass
//	 *            The class of the Fragment to instantiate for this tab
//	 * @param args
//	 *            An optional Bundle to pass along to the Fragment (may be null)
//	 */
//	protected void addTab(CharSequence title,
//			Class<? extends Fragment> fragmentClass) {
//		tabsAdapter.addTab(title, fragmentClass);
//	}
//
//	/**
//	 * Add a tab with a backing Fragment to the action bar
//	 * 
//	 * @param titleRes
//	 *            A string resource pointing to the title for the tab
//	 * @param fragmentClass
//	 *            The class of the Fragment to instantiate for this tab
//	 * @param args
//	 *            An optional Bundle to pass along to the Fragment (may be null)
//	 */
//	protected void addTab(int titleRes, Class<? extends Fragment> fragmentClass) {
//		tabsAdapter.addTab(getString(titleRes), fragmentClass);
//	}
//
//	/** WARNING */
//	protected void setSelectedTab(int i) {
//		viewPager.setCurrentItem(i);
//	}
//
//	protected Fragment getFragmentByPosition(int pos) {
//		String tag = "android:switcher:" + viewPager.getId() + ":" + pos;
//		return getSupportFragmentManager().findFragmentByTag(tag);
//	}
//
//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.main_layout);
//
//		viewPager = (ViewPager) findViewById(R.id.pager);
//
//		tabsAdapter = new TabsAdapter(this, viewPager);
//		viewPager.setAdapter(tabsAdapter);
//		viewPager.setOnPageChangeListener(tabsAdapter);
//
//	}
//}