package de.prob2.ui.visb.exceptions;

public class VisBParseException extends Exception {
	private static final long serialVersionUID = 1L;

	private String message;

	public VisBParseException(){
	}

	public VisBParseException(String message){
		this.message = message;
	}

	public String getMessage(){
		return message;
	}

}
