package de.prob2.ui.visb.visbobjects;

import java.util.List;

/**
 * The VisBEvent is designed for the JSON / VisB file
 */
public class VisBEvent {
	private String id;
	private String event;
	private Boolean eventIsOptional; // do not complain if the event does not exist
	private List<String> predicates;
	private List<VisBHover> hovers; // TO DO: maybe provide multiple values and allow value to depend on B state


	/**
	 * These two parameters will be mapped to the id of the corresponding svg element
	 * @param id the id of the svg element, that should be clickable
	 * @param event the event has to be an executable operation of the corresponding B machine
	 * @param predicates the predicates have to be the predicates, which are used for the event above
	 */
	public VisBEvent(String id, Boolean eventIsOptional,
	                 String event, List<String> predicates,
	                 List<VisBHover> hovers){
		this.id = id;
		this.eventIsOptional = eventIsOptional;
		this.event = event;
		this.predicates = predicates;
		this.hovers = hovers;
	}

	public String getEvent() {
		return event;
	}

	public Boolean eventIsOptional() {
		return eventIsOptional;
	}

	public List<String> getPredicates() {
		return predicates;
	}

	public String getId() {
		return id;
	}
	
	public List<VisBHover> getHovers() {
		return hovers;
	}

	@Override
	public String toString(){
		return "ID: "+id+"\nEVENT: "+event+"\nPREDICATES: "+predicates+"\n";
	}
}
