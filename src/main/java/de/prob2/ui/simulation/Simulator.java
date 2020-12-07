package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.FormulaExpand;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

@Singleton
public class Simulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Simulator.class);

    private SimulationConfiguration config;

    private int interval;

	private Timer timer;

	private Map<String, Integer> initialOperationToRemainingTime;

	private Map<String, Integer> operationToRemainingTime;

	private final CurrentTrace currentTrace;

	private final BooleanProperty runningProperty;

	private final BooleanProperty executingOperationProperty;

	@Inject
	public Simulator(final CurrentTrace currentTrace, final DisablePropertyController disablePropertyController) {
		this.currentTrace = currentTrace;
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
		calculateInterval();
	    initializeRemainingTime();
	}

	private void calculateInterval() {
		List<Integer> relevantTimes = new ArrayList<>(config.getOperationConfigurations().stream()
				.map(OperationConfiguration::getTime)
				.collect(Collectors.toList()));
		for(OperationConfiguration opConfig : config.getOperationConfigurations()) {
			if(opConfig.getDelay() != null) {
				relevantTimes.addAll(opConfig.getDelay().values());
			}
		}

		Optional<Integer> result = relevantTimes.stream().reduce(Simulator::gcd);
		result.ifPresent(integer -> interval = integer);
	}

	private static int gcd(int a, int b) {
		if(b == 0) {
			return a;
		}
		return gcd(b, a % b);
	}

	private void initializeRemainingTime() {
		config.getOperationConfigurations()
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
				execute(trace);
				executingOperationProperty.set(false);
			}
		};
		timer.scheduleAtFixedRate(task, 0, interval);
	}

	public void execute(Trace trace) {
		State currentState = trace.getCurrentState();
		// Pass trace through execute operations to avoid race condition
		if(!currentState.isInitialised()) {
			List<String> nextTransitions = trace.getNextTransitions().stream().map(Transition::getName).collect(Collectors.toList());
			if(nextTransitions.contains("$setup_constants")) {
				currentTrace.set(executeSetupConstants(currentState, trace));
			} else if(nextTransitions.contains("$initialise_machine")) {
				currentTrace.set(executeInitialisation(currentState, trace));
			}
		} else {
			executeOperations(currentTrace, currentState, trace);
		}
    }

    private Trace executeSetupConstants(State currentState, Trace trace) {
		List<VariableChoice> setupConfigs = config.getSetupConfigurations();
		Transition nextTransition;
		if(setupConfigs == null) {
			nextTransition = currentState.findTransition("$setup_constants", "1=1");
		} else {
			String predicate = setupConfigs.stream()
					.map(VariableChoice::getChoice)
					.map(choice -> chooseVariableValues(currentState, choice))
					.collect(Collectors.joining(" & "));
			nextTransition = currentState.findTransition("$setup_constants", predicate);
		}
		return trace.add(nextTransition);
	}

    private Trace executeInitialisation(State currentState, Trace trace) {
		List<VariableChoice> initialisationConfigs = config.getInitialisationConfigurations();
		Transition nextTransition;
		if(initialisationConfigs == null) {
			nextTransition = currentState.findTransition("$initialise_machine", "1=1");
		} else {
			String predicate = initialisationConfigs.stream()
					.map(VariableChoice::getChoice)
					.map(choice -> chooseVariableValues(currentState, choice))
					.collect(Collectors.joining(" & "));
			nextTransition = currentState.findTransition("$initialise_machine", predicate);
		}
		return trace.add(nextTransition);
	}

	private String chooseVariableValues(State currentState, List<VariableConfiguration> choice) {
		double ranDouble = Math.random();
		double minimumProbability = 0.0;
		VariableConfiguration chosenConfiguration = choice.get(0);

		//Choose configuration for execution
		for(VariableConfiguration config : choice) {
			AbstractEvalResult probabilityResult = currentState.eval(config.getProbability(), FormulaExpand.EXPAND);
			minimumProbability += Double.parseDouble(probabilityResult.toString());
			chosenConfiguration = config;
			if(minimumProbability > ranDouble) {
				break;
			}
		}

		Map<String, String> chosenValues = chosenConfiguration.getValues();
		List<String> conjuncts = new ArrayList<>();
		for(String key : chosenValues.keySet()) {
			AbstractEvalResult evalResult = currentState.eval(chosenValues.get(key), FormulaExpand.EXPAND);
			conjuncts.add(key + " = " + evalResult.toString());
		}
		return String.join(" & ", conjuncts);
	}

    public void updateRemainingTime() {
		for(String key : operationToRemainingTime.keySet()) {
			operationToRemainingTime.computeIfPresent(key, (k, v) -> v - interval);
		}
	}



	private void executeOperations(CurrentTrace currentTrace, State currentState, Trace trace) {
		//1. select operations where time <= 0
		List<OperationConfiguration> nextOperations = config.getOperationConfigurations().stream()
				.filter(opConfig -> operationToRemainingTime.get(opConfig.getOpName()) <= 0)
				.collect(Collectors.toList());


		//2. sort operations after priority (less number means higher priority)
		nextOperations.sort(Comparator.comparingInt(OperationConfiguration::getPriority));

		Trace newTrace = trace;
		State newCurrentState = currentState;

		for(OperationConfiguration opConfig : nextOperations) {
			String opName = opConfig.getOpName();
			//time for next execution has been delayed by a previous transition
			if(operationToRemainingTime.get(opName) > 0) {
				continue;
			}

			operationToRemainingTime.computeIfPresent(opName, (k, v) -> initialOperationToRemainingTime.get(opName));

			double ranDouble = Math.random();

			AbstractEvalResult evalResult = newCurrentState.eval(opConfig.getProbability(), FormulaExpand.EXPAND);

			//3. calculate probability for each operation whether it should be executed
			if(Double.parseDouble(evalResult.toString()) > ranDouble) {
				List<String> enabledOperations = newTrace.getNextTransitions().stream()
						.map(Transition::getName)
						.collect(Collectors.toList());

				//4. check whether operation is executable
				if (enabledOperations.contains(opName)) {
					List<VariableChoice> choices = opConfig.getVariableChoices();

					//5. execute operation and append to trace
					boolean operationExecuted = false;
					if(choices == null) {
						List<Transition> transitions = newCurrentState.getTransitions().stream()
								.filter(trans -> trans.getName().equals(opName))
								.collect(Collectors.toList());
						Random rand = new Random();
						Transition transition = transitions.get(rand.nextInt(transitions.size()));
						newTrace = newTrace.add(transition);
						newCurrentState = newTrace.getCurrentState();
						currentTrace.set(newTrace);
						operationExecuted = true;
					} else {
						State finalNewCurrentState = newCurrentState;
						String predicate = choices.stream()
								.map(VariableChoice::getChoice)
								.map(choice -> chooseVariableValues(finalNewCurrentState, choice))
								.collect(Collectors.joining(" & "));
						if(newCurrentState.getStateSpace().isValidOperation(newCurrentState, opName, predicate)) {
							Transition transition = newCurrentState.findTransition(opName, predicate);
							newTrace = newTrace.add(transition);
							newCurrentState = newTrace.getCurrentState();
							currentTrace.set(newTrace);
							operationExecuted = true;
						}
					}
					Map<String, Integer> delay = opConfig.getDelay();
					if(operationExecuted && delay != null) {
						for (String key : delay.keySet()) {
							operationToRemainingTime.computeIfPresent(key, (k, v) -> v + delay.get(key));
						}
					}
				}
			}
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

	public SimulationConfiguration getConfig() {
		return config;
	}
}
