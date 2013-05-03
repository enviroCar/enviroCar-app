package com.ifgi.obd2.importedCommands;

import com.ifgi.obd2.commands.CommonCommand;

/**
 * This command will turn-off echo.
 */
public class EchoOff extends CommonCommand {

	/**
	 * @param command
	 */
	public EchoOff() {
		super("AT E0");
	}

	@Override
	public String getResult() {
		return getRawData();
	}

	@Override
	public String getCommandName() {
		return "Echo Off";
	}

}