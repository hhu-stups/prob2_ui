package de.prob2.ui.visb.exceptions;

public class VisBParseException extends Exception {
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
