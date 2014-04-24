package org.rtmplite.simple.restreamer;

public class StartRestreamException extends Exception {
	
	private static final long serialVersionUID = 8757932972917112669L;
	
	private String message;
	
	public StartRestreamException(String message) {
		this.message = message;
	}
	
	@Override
	public String getMessage() {
		return message;
	}
}
