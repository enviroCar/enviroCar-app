package org.envirocar.app.exception;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class UUIDSanityCheckFailedException extends Exception {

    /**
     * Constructor.
     */
    public UUIDSanityCheckFailedException() {
        super("The UUID sanity check failed or is not supported?!");
    }
}
