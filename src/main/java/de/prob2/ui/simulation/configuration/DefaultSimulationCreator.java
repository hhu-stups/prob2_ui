package de.prob2.ui.simulation.configuration;

import com.google.inject.Inject;
import de.prob.json.JsonMetadata;
import de.prob.statespace.LoadedMachine;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.model.SimulationModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.prob.statespace.Transition.INITIALISE_MACHINE_NAME;
import static de.prob.statespace.Transition.SETUP_CONSTANTS_NAME;

public class DefaultSimulationCreator {

	public static SimulationModelConfiguration createDefaultSimulation(LoadedMachine loadedMachine) {
		Map<String, String> variables = new HashMap<>();
		List<ActivationConfiguration> activations = new ArrayList<>();
		List<UIListenerConfiguration> uiListenerConfigurations = new ArrayList<>();
		JsonMetadata metadata = SimulationModelConfiguration.metadataBuilder(SimulationModelConfiguration.SimulationFileType.SIMULATION)
				.withUserCreator()
				.withSavedNow()
				.build();

		if(loadedMachine.containsOperations(SETUP_CONSTANTS_NAME)) {
			activations.add(new ActivationOperationConfiguration(SETUP_CONSTANTS_NAME, SETUP_CONSTANTS_NAME,
					"0", 0, null, ActivationOperationConfiguration.ActivationKind.MULTI, null, null, null, true, null, null));
		}
		activations.add(new ActivationOperationConfiguration(INITIALISE_MACHINE_NAME, INITIALISE_MACHINE_NAME,
				"0", 1, null, ActivationOperationConfiguration.ActivationKind.MULTI, null, null, null, true, null, null));

		return new SimulationModelConfiguration(variables, activations, uiListenerConfigurations, metadata);
	}

}
