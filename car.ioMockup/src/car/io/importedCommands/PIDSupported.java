package car.io.importedCommands;

import car.io.commands.CommonCommand;

/**
 * Turns off line-feed.
 */
public class PIDSupported extends CommonCommand {

	/**
	 * @param command
	 */
	public PIDSupported() {
		super("01 00");
	}

	@Override
	public String getResult() {
		return getRawData();
	}

	@Override
	public String getCommandName() {
		return "01 00"; 
	}

}