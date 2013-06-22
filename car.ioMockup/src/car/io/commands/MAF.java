package car.io.commands;

/**
 * Mass Air Flow Value PID 01 10
 * 
 * @author jakob
 * 
 */
public class MAF extends CommonCommand {

	public MAF() {
		super("01 10");
	}

	@Override
	public String getResult() {

		float maf = 0.0f;

		if (!"NODATA".equals(getRawData())) {
			int bytethree = buffer.get(2);
			int bytefour = buffer.get(3);
			maf = (bytethree * 256 + bytefour) / 100.0f;
		}

		return String.format("%.2f%s", maf, "");
	}

	@Override
	public String getCommandName() {
		return "Mass Air Flow";
	}
}