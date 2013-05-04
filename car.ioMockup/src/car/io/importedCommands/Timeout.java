package car.io.importedCommands;

import car.io.commands.CommonCommand;

/**
 * This will set the value of time in milliseconds (ms) that the OBD interface
 * will wait for a response from the ECU. If exceeds, the response is "NO DATA".
 */
public class Timeout extends CommonCommand {

	/**
	 * @param a
	 *            value between 0 and 255 that multiplied by 4 results in the
	 *            desired timeout in milliseconds (ms).
	 */
	public Timeout(int timeout) {
		super("AT ST " + Integer.toHexString(0xFF & timeout));
	}

	@Override
	public String getResult() {
		return getRawData();
	}

	@Override
	public String getCommandName() {
		return "Timeout";
	}

}