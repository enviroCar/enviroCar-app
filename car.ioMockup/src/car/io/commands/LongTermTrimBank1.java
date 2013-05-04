package car.io.commands;

/**
 * Long Term Fuel Trim (Cylinder) Bank 1 (PID 01 07)
 * 
 * @author jakob
 * 
 */
public class LongTermTrimBank1 extends CommonCommand {

	public LongTermTrimBank1() {
		super("01 07");
	}

	@Override
	public String getResult() {

		float fuelTrimValue = 0.0f;

		if (!"NODATA".equals(getRawData())) {
			int tmpValue = buffer.get(2);
			Double perc = (tmpValue - 128) * (100.0 / 128);
			fuelTrimValue = Float.parseFloat(perc.toString());
		}

		return String.format("%.2f%s", fuelTrimValue, "");
	}

	@Override
	public String getCommandName() {

		return "Long Term Fuel Trim Bank 1";
	}

}