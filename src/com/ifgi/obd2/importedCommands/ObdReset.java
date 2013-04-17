package com.ifgi.obd2.importedCommands;

import java.io.IOException;
import java.io.InputStream;

import com.ifgi.obd2.commands.CommonCommand;

/**
 * This method will reset the OBD connection.
 */
public class ObdReset extends CommonCommand {

	public ObdReset() {
		super("AT Z");
	}

	/**
	 * Reset command returns an empty string, so we must override the following
	 * two methods.
	 * 
	 * @throws IOException
	 */
	@Override
	public void readResult(InputStream in) throws IOException {
		return;
	}

	@Override
	public String getRawData() {
		return "";
	}

	@Override
	public String getResult() {
		return getRawData();
	}

	@Override
	public String getCommandName() {
		return "Reset OBD";
	}

}