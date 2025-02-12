package de.prob2.ui.simulation.configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.prob.statespace.OperationInfo;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Transition;

public class SimulationModelConfigurationChecker {

	private final StateSpace stateSpace;

	private final SimulationModelConfiguration simulationConfiguration;

	private final List<ConfigurationCheckingError> errors;

	public SimulationModelConfigurationChecker(StateSpace stateSpace, SimulationModelConfiguration simulationConfiguration) {
		this.stateSpace = stateSpace;
		this.simulationConfiguration = simulationConfiguration;
		this.errors = new ArrayList<>();
	}

	public void check() {
		checkActivationConfigurations();
	}

	private void checkActivationConfigurations() {
		for (var ac : simulationConfiguration.getActivations()) {
			if (ac instanceof ActivationOperationConfiguration aoc) {
				this.checkActivationOperationConfiguration(aoc);
			}
		}
	}

	private void checkActivationOperationConfiguration(ActivationOperationConfiguration activation) {
		var probabilisticVariables = activation.getProbabilisticVariables();
		String activatedOp = activation.getExecute();
		if (Transition.isArtificialTransitionName(activatedOp)) {
			return;
		}

		// Check whether given operation name exists
		OperationInfo opInfo = stateSpace.getLoadedMachine().getMachineOperationInfo(activatedOp);
		if (!"skip".equals(activatedOp) && opInfo == null) {
			errors.add(new ConfigurationCheckingError(String.format("Used operation %s does not exist", activatedOp)));
			return;
		}

		Set<String> operationVariables = new HashSet<>();
		if (opInfo != null) {
			operationVariables.addAll(opInfo.getNonDetWrittenVariables());
			operationVariables.addAll(opInfo.getParameterNames());
		}

		Set<String> configurationVariables = new HashSet<>();
		if (activation.getFixedVariables() != null) {
			configurationVariables.addAll(activation.getFixedVariables().keySet());
		}

		if (probabilisticVariables instanceof ProbabilisticVariables.PerVariable(var probabilities)) {
			// Check whether fixed variables, and those for probabilistic choice are disjunct
			if (configurationVariables.stream().anyMatch(probabilities::containsKey)) {
				errors.add(new ConfigurationCheckingError(String.format(Locale.ROOT, "Fixed variables and those defined for probabilistic choice for operation %s must be disjunct", activatedOp)));
			}

			configurationVariables.addAll(probabilities.keySet());

			// Check whether given variables are covered by all non-deterministic variables and parameters of the operation
			if (!configurationVariables.equals(operationVariables)) {
				errors.add(new ConfigurationCheckingError(String.format(Locale.ROOT, "Given parameters for triggering operation %s do not cover whole operation", activatedOp)));
			}
		}
	}

	public List<ConfigurationCheckingError> getErrors() {
		return errors;
	}
}
