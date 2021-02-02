package de.prob2.ui.simulation;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.statespace.State;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.simulators.ProbabilityBasedSimulator;
import javafx.scene.control.Alert;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class SimulationHelperFunctions {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimulationHelperFunctions.class);

    public static AbstractEvalResult evaluateForSimulation(State state, String formula) {
        // Note: Rodin parser does not have IF-THEN-ELSE nor REAL
        return state.eval(new ClassicalB(formula, FormulaExpand.TRUNCATE));
    }

	public static void initSimulator(StageManager stageManager, Window window, ProbabilityBasedSimulator simulator, File file) {
		try {
			simulator.initSimulator(file);
		} catch (IOException e) {
			LOGGER.debug("Tried to load simulation configuration file");
			alert(stageManager, window, e, "simulation.error.header.fileNotFound","simulation.error.body.fileNotFound");
		} catch (RuntimeException e) {
			LOGGER.debug("Errors in simulation configuration file detected");
			alert(stageManager, window, e, "simulation.error.header.configurationError", "simulation.error.body.configurationError", e.getMessage());
		}
	}


	private static void alert(StageManager stageManager, Window window, Throwable ex, String header, String body, Object... params){
		final Alert alert = stageManager.makeExceptionAlert(ex, header, body, params);
		alert.initOwner(window);
		alert.showAndWait();
	}

}
