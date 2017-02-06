package de.prob2.ui.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OperationItem {
	public enum Status {
		DISABLED, ENABLED, TIMEOUT, MAX_REACHED
	}
	
	private final String id;
	private final String name;
	private final List<String> params;
	private final List<String> returnValues;
	private final OperationItem.Status status;
	private final boolean explored;
	private final boolean errored;

	public OperationItem(
		final String id,
		final String name,
		final List<String> params,
		final List<String> returnValues,
		final OperationItem.Status status,
		final boolean explored,
		final boolean errored
	) {
		this.id = Objects.requireNonNull(id);
		this.name = Objects.requireNonNull(name);
		this.params = Objects.requireNonNull(params);
		this.returnValues = Objects.requireNonNull(returnValues);
		this.status = Objects.requireNonNull(status);
		this.explored = explored;
		this.errored = errored;
	}
	
	public String getName() {
		return name;
	}
	
	public List<String> getParams() {
		return new ArrayList<>(params);
	}
	
	public String getId() {
		return id;
	}
	
	public OperationItem.Status getStatus() {
		return status;
	}
	
	public List<String> getReturnValues() {
		return new ArrayList<>(returnValues);
	}
	
	public boolean isExplored() {
		return explored;
	}
	
	public boolean isErrored() {
		return errored;
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
