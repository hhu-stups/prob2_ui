package de.prob2.ui.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.base.MoreObjects;

import de.prob.statespace.Trace;
import de.prob.statespace.Transition;

public class OperationItem {
	public enum Status {
		DISABLED, ENABLED, TIMEOUT
	}

	private final Trace trace;
	private final Transition transition;
	private final String name;
	private final OperationItem.Status status;
	private final List<String> parameterNames;
	private final List<String> parameterValues;
	private final List<String> returnParameterNames;
	private final List<String> returnParameterValues;
	private final Map<String, String> constants;
	private final Map<String, String> variables;

	public OperationItem(
		final Trace trace,
		final Transition transition,
		final String name,
		final Status status,
		final List<String> parameterNames,
		final List<String> parameterValues,
		final List<String> returnParameterNames,
		final List<String> returnParameterValues,
		final Map<String, String> constants,
		final Map<String, String> variables
	) {
		this.trace = Objects.requireNonNull(trace);
		this.transition = transition;
		this.name = Objects.requireNonNull(name);
		this.status = Objects.requireNonNull(status);
		this.parameterNames = Objects.requireNonNull(parameterNames);
		this.returnParameterNames = Objects.requireNonNull(returnParameterNames);
		this.parameterValues = Objects.requireNonNull(parameterValues);
		this.returnParameterValues = Objects.requireNonNull(returnParameterValues);
		this.constants = Objects.requireNonNull(constants);
		this.variables = Objects.requireNonNull(variables);
	}

	public Trace getTrace() {
		return this.trace;
	}

	public Transition getTransition() {
		return this.transition;
	}

	public String getName() {
		return name;
	}

	public OperationItem.Status getStatus() {
		return status;
	}

	public List<String> getParameterNames() {
		return new ArrayList<>(parameterNames);
	}

	public List<String> getParameterValues() {
		return new ArrayList<>(parameterValues);
	}

	public List<String> getReturnParameterNames() {
		return new ArrayList<>(returnParameterNames);
	}

	public List<String> getReturnParameterValues() {
		return new ArrayList<>(returnParameterValues);
	}

	public Map<String, String> getConstants() {
		return this.constants;
	}

	public Map<String, String> getVariables() {
		return this.variables;
	}

	public boolean isExplored() {
		return this.getTransition() != null && this.getTransition().getDestination().isExplored();
	}

	public boolean isErrored() {
		return this.isExplored() && !this.getTransition().getDestination().isInvariantOk();
	}

	public boolean isSkip() {
		return this.getTransition() != null && this.getTransition().getSource().equals(this.getTransition().getDestination());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("trace", this.getTrace())
			.add("transition", this.getTransition())
			.add("name", this.getName())
			.add("status", this.getStatus())
			.add("parameterNames", this.getParameterNames())
			.add("parameterValues", this.getParameterValues())
			.add("returnParameterNames", this.getReturnParameterNames())
			.add("returnParameterValues", this.getReturnParameterValues())
			.add("constants", this.getConstants())
			.add("variables", this.getVariables())
			.toString();
	}
}
