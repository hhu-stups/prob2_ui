package de.prob2.ui.groovy;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

@SuppressWarnings("serial")
public class CodeCompletionEvent extends Event {
	
    public static final EventType<CodeCompletionEvent> CODECOMPLETION = new EventType<>(Event.ANY, "CODECOMPLETION");
    
    private Event event;
    private KeyCode code;
    private String choice;
	

	public CodeCompletionEvent(Event event, String choice) {
		super(CODECOMPLETION);
		this.event = event;
		this.choice = choice;
		this.code = null;
		if(event instanceof KeyEvent) {
			this.code = ((KeyEvent) event).getCode();
		}
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
