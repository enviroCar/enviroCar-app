package car.io.commands;

/**
 * Intake Manifold Pressure on PID 01 0B
 * 
 * @author jakob
 * 
 */

public class IntakePressure extends CommonCommand {

	public IntakePressure() {
		super("01 0B");
	}

	@Override
	public String getCommandName() {
		return "Intake Manifold Pressure";
	}

	@Override
	public String getResult() {
		String result = getRawData();

		if (!"NODATA".equals(result)) {
			int pressure = buffer.get(2);
			result = String.format("%d%s", pressure, "");

		}

		return result;
	}

}