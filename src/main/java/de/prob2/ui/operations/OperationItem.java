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
	private final List<String> parameterNames;
	private final List<String> parameterValues;
	private final List<String> returnValues;
	private final List<String> outputParameterNames;
	private final OperationItem.Status status;
	private final Map<String, String> constants;
	private final Map<String, String> variables;

	public OperationItem(
		final Trace trace,
		final Transition transition,
		final String name,
		final List<String> params,
		final List<String> returnValues,
		final OperationItem.Status status,
		final List<String> parameterNames,
		final List<String> returnParameterNames,
		final Map<String, String> constants,
		final Map<String, String> variables
	) {
		this.trace = Objects.requireNonNull(trace);
		this.transition = transition;
		this.name = Objects.requireNonNull(name);
		this.parameterValues = Objects.requireNonNull(params);
		this.returnValues = Objects.requireNonNull(returnValues);
		this.status = Objects.requireNonNull(status);
		this.parameterNames = Objects.requireNonNull(parameterNames);
		this.outputParameterNames = Objects.requireNonNull(returnParameterNames);
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

	public List<String> getParameterNames() {
		return new ArrayList<>(parameterNames);
	}

	public Map<String, String> getConstants() {
		return this.constants;
	}

	public Map<String, String> getVariables() {
		return this.variables;
	}

	public List<String> getParameterValues() {
		return new ArrayList<>(parameterValues);
	}

	public OperationItem.Status getStatus() {
		return status;
	}

	public List<String> getOutputParameterNames() {
		return new ArrayList<>(outputParameterNames);
	}

	public List<String> getReturnValues() {
		return new ArrayList<>(returnValues);
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
			.add("parameterNames", this.getParameterNames())
			.add("parameterValues", this.getParameterValues())
			.add("returnValues", this.getReturnValues())
			.add("outputParameterNames", this.getOutputParameterNames())
			.add("status", this.getStatus())
			.add("constants", this.getConstants())
			.add("variables", this.getVariables())
			.toString();
	}
}
