package car.io.importedCommands;

import car.io.commands.CommonCommand;

/**
 * Turns off line-feed.
 */
public class Ok extends CommonCommand {

	/**
	 * @param command
	 */
	public Ok() {
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