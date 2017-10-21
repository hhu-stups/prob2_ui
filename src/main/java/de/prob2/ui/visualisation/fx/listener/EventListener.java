package de.prob2.ui.visualisation.fx.listener;

/**
 * Description of class
 *
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 26.09.17
 */
public abstract class EventListener {

    private final String event;

    public EventListener(String event) {
        this.event = event;
    }

    public String getEvent() {
        return event;
    }

    public abstract void eventExcecuted();

}
