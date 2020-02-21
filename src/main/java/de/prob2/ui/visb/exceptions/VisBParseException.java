package de.prob2.ui.visb.exceptions;

/**
 * Created by
 * @author Michelle Werth
 * @since 21.03.2019
 * @version 0.1.0
 *
 * */

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
