package de.prob2.ui.history;

import java.util.ArrayList;
import java.util.List;

import de.prob.statespace.Trace;

import de.prob.statespace.TraceElement;
import de.prob2.ui.operations.OperationItem;

public class HistoryItem {
	private final Trace trace;
	private final int index;
	
	public HistoryItem(final Trace trace, final int index) {
		this.trace = trace;
		this.index = index;
	}

	public static HistoryItem extractItem(final Trace trace, int index) {
		return new HistoryItem(trace, index);
	}
	
	public static List<HistoryItem> itemsForTrace(final Trace trace) {
		final List<HistoryItem> items = new ArrayList<>();
		for (int i = -1; i <= trace.getHead().getIndex(); i++) {
			items.add(extractItem(trace, i));
		}
		return items;
	}
	
	public Trace getTrace() {
		return this.trace;
	}
	
	public int getIndex() {
		return this.index;
	}
	
	public String toPrettyString() {
		if (this.getIndex() == -1) {
			return "---root---";
		} else {
			return OperationItem.forTransitionFast(this.getTrace().getStateSpace(), this.getTrace().getTransitionList().get(this.getIndex())).toPrettyString(true);
		}
	}
}
