package de.prob2.ui.rulevalidation;

import com.google.inject.Singleton;
import de.be4.classicalb.core.parser.rules.AbstractOperation;
import de.be4.classicalb.core.parser.rules.ComputationOperation;
import de.be4.classicalb.core.parser.rules.RuleOperation;
import de.prob.animator.domainobjects.IdentifierNotInitialised;
import de.prob.model.brules.*;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.*;

/**
 * @author Christoph Heinzen
 * @since 20.12.17
 */
@Singleton
public final class RulesDataModel {
	private static final IdentifierNotInitialised IDENTIFIER_NOT_INITIALISED = new IdentifierNotInitialised(new ArrayList<>());

	private RulesModel model;

	// dynamic information about the loaded rules machine
	private Map<String, SimpleObjectProperty<Object>> ruleValueMap;
	private Map<String, SimpleObjectProperty<Object>> computationValueMap;
	// static information about the loaded rules machine
	private final Map<String, RuleOperation> ruleMap = new LinkedHashMap<>();
	private final Map<String, ComputationOperation> computationMap = new LinkedHashMap<>();

	// Summary properties
	private final SimpleStringProperty failedRules = new SimpleStringProperty("-");
	private final SimpleStringProperty successRules = new SimpleStringProperty("-");
	private final SimpleStringProperty notCheckedRules = new SimpleStringProperty("-");
	private final SimpleStringProperty disabledRules = new SimpleStringProperty("-");

	// Methods to access properties
	public Map<String, SimpleObjectProperty<Object>> getRuleValueMap() {
		return ruleValueMap;
	}
	public SimpleObjectProperty<Object> getRuleValue(String rule) {
		return ruleValueMap.get(rule);
	}
	public Map<String, RuleOperation> getRuleMap() {
		return ruleMap;
	}
	public Map<String, SimpleObjectProperty<Object>> getComputationValueMap() {
		return computationValueMap;
	}
	public Map<String, ComputationOperation> getComputationMap() {
		return computationMap;
	}
	public SimpleObjectProperty<Object> getComputationValue(String computation) {
		return computationValueMap.get(computation);
	}
	public SimpleStringProperty failedRulesProperty() {
		return failedRules;
	}
	public SimpleStringProperty successRulesProperty() {
		return successRules;
	}
	public SimpleStringProperty notCheckedRulesProperty() {
		return notCheckedRules;
	}
	public SimpleStringProperty disabledRulesProperty() {
		return disabledRules;
	}

	void initialize(RulesModel newModel) {
		this.model = newModel;

		model.getRulesProject().getOperationsMap().forEach((name, op) -> {
			if (op instanceof RuleOperation)
				ruleMap.put(name, (RuleOperation) op);
			else if (op instanceof ComputationOperation)
				computationMap.put(name, (ComputationOperation) op);
		});

		ruleValueMap = new LinkedHashMap<>(ruleMap.size());
		initializeValueMap(ruleMap.keySet(), ruleValueMap);

		computationValueMap = new LinkedHashMap<>(computationMap.size());
		initializeValueMap(computationMap.keySet(), computationValueMap);
	}

	void update(Trace newTrace) {
		if (newTrace.getCurrentState().isInitialised()) {
			updateRuleResults(newTrace.getCurrentState());
			updateComputationResults(newTrace.getCurrentState());
		} else {
			for (SimpleObjectProperty<Object> prop : ruleValueMap.values()) {
				prop.set(IDENTIFIER_NOT_INITIALISED);
			}
			for (SimpleObjectProperty<Object> prop : computationValueMap.values()) {
				prop.set(IDENTIFIER_NOT_INITIALISED);
			}
		}
	}

	void clear() {
		failedRules.set("-");
		successRules.set("-");
		notCheckedRules.set("-");
		disabledRules.set("-");
	}

	private void initializeValueMap(Set<String> operations, Map<String, SimpleObjectProperty<Object>> operationsValueMap) {
		List<String> sortedOperations = new ArrayList<>(operations);
		Collections.sort(sortedOperations);

		for (String operation : sortedOperations) {
			operationsValueMap.put(operation, new SimpleObjectProperty<>(IDENTIFIER_NOT_INITIALISED));
		}
	}

