package car.io.obd;

import car.io.commands.CommonCommand;

/**
 * Interface that listens for updates from the current obd job
 * 
 * @author jakob
 * 
 */

public interface Listener {


	void receiveUpdate(CommonCommand currentJob);


}