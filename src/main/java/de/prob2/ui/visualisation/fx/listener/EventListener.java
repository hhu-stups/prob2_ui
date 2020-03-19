package de.prob2.ui.visualisation.fx.listener;

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
