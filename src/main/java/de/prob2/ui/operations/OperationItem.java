package de.prob2.ui.operations;

import com.google.common.base.MoreObjects;
import de.prob.statespace.Trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OperationItem {
	public enum Status {
		DISABLED, ENABLED, TIMEOUT, MAX_REACHED
	}

	private final Trace trace;
	private final String id;
	private final String name;
	private final List<String> parameterNames;
	private final List<String> parameterValues;
	private final List<String> returnValues;
	private final List<String> outputParameterNames;
	private final OperationItem.Status status;
	private final boolean explored;
	private final boolean errored;
	private final boolean skip;
	private final Map<String, String> constants;
	private final Map<String, String> variables;


	
	public OperationItem(final Trace trace, final String id, final String name, final List<String> params,
			final List<String> returnValues, final OperationItem.Status status, final boolean explored,
			final boolean errored, final boolean skip, final List<String> parameterNames,
			final List<String> returnParameterNames, final Map<String, String> constants,
			final Map<String, String> variables) {
		this.trace = Objects.requireNonNull(trace);
		this.id = Objects.requireNonNull(id);
		this.name = Objects.requireNonNull(name);
		this.parameterValues = Objects.requireNonNull(params);
		this.returnValues = Objects.requireNonNull(returnValues);
		this.status = Objects.requireNonNull(status);
		this.explored = explored;
		this.errored = errored;
		this.skip = skip;
		this.parameterNames = Objects.requireNonNull(parameterNames);
		this.outputParameterNames = Objects.requireNonNull(returnParameterNames);
		this.constants = Objects.requireNonNull(constants);
		this.variables = Objects.requireNonNull(variables);
	}

	public OperationItem(
			final Trace trace,
			final String id,
			final String name,
			final OperationItem.Status status
		) {
		this(trace, id, name, Collections.emptyList(),
				Collections.emptyList(), OperationItem.Status.MAX_REACHED, false, false, false,
				Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
		
		}

	public Trace getTrace() {
		return this.trace;
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

	public String getId() {
		return id;
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
		return explored;
	}

	public boolean isErrored() {
		return errored;
	}

	public boolean isSkip() {
		return skip;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("trace", trace).add("id", id).add("name", name)
				.add("params", parameterValues).add("returnValues", returnValues).add("status", status)
				.add("explored", explored).add("errored", errored).add("skip", skip).toString();
	}
}
