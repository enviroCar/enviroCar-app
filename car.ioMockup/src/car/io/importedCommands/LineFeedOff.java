package car.io.importedCommands;

import car.io.commands.CommonCommand;

/**
 * Turns off line-feed.
 */
public class LineFeedOff extends CommonCommand {

	/**
	 * @param command
	 */
	public LineFeedOff() {
		super("AT L0");
	}

	@Override
	public String getResult() {
		return getRawData();
	}

	@Override
	public String getCommandName() {
		return "Line Feed Off";
	}

}