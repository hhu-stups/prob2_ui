package de.prob2.ui.simulation.configuration;

import java.util.ArrayList;
import java.util.List;

public class SimulationConfigurationChecker {

    private final SimulationConfiguration simulationConfiguration;

    private final List<ConfigurationCheckingError> errors;

    public SimulationConfigurationChecker(SimulationConfiguration simulationConfiguration) {
        this.simulationConfiguration = simulationConfiguration;
        this.errors = new ArrayList<>();
    }

    public void check() {
        checkVariableChoices();
        checkActivationConfigurations();
    }

    private void checkVariableChoices() {
        simulationConfiguration.getTimingConfigurations().stream()
                .filter(timingConfiguration -> !"$initialise_machine".equals(timingConfiguration.getOpName()) && !"setup_constants".equals(timingConfiguration.getOpName()))
                .filter(timingConfiguration -> timingConfiguration.getVariableChoices() != null)
                .forEach(timingConfiguration -> errors.add(new ConfigurationCheckingError(String.format("Field variableChoices is not allowed for operation: %s", timingConfiguration.getOpName()))));
    }

    private void checkActivationConfigurations() {
        for(ActivationConfiguration activationConfiguration : simulationConfiguration.getActivationConfigurations().values()) {
            Object probability = activationConfiguration.getProbability();
            if(probability == null) {
                // TODO: parameters should cover whole operation
            } else if(probability instanceof String) {
                if(!"uniform".equals(probability)) {
                    errors.add(new ConfigurationCheckingError(String.format("Value %s for probability in activation configuration is not allowed", probability.toString())));
                }
            } else {
                // Map<String, Object> or Map<String, Map<String, String>>?
            }
        }

    }

}
