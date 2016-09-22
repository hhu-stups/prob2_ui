package de.prob2.ui.groovy;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

@SuppressWarnings("serial")
public class CodeCompletionEvent extends Event {
	
    public static final EventType<CodeCompletionEvent> CODECOMPLETION = new EventType<>(Event.ANY, "CODECOMPLETION");
    
    private Event event;
    private KeyCode code;
    private String choice;
	
	public CodeCompletionEvent(KeyEvent event) {
		super(CODECOMPLETION);
		this.event = event;
		this.code = event.getCode();
	}
	
	public CodeCompletionEvent(KeyEvent event, String choice) {
		super(CODECOMPLETION);
		this.event = event;
		this.code = event.getCode();
		this.choice = choice;
	}
	
	public CodeCompletionEvent(MouseEvent event) {
		super(CODECOMPLETION);
		this.event = event;
		this.code = null;
	}
	
	public Event getEvent() {
		return event;
	}
	
	public KeyCode getCode() {
		return code;
	}
	
	public String getChoice() {
		return choice;
	}
	
}
