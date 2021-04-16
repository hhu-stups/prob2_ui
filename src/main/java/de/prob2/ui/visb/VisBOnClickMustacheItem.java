package de.prob2.ui.visb;

public class VisBOnClickMustacheItem {

	/*
	 * These fields must be public for usage in Mustache templates
	 */
	public String enterAction;
	public String leaveAction;
	public String eventID;
	public String eventName;

	public VisBOnClickMustacheItem(String enterAction, String leaveAction, String eventID, String eventName) {
		this.enterAction = enterAction;
		this.leaveAction = leaveAction;
		this.eventID = eventID;
		this.eventName = eventName;
	}

}
