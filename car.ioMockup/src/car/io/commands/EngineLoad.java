package car.io.commands;

/**
 * EngineLoad Value on PID 01 04
 * 
 * @author jakob
 * 
 */
public class EngineLoad extends CommonCommand {

	/**
	 * Create the Command
	 */
	public EngineLoad() {
		super("01 04");
	}

	@Override
	public String getCommandName() {
		return "Engine Load";
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