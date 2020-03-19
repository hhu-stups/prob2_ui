package de.prob2.ui.visb.exceptions;

/**
 * Created by
 * @author Michelle Werth
 * @since 21.03.2019
 * @version 0.1.0
 *
 * */

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
