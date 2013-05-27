package car.io.importedCommands;

import car.io.commands.CommonCommand;

/**
 * This command will turn-off memory.
 */
public class MemoryOff extends CommonCommand {

	/**
	 * @param command
	 */
	public MemoryOff() {
		super("AT M0");
	}

	@Override
	public String getResult() {
		return getRawData();
	}

	@Override
	public String getCommandName() {
		return "Memory Off";
	}

}