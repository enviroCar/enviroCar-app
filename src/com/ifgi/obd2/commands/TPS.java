package com.ifgi.obd2.commands;

/**
 * Throttle position on PID 01 11
 * 
 * @author jakob
 * 
 */
public class TPS extends CommonCommand {

	public TPS() {
		super("01 11");
	}

	@Override
	public String getCommandName() {
		return "Throttle Position";
	}

	@Override
	public String getResult() {
		String result = getRawData();

		if (!"NODATA".equals(result)) {
			float tempValue = (buffer.get(2) * 100.0f) / 255.0f;
			result = String.format("%.1f%s", tempValue, "");
		}
		return result;
	}

}