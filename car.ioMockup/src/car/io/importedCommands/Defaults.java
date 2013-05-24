package car.io.importedCommands;

import car.io.commands.CommonCommand;

/**
 * Turns off line-feed.
 */
public class Defaults extends CommonCommand {

	/**
	 * @param command
	 */
	public Defaults() {
		super("AT D");
	}

	@Override
	public String getResult() {
		return getRawData();
	}

	@Override
	public String getCommandName() {
		return "Defaults";
	}

}