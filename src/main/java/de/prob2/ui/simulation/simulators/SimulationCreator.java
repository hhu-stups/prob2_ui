package de.prob2.ui.simulation.simulators;

import com.google.inject.Singleton;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
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

@Singleton
public class SimulationCreator {

    public SimulationConfiguration createConfiguration(Trace trace, List<Integer> timestamps) {
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
            String nextOp = nextTransitionName == null ? null : "$setup_constants".equals(nextTransitionName) || "$initialse_machine".equals(nextTransitionName) ? nextTransitionName : nextTransitionName + "_" + j;
            String id = "$setup_constants".equals(op) || "$initialise_machine".equals(op) ? op : op + "_" + i;

            int time = timestamps.get(i) - currentTimestamp;
            Map<String, String> fixedVariables = SimulationHelperFunctions.mergeValues(transition.getParameters(), transition.getDestinationStateVariables());
            Map<String, String> newFixedVariables = new HashMap<>(fixedVariables);
            if(opInfo != null) {
                for (String key : fixedVariables.keySet()) {
                    if (!opInfo.getNonDetWrittenVariables().contains(key) && !opInfo.getParameterNames().contains(key)) {
                        newFixedVariables.remove(key);
                    }
                }
            }
            fixedVariables = newFixedVariables.isEmpty() ? null : newFixedVariables;

            List<String> activations = "$setup_constants".equals(op) || nextOp == null ? null : Collections.singletonList(nextOp);
            ActivationOperationConfiguration activationConfig = new ActivationOperationConfiguration(id, op, String.valueOf(time), 0, "1=1", ActivationOperationConfiguration.ActivationKind.MULTI, fixedVariables, null, activations);
            activationConfigurations.add(activationConfig);
            currentTimestamp = timestamps.get(i);
        }
        return new SimulationConfiguration(activationConfigurations);
    }

}
