package de.prob2.ui.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.operations.OperationItem;

public final class HistoryItem {

	private final OperationItem operation;
	private final int index;

	private HistoryItem(final OperationItem operation, final int index) {
		this.operation = operation;
		this.index = index;
	}
	
	public static List<HistoryItem> itemsForTrace(final Trace trace) {
		// additional information is queried by OperationsDetailsStage when opened
		final Map<Transition, OperationItem> operations = OperationItem.forTransitionsFast(trace.getStateSpace(), trace.getTransitionList());
		final List<HistoryItem> items = new ArrayList<>();
		items.add(new HistoryItem(null, -1)); // Root state
		int i = 0;
		for (final Transition t : trace.getTransitionList()) {
			items.add(new HistoryItem(operations.get(t), i));
			i++;
		}
		return items;
	}
	
	public OperationItem getOperation() {
		return this.operation;
	}
	
	public int getIndex() {
		return this.index;
	}
	
	public String toPrettyString() {
		if (this.getIndex() == -1) {
			return "---root---";
		} else {
			return this.operation.toPrettyString(true);
		}
	}
}
