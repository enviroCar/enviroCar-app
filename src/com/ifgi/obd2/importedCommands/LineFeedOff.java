package com.ifgi.obd2.importedCommands;

import com.ifgi.obd2.commands.CommonCommand;

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