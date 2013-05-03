package com.ifgi.obd2.obd;

import android.view.View;

import com.ifgi.obd2.commands.CommonCommand;

/**
 * Interface that listens for updates from the current obd job
 * 
 * @author jakob
 * 
 */

public interface Listener {


	void receiveUpdate(CommonCommand currentJob);

	void setView(View view);

}