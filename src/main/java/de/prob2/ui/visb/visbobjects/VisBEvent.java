package de.prob2.ui.visb.visbobjects;

import java.util.ArrayList;
import de.prob2.ui.visb.visbobjects.VisBHover;

/**
 * The VisBEvent is designed for the JSON / VisB file
 */
public class VisBEvent {
	private String id;
	private String event;
	private ArrayList<String> predicates;
	private VisBHover hover; // TO DO: maybe provide multiple values and allow value to depend on B state


	/**
	 * These two parameters will be mapped to the id of the corresponding svg element
	 * @param id the id of the svg element, that should be clickable
	 * @param event the event has to be an executable operation of the corresponding B machine
	 * @param predicates the predicates have to be the predicates, which are used for the event above
	 */
	public VisBEvent(String id, String event, ArrayList<String> predicates,
	                 VisBHover hover){
		this.id = id;
		this.event = event;
		this.predicates = predicates;
		this.hover = hover;
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

	public boolean hasHover() {  // TO DO: return number of hovers
	    if(hover!=null) return true ; return false;
	}
	
	public String getHoverId() {
		return hover.getHoverId();
	}
	public String getHoverAttr() {
		return hover.getHoverAttr();
	}
	public String getHoverEnterVal() {
		return hover.getHoverEnterVal();
	}
	public String getHoverLeaveVal() {
		return hover.getHoverLeaveVal();
	}

	@Override
	public String toString(){
		return "ID: "+id+"\nEVENT: "+event+"\nPREDICATES: "+predicates+"\n";
	}
}
