package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.List;

import de.prob.statespace.Trace;

public class TraceInformationItem {

	private int depth;

	private String operations;

	private String operation;
	
	private String guard;
	
	private boolean enabled;

	private transient Trace trace;

	public TraceInformationItem(final int depth, final List<String> operations, final String operation, final String guard, final boolean enabled, final Trace trace) {
		this.depth = depth;
		this.operations = String.join(",\n", operations);
		this.operation = operation;
		this.guard = guard;
		this.enabled = enabled;
		this.trace = trace;
	}

	public int getDepth() {
		return depth;
	}

	public String getOperations() {
		return operations;
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
