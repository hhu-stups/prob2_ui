package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

@Singleton
public class Simulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Simulator.class);

    private SimulationConfiguration config;

	private Timer timer;

	private Map<String, Integer> initialOperationToRemainingTime;

	private Map<String, Integer> operationToRemainingTime;

	private final CurrentTrace currentTrace;

	private final BooleanProperty runningProperty;

	private final BooleanProperty executingOperationProperty;

	private boolean operationExecutedInThisStep;

	@Inject
	public Simulator(final CurrentTrace currentTrace, final DisablePropertyController disablePropertyController) {
		this.currentTrace = currentTrace;
		this.operationExecutedInThisStep = false;
		this.runningProperty = new SimpleBooleanProperty(false);
		this.executingOperationProperty = new SimpleBooleanProperty(false);
        disablePropertyController.addDisableExpression(this.executingOperationProperty);
	}

	public void initSimulator(File configFile) {
	    this.config = null;
	    try {
            this.config = SimulationFileHandler.constructConfigurationFromJSON(configFile);
        } catch (IOException e) {
            LOGGER.debug("Tried to load simulation configuration file");
            //TODO: Implement alert
            return;
        }
		this.initialOperationToRemainingTime = new HashMap<>();
		this.operationToRemainingTime = new HashMap<>();
	    initializeRemainingTime();
	}

	private void initializeRemainingTime() {
		config.getOperationConfigurations()
				.stream()
				.filter(config -> config.getTime() > 0)
				.forEach(config -> {
					operationToRemainingTime.put(config.getOpName(), config.getTime());
					initialOperationToRemainingTime.put(config.getOpName(), config.getTime());
				});
	}

	public void run() {
		this.timer = new Timer();
		runningProperty.set(true);
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				executingOperationProperty.set(true);
				// Read trace and pass it through chooseOperation to avoid race condition
				Trace trace = currentTrace.get();
				updateRemainingTime();
                chooseOperation(trace);
				operationExecutedInThisStep = false;
				executingOperationProperty.set(false);
			}
		};
		timer.scheduleAtFixedRate(task, 0, config.getTime());
	}

	public void chooseOperation(Trace trace) {
		if(!trace.getCurrentState().isInitialised()) {
			currentTrace.set(trace.randomAnimation(1));
			return;
		}

		// Pass trace through executeWithTime and executeWithProbability to avoid race condition
		executeWithTime(trace);
		if(operationExecutedInThisStep) {
			return;
		}
		executeWithProbability(trace);
    }

    public void updateRemainingTime() {
		for(String key : operationToRemainingTime.keySet()) {
			operationToRemainingTime.computeIfPresent(key, (k, v) -> v - config.getTime());
		}
	}

    private void executeWithTime(Trace trace) {

		List<String> executedOperation = new ArrayList<>();

		// Update operation that must be executed
		for(String key : operationToRemainingTime.keySet()) {
			int remainingTime = operationToRemainingTime.get(key);
			if(remainingTime <= 0) {
				executedOperation.add(key);
			}
		}

		// Execute operations that must be executed if possible
		Trace newTrace = trace;
        State currentState = trace.getCurrentState();
		while(executedOperation.size() > 0) {
			int previousExecutedOperationSize = executedOperation.size();
			List<String> removedExecutedOperations = new ArrayList<>();
			for(int i = 0; i < executedOperation.size(); i++) {
				String chosenOperation = executedOperation.get(i);
				if(currentState.getTransitions()
						.stream()
						.map(Transition::getName)
						.collect(Collectors.toList()).contains(chosenOperation)) {
					Transition nextTransition = currentState.findTransition(chosenOperation, "1=1");
					newTrace = newTrace.add(nextTransition);
					removedExecutedOperations.add(chosenOperation);
					operationToRemainingTime.computeIfPresent(chosenOperation, (k, v) -> initialOperationToRemainingTime.get(chosenOperation));
					operationExecutedInThisStep = true;
				}
			}
			executedOperation.removeAll(removedExecutedOperations);

			if(previousExecutedOperationSize == executedOperation.size()) {
				break;
			}
		}
		currentTrace.set(newTrace);
	}

    private void executeWithProbability(Trace trace) {
	    State currentState = trace.getCurrentState();

		//Calculate executable operations
		List<OperationConfiguration> possibleOperations = config.getOperationConfigurations()
				.stream()
				.filter(config -> config.getProbability() > 0.0)
				.collect(Collectors.toList());

		//Calculate executable operations that are enabled
		List<OperationConfiguration> enabledPossibleOperations = possibleOperations.stream()
				.filter(op -> currentState.getTransitions()
						.stream()
						.map(Transition::getName)
						.collect(Collectors.toSet()).contains(op.getOpName()))
				.collect(Collectors.toList());

		double ranDouble = Math.random();
		double minimumProbability = 0.0;
		String chosenOperation = "";

		//Choose operation for execution
		for(OperationConfiguration config : enabledPossibleOperations) {
			float newProbablity = (config.getProbability()/enabledPossibleOperations.size()) * possibleOperations.size();
			minimumProbability += newProbablity;
			chosenOperation = config.getOpName();
			if(minimumProbability > ranDouble) {
				break;
			}
		}

		//Execute chosen operation
		if(!"".equals(chosenOperation)) {
			Transition nextTransition = currentState.findTransition(chosenOperation, "1=1");
			Trace newTrace = trace.add(nextTransition);
			currentTrace.set(newTrace);
			operationExecutedInThisStep = true;
		}
	}

	public void stop() {
		if(timer != null) {
			timer.cancel();
			timer = null;
		}
		runningProperty.set(false);
	}

	public BooleanProperty runningPropertyProperty() {
		return runningProperty;
	}

	public boolean isRunning() {
		return runningProperty.get();
	}

}