	private void updateRuleResults(State currentState) {
		RuleResults ruleResults = new RuleResults(model.getRulesProject(), currentState, -1, -1, -1);
		int notCheckableCounter = 0;
		for (String ruleStr : ruleValueMap.keySet()) {
			RuleResult result = ruleResults.getRuleResultMap().get(ruleStr);
			ruleValueMap.get(ruleStr).set(result);
			if ((result.getFailedDependencies() != null && !result.getFailedDependencies().isEmpty()) ||
					!getDisabledDependencies(ruleStr).isEmpty()) {
				notCheckableCounter++;
			}
		}

		//update summary
		RuleResults.ResultSummary summary = ruleResults.getSummary();
		failedRules.set(summary.numberOfRulesFailed + "");
		successRules.set(summary.numberOfRulesSucceeded + "");
		notCheckedRules.set((summary.numberOfRulesNotChecked - notCheckableCounter) + " (" + notCheckableCounter + ")");
		disabledRules.set(summary.numberOfRulesDisabled + "");
	}

	private void updateComputationResults(State currentState) {
		Map<AbstractOperation,OperationStatus> computationResults = OperationStatuses.getStatuses(model, currentState);
		computationResults.forEach((op, result) -> {
			SimpleObjectProperty<Object> prop = computationValueMap.get(op.getName());
			if (prop != null) {
				prop.set(result);
			}
		});
	}

	public List<String> getFailedDependenciesOfComputation(String comp) {
		List<String> failedDependencies = new ArrayList<>();
 		for (AbstractOperation op : computationMap.get(comp).getTransitiveDependencies()) {
			if (op instanceof RuleOperation && ruleValueMap.containsKey(op.getName())
					&& getRuleValue(op.getName()).get() instanceof RuleResult ruleResult
					&& ruleResult.getRuleState() == RuleStatus.FAIL) {
				failedDependencies.add(op.getName());
			}
		}
		return failedDependencies;
	}

	public List<String> getNotCheckedDependenciesOfComputation(String comp) {
		List<String> notCheckedDependencies = new ArrayList<>();
		for (AbstractOperation op : getComputationMap().get(comp).getTransitiveDependencies()) {
			if (op instanceof RuleOperation && ruleValueMap.containsKey(op.getName())
					&& getRuleValue(op.getName()).get() instanceof RuleResult ruleResult
					&& ruleResult.getRuleState().isNotExecuted()) {
				notCheckedDependencies.add(op.getName());
			} else if (op instanceof ComputationOperation && computationValueMap.containsKey(op.getName())
					&& getComputationValue(op.getName()).get() == ComputationStatus.NOT_EXECUTED) {
				notCheckedDependencies.add(op.getName());
			}
		}
		Collections.sort(notCheckedDependencies);
		return notCheckedDependencies;
	}

	public List<String> getDisabledDependencies(String operation) {
		Set<AbstractOperation> dependencies = new HashSet<>();
		if (ruleMap.containsKey(operation)) {
			dependencies = ruleMap.get(operation).getTransitiveDependencies();
		} else if (computationMap.containsKey(operation)) {
			dependencies = computationMap.get(operation).getTransitiveDependencies();
		}

		List<String> disableDependencies = new ArrayList<>();
		for (AbstractOperation op : dependencies) {
			if (op instanceof RuleOperation && ruleValueMap.containsKey(op.getName())
					&& getRuleValue(op.getName()).get() instanceof RuleResult ruleResult
					&& ruleResult.getRuleState().isDisabled()) {
				disableDependencies.add(op.getName());
			} else if (op instanceof ComputationOperation && computationValueMap.containsKey(op.getName())
					&& getComputationValue(op.getName()).get() == ComputationStatus.DISABLED) {
				disableDependencies.add(op.getName());
			}
		}
		Collections.sort(disableDependencies);
		return disableDependencies;
	}
}
