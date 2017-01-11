package de.prob2.ui.operations;

import java.util.List;

public class OperationItem {
	public enum Status {
		DISABLED, ENABLED, TIMEOUT
	}
	
	public final String name;
	public final List<String> params;
	public final String id;
	public final Status status;
	public final List<String> returnValues;
	final boolean explored;
	final boolean errored;

	public OperationItem(
		final String id,
		final String name,
		final List<String> params,
		final List<String> returnValues,
		final OperationItem.Status status,
		final boolean explored,
		final boolean errored
	) {
		this.id = id;
		this.name = name;
		this.params = params;
		this.returnValues = returnValues;
		this.status = status;
		this.explored = explored;
		this.errored = errored;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (!returnValues.isEmpty()) {
			sb.append(String.join(", ", returnValues));
			sb.append(" ← ");
		}
		sb.append(name);
		if (!params.isEmpty()) {
			sb.append("(");
			sb.append(String.join(", ", params));
			sb.append(")");
		}
		return sb.toString();
	}
}
