package car.io.obd;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import car.io.commands.CommonCommand;

/**
 * Connector Class for bluetooth service. Partly imported from Android OBD
 * Project
 * 
 * @author jakob
 * 
 */
public class ServiceConnector implements ServiceConnection {

	private Monitor localMonitor = null;
	private Listener localListener = null;

	public void onServiceConnected(ComponentName componentName, IBinder binder) {
		localMonitor = (Monitor) binder;
		localMonitor.setListener(localListener);
	}

	public void onServiceDisconnected(ComponentName name) {
		localMonitor = null;
	}

	/**
	 * Check whether service is running
	 * 
	 * @return True if running
	 */
	public boolean isRunning() {
		if (localMonitor == null) {
			return false;
		}

		return localMonitor.isRunning();
	}

	/**
	 * Add a new CommandJob to the waiting List
	 * 
	 * @param newJob
	 *            New CommandJob
	 */
	public void addJobToWaitingList(CommonCommand newJob) {
		if (null != localMonitor)
			localMonitor.newJobToWaitingList(newJob);
	}

	/**
	 * Set the Local Listener
	 * 
	 * @param listener
	 */
	public void setServiceListener(Listener listener) {
		localListener = listener;
	}

}