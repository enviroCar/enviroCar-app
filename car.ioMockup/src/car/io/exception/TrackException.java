package car.io.exception;

public class TrackException extends Exception {

	/**
	 * Exception for problems with a track.
	 */
	private static final long serialVersionUID = 5516590597842732153L;

	public TrackException() {
		super("Problem with the track.");
	}

	public TrackException(String string) {
		super(string);
	}

}
