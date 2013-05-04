package car.io.commands;

/**
 * Intake temperature on PID 01 0F
 * 
 * @author jakob
 * 
 */
public class IntakeTemperature extends CommonCommand {

	public IntakeTemperature() {
		super("01 0F");
	}

	@Override
	public String getCommandName() {
		return "Air Intake Temperature";
	}

	@Override
	public String getResult() {
		String result = getRawData();

		if (!"NODATA".equals(result)) {
			float temperature = buffer.get(2) - 40;
			result = String.format("%.0f%s", temperature, "");
		}

		return result;
	}

}