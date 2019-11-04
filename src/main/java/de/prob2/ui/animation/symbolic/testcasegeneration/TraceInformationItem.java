package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.List;

import de.prob.statespace.Trace;

public class TraceInformationItem {

	private int depth;

	private String transitions;

	private String operation;
	
	private String guard;
	
	private boolean enabled;

	private transient Trace trace;

	public TraceInformationItem(final int depth, final List<String> transitions, final String operation, final String guard, final boolean enabled, final Trace trace) {
		this.depth = depth;
		this.transitions = String.join(",\n", transitions);
		this.operation = operation;
		this.guard = guard;
		this.enabled = enabled;
		this.trace = trace;
	}

	public int getDepth() {
		return depth;
	}

	public String getTransitions() {
		return transitions;
	}
	
	public String getOperation() {
		return operation;
	}
	
	public String getGuard() {
		return guard;
	}
	
	public boolean isEnabled() {
		return enabled;
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
