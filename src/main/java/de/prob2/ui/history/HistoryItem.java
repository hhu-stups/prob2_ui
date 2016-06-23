package de.prob2.ui.history;

import de.prob.statespace.Transition;

public class HistoryItem {

	public final HistoryStatus status;
	public final Transition transition;
	public final boolean root;
	
	public HistoryItem(HistoryStatus status) {
		this.status = status;
		this.transition = null;
		this.root = true;
	}

	public HistoryItem(Transition transition, HistoryStatus status) {
		this.transition = transition;
		this.status = status;
		this.root = false;
	}


	
}
