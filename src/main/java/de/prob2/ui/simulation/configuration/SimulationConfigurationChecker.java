package de.prob2.ui.simulation.configuration;

import de.prob.statespace.OperationInfo;
import de.prob.statespace.StateSpace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimulationConfigurationChecker {

	private final StateSpace stateSpace;

    private final SimulationConfiguration simulationConfiguration;

    private final List<ConfigurationCheckingError> errors;

    public SimulationConfigurationChecker(StateSpace stateSpace, SimulationConfiguration simulationConfiguration) {
    	this.stateSpace = stateSpace;
        this.simulationConfiguration = simulationConfiguration;
        this.errors = new ArrayList<>();
    }

    public void check() {
        checkActivationConfigurations();
    }

    private void checkActivationConfigurations() {
    	for(ActivationConfiguration activationConfiguration : simulationConfiguration.getActivationConfigurations()) {
    		if(activationConfiguration instanceof ActivationOperationConfiguration) {
				this.checkActivationOperationConfiguration((ActivationOperationConfiguration) activationConfiguration);
			}
		}
	}

	private void checkActivationOperationConfiguration(ActivationOperationConfiguration activation) {
		Object probability = activation.getProbabilisticVariables();
		String activatedOp = activation.getOpName();
		if("$initialise_machine".equals(activatedOp) || "$setup_constants".equals(activatedOp)) {
			return;
		}
		if(probability == null) {

			//Check whether given operation name exists
			OperationInfo opInfo = stateSpace.getLoadedMachine().getMachineOperationInfo(activatedOp);
			if(opInfo == null) {
				errors.add(new ConfigurationCheckingError(String.format("Used operation %s does not exist", activatedOp)));
			} else {

				// Check whether given variables cover all non-deterministic variables and parameters of the operation
				Set<String> operationVariables = new HashSet<>();
				operationVariables.addAll(opInfo.getNonDetWrittenVariables());
				operationVariables.addAll(opInfo.getParameterNames());
				if(operationVariables.isEmpty())

				if (activation.getFixedVariables() != null && (!activation.getFixedVariables().keySet().containsAll(operationVariables) || !operationVariables.containsAll(activation.getFixedVariables().keySet()))) {
					errors.add(new ConfigurationCheckingError(String.format("Given parameters for triggering operation %s do not cover whole operation", activatedOp)));
				}
			}
		} else if(probability instanceof String) {
			// Currently, only uniform for probabilistic choice is allowed
			// TODO: Implement first
			if(!"uniform".equals(probability)) {
				errors.add(new ConfigurationCheckingError(String.format("Value %s for probability in activation configuration is not allowed", probability.toString())));
			}
		} else {

			//Check whether given operation name exists
			OperationInfo opInfo = stateSpace.getLoadedMachine().getMachineOperationInfo(activatedOp);
			if(opInfo == null) {
				errors.add(new ConfigurationCheckingError(String.format("Used operation %s does not exist", activatedOp)));
			} else {
				Set<String> operationVariables = new HashSet<>();
				operationVariables.addAll(opInfo.getNonDetWrittenVariables());
				operationVariables.addAll(opInfo.getParameterNames());

				Map<String, Map<String, String>> probabilityAsMap = (Map<String, Map<String, String>>) probability;
				Set<String> configurationVariables = new HashSet<>();
				configurationVariables.addAll(probabilityAsMap.keySet());
				if (activation.getFixedVariables() != null) {
					configurationVariables.addAll(activation.getFixedVariables().keySet());
					// Check whether fixed variables, and those for probabilistic choice are disjunct
					if (activation.getFixedVariables().keySet().stream().anyMatch(probabilityAsMap::containsKey)) {
						errors.add(new ConfigurationCheckingError(String.format("Fixed variables and those defined for probabilistic choice for operation %s must be disjunct", activatedOp)));
					}
				}


				// Check whether given variables cover all non-deterministic variables and parameters of the operation
				if (!operationVariables.containsAll(configurationVariables) || !configurationVariables.containsAll(operationVariables)) {
					errors.add(new ConfigurationCheckingError(String.format("Given parameters for triggering operation %s do not cover whole operation", activatedOp)));
				}

			}
		}
	}

	public List<ConfigurationCheckingError> getErrors() {
		return errors;
	}
}
