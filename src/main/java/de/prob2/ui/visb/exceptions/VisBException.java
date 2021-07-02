package de.prob2.ui.visb.exceptions;

public class VisBException extends Exception {
	private static final long serialVersionUID = 1L;

	private String message;

	public VisBException(String message){
		this.message = message;
	}

	public String getMessage(){
		return message;
	}
}
