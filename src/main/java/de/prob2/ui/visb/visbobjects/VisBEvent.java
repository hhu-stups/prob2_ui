package de.prob2.ui.visb.visbobjects;

import java.util.ArrayList;

/**
 * The VisBEvent is designed for the JSON / VisB file
 */
public class VisBEvent {
	private String id;
	private String event;
	private ArrayList<String> predicates;
	private String hover_id; // id of the object whose attribute is modified upon hover
	private String hover_attribute; // TO DO: maybe provide multiple values and allow value to depend on B state
	private String hover_enter_value;
	private String hover_leave_value;

	/**
	 * These two parameters will be mapped to the id of the corresponding svg element
	 * @param id the id of the svg element, that should be clickable
	 * @param event the event has to be an executable operation of the corresponding B machine
	 * @param predicates the predicates have to be the predicates, which are used for the event above
	 */
	public VisBEvent(String id, String event, ArrayList<String> predicates,
	                 String hoverid, String attr, String enter, String leave){
		this.id = id;
		this.event = event;
		this.predicates = predicates;
		this.hover_id = hoverid;
		this.hover_attribute = attr;
		this.hover_enter_value = enter;
		this.hover_leave_value = leave;
	}

	public String getEvent() {
		return event;
	}

	public ArrayList<String> getPredicates() {
		return predicates;
	}

	public String getId() {
		return id;
	}

	public String getHoverId() {
		return hover_id;
	}
	public String getHoverAttr() {
		return hover_attribute;
	}
	public String getHoverEnterVal() {
		return hover_enter_value;
	}
	public String getHoverLeaveVal() {
		return hover_leave_value;
	}

	@Override
	public String toString(){
		return "ID: "+id+"\nEVENT: "+event+"\nPREDICATES: "+predicates+"\n";
	}
}
