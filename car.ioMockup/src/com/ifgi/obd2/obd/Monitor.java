package com.ifgi.obd2.obd;

import com.ifgi.obd2.commands.CommonCommand;

/**
 * Interface that adds jobs to the waiting list and executes it
 * 
 * @author jakob
 * 
 */
public interface Monitor {

	void setListener(Listener listener);

	boolean isRunning();

	void executeWaitingList();

	void newJobToWaitingList(CommonCommand newJob);

}