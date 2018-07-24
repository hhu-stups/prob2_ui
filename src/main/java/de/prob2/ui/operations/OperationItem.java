package de.prob2.ui.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.base.MoreObjects;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob.statespace.LoadedMachine;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.StateSpace;
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

	public OperationItem(final Trace trace, final Transition transition, final String name, final Status status,
			final List<String> parameterNames, final List<String> parameterValues,
			final List<String> returnParameterNames, final List<String> returnParameterValues,
			final Map<String, String> constants, final Map<String, String> variables) {
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

	private static LinkedHashMap<String, String> getNextStateValues(Transition transition,
			List<IEvalElement> formulas) {
		// It seems that there is no way to easily find out the
		// constant/variable values which a specific $setup_constants or
		// $initialise_machine transition would set.
		// So we look at the values of all constants/variables in the
		// transition's destination state.
		final LinkedHashMap<String, String> values = new LinkedHashMap<>();
		final List<AbstractEvalResult> results = transition.getDestination().eval(formulas);
		for (int i = 0; i < formulas.size(); i++) {
			final AbstractEvalResult value = results.get(i);
			final String valueString;
			if (value instanceof EvalResult) {
				valueString = ((EvalResult) value).getValue();
			} else {
				// noinspection ObjectToString
				valueString = value.toString();
			}
			values.put(formulas.get(i).getCode(), valueString);
		}

		return values;
	}

	public static OperationItem forTransition(final Trace trace, final Transition transition) {
		final StateSpace stateSpace = trace.getStateSpace();
		final LoadedMachine loadedMachine = stateSpace.getLoadedMachine();
		OperationInfo opInfo;
		try {
			opInfo = loadedMachine.getMachineOperationInfo(transition.getName());
		} catch (ProBError e) {
			// fallback solution if getMachineOperationInfo throws a ProBError
			opInfo = null;
		}

		final Map<String, String> constants;
		final Map<String, String> variables;
		switch (transition.getName()) {
		case "$setup_constants":
			constants = getNextStateValues(transition, getConstantsAsTruncatedEvalElements(stateSpace));
			variables = Collections.emptyMap();
			break;

		case "$initialise_machine":
			variables = getNextStateValues(transition, getVariablesAsTruncatedEvalElements(stateSpace));
			constants = Collections.emptyMap();
			break;

		default:
			constants = Collections.emptyMap();
			if (opInfo == null) {
				variables = Collections.emptyMap();
			} else {
				variables = getNextStateValues(transition,
						opInfo.getNonDetWrittenVariables().stream()
								.map(var -> trace.getStateSpace().getModel().parseFormula(var, FormulaExpand.TRUNCATE))
								.collect(Collectors.toList()));
			}
		}

		final List<String> paramNames = opInfo == null ? Collections.emptyList() : opInfo.getParameterNames();
		final List<String> outputNames = opInfo == null ? Collections.emptyList() : opInfo.getOutputParameterNames();

		return new OperationItem(trace, transition, transition.getName(), Status.ENABLED, paramNames,
				transition.getParameterValues(), outputNames, transition.getReturnValues(), constants, variables);
	}

	private static List<IEvalElement> getConstantsAsTruncatedEvalElements(StateSpace sp) {
		List<IEvalElement> constantEvalElements = new ArrayList<>();
		for (String string : sp.getLoadedMachine().getConstantNames()) {
			constantEvalElements.add(sp.getModel().parseFormula(string, FormulaExpand.TRUNCATE));
		}
		return constantEvalElements;
	}

	private static List<IEvalElement> getVariablesAsTruncatedEvalElements(StateSpace sp) {
		List<IEvalElement> variableEvalElements = new ArrayList<>();
		for (String string : sp.getLoadedMachine().getVariableNames()) {
			variableEvalElements.add(sp.getModel().parseFormula(string, FormulaExpand.TRUNCATE));
		}
		return variableEvalElements;
	}

	public static OperationItem forDisabled(final Trace trace, final String name, final Status status,
			final List<String> parameters) {
		return new OperationItem(trace, null, name, status, Collections.emptyList(), parameters,
				Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
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
		return this.getTransition() != null
				&& this.getTransition().getSource().equals(this.getTransition().getDestination());
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("trace", this.getTrace()).add("transition", this.getTransition())
				.add("name", this.getName()).add("status", this.getStatus())
				.add("parameterNames", this.getParameterNames()).add("parameterValues", this.getParameterValues())
				.add("returnParameterNames", this.getReturnParameterNames())
				.add("returnParameterValues", this.getReturnParameterValues()).add("constants", this.getConstants())
				.add("variables", this.getVariables()).toString();
	}

	private static String getPrettyName(final String name) {
		switch (name) {
		case "$setup_constants":
			return "SETUP_CONSTANTS";

		case "$initialise_machine":
			return "INITIALISATION";

		default:
			return name;
		}
	}

	public String toPrettyString() {
		StringBuilder sb = new StringBuilder(getPrettyName(this.getName()));

		final List<String> args = new ArrayList<>();

		final List<String> paramNames = this.getParameterNames();
		final List<String> paramValues = this.getParameterValues();
		if (paramNames.isEmpty()) {
			// Parameter names not available
			args.addAll(paramValues);
		} else {
			assert paramNames.size() == paramValues.size();
			for (int i = 0; i < paramValues.size(); i++) {
				args.add(paramNames.get(i) + '=' + paramValues.get(i));
			}
		}

		if (this.name.equals("$setup_constants") || this.name.equals("$initialise_machine")) {
			this.getConstants().forEach((key, value) -> args.add(key + ":=" + value));
			this.getVariables().forEach((key, value) -> args.add(key + ":=" + value));
		}

		if (!args.isEmpty()) {
			sb.append('(');
			sb.append(String.join(", ", args));
			sb.append(')');
		}

		final List<String> returnValues = this.getReturnParameterValues();
		if (!returnValues.isEmpty()) {
			sb.append(" → ");
			sb.append(String.join(", ", returnValues));
		}

		return sb.toString();
	}
}
