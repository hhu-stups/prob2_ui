package de.prob2.ui.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.MoreObjects;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob.statespace.LoadedMachine;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Transition;

public class OperationItem {
	public enum Status {
		DISABLED, ENABLED, TIMEOUT
	}
	
	/**
	 * Internal helper class that holds an operation's name and parameter values. Used for grouping in {@link #computeUnambiguousConstantsAndVariables(Collection)}.
	 */
	private static final class OperationNameAndParameterValues {
		final String name;
		final List<String> parameterValues;
		
		private OperationNameAndParameterValues(final String name, final List<String> parameterValues) {
			this.name = name;
			this.parameterValues = parameterValues;
		}
		
		private OperationNameAndParameterValues(final OperationItem item) {
			this(item.getName(), item.getParameterValues());
		}
		
		private String getName() {
			return this.name;
		}
		
		private List<String> getParameterValues() {
			return this.parameterValues;
		}
		
		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || this.getClass() != o.getClass()) {
				return false;
			}
			final OperationNameAndParameterValues other = (OperationNameAndParameterValues)o;
			return this.getName().equals(other.getName()) && this.getParameterValues().equals(other.getParameterValues());
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.getName(), this.getParameterValues());
		}
		
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
				.add("name", name)
				.add("parameterValues", parameterValues)
				.toString();
		}
	}
	
	private final Transition transition;
	private final String name;
	private final OperationItem.Status status;
	private final List<String> parameterNames;
	private final List<String> parameterValues;
	private final List<String> returnParameterNames;
	private final List<String> returnParameterValues;
	private final Map<String, String> constants;
	private final Map<String, String> variables;
	private final Collection<String> unambiguousConstantNames;
	private final Collection<String> unambiguousVariableNames;

	public OperationItem(final Transition transition, final String name, final Status status,
			final List<String> parameterNames, final List<String> parameterValues,
			final List<String> returnParameterNames, final List<String> returnParameterValues,
			final Map<String, String> constants, final Map<String, String> variables,
			final Collection<String> unambiguousConstantNames,
			final Collection<String> unambiguousVariableNames) {
		this.transition = transition;
		this.name = Objects.requireNonNull(name);
		this.status = Objects.requireNonNull(status);
		this.parameterNames = Objects.requireNonNull(parameterNames);
		this.returnParameterNames = Objects.requireNonNull(returnParameterNames);
		this.parameterValues = Objects.requireNonNull(parameterValues);
		this.returnParameterValues = Objects.requireNonNull(returnParameterValues);
		this.constants = Objects.requireNonNull(constants);
		this.variables = Objects.requireNonNull(variables);
		this.unambiguousConstantNames = Objects.requireNonNull(unambiguousConstantNames);
		this.unambiguousVariableNames = Objects.requireNonNull(unambiguousVariableNames);
	}

	private static Map<String, String> getNextStateValues(Transition transition, List<IEvalElement> formulas) {
		// It seems that there is no way to easily find out the
		// constant/variable values which a specific $setup_constants or
		// $initialise_machine transition would set.
		// So we look at the values of all constants/variables in the
		// transition's destination state.
		final Map<String, String> values = new LinkedHashMap<>();
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

	public static Collection<OperationItem> forTransitions(final StateSpace stateSpace, final Collection<Transition> transitions) {
		final LoadedMachine loadedMachine = stateSpace.getLoadedMachine();
		final List<OperationItem> items = new ArrayList<>();
		for (final Transition transition : transitions) {
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
				case Transition.SETUP_CONSTANTS_NAME:
					constants = getNextStateValues(transition, loadedMachine.getConstantEvalElements(FormulaExpand.TRUNCATE));
					variables = Collections.emptyMap();
					break;
				
				case Transition.INITIALISE_MACHINE_NAME:
					variables = getNextStateValues(transition, loadedMachine.getVariableEvalElements(FormulaExpand.TRUNCATE));
					constants = Collections.emptyMap();
					break;
				
				default:
					constants = Collections.emptyMap();
					if (opInfo == null) {
						variables = Collections.emptyMap();
					} else {
						variables = getNextStateValues(transition,
							opInfo.getNonDetWrittenVariables().stream()
								.map(var -> stateSpace.getModel().parseFormula(var, FormulaExpand.TRUNCATE))
								.collect(Collectors.toList()));
					}
			}
			
			final List<String> paramNames = opInfo == null ? Collections.emptyList() : opInfo.getParameterNames();
			final List<String> outputNames = opInfo == null ? Collections.emptyList() : opInfo.getOutputParameterNames();
			
			items.add(new OperationItem(transition, transition.getName(), Status.ENABLED, paramNames,
				transition.getParameterValues(), outputNames, transition.getReturnValues(), constants, variables,
				Collections.emptySet(), Collections.emptySet()));
		}
		return items;
	}

	public static OperationItem forTransition(final StateSpace stateSpace, final Transition transition) {
		final Collection<OperationItem> items = forTransitions(stateSpace, Collections.singletonList(transition));
		assert items.size() == 1;
		return items.iterator().next();
	}

	public static OperationItem forDisabled(final String name, final Status status, final List<String> parameters) {
		return new OperationItem(null, name, status, parameters, Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(),
				Collections.emptySet(), Collections.emptySet());
	}

	private static Stream<OperationItem> computeUnambiguousConstantsAndVariablesInternal(final Collection<OperationItem> items) {
		assert !items.isEmpty();
		final OperationItem first = items.iterator().next();
		final Set<String> unambiguousConstants = new HashSet<>(first.getConstants().keySet());
		final Set<String> unambiguousVariables = new HashSet<>(first.getVariables().keySet());
		for (final OperationItem item : items) {
			assert item.getName().equals(first.getName());
			assert item.getParameterValues().equals(first.getParameterValues());
			unambiguousConstants.removeIf(name -> !item.getConstants().get(name).equals(first.getConstants().get(name)));
			unambiguousVariables.removeIf(name -> !item.getVariables().get(name).equals(first.getVariables().get(name)));
		}
		
		return items.stream()
			.map(item -> new OperationItem(
				item.getTransition(), item.getName(), item.getStatus(),
				item.getParameterNames(), item.getParameterValues(),
				item.getReturnParameterNames(), item.getReturnParameterValues(),
				item.getConstants(), item.getVariables(),
				unambiguousConstants, unambiguousVariables
			));
	}
	
	public static Collection<OperationItem> computeUnambiguousConstantsAndVariables(final Collection<OperationItem> items) {
		// Group by operation name and parameter values, compute unambiguous constants/variables inside each group, then combine them again.
		// The grouping step ensures that only operations with the same name and parameter values are considered when checking for ambiguity.
		// Otherwise operations that change the same variables, but have different names or parameters, would be incorrectly detected as ambiguous.
		return items.stream()
			.collect(Collectors.groupingBy(OperationNameAndParameterValues::new))
			.values()
			.stream()
			.flatMap(OperationItem::computeUnambiguousConstantsAndVariablesInternal)
			.collect(Collectors.toList());
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

	public Collection<String> getUnambiguousConstantNames() {
		return this.unambiguousConstantNames;
	}

	public Collection<String> getUnambiguousVariableNames() {
		return this.unambiguousVariableNames;
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
		return MoreObjects.toStringHelper(this).add("transition", this.getTransition())
				.add("name", this.getName()).add("status", this.getStatus())
				.add("parameterNames", this.getParameterNames()).add("parameterValues", this.getParameterValues())
				.add("returnParameterNames", this.getReturnParameterNames())
				.add("returnParameterValues", this.getReturnParameterValues())
				.add("constants", this.getConstants()).add("variables", this.getVariables())
				.add("unambiguousConstantNames", this.getUnambiguousConstantNames())
				.add("unambiguousVariableNames", this.getUnambiguousVariableNames())
				.toString();
	}

	public String toPrettyString(final boolean includeUnambiguous) {
		StringBuilder sb = new StringBuilder(Transition.prettifyName(this.getName()));

		final List<String> args = new ArrayList<>();

		final List<String> paramNames = this.getParameterNames();
		final List<String> paramValues = this.getParameterValues();
		if (paramNames.isEmpty()) {
			// Parameter names not available
			args.addAll(paramValues);
		} else if (paramValues.isEmpty()) {
			// Parameter names without values (disabled/timed out operation)
			args.addAll(paramNames);
		} else {
			assert paramNames.size() == paramValues.size();
			for (int i = 0; i < paramValues.size(); i++) {
				args.add(paramNames.get(i) + '=' + paramValues.get(i));
			}
		}

		this.getConstants().forEach((key, value) -> {
			if (includeUnambiguous || !this.getUnambiguousConstantNames().contains(key)) {
				args.add(key + ":=" + value);
			}
		});
		this.getVariables().forEach((key, value) -> {
			if (includeUnambiguous || !this.getUnambiguousVariableNames().contains(key)) {
				args.add(key + ":=" + value);
			}
		});

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
