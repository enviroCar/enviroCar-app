package org.envirocar.core.exception;

public class ServerException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ServerException(Exception e) {
		super(e);
	}

	public ServerException(String string) {
		super(string);
	}

}
