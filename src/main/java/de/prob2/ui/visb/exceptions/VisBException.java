package de.prob2.ui.visb.exceptions;

public class VisBException extends Exception {
	private String message;

	public VisBException(){}

	public VisBException(String message){
		this.message = message;
	}

	public String getMessage(){
		return message;
	}
}
