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
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
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
import java.util.stream.Collectors;

@Singleton
public class UIInteractionHandler {

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
		if(!(realTimeSimulator.getConfig() instanceof SimulationModelConfiguration)) {
			return;
		}
		SimulationModelConfiguration config = (SimulationModelConfiguration) realTimeSimulator.getConfig();
		List<UIListenerConfiguration> uiListeners = config.getUiListenerConfigurations();
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

		// TODO: Handle parameter predicates
		for(UIListenerConfiguration uiListener : uiListenersForEvent) {
			for(String activatingEvent : uiListener.getActivating()) {
				realTimeSimulator.handleOperationConfiguration(transition.getDestination(),  realTimeSimulator.getActivationConfigurationMap().get(activatingEvent), new ArrayList<>(), "1=1");
				anyActivated = true;
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
		if(!realTimeSimulator.isRunning()) {
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
		timestamps.add(realTimeSimulator.getTime());
	}

	private List<ActivationConfiguration> createUserInteractions(RealTimeSimulator realTimeSimulator) {
		SimulationModelConfiguration simulationModelConfiguration = (SimulationModelConfiguration) realTimeSimulator.getConfig();
		List<UIListenerConfiguration> uiListeners = simulationModelConfiguration.getUiListenerConfigurations();
		List<ActivationConfiguration> userInteractions = new ArrayList<>();
		for(int interactionCounter = 0; interactionCounter < userTransitions.size(); interactionCounter++) {
			userInteractions.add(createUserInteraction(interactionCounter, uiListeners));
		}
		return userInteractions;
	}

	private ActivationConfiguration createUserInteraction(int interactionCounter, List<UIListenerConfiguration> uiListeners) {
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
		List<String> activations = resolveActivations(op, uiListeners);

		return new ActivationOperationConfiguration(id, op, String.valueOf(time), 0, null, ActivationOperationConfiguration.ActivationKind.MULTI, fixedVariables, null, activations);
	}

	private List<String> resolveActivations(String op, List<UIListenerConfiguration> uiListeners) {
		List<String> activations = new ArrayList<>();
		for (UIListenerConfiguration uiListener : uiListeners) {
			// TODO: Handle predicate
			if (uiListener.getEvent().equals(op)) {
				activations = uiListener.getActivating();
				break;
			}
		}
		return activations;
	}

	private List<ActivationConfiguration> createActivationConfigurationsFromUserInteraction(List<ActivationConfiguration> activationConfigurations, List<ActivationConfiguration> userInteractions) {
		List<ActivationConfiguration> activationConfigurationsForResult = new ArrayList<>();

		boolean hasSetupConstants = false;
		boolean hasInitialization = false;
		List<String> activations = new ArrayList<>();
		for(ActivationConfiguration activationConfiguration : activationConfigurations) {
			if(Transition.INITIALISE_MACHINE_NAME.equals(activationConfiguration.getId())) {
				hasInitialization = true;
				ActivationOperationConfiguration initializationConfiguration = (ActivationOperationConfiguration) activationConfiguration;
				activations = new ArrayList<>(initializationConfiguration.getActivating());
				activations.addAll(userInteractions.stream().map(ActivationConfiguration::getId).collect(Collectors.toList()));
				activationConfigurationsForResult.add(new ActivationOperationConfiguration(Transition.INITIALISE_MACHINE_NAME, Transition.INITIALISE_MACHINE_NAME, initializationConfiguration.getAfter(), initializationConfiguration.getPriority(), initializationConfiguration.getAdditionalGuards(), initializationConfiguration.getActivationKind(), initializationConfiguration.getFixedVariables(), initializationConfiguration.getProbabilisticVariables(), activations));
			} else if(Transition.SETUP_CONSTANTS_NAME.equals(activationConfiguration.getId())) {
				hasSetupConstants = true;
			} else {
				activationConfigurationsForResult.add(activationConfiguration);
			}
		}

		if(!hasSetupConstants) {
			PersistentTrace persistentTrace = new PersistentTrace(currentTrace.get());
			if(Transition.SETUP_CONSTANTS_NAME.equals(persistentTrace.getTransitionList().get(0).getOperationName())) {
				OperationInfo opInfo = currentTrace.getStateSpace().getLoadedMachine().getMachineOperationInfo(Transition.SETUP_CONSTANTS_NAME);
				State destination = setupConstantsTransition.getDestination();
				// Somehow the constructor with 1 argument always sets using destination state to false
				Map<String, String> fixedVariables = SimulationCreator.createFixedVariables(SimulationCreator.computeFixedVariablesFromDestinationValues(destination.getConstantValues(FormulaExpand.EXPAND)), opInfo);
				activationConfigurationsForResult.add(0, new ActivationOperationConfiguration(Transition.SETUP_CONSTANTS_NAME, Transition.SETUP_CONSTANTS_NAME, null, 0, null, ActivationOperationConfiguration.ActivationKind.MULTI, fixedVariables, null, new ArrayList<>()));
			}
		}

		if(!hasInitialization) {
			activations.addAll(userInteractions.stream().map(ActivationConfiguration::getId).collect(Collectors.toList()));
			OperationInfo opInfo = currentTrace.getStateSpace().getLoadedMachine().getMachineOperationInfo(Transition.INITIALISE_MACHINE_NAME);
			// Somehow the constructor with 1 argument always sets using destination state to false
			State destination = initializationTransition.getDestination();
			Map<String, String> fixedVariables = SimulationCreator.createFixedVariables(SimulationCreator.computeFixedVariablesFromDestinationValues(destination.getVariableValues(FormulaExpand.EXPAND)), opInfo);
			activationConfigurationsForResult.add(0, new ActivationOperationConfiguration(Transition.INITIALISE_MACHINE_NAME, Transition.INITIALISE_MACHINE_NAME, null, 0, null, ActivationOperationConfiguration.ActivationKind.MULTI, fixedVariables, null, activations));
		}

		activationConfigurationsForResult.addAll(userInteractions);
		return activationConfigurationsForResult;
	}

	public SimulationModelConfiguration createUserInteractionSimulation(RealTimeSimulator realTimeSimulator) {
		SimulationModelConfiguration simulationModelConfiguration = (SimulationModelConfiguration) realTimeSimulator.getConfig();
		List<ActivationConfiguration> activationConfigurations = simulationModelConfiguration.getActivationConfigurations();
		List<ActivationConfiguration> userInteractions = createUserInteractions(realTimeSimulator);
		List<ActivationConfiguration> activationConfigurationsForResult = createActivationConfigurationsFromUserInteraction(activationConfigurations, userInteractions);
		return new SimulationModelConfiguration(activationConfigurationsForResult, new ArrayList<>(), SimulationModelConfiguration.metadataBuilder(SimulationModelConfiguration.SimulationFileType.INTERACTION_REPLAY).withSavedNow().withUserCreator().build());
	}

	public ObjectProperty<Transition> getLastUserInteraction() {
		return lastUserInteraction;
	}

}
