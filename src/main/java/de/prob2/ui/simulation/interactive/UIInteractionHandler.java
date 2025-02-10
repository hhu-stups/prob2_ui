package de.prob2.ui.simulation.interactive;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.State;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.EvaluationMode;
import de.prob2.ui.simulation.configuration.DiagramConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.configuration.UIListenerConfiguration;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;
import de.prob2.ui.simulation.simulators.Scheduler;
import de.prob2.ui.simulation.simulators.SimulationCreator;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public final class UIInteractionHandler {
	private final ObjectProperty<Transition> lastUserInteraction;

	private final Scheduler scheduler;

	private final CurrentTrace currentTrace;

	private final CurrentProject currentProject;

	private final List<Transition> userTransitions;

	private final List<Integer> timestamps;

	// maps from event to list of corresponding UI listeners
	private final Map<String, List<UIListenerConfiguration>> uiListenerConfigurationMap;

	private Transition setupConstantsTransition;

	private Transition initializationTransition;

	@Inject
	public UIInteractionHandler(final Scheduler scheduler, final CurrentTrace currentTrace, final CurrentProject currentProject) {
		this.scheduler = scheduler;
		this.currentTrace = currentTrace;
		this.lastUserInteraction = new SimpleObjectProperty<>(null);
		this.userTransitions = new ArrayList<>();
		this.timestamps = new ArrayList<>();
		this.uiListenerConfigurationMap = new HashMap<>();
		this.currentProject = currentProject;
		initialize();
	}

	private void initialize() {
		currentProject.addListener((observable, from, to) -> reset());
		currentProject.currentMachineProperty().addListener((observable, from, to) -> reset());
	}

	public void loadUIListenersIntoSimulator(RealTimeSimulator realTimeSimulator) {
		if(!(realTimeSimulator.getConfig() instanceof SimulationModelConfiguration config)) {
			return;
		}
		List<UIListenerConfiguration> uiListeners = config.getListeners();
		for(UIListenerConfiguration uiListener : uiListeners) {
			String event = uiListener.getEvent();
			List<UIListenerConfiguration> uiListenersForEvent = uiListenerConfigurationMap.get(event);
			if(uiListenersForEvent == null) {
				// Do not use Collections.singletonList or Arrays.asList as suggested by IntelliJ as those lists are unmodifiable and lead to an UnsupportedOperationException
				List<UIListenerConfiguration> entries = new ArrayList<>();
				entries.add(uiListener);
				uiListenerConfigurationMap.put(event, entries);
			} else {
				uiListenerConfigurationMap.get(event).add(uiListener);
			}
		}
	}

	public void handleUserInteraction(RealTimeSimulator realTimeSimulator, Transition transition) {
		if(transition == null) {
			return;
		}
		boolean anyActivated = triggerSimulationBasedOnUserInteraction(realTimeSimulator, transition);
		if(anyActivated) {
			scheduler.runWithoutInitialisation();
		}
	}

	private boolean triggerSimulationBasedOnUserInteraction(RealTimeSimulator realTimeSimulator, Transition transition) {
		boolean anyActivated = false;

		List<UIListenerConfiguration> uiListenersForEvent = uiListenerConfigurationMap.get(transition.getName());

		if(uiListenersForEvent == null) {
			return false;
		}

		for(UIListenerConfiguration uiListener : uiListenersForEvent) {
			for(String activatingEvent : uiListener.getActivating()) {
				State destination = transition.getDestination();
				String parameterRes = realTimeSimulator.getSimulationEventHandler().evaluateWithParameters(destination, uiListener.getPredicate(), transition.getParameterNames(), transition.getParameterPredicate(), EvaluationMode.extractMode(currentTrace.getModel()));
				if(parameterRes.startsWith("TRUE")) {
					realTimeSimulator.handleOperationConfiguration(destination, realTimeSimulator.getActivationConfigurationMap().get(activatingEvent), transition.getParameterNames(), transition.getParameterPredicate());
					anyActivated = true;
				}
			}
		}

		return anyActivated;
	}

	public void reset() {
		userTransitions.clear();
		timestamps.clear();
		setupConstantsTransition = null;
		initializationTransition = null;
	}

	public void addUserInteraction(RealTimeSimulator realTimeSimulator, Transition transition) {
		if(realTimeSimulator.getConfig() == null) {
			return;
		}
		String name = transition.getName();
		if (Transition.SETUP_CONSTANTS_NAME.equals(name)) {
			setupConstantsTransition = transition;
			return;
		} else if(Transition.INITIALISE_MACHINE_NAME.equals(name)) {
			initializationTransition = transition;
			return;
		}
		lastUserInteraction.set(transition);
		userTransitions.add(transition);
		realTimeSimulator.getTimestamps().add(realTimeSimulator.getTime());
		timestamps.add(realTimeSimulator.getTime());
	}

	private List<DiagramConfiguration.NonUi> createUserInteractions(RealTimeSimulator realTimeSimulator) {
		SimulationModelConfiguration simulationModelConfiguration = (SimulationModelConfiguration) realTimeSimulator.getConfig();
		List<UIListenerConfiguration> uiListeners = simulationModelConfiguration.getListeners();
		List<DiagramConfiguration.NonUi> userInteractions = new ArrayList<>();
		for(int interactionCounter = 0; interactionCounter < userTransitions.size(); interactionCounter++) {
			userInteractions.add(createUserInteraction(realTimeSimulator, interactionCounter, uiListeners));
		}
		return userInteractions;
	}

		private DiagramConfiguration.NonUi createUserInteraction(RealTimeSimulator realTimeSimulator, int interactionCounter, List<UIListenerConfiguration> uiListeners) {
		Transition transition = userTransitions.get(interactionCounter);
		int time = timestamps.get(interactionCounter);

		String id = "user_interaction_" + interactionCounter;
		String op = transition.getName();
		Map<String, String> fixedVariables = new HashMap<>();
		for (int i = 0; i < transition.getParameterNames().size(); i++) {
			String name = transition.getParameterNames().get(i);
			String value = transition.getParameterValues().get(i);
			fixedVariables.put(name, value);
		}
		List<String> activations = resolveActivations(realTimeSimulator, transition, uiListeners);

		return new ActivationOperationConfiguration(id, op, String.valueOf(time), 0, null, ActivationOperationConfiguration.ActivationKind.MULTI, fixedVariables, null, activations, true, null, null);
	}

	private List<String> resolveActivations(RealTimeSimulator realTimeSimulator, Transition transition, List<UIListenerConfiguration> uiListeners) {
		String op = transition.getName();
		List<String> activations = new ArrayList<>();
		for (UIListenerConfiguration uiListener : uiListeners) {
			if (uiListener.getEvent().equals(op)) {
				State destination = transition.getDestination();
				String parameterRes = realTimeSimulator.getSimulationEventHandler().evaluateWithParameters(destination, uiListener.getPredicate(), transition.getParameterNames(), transition.getParameterPredicate(), EvaluationMode.extractMode(currentTrace.getModel()));
				if(parameterRes.startsWith("TRUE")) {
					activations.addAll(uiListener.getActivating());
				}
			}
		}
		return activations;
	}

	private List<DiagramConfiguration.NonUi> createActivationConfigurationsFromUserInteraction(List<DiagramConfiguration.NonUi> activationConfigurations, List<DiagramConfiguration.NonUi> userInteractions) {
		List<DiagramConfiguration.NonUi> activationConfigurationsForResult = new ArrayList<>();

		boolean hasSetupConstants = false;
		boolean hasInitialization = false;
		List<String> activations = new ArrayList<>();
		for(DiagramConfiguration.NonUi diagramConfiguration : activationConfigurations) {
			if(Transition.INITIALISE_MACHINE_NAME.equals(diagramConfiguration.getId())) {
				hasInitialization = true;
				ActivationOperationConfiguration initializationConfiguration = (ActivationOperationConfiguration) diagramConfiguration;
				activations = new ArrayList<>(initializationConfiguration.getActivating());
				activations.addAll(userInteractions.stream().map(DiagramConfiguration::getId).toList());
				activationConfigurationsForResult.add(new ActivationOperationConfiguration(Transition.INITIALISE_MACHINE_NAME, Transition.INITIALISE_MACHINE_NAME, initializationConfiguration.getAfter(), initializationConfiguration.getPriority(), initializationConfiguration.getAdditionalGuards(), initializationConfiguration.getActivationKind(), initializationConfiguration.getFixedVariables(), initializationConfiguration.getProbabilisticVariables(), activations, true, null, null));
			} else if(Transition.SETUP_CONSTANTS_NAME.equals(diagramConfiguration.getId())) {
				hasSetupConstants = true;
			} else {
				activationConfigurationsForResult.add(diagramConfiguration);
			}
		}

		if(!hasSetupConstants) {
			PersistentTrace persistentTrace = new PersistentTrace(currentTrace.get());
			if(Transition.SETUP_CONSTANTS_NAME.equals(persistentTrace.getTransitionList().get(0).getOperationName())) {
				OperationInfo opInfo = currentTrace.getStateSpace().getLoadedMachine().getMachineOperationInfo(Transition.SETUP_CONSTANTS_NAME);
				State destination = setupConstantsTransition.getDestination();
				// Somehow the constructor with 1 argument always sets using destination state to false
				Map<String, String> fixedVariables = SimulationCreator.createFixedVariables(SimulationCreator.computeFixedVariablesFromDestinationValues(destination.getConstantValues(FormulaExpand.EXPAND)), opInfo);
				activationConfigurationsForResult.add(0, new ActivationOperationConfiguration(Transition.SETUP_CONSTANTS_NAME, Transition.SETUP_CONSTANTS_NAME, null, 0, null, ActivationOperationConfiguration.ActivationKind.MULTI, fixedVariables, null, new ArrayList<>(), true, null, null));
			}
		}

		if(!hasInitialization) {
			activations.addAll(userInteractions.stream().map(DiagramConfiguration::getId).toList());
			OperationInfo opInfo = currentTrace.getStateSpace().getLoadedMachine().getMachineOperationInfo(Transition.INITIALISE_MACHINE_NAME);
			// Somehow the constructor with 1 argument always sets using destination state to false
			State destination = initializationTransition.getDestination();
			Map<String, String> fixedVariables = SimulationCreator.createFixedVariables(SimulationCreator.computeFixedVariablesFromDestinationValues(destination.getVariableValues(FormulaExpand.EXPAND)), opInfo);
			activationConfigurationsForResult.add(0, new ActivationOperationConfiguration(Transition.INITIALISE_MACHINE_NAME, Transition.INITIALISE_MACHINE_NAME, null, 0, null, ActivationOperationConfiguration.ActivationKind.MULTI, fixedVariables, null, activations, true, null, null));
		}

		activationConfigurationsForResult.addAll(userInteractions);
		return activationConfigurationsForResult;
	}

	public SimulationModelConfiguration createUserInteractionSimulation(RealTimeSimulator realTimeSimulator) {
		SimulationModelConfiguration simulationModelConfiguration = (SimulationModelConfiguration) realTimeSimulator.getConfig();
		Map<String, String> variables = simulationModelConfiguration.getVariables();
		List<DiagramConfiguration.NonUi> activationConfigurations = simulationModelConfiguration.getActivations();
		List<DiagramConfiguration.NonUi> userInteractions = createUserInteractions(realTimeSimulator);
		List<DiagramConfiguration.NonUi> activationConfigurationsForResult = createActivationConfigurationsFromUserInteraction(activationConfigurations, userInteractions);
		return new SimulationModelConfiguration(variables, activationConfigurationsForResult, new ArrayList<>(), SimulationModelConfiguration.metadataBuilder().build());
	}

	public ObjectProperty<Transition> getLastUserInteraction() {
		return lastUserInteraction;
	}

}
