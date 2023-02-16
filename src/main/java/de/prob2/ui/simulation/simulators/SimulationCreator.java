package de.prob2.ui.simulation.simulators;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.json.JsonMetadata;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.SimulationHelperFunctions;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulationCreator {

	public static SimulationConfiguration createConfiguration(Trace trace, List<Integer> timestamps, boolean forSave, JsonMetadata metadata) {
		PersistentTrace persistentTrace = new PersistentTrace(trace);
		List<PersistentTransition> transitions = persistentTrace.getTransitionList();

		List<ActivationConfiguration> activationConfigurations = new ArrayList<>();
		int currentTimestamp = 0;

		for(int i = 0; i < transitions.size(); i++) {
			PersistentTransition transition = transitions.get(i);

			String op = transition.getOperationName();
			OperationInfo opInfo = trace.getStateSpace().getLoadedMachine().getMachineOperationInfo(op);
			int j = i + 1;


			String nextTransitionName = i >= transitions.size() - 1 ? null : transitions.get(j).getOperationName();
			String nextOp = nextTransitionName == null ? null : Transition.isArtificialTransitionName(nextTransitionName) ? nextTransitionName : nextTransitionName + "_" + j;
			String id = Transition.isArtificialTransitionName(op) ? op : op + "_" + i;

			int time = timestamps.get(i) - currentTimestamp;
			Map<String, String> fixedVariables = createFixedVariables(SimulationHelperFunctions.mergeValues(transition.getParameters(), transition.getDestinationStateVariables()), opInfo);
			fixedVariables = fixedVariables.isEmpty() ? null : fixedVariables;

			List<String> activations = Transition.SETUP_CONSTANTS_NAME.equals(op) || nextOp == null ? null : Collections.singletonList(nextOp);
			ActivationOperationConfiguration activationConfig = new ActivationOperationConfiguration(id, op, String.valueOf(time), 0, forSave ? null : "1=1", forSave ? null : ActivationOperationConfiguration.ActivationKind.MULTI, fixedVariables, null, activations);
			activationConfigurations.add(activationConfig);
			currentTimestamp = timestamps.get(i);
		}
		return new SimulationConfiguration(activationConfigurations, new ArrayList<>(), metadata);
	}

	public static Map<String, String> computeFixedVariablesFromDestinationValues(Map<IEvalElement, AbstractEvalResult> destinationValueMap) {
		Map<String, String> fixedVariables = new HashMap<>();
		for(IEvalElement key : destinationValueMap.keySet()) {
			String val = destinationValueMap.get(key).toString();
			fixedVariables.put(key.getCode(), val);
		}
		return fixedVariables;
	}

	public static Map<String, String> createFixedVariables(Map<String, String> fixedVariables, OperationInfo opInfo) {
		Map<String, String> newFixedVariables = new HashMap<>(fixedVariables);
		if(opInfo != null) {
			for (String key : fixedVariables.keySet()) {
				if (!opInfo.getNonDetWrittenVariables().contains(key) && !opInfo.getParameterNames().contains(key)) {
					newFixedVariables.remove(key);
				}
			}
		}
		fixedVariables = newFixedVariables.isEmpty() ? null : newFixedVariables;
		return fixedVariables;
	}

}
