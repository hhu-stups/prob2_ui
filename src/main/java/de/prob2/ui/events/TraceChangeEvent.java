package de.prob2.ui.events;

public class TraceChangeEvent {

	private int index;
	private TraceChangeDirection direction;
	
	public TraceChangeEvent(int index) {
		this.index = index;
		direction = TraceChangeDirection.BYINDEX;
	}
	
	public TraceChangeEvent(TraceChangeDirection direction) {
		this.index = -1;
		this.direction = direction;
	}
	
	public TraceChangeDirection getDirection() {
		return direction;
	}
	
	public int getIndex() {
		return index;
	}
	
}
