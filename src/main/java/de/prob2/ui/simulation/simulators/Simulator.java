package de.prob2.ui.simulation.simulators;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;

import de.prob.animator.command.GetPreferenceCommand;
import de.prob.model.representation.XTLModel;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.configuration.ActivationKind;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.DiagramConfiguration;
import de.prob2.ui.simulation.configuration.ISimulationModelConfiguration;
import de.prob2.ui.simulation.configuration.SimulationExternalConfiguration;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.configuration.SimulationModelConfigurationChecker;
import de.prob2.ui.simulation.configuration.TransitionSelection;
import de.prob2.ui.simulation.external.ExternalSimulationStep;
import de.prob2.ui.simulation.external.ExternalSimulatorExecutor;
import de.prob2.ui.simulation.simulators.check.ISimulationPropertyChecker;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

public abstract class Simulator {

	protected final CurrentTrace currentTrace;
	protected final CurrentProject currentProject;
	private final Provider<ObjectMapper> objectMapperProvider;
	protected final SimulationEventHandler simulationEventHandler;
	protected final IntegerProperty time;
	protected final ChangeListener<? super Trace> traceListener;
	protected ISimulationModelConfiguration config;
	protected Map<String, String> variables;
	protected int delay;
	protected int stepCounter;
	protected Map<String, List<Activation>> configurationToActivation;
	protected List<ActivationOperationConfiguration> activationConfigurationsSorted;
	protected Map<String, DiagramConfiguration> activationConfigurationMap;
	protected Map<String, Set<String>> operationToActivations;
	protected List<Integer> timestamps;
	protected ObjectProperty<Activation> performedActivation;
	protected int maxTransitionsBeforeInitialisation;
	protected int maxTransitions;
	protected boolean noActivationQueued;
	protected ExternalSimulatorExecutor externalSimulatorExecutor;

