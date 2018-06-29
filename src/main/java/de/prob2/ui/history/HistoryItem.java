package de.prob2.ui.history;

import de.prob.statespace.Transition;

public class HistoryItem {
	private final Transition transition;
	private final int index;
	
	public HistoryItem(Transition transition, int index) {
		this.transition = transition;
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	public Transition getTransition() {
		return transition;
	}
}
