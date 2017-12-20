package de.prob2.ui.history;

import de.prob.statespace.Transition;

public class HistoryItem {

	public final HistoryStatus status;
	public final Transition transition;
	public final int index;
	
	public HistoryItem(HistoryStatus status, int i) {
		this(null, status, i);
	}

	public HistoryItem(Transition transition, HistoryStatus status, int i) {
		this.transition = transition;
		this.status = status;
		this.index = i;
	}
}