	public Simulator(final CurrentTrace currentTrace, final CurrentProject currentProject, Provider<ObjectMapper> objectMapperProvider) {
		super();
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.objectMapperProvider = objectMapperProvider;
		this.simulationEventHandler = new SimulationEventHandler(this, currentTrace, currentProject);
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

		this.currentProject.addListener((observable, from, to) -> {
			if((from == null && to != null) || !Objects.equals(from, to)) {
				resetSimulator();
			}
		});

		this.currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			if((from == null && to != null) || !Objects.equals(from, to)) {
				resetSimulator();
			}
		});
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
		this.performedActivation = new SimpleObjectProperty<>(null);

		this.delay = 0;
		this.time.set(0);
		this.performedActivation.set(null);
		this.stepCounter = 0;
		this.noActivationQueued = false;

		if(config != null) {
			if(config instanceof SimulationModelConfiguration modelConfig) {
				// sort after priority
				this.variables = modelConfig.getVariables() != null ? new HashMap<>(modelConfig.getVariables()) : new HashMap<>();
				this.activationConfigurationsSorted = modelConfig.getActivations().stream()
						.filter(activationConfiguration -> activationConfiguration instanceof ActivationOperationConfiguration)
						.map(activationConfiguration -> (ActivationOperationConfiguration) activationConfiguration)
						.sorted(Comparator.comparingInt(ActivationOperationConfiguration::getPriority))
						.collect(Collectors.toList());

				modelConfig.getActivations().forEach(activationConfiguration -> activationConfigurationMap.put(activationConfiguration.getId(), activationConfiguration));
				modelConfig.getActivations().stream()
						.filter(activationConfiguration -> activationConfiguration instanceof ActivationOperationConfiguration)
						.map(activationConfiguration -> (ActivationOperationConfiguration) activationConfiguration)
						.forEach(activationConfiguration -> {
							String opName = activationConfiguration.getExecute();
							operationToActivations.putIfAbsent(opName, new HashSet<>());
							operationToActivations.get(opName).add(activationConfiguration.getId());
						});
				activationConfigurationsSorted.forEach(config -> configurationToActivation.put(config.getId(), new ArrayList<>()));
				if(this.externalSimulatorExecutor != null) {
					this.externalSimulatorExecutor.close();
					this.externalSimulatorExecutor = null;
				}
				currentTrace.removeListener(traceListener);
			} else if(config instanceof SimulationExternalConfiguration) {
				if(this.externalSimulatorExecutor == null) {
					this.externalSimulatorExecutor = new ExternalSimulatorExecutor(this.objectMapperProvider.get(), this, ((SimulationExternalConfiguration) config).getExternalPath());
					this.externalSimulatorExecutor.start();
				} else {
					if(this instanceof RealTimeSimulator || !this.externalSimulatorExecutor.getPythonFile().equals(((SimulationExternalConfiguration) config).getExternalPath())) {
						this.externalSimulatorExecutor.close();
						this.externalSimulatorExecutor = new ExternalSimulatorExecutor(this.objectMapperProvider.get(), this, ((SimulationExternalConfiguration) config).getExternalPath());
						this.externalSimulatorExecutor.start();
					} else {
						this.externalSimulatorExecutor.reset();
					}
				}
				currentTrace.removeListener(traceListener);
			}
		}
	}

	private ActivationOperationConfiguration createDynamicActivation(String id, String op, String time, int priority, String additionalGuards, ActivationKind activationKind,
	                                                                 Map<String, String> fixedVariables, Map<String, Map<String, String>> probabilisticVariables, TransitionSelection transitionSelection,
	                                                                 List<String> activations, boolean activatingOnlyWhenExecuted,
	                                                                 Map<String, String> updating, String withPredicate) {
		if(id == null || op == null) {
			throw new RuntimeException("Provided operation is null. There is an error when sending the operation to be executed from the external simulation.");
		}

		ActivationOperationConfiguration activationConfig = new ActivationOperationConfiguration(id, op, time, priority, additionalGuards, activationKind, fixedVariables, probabilisticVariables, transitionSelection, activations, activatingOnlyWhenExecuted, updating, withPredicate, "");
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
		if(config instanceof SimulationModelConfiguration modelConfig) {
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
		for (List<Activation> activations : configurationToActivation.values()) {
			for (int i = 0, len = activations.size(); i < len; i++) {
				activations.set(i, activations.get(i).decreaseTime(delay));
			}
		}
	}

	public void updateDelay() {
		this.noActivationQueued = true;
		int delay = Integer.MAX_VALUE;
		for(List<Activation> activations : configurationToActivation.values()) {
			for(Activation activation : activations) {
				this.noActivationQueued = false;
				if(activation.time() < delay) {
					delay = activation.time();
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
			processExternalConfiguration(trace);
		} else if(configurationToActivation.containsKey(operation)) {
			ActivationOperationConfiguration setupConfiguration = (ActivationOperationConfiguration) activationConfigurationMap.get(operation);
			simulationEventHandler.activateOperation(trace.getCurrentState(), setupConfiguration, new ArrayList<>(), "1=1");
		}
	}

	public void setupBeforeSimulation(Trace trace) {
		updateStartingInformation(trace);
		if (currentTrace.getModel() instanceof XTLModel) {
			activateBeforeInitialisation(trace, "start_xtl_system");
		} else if (!trace.getCurrentState().isInitialised()) {
			activateBeforeInitialisation(trace, Transition.SETUP_CONSTANTS_NAME);
			if (!(config instanceof SimulationExternalConfiguration)) {
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
		if(activationForOperation == null) {
			return trace;
		}
		List<Activation> activationForOperationCopy = new ArrayList<>(activationForOperation);

		Trace newTrace = trace;
		for(Activation activation : activationForOperationCopy) {
			//select operation only if its time is 0
			if(activation.time() > 0) {
				break;
			}
			activationForOperation.remove(activation);
			State currentState = newTrace.getCurrentState();
			Transition transition = simulationEventHandler.selectTransition(activation, currentState, variables);
			if (transition != null) {
				newTrace = newTrace.add(transition);
				stepCounter++;
				updateStartingInformation(newTrace);
				List<String> parameterNames = transition.getParameterNames() == null ? new ArrayList<>() : transition.getParameterNames();
				String parameterPredicate = transition.getParameterPredicate() == null ? "1=1" : transition.getParameterPredicate();
				simulationEventHandler.activateOperations(newTrace.getCurrentState(), activationConfiguration, parameterNames, parameterPredicate);
				timestamps.add(time.get());
				// Set null to make sure that property receives new updates although activations are equal
				performedActivation.set(null);
				performedActivation.set(activation);
				simulationEventHandler.updateVariables(newTrace.getCurrentState(), variables, activationConfig.getUpdating());
				processExternalConfiguration(newTrace);
			} else if("skip".equals(activation.operation())) {
				updateStartingInformation(newTrace);
				simulationEventHandler.activateOperations(newTrace.getCurrentState(), activationConfiguration, new ArrayList<>(), "1=1");
				simulationEventHandler.updateVariables(newTrace.getCurrentState(), variables, activationConfig.getUpdating());
				processExternalConfiguration(newTrace);
			} else if(!activationConfig.isActivatingOnlyWhenExecuted()) {
				simulationEventHandler.activateOperations(newTrace.getCurrentState(), activationConfiguration, new ArrayList<>(), "1=1");
				simulationEventHandler.updateVariables(newTrace.getCurrentState(), variables, activationConfig.getUpdating());
				processExternalConfiguration(newTrace);
			}
		}
		return newTrace;
	}

	private void processExternalConfiguration(Trace newTrace) {
		if(config instanceof SimulationExternalConfiguration) {
			if(!externalSimulatorExecutor.isDone()) {
				ExternalSimulationStep step = externalSimulatorExecutor.execute(newTrace);

				if (step == null) {
					return;
				}

				ActivationOperationConfiguration newActivation = createDynamicActivation(step.getOp(), step.getOp(), step.getDelta(), 0,
						null, ActivationKind.SINGLE, null, null, TransitionSelection.FIRST,
						null, true, null, step.getPredicate());
				simulationEventHandler.activateOperation(newTrace.getCurrentState(), newActivation, new ArrayList<>(), "1=1");
			}
		}
	}

	public void updateStartingInformation(Trace trace) {
		// This is used in Monte Carlo Simulation to check when the starting condition is reached
	}

	public void handleOperationConfiguration(State state, DiagramConfiguration activationConfiguration, List<String> parametersAsString, String parameterPredicates) {
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

	public ObjectProperty<Activation> performedActivationProperty() {
		return performedActivation;
	}

	public int getMaxTransitions() {
		return maxTransitions;
	}

	public int getMaxTransitionsBeforeInitialisation() {
		return maxTransitionsBeforeInitialisation;
	}

	public Map<String, DiagramConfiguration> getActivationConfigurationMap() {
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
