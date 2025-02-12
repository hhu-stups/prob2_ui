package de.prob2.ui.simulation.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
		this.checkActivationConfigurations();
	}

	private void checkActivationConfigurations() {
		for (var ac : this.simulationConfiguration.getActivations()) {
			if (ac instanceof ActivationOperationConfiguration aoc) {
				this.checkActivationOperationConfiguration(aoc);
			}
		}
	}

	private void checkActivationOperationConfiguration(ActivationOperationConfiguration activation) {
		var probabilities = activation.getProbabilisticVariables();
		var opName = activation.getExecute();
		if (!Transition.isArtificialTransitionName(opName)) {
			// Check whether given operation name exists
			OperationInfo opInfo = this.stateSpace.getLoadedMachine().getMachineOperationInfo(opName);
			if (!"skip".equals(opName) && opInfo == null) {
				this.errors.add(new ConfigurationCheckingError(String.format("Used operation %s does not exist", opName)));
			}
		}

		// Check whether fixed variables, and those for probabilistic choice are disjunct
		if (activation.getFixedVariables().keySet().stream().anyMatch(probabilities::containsKey)) {
			this.errors.add(new ConfigurationCheckingError(String.format(Locale.ROOT, "Fixed variables and those defined for probabilistic choice for operation %s must be disjunct", opName)));
		}
	}

	public List<ConfigurationCheckingError> getErrors() {
		return this.errors;
	}
}
