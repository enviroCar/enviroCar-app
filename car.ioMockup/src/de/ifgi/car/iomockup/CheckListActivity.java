package de.ifgi.car.iomockup;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class CheckListActivity extends SherlockActivity implements OnClickListener {
	
	
	public static final class TYPEFACE {
	    public static final Typeface Raleway(Context ctx){
	        Typeface typeface = Typeface.createFromAsset(ctx.getAssets(), "Raleway-Regular.ttf");
	        return typeface;
	    }
	    public static final Typeface Newscycle(Context ctx){
	        Typeface typeface = Typeface.createFromAsset(ctx.getAssets(), "newscycle_regular.ttf");
	        return typeface;
	    }
	    public static void applyCustomFont(ViewGroup list, Typeface customTypeface) {
            for (int i = 0; i < list.getChildCount(); i++) {
                View view = list.getChildAt(i);
                if (view instanceof ViewGroup) {
                    applyCustomFont((ViewGroup) view, customTypeface);
                } else if (view instanceof TextView) {
                    ((TextView) view).setTypeface(customTypeface);
                }
            }
        }	    
	} 
	

	private ActionBar actionBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.checklist_layout);

		actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		View rootView = findViewById(R.id.checklist_root_layout);
		TYPEFACE.applyCustomFont((ViewGroup)rootView, TYPEFACE.Newscycle(this));
		
		((TextView) this.findViewById(R.id.welcome)).setTypeface(TYPEFACE.Raleway(this));
		
		this.findViewById(R.id.fourthItem).setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {

	    case android.R.id.home:
	         //finish();
	    	NavUtils.navigateUpFromSameTask(this);
	         return true;


	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	public void onClick(View arg0) {
		//up until now, just the login activity is implemented, omitting the switch block
		startActivity(new Intent(CheckListActivity.this, LoginActivity.class));
	}
	

}