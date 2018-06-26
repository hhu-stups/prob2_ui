package de.prob2.ui.history;

import de.prob.statespace.Transition;

public class HistoryItem {

	private final HistoryStatus status;
	private final Transition transition;
	private final int index;
	
	public HistoryItem(Transition transition, HistoryStatus status, int index) {
		this.transition = transition;
		this.status = status;
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	public Transition getTransition() {
		return transition;
	}
	
	public HistoryStatus getStatus() {
		return status;
	}
}
