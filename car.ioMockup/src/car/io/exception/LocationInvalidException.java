package car.io.exception;

/**
 * Exception that is thrown when the location cannot be correct (latitude AND
 * longitude equal 0.0... this is somewhere in the ocean and therefore usually
 * an indicator that the GPS is not synced yet)f
 * 
 * @author jakob
 * 
 */

public class LocationInvalidException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -630826885585474670L;

	public LocationInvalidException() {
		super(
				"Location Coordinates are invalid. Did you turn on GPS? Do you have a connection?");
	}
}
