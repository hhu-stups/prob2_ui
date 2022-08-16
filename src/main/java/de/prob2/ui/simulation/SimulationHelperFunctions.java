package de.prob2.ui.simulation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.State;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.simulation.configuration.SimulationFileHandler;
import de.prob2.ui.simulation.simulators.Simulator;

import javafx.scene.control.Alert;
import javafx.stage.Window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulationHelperFunctions {

	public enum EvaluationMode {
		CLASSICAL_B, EVENT_B
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(SimulationHelperFunctions.class);

	public static EvaluationMode extractMode(AbstractModel model) {
		// TODO: Handle mode for other formalisms
		return model instanceof ClassicalBModel ? SimulationHelperFunctions.EvaluationMode.CLASSICAL_B :
				model instanceof EventBModel? SimulationHelperFunctions.EvaluationMode.EVENT_B : null;
	}

	public static AbstractEvalResult evaluateForSimulation(State state, String formula, EvaluationMode mode) {
		// TODO: Handle mode for other formalisms
		switch (mode) {
			case CLASSICAL_B:
				return state.eval(new ClassicalB(formula, FormulaExpand.TRUNCATE));
			case EVENT_B:
				return state.eval(new EventB(formula, FormulaExpand.TRUNCATE));
			default:
				throw new RuntimeException("Evaluation mode is not supported");
		}
	}

	public static void initSimulator(StageManager stageManager, Window window, Simulator simulator, Path path) {
		try {
			simulator.initSimulator(SimulationFileHandler.constructConfigurationFromJSON(path));
		} catch (IOException e) {
			LOGGER.debug("Tried to load simulation configuration file", e);
			alert(stageManager, window, e, "simulation.error.header.fileNotFound","simulation.error.body.fileNotFound");
		} catch (RuntimeException e) {
			LOGGER.debug("Errors in simulation configuration file detected", e);
			alert(stageManager, window, e, "simulation.error.header.configurationError", "simulation.error.body.configurationError");
		}
	}


	private static void alert(StageManager stageManager, Window window, Throwable ex, String header, String body, Object... params){
		final Alert alert = stageManager.makeExceptionAlert(ex, header, body, params);
		alert.initOwner(window);
		alert.showAndWait();
	}

	public static Map<String, String> mergeValues(Map<String, String> values1, Map<String, String> values2) {
		if(values1 == null) {
			return values2;
		}

		if(values2 == null) {
			return values1;
		}

		Map<String, String> newValues = new HashMap<>(values1);
		newValues.putAll(values2);
		return newValues;
	}

}
