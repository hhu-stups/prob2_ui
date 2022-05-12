package de.prob2.ui.visb.exceptions;

public class VisBNestedException extends Exception {
	private static final long serialVersionUID = 1L;

	private String message;
	private Exception innerException;

	public VisBNestedException(){
	}

	public VisBNestedException(String message, Exception innerException){
		this.message = message;
		this.innerException = innerException;
	}

	@Override
	public String getMessage(){
		return message + innerException.getMessage();
	}

}
