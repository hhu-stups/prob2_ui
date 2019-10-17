package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.List;

import de.prob.statespace.Trace;

public class TraceInformationItem {

	private int depth;

	private String transitions;

	private boolean complete;

	private String operation;
	
	private String guard;

	private transient Trace trace;

	public TraceInformationItem(final int depth, final List<String> transitions, final boolean complete, final String operation, final String guard, final Trace trace) {
		this.depth = depth;
		this.transitions = String.join(",\n", transitions);
		this.complete = complete;
		this.operation = operation;
		this.guard = guard;
		this.trace = trace;
	}

	public int getDepth() {
		return depth;
	}

	public String getTransitions() {
		return transitions;
	}

	public boolean isComplete() {
		return complete;
	}

	public String getOperation() {
		return operation;
	}
	
	public String getGuard() {
		return guard;
	}

	public Trace getTrace() {
		return trace;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("OPERATION: ");
		sb.append(operation);
		sb.append(", ");
		sb.append("GUARD: ");
		sb.append(guard);
		return sb.toString();
	}

}
