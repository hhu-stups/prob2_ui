package de.prob2.ui.history;

import java.util.ArrayList;
import java.util.List;

import de.prob.statespace.Trace;

import de.prob2.ui.operations.OperationItem;

public class HistoryItem {
	private final Trace trace;
	private final OperationItem operationItem;
	
	public HistoryItem(final Trace trace) {
		this.trace = trace;
		this.operationItem = trace.canGoBack() ? OperationItem.forTransition(trace.getStateSpace(), trace.getCurrentTransition()) : null;
	}
	
	public static List<HistoryItem> itemsForTrace(final Trace trace) {
		final List<HistoryItem> items = new ArrayList<>();
		Trace t = trace.gotoPosition(trace.size()-1);
		while (true) {
			items.add(new HistoryItem(t));
			if (!t.canGoBack()) {
				break;
			}
			t = t.back();
		}
		return items;
	}
	
	public Trace getTrace() {
		return this.trace;
	}
	
	public OperationItem getOperationItem() {
		return this.operationItem;
	}
	
	public int getIndex() {
		return this.getTrace().getCurrent().getIndex();
	}
	
	public String toPrettyString() {
		return this.getOperationItem() == null ? "---root---" : this.getOperationItem().toPrettyString();
	}
}
