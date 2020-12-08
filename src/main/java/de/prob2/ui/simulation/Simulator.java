package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.verifications.modelchecking.ModelCheckingJobItem;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
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

	private ChangeListener<Trace> listener;

    private SimulationConfiguration config;

    private IntegerProperty time;

    private int interval;

	private Timer timer;

	private Map<String, Integer> initialOperationToRemainingTime;

	private Map<String, Integer> operationToRemainingTime;

	private final CurrentTrace currentTrace;

	private final Injector injector;

	private final BooleanProperty runningProperty;

	private final BooleanProperty executingOperationProperty;

	@Inject
	public Simulator(final CurrentTrace currentTrace, final Injector injector, final DisablePropertyController disablePropertyController) {
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.runningProperty = new SimpleBooleanProperty(false);
		this.executingOperationProperty = new SimpleBooleanProperty(false);
		this.time = new SimpleIntegerProperty(0);
		this.listener = (observable, from, to) -> {
			if(to != null) {
				if (!to.getCurrentState().isInitialised()) {
					setupBeforeSimulation();
				}
			}
		};
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
		this.time.set(0);
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
		setupBeforeSimulation();

		currentTrace.addListener(listener);

		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				executingOperationProperty.set(true);
				// Read trace and pass it through chooseOperation to avoid race condition
				Trace trace = currentTrace.get();
				State currentState = trace.getCurrentState();
				State finalState = currentTrace.getCurrentState();
				if(finalState.isInitialised()) {
					boolean endingTimeReached = config.getEndingTime() > 0 && time.get() >= config.getEndingTime();
					boolean endingConditionReached = false;
					if(!config.getEndingCondition().isEmpty()) {
						AbstractEvalResult endingConditionEvalResult = evaluateForSimulation(finalState, config.getEndingCondition());
						endingConditionReached = "TRUE".equals(endingConditionEvalResult.toString());
					}
					if (endingTimeReached || endingConditionReached) {
						this.cancel();
						currentTrace.removeListener(listener);
						executingOperationProperty.set(false);
						return;
					}
				}
				updateRemainingTime();
				executeOperations(currentTrace, currentState, trace);
				executingOperationProperty.set(false);
			}
		};
		timer.scheduleAtFixedRate(task, 0, interval);
	}

	public AbstractEvalResult evaluateForSimulation(State state, String formula) {
		// Note: Rodin parser does not have IF-THEN-ELSE nor REAL
		return state.eval(new ClassicalB(formula, FormulaExpand.EXPAND));
	}

	private void setupBeforeSimulation() {
		Trace trace = currentTrace.get();
		State currentState = trace.getCurrentState();
		if(!currentState.isInitialised()) {
			List<String> nextTransitions = trace.getNextTransitions().stream().map(Transition::getName).collect(Collectors.toList());
			if(nextTransitions.contains("$setup_constants")) {
				currentTrace.set(executeBeforeInitialisation("$setup_constants", config.getSetupConfigurations(), currentState, trace));
			}
			trace = currentTrace.get();
			currentState = trace.getCurrentState();
			nextTransitions = trace.getNextTransitions().stream().map(Transition::getName).collect(Collectors.toList());
			if(nextTransitions.contains("$initialise_machine")) {
				currentTrace.set(executeBeforeInitialisation("$initialise_machine", config.getSetupConfigurations(), currentState, trace));
			}
		}

		if(!config.getStartingCondition().isEmpty()) {
			trace = currentTrace.get();
			StateSpace stateSpace = currentTrace.getStateSpace();
			currentState = trace.getCurrentState();
			AbstractEvalResult startingResult = evaluateForSimulation(currentState, config.getStartingCondition());
			// Model Checking for goal
			/*if("FALSE".equals(startingResult.toString())) {
				final IModelCheckListener mcListener = new IModelCheckListener() {
					@Override
					public void updateStats(final String jobId, final long timeElapsed, final IModelCheckingResult result, final StateSpaceStats stats) {

					}

					@Override
					public void isFinished(final String jobId, final long timeElapsed, final IModelCheckingResult result, final StateSpaceStats stats) {
						if (result instanceof ITraceDescription) {
							Trace newTrace = ((ITraceDescription) result).getTrace(stateSpace);
							String firstID = newTrace.getTransitionList().get(0).getId()
							currentTrace.set();
						}
					}
				};
				ConsistencyChecker checker = new ConsistencyChecker(stateSpace, new ModelCheckingOptions().breadthFirst(true).recheckExisting(true).checkGoal(true), new ClassicalB(config.getStartingCondition(), FormulaExpand.EXPAND), mcListener);
				checker.call();
			}*/
		}

	}

	private String joinPredicateFromConfig(State currentState, List<VariableChoice> configs) {
		if(configs == null) {
			return "1=1";
		} else {
			return configs.stream()
					.map(VariableChoice::getChoice)
					.map(choice -> chooseVariableValues(currentState, choice))
					.collect(Collectors.joining(" & "));
		}
	}

	private Trace executeBeforeInitialisation(String operation, List<VariableChoice> configs, State currentState, Trace trace) {
		Transition nextTransition = currentState.findTransition(operation, joinPredicateFromConfig(currentState, configs));
		return trace.add(nextTransition);
	}

	private String chooseVariableValues(State currentState, List<VariableConfiguration> choice) {
		double ranDouble = Math.random();
		double minimumProbability = 0.0;
		VariableConfiguration chosenConfiguration = choice.get(0);

		//Choose configuration for execution
		for(VariableConfiguration config : choice) {
			AbstractEvalResult probabilityResult = evaluateForSimulation(currentState, config.getProbability());
			minimumProbability += Double.parseDouble(probabilityResult.toString());
			chosenConfiguration = config;
			if(minimumProbability > ranDouble) {
				break;
			}
		}

		Map<String, String> chosenValues = chosenConfiguration.getValues();
		List<String> conjuncts = new ArrayList<>();
		for(String key : chosenValues.keySet()) {
			AbstractEvalResult evalResult = evaluateForSimulation(currentState, chosenValues.get(key));
			conjuncts.add(key + " = " + evalResult.toString());
		}
		return String.join(" & ", conjuncts);
	}

    public void updateRemainingTime() {
		this.time.set(this.time.get() + this.interval);
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
			if(!config.getEndingCondition().isEmpty()) {
				AbstractEvalResult endingConditionEvalResult = evaluateForSimulation(newCurrentState, config.getEndingCondition());
				if ("TRUE".equals(endingConditionEvalResult.toString())) {
					return;
				}
			}
			String opName = opConfig.getOpName();
			//time for next execution has been delayed by a previous transition
			if(operationToRemainingTime.get(opName) > 0) {
				continue;
			}

			operationToRemainingTime.computeIfPresent(opName, (k, v) -> initialOperationToRemainingTime.get(opName));

			double ranDouble = Math.random();

			AbstractEvalResult evalResult = evaluateForSimulation(newCurrentState, opConfig.getProbability());

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
		currentTrace.removeListener(listener);
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

	public IntegerProperty timeProperty() {
		return time;
	}
}
