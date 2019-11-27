package org.envirocar.obd.exception;

/**
 * @author dewall
 */
public class EngineNotRunningException extends Exception {
    private static final long serialVersionUID = 1L;

    public EngineNotRunningException(String string) {
        super(string);
    }
}
