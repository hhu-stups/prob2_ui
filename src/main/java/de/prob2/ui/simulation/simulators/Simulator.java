package de.prob2.ui.simulation.simulators;

import de.prob.animator.command.GetPreferenceCommand;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.ISimulationModelConfiguration;
import de.prob2.ui.simulation.configuration.SimulationExternalConfiguration;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.configuration.SimulationModelConfigurationChecker;
import de.prob2.ui.simulation.external.ExternalSimulationStep;
import de.prob2.ui.simulation.external.ExternalSimulatorExecutor;
import de.prob2.ui.simulation.simulators.check.ISimulationPropertyChecker;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

public abstract class Simulator {

	protected ISimulationModelConfiguration config;

	protected Map<String, String> variables;

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

	protected ExternalSimulatorExecutor externalSimulatorExecutor;

	public Simulator(final CurrentTrace currentTrace) {
		super();
		this.currentTrace = currentTrace;
		this.simulationEventHandler = new SimulationEventHandler(this, currentTrace);
		this.time = new SimpleIntegerProperty(0);
		this.stepCounter = 0;
		this.delay = 0;
		this.noActivationQueued = false;
		this.variables = new ConcurrentHashMap<>();
		this.configurationToActivation = new ConcurrentHashMap<>();
		this.activationConfigurationMap = new ConcurrentHashMap<>();
		this.activationConfigurationsSorted = new CopyOnWriteArrayList<>();
		this.operationToActivations = new ConcurrentHashMap<>();
		this.timestamps = new CopyOnWriteArrayList<>();

		this.traceListener = (observable, from, to) -> {
			if(config != null && to != null && to.getStateSpace() != null) {
				setPreferences(to);
			}
		};
	}


	public void initSimulator(ISimulationModelConfiguration config) {
		this.config = config;
		if(currentTrace.get() != null && currentTrace.getStateSpace() != null) {
			setPreferences(currentTrace.get());
		} else {
			currentTrace.addListener(traceListener);
		}
		resetSimulator();
	}

	public void resetSimulator() {
		this.variables = new ConcurrentHashMap<>();
		this.configurationToActivation = new ConcurrentHashMap<>();
		this.activationConfigurationMap = new ConcurrentHashMap<>();
		this.activationConfigurationsSorted = new CopyOnWriteArrayList<>();
		this.operationToActivations = new ConcurrentHashMap<>();
		this.timestamps = new CopyOnWriteArrayList<>();

		this.delay = 0;
		this.time.set(0);
		this.stepCounter = 0;
		this.noActivationQueued = false;

		if(config != null) {
			if(config instanceof SimulationModelConfiguration) {
				SimulationModelConfiguration modelConfig = (SimulationModelConfiguration) config;
				// sort after priority
				this.variables = modelConfig.getVariables() != null ? new HashMap<>(modelConfig.getVariables()) : new HashMap<>();
				this.activationConfigurationsSorted = modelConfig.getActivationConfigurations().stream()
						.filter(activationConfiguration -> activationConfiguration instanceof ActivationOperationConfiguration)
						.map(activationConfiguration -> (ActivationOperationConfiguration) activationConfiguration)
						.sorted(Comparator.comparingInt(ActivationOperationConfiguration::getPriority))
						.collect(Collectors.toList());

				modelConfig.getActivationConfigurations().forEach(activationConfiguration -> activationConfigurationMap.put(activationConfiguration.getId(), activationConfiguration));
				modelConfig.getActivationConfigurations().stream()
						.filter(activationConfiguration -> activationConfiguration instanceof ActivationOperationConfiguration)
						.map(activationConfiguration -> (ActivationOperationConfiguration) activationConfiguration)
						.forEach(activationConfiguration -> {
							String opName = activationConfiguration.getOpName();
							operationToActivations.putIfAbsent(opName, new HashSet<>());
							operationToActivations.get(opName).add(activationConfiguration.getId());
						});
				activationConfigurationsSorted.forEach(config -> configurationToActivation.put(config.getId(), new ArrayList<>()));
				currentTrace.removeListener(traceListener);
			} else if(config instanceof SimulationExternalConfiguration) {
				if(this.externalSimulatorExecutor == null) {
					this.externalSimulatorExecutor = new ExternalSimulatorExecutor(this, ((SimulationExternalConfiguration) config).getExternalPath(), (ClassicalBModel) currentTrace.getModel());
					this.externalSimulatorExecutor.start();
				} else {
					if(!this.externalSimulatorExecutor.getPythonFile().equals(((SimulationExternalConfiguration) config).getExternalPath())) {
						this.externalSimulatorExecutor.close();
						this.externalSimulatorExecutor = new ExternalSimulatorExecutor(this, ((SimulationExternalConfiguration) config).getExternalPath(), (ClassicalBModel) currentTrace.getModel());
						this.externalSimulatorExecutor.start();
					} else {
						this.externalSimulatorExecutor.reset();
					}
				}
				currentTrace.removeListener(traceListener);
			}
		}
	}

