package car.io.commands;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Abstract command class that the other commands have to extend. Many things
 * are imported from Android OBD Reader project!
 * 
 * @author jakob
 * 
 */
public abstract class CommonCommand {

	protected ArrayList<Integer> buffer = null;
	protected String command = null;
	protected String rawData = null;
	private Long commandId;
	private CommonCommandState commandState;

	/**
	 * Default constructor to use
	 * 
	 * @param command
	 *            the command to send
	 */
	public CommonCommand(String command) {
		this.command = command;
		setCommandState(CommonCommandState.NEW);
		this.buffer = new ArrayList<Integer>();
	}

	/**
	 * The state of the command.
	 */
	public enum CommonCommandState {
		NEW, RUNNING, FINISHED, EXECUTION_ERROR, QUEUE_ERROR
	}

	/**
	 * Sends the OBD-II request and deals with the response.
	 * 
	 * This method CAN be overriden in fake commands.
	 */
	public void run(InputStream in, OutputStream out) throws IOException,
			InterruptedException {
		sendCommand(out);
		readResult(in);
	}

	/**
	 * Sends the OBD-II request.
	 * 
	 * This method may be overriden in subclasses, such as ObMultiCommand or
	 * TroubleCodesObdCommand.
	 * 
	 * @param command
	 *            The command to send.
	 */
	protected void sendCommand(OutputStream outputStream) throws IOException,
			InterruptedException {
		// add the carriage return char
		command += "\r";

		// write to OutputStream, or in this case a BluetoothSocket
		outputStream.write(command.getBytes());
		outputStream.flush();

		Thread.sleep(200);
	}

	/**
	 * Resends this command.
	 */
	protected void resendCommand(OutputStream outputStream) throws IOException,
			InterruptedException {
		outputStream.write("\r".getBytes());
		outputStream.flush();
	}

	/**
	 * Reads the OBD-II response.
	 */
	protected void readResult(InputStream inputStream) throws IOException {
		byte b = 0;
		StringBuilder stringbuilder = new StringBuilder();

		// read until '>' arrives
		while ((char) (b = (byte) inputStream.read()) != '>')
			if ((char) b != ' ')
				stringbuilder.append((char) b);

		rawData = stringbuilder.toString().trim();

		// clear buffer
		buffer.clear();

		// read string each two chars
		int begin = 0;
		int end = 2;
		while (end <= rawData.length()) {
			String temp = "0x" + rawData.substring(begin, end);
			buffer.add(Integer.decode(temp));
			begin = end;
			end += 2;
		}
	}

	/**
	 * @return the raw command response in string representation.
	 */
	//TODO null pointer when car is off...
	
	public String getRawData() {
		if (rawData.contains("SEARCHING") || rawData.contains("DATA")) {
			rawData = "NODATA";
		}

		return rawData;
	}

	/**
	 * Returns this command in string representation.
	 * 
	 * @return the command
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * @return the OBD command name.
	 */
	public abstract String getCommandName();

	/**
	 * @return a formatted command response in string representation.
	 */
	public abstract String getResult();

	/**
	 * @return the commandId
	 */
	public Long getCommandId() {
		return commandId;
	}

	/**
	 * @param commandId
	 *            the commandId to set
	 */
	public void setCommandId(Long commandId) {
		this.commandId = commandId;
	}

	/**
	 * @return the commandState
	 */
	public CommonCommandState getCommandState() {
		return commandState;
	}

	/**
	 * @param commandState
	 *            the commandState to set
	 */
	public void setCommandState(CommonCommandState commandState) {
		this.commandState = commandState;
	}

}