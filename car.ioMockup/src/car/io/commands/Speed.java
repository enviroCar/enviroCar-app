package car.io.commands;

/**
 * Speed Command PID 01 0D
 * 
 * @author jakob
 * 
 */
public class Speed extends CommonCommand {

	public Speed() {
		super("01 0D");
	}

	@Override
	public String getResult() {
		String result = getRawData();

		if (!"NODATA".equals(result)) {
			Integer metricSpeed = buffer.get(2);
			result = String.format("%d%s", metricSpeed, "");

		}

		return result;
	}

	@Override
	public String getCommandName() {
		return "Vehicle Speed";
	}

}