	private ActivationOperationConfiguration createDynamicActivation(String id, String op, String time, int priority, String additionalGuards, ActivationOperationConfiguration.ActivationKind activationKind,
										 Map<String, String> fixedVariables, Object probabilisticVariables, List<String> activations, boolean activatingOnlyWhenExecuted,
										 Map<String, String> updating, String withPredicate) {
		ActivationOperationConfiguration activationConfig = new ActivationOperationConfiguration(id, op, time, priority, additionalGuards, activationKind, fixedVariables, probabilisticVariables, activations, activatingOnlyWhenExecuted, updating, withPredicate);
		if(!activationConfigurationsSorted.contains(activationConfig)) {
			this.activationConfigurationsSorted.add(activationConfig);
		}
		if (!operationToActivations.containsKey(op)) {
			operationToActivations.put(op, new HashSet<>());
			operationToActivations.get(op).add(id);
		}

		activationConfigurationMap.putIfAbsent(id, activationConfig);
		configurationToActivation.putIfAbsent(id, new ArrayList<>());
		return activationConfig;
	}

	protected void setPreferences(Trace trace) {
		if(config instanceof SimulationModelConfiguration) {
			SimulationModelConfiguration modelConfig = (SimulationModelConfiguration) config;
			SimulationModelConfigurationChecker simulationConfigurationChecker = new SimulationModelConfigurationChecker(trace.getStateSpace(), modelConfig);
			simulationConfigurationChecker.check();
			if(!simulationConfigurationChecker.getErrors().isEmpty()) {
				throw new RuntimeException(simulationConfigurationChecker.getErrors().stream().map(Throwable::getMessage).collect(Collectors.joining("\n")));
			}
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
				break;
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
		if(config instanceof SimulationExternalConfiguration) {
			createDynamicActivation("$setup_constants", "$setup_constants", "0", 0,
					null, ActivationOperationConfiguration.ActivationKind.SINGLE, null, null, null,
					true, null, null);
		}
		if(configurationToActivation.containsKey(operation)) {
			ActivationOperationConfiguration setupConfiguration = (ActivationOperationConfiguration) activationConfigurationMap.get(operation);
			simulationEventHandler.activateOperation(trace.getCurrentState(), setupConfiguration, new ArrayList<>(), "1=1");
		}
	}

	public void setupBeforeSimulation(Trace trace) {
		updateStartingInformation(trace);
		if(!trace.getCurrentState().isInitialised()) {
			activateBeforeInitialisation(trace, Transition.SETUP_CONSTANTS_NAME);
			if(!(config instanceof SimulationExternalConfiguration)) {
				activateBeforeInitialisation(trace, Transition.INITIALISE_MACHINE_NAME);
			}
		}
	}

	protected Trace executeActivatedOperations(Trace trace) {
		Trace newTrace = trace;
		for(ActivationOperationConfiguration opConfig : activationConfigurationsSorted) {
			if (endingConditionReached(newTrace)) {
				if(externalSimulatorExecutor != null) {
					externalSimulatorExecutor.sendFinish();
				}
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
				simulationEventHandler.updateVariables(newTrace.getCurrentState(), variables, activationConfig.getUpdating());
				if(config instanceof SimulationExternalConfiguration) {

					if(!externalSimulatorExecutor.isDone()) {
						FutureTask<ExternalSimulationStep> stepFuture = externalSimulatorExecutor.execute(newTrace);

						ExternalSimulationStep step = null;
						try {
							step = stepFuture.get();
						} catch (Exception e) {
							e.printStackTrace();
						}

						if (step == null) {
							return trace;
						}

						ActivationOperationConfiguration newActivation = createDynamicActivation(step.getOp(), step.getOp(), step.getDelta(), 0,
								null, ActivationOperationConfiguration.ActivationKind.SINGLE, null, null, null,
								true, null, step.getPredicate());
						simulationEventHandler.activateOperation(newTrace.getCurrentState(), newActivation, new ArrayList<>(), "1=1");
					}
				}
			} else if("skip".equals(activation.getOperation())) {
				updateStartingInformation(newTrace);
				simulationEventHandler.activateOperations(newTrace.getCurrentState(), activationConfiguration, new ArrayList<>(), "1=1");
				simulationEventHandler.updateVariables(newTrace.getCurrentState(), variables, activationConfig.getUpdating());
			} else if(!activationConfig.isActivatingOnlyWhenExecuted()) {
				simulationEventHandler.activateOperations(newTrace.getCurrentState(), activationConfiguration, new ArrayList<>(), "1=1");
			}
		}
		return newTrace;
	}

	public void updateStartingInformation(Trace trace) {
		// This is used in Monte Carlo Simulation to check when the starting condition is reached
	}

	public void handleOperationConfiguration(State state, ActivationConfiguration activationConfiguration, List<String> parametersAsString, String parameterPredicates) {
		simulationEventHandler.handleOperationConfiguration(state, activationConfiguration, parametersAsString, parameterPredicates);
	}

	public boolean endingConditionReached(Trace trace) {
		return noActivationQueued;
	}

	public ISimulationModelConfiguration getConfig() {
		return config;
	}

	public Map<String, String> getVariables() {
		return variables;
	}

	public IntegerProperty timeProperty() {
		return time;
	}

	public int getTime() {
		return time.get();
	}

	public abstract void run(ISimulationPropertyChecker simulationPropertyChecker);

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

	public boolean hasNoActivationQueued() {
		return noActivationQueued;
	}

	public SimulationEventHandler getSimulationEventHandler() {
		return simulationEventHandler;
	}
}
