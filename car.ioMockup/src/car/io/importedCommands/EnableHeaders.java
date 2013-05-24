package car.io.importedCommands;

import car.io.commands.CommonCommand;

/**
 * Turns off line-feed.
 */
public class EnableHeaders extends CommonCommand {

	/**
	 * @param command
	 */
	public EnableHeaders() {
		super("AT H1");
	}

	@Override
	public String getResult() {
		return getRawData();
	}

	@Override
	public String getCommandName() {
		return "Enable Headers";
	}

}