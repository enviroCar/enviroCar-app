package car.io.importedCommands;

import car.io.commands.CommonCommand;

/**
 * Select the protocol to use.
 */
public class SelectAutoProtocol extends CommonCommand {

	/**
	 * @param command
	 */
	public SelectAutoProtocol() {
		super("AT SP " + 0);
	}

	@Override
	public String getResult() {
		return getRawData();
	}

	@Override
	public String getCommandName() {
		return "Protocol: Auto";
	}

}