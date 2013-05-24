package car.io.importedCommands;

import car.io.commands.CommonCommand;

/**
 * This command will turn-off echo.
 */
public class SpacesOff extends CommonCommand {

	/**
	 * @param command
	 */
	public SpacesOff() {
		super("AT S0");
	}

	@Override
	public String getResult() {
		return getRawData();
	}

	@Override
	public String getCommandName() {
		return "Spaces Off";
	}

}