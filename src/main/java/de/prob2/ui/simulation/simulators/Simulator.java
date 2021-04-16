package de.prob2.ui.simulation.simulators;

import de.prob.animator.command.GetPreferenceCommand;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfigurationChecker;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Simulator {

	protected SimulationConfiguration config;

	protected IntegerProperty time;

	protected int delay;

	protected int stepCounter;

	protected Map<String, List<Activation>> configurationToActivation;

	protected List<ActivationOperationConfiguration> activationConfigurationsSorted;

	protected Map<String, ActivationConfiguration> activationConfigurationMap;

	protected Map<String, Set<String>> operationToActivations;

	protected final CurrentTrace currentTrace;

	protected final SimulationEventHandler simulationEventHandler;

	protected ChangeListener<? super Trace> traceListener;

	protected List<Integer> timestamps;

	protected int maxTransitionsBeforeInitialisation;

	protected int maxTransitions;

	protected boolean noActivationQueued;

	public Simulator(final CurrentTrace currentTrace) {
		super();
		this.currentTrace = currentTrace;
		this.simulationEventHandler = new SimulationEventHandler(this, currentTrace);
		this.time = new SimpleIntegerProperty(0);
		this.noActivationQueued = false;

		this.traceListener = (observable, from, to) -> {
			if(config != null && to != null && to.getStateSpace() != null) {
				setPreferences(to);
			}
		};
	}


	public void initSimulator(SimulationConfiguration config) throws IOException {
		this.config = config;
		if(currentTrace.get() != null && currentTrace.getStateSpace() != null) {
			setPreferences(currentTrace.get());
		} else {
			currentTrace.addListener(traceListener);
		}
		resetSimulator();
	}

	public void resetSimulator() {
		this.configurationToActivation = new HashMap<>();
		this.activationConfigurationMap = new HashMap<>();
		this.operationToActivations = new HashMap<>();
		this.timestamps = new ArrayList<>();

		this.delay = 0;
		this.time.set(0);
		this.stepCounter = 0;
		if(config != null) {
			// sort after priority
			this.activationConfigurationsSorted = config.getActivationConfigurations().stream()
					.filter(activationConfiguration -> activationConfiguration instanceof ActivationOperationConfiguration)
					.map(activationConfiguration -> (ActivationOperationConfiguration) activationConfiguration)
					.sorted(Comparator.comparingInt(ActivationOperationConfiguration::getPriority))
					.collect(Collectors.toList());

			config.getActivationConfigurations().forEach(activationConfiguration -> activationConfigurationMap.put(activationConfiguration.getId(), activationConfiguration));
			config.getActivationConfigurations().stream()
					.filter(activationConfiguration -> activationConfiguration instanceof ActivationOperationConfiguration)
					.map(activationConfiguration -> (ActivationOperationConfiguration) activationConfiguration)
					.forEach(activationConfiguration -> {
						String opName = activationConfiguration.getOpName();
						operationToActivations.putIfAbsent(opName, new HashSet<>());
						operationToActivations.get(opName).add(activationConfiguration.getId());
					});
			activationConfigurationsSorted.forEach(config -> configurationToActivation.put(config.getId(), new ArrayList<>()));
			currentTrace.removeListener(traceListener);
		}
	}

	protected void setPreferences(Trace trace) {
		SimulationConfigurationChecker simulationConfigurationChecker = new SimulationConfigurationChecker(trace.getStateSpace(), this.config);
		simulationConfigurationChecker.check();
		if(!simulationConfigurationChecker.getErrors().isEmpty()) {
			throw new RuntimeException(simulationConfigurationChecker.getErrors().stream().map(Throwable::getMessage).collect(Collectors.joining("\n")));
		}
		GetPreferenceCommand cmd = new GetPreferenceCommand("MAX_INITIALISATIONS");
		currentTrace.getStateSpace().execute(cmd);
		this.maxTransitionsBeforeInitialisation = Integer.parseInt(cmd.getValue());

		cmd = new GetPreferenceCommand("MAX_OPERATIONS");
		currentTrace.getStateSpace().execute(cmd);
		this.maxTransitions = Integer.parseInt(cmd.getValue());
		currentTrace.removeListener(traceListener);
	}

	public void updateRemainingTime() {
		updateRemainingTime(this.delay);
	}

	public void updateRemainingTime(int delay) {
		this.time.set(this.time.get() + delay);
		for(String key : configurationToActivation.keySet()) {
			for(Activation activation : configurationToActivation.get(key)) {
				activation.decreaseTime(delay);
			}
		}
	}

	public void updateDelay() {
		this.noActivationQueued = true;
		int delay = Integer.MAX_VALUE;
		for(List<Activation> activations : configurationToActivation.values()) {
			for(Activation activation : activations) {
				this.noActivationQueued = false;
				if(activation.getTime() < delay) {
					delay = activation.getTime();
				}
			}
		}
		this.delay = delay;
	}

	protected Trace simulationStep(Trace trace) {
		Trace newTrace = trace;
		updateRemainingTime();
		newTrace = executeActivatedOperations(newTrace);
		updateDelay();
		return newTrace;
	}

	private void activateBeforeInitialisation(Trace trace, String operation) {
		if(configurationToActivation.containsKey(operation)) {
			ActivationOperationConfiguration setupConfiguration = (ActivationOperationConfiguration) activationConfigurationMap.get(operation);
			simulationEventHandler.activateOperation(trace.getCurrentState(), setupConfiguration, new ArrayList<>(), "1=1");
		}
	}

	public void setupBeforeSimulation(Trace trace) {
		updateStartingInformation(trace);
		if(!trace.getCurrentState().isInitialised()) {
			activateBeforeInitialisation(trace, "$setup_constants");
			activateBeforeInitialisation(trace, "$initialise_machine");
		}
	}

	protected Trace executeActivatedOperations(Trace trace) {
		Trace newTrace = trace;
		for(ActivationOperationConfiguration opConfig : activationConfigurationsSorted) {
			if (endingConditionReached(newTrace)) {
				break;
			}
			newTrace = executeActivatedOperation(opConfig, newTrace);
		}
		return newTrace;
	}

	public Trace executeActivatedOperation(ActivationOperationConfiguration activationConfig, Trace trace) {
		String id = activationConfig.getId();
		List<String> activationConfiguration = activationConfig.getActivating();

		List<Activation> activationForOperation = configurationToActivation.get(id);
		List<Activation> activationForOperationCopy = new ArrayList<>(activationForOperation);

		Trace newTrace = trace;
		for(Activation activation : activationForOperationCopy) {
			//select operation only if its time is 0
			if(activation.getTime() > 0) {
				break;
			}
			activationForOperation.remove(activation);
			State currentState = newTrace.getCurrentState();
			Transition transition = simulationEventHandler.selectTransition(activation, currentState);
			if (transition != null) {
				newTrace = newTrace.add(transition);
				stepCounter++;
				updateStartingInformation(newTrace);
				List<String> parameterNames = transition.getParameterNames() == null ? new ArrayList<>() : transition.getParameterNames();
				String parameterPredicate = transition.getParameterPredicate() == null ? "1=1" : transition.getParameterPredicate();
				simulationEventHandler.activateOperations(newTrace.getCurrentState(), activationConfiguration, parameterNames, parameterPredicate);
				timestamps.add(time.get());
			}
		}
		return newTrace;
	}


	public void updateStartingInformation(Trace trace) {
		// This is used in Monte Carlo Simulation to check when the starting condition is reached
	}

	public boolean endingConditionReached(Trace trace) {
		return noActivationQueued;
	}

	public SimulationConfiguration getConfig() {
		return config;
	}

	public IntegerProperty timeProperty() {
		return time;
	}

	public int getTime() {
		return time.get();
	}

	protected abstract void run();

	public int getDelay() {
		return delay;
	}

	public List<Integer> getTimestamps() {
		return timestamps;
	}

	public int getMaxTransitions() {
		return maxTransitions;
	}

	public int getMaxTransitionsBeforeInitialisation() {
		return maxTransitionsBeforeInitialisation;
	}

	public Map<String, ActivationConfiguration> getActivationConfigurationMap() {
		return activationConfigurationMap;
	}

	public Map<String, List<Activation>> getConfigurationToActivation() {
		return configurationToActivation;
	}

	public Map<String, Set<String>> getOperationToActivations() {
		return operationToActivations;
	}
}
