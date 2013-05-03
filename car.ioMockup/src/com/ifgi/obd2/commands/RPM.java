package com.ifgi.obd2.commands;

/**
 * Engine RPM on PID 01 0C
 * 
 * @author jakob
 * 
 */
public class RPM extends CommonCommand {

	public RPM() {
		super("01 0C");
	}

	@Override
	public String getResult() {

		int rpm = -1;

		if (!"NODATA".equals(getRawData())) {
			int bytethree = buffer.get(2);
			int bytefour = buffer.get(3);
			rpm = (bytethree * 256 + bytefour) / 4;
		}

		return String.format("%d%s", rpm, "");
	}

	@Override
	public String getCommandName() {
		return "Engine RPM";
	}

}