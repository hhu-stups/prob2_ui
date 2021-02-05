package de.prob2.ui.simulation.simulators;

import com.google.inject.Singleton;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.json.JsonManager;
import de.prob.statespace.Trace;
import de.prob2.ui.simulation.SimulationHelperFunctions;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;


import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Singleton
public class SimulationSaver {

    private final JsonManager<SimulationConfiguration> jsonManager;

    private final SimulationCreator simulationCreator;

    public SimulationSaver(final JsonManager<SimulationConfiguration> jsonManager, final SimulationCreator simulationCreator) {
        this.jsonManager = jsonManager;
        this.simulationCreator = simulationCreator;
    }


    public void saveConfiguration(Trace trace, List<Integer> timestamps, Path location) throws IOException {
        SimulationConfiguration configuration = simulationCreator.createConfiguration(trace, timestamps);
        this.jsonManager.writeToFile(location, configuration);
    }

}
