package car.io.commands;

/**
 * Short Term Trim (Cylinder) Bank 1, PID 01 06
 * 
 * @author jakob
 * 
 */
public class ShortTermTrimBank1 extends CommonCommand {

	public ShortTermTrimBank1() {
		super("01 06");
	}

	@Override
	public String getResult() {

		float fuelTrimValue = 0.0f;

		if (!"NODATA".equals(getRawData())) {
			float tmpValue = buffer.get(2);
			Double perc = (tmpValue - 128) * (100.0 / 128);
			fuelTrimValue = Float.parseFloat(perc.toString());
		}

		return String.format("%.2f%s", fuelTrimValue, "");
	}

	@Override
	public String getCommandName() {

		return "Short Term Fuel Trim Bank 1";
	}

}