package de.prob2.ui.consoles.groovy.codecompletion;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

@SuppressWarnings("serial")
public class CodeCompletionEvent extends Event {
    public static final EventType<CodeCompletionEvent> CODECOMPLETION = new EventType<>(Event.ANY, "CODECOMPLETION");

    private final Event event;
    private final String choice;
    private KeyCode code;
    private String currentSuggestion;

    public CodeCompletionEvent(Event event, String choice, String suggestion) {
        super(CODECOMPLETION);
        this.event = event;
        this.choice = choice;
        this.code = null;
        this.currentSuggestion = suggestion;
        if (event instanceof KeyEvent) {
            this.code = ((KeyEvent) event).getCode();
        }
    }

    public CodeCompletionEvent(Event event) {
        super(CODECOMPLETION);
        this.event = event;
        this.choice = "";
        this.code = null;
        if (event instanceof KeyEvent) {
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

    public String getCurrentSuggestion() {
        return currentSuggestion;
    }
}
