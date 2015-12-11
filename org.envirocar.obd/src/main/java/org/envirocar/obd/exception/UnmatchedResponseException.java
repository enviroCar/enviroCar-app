package org.envirocar.obd.exception;

/**
 * Created by matthes on 29.10.15.
 */
public class UnmatchedResponseException extends Exception {

    public UnmatchedResponseException() {
        super("no further information available");
    }

}
