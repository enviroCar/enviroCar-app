package car.io.importedCommands;

import car.io.commands.CommonCommand;

/**
 * Turns off line-feed.
 */
public class HeadersOff extends CommonCommand {

	/**
	 * @param command
	 */
	public HeadersOff() {
		super("AT H0");
	}

	@Override
	public String getResult() {
		return getRawData();
	}

	@Override
	public String getCommandName() {
		return "Disable Headers";
	}

}