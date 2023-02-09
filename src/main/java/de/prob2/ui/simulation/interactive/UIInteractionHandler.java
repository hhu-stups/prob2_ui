package de.prob2.ui.simulation.interactive;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.statespace.State;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.configuration.UIListenerConfiguration;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;
import de.prob2.ui.simulation.simulators.Scheduler;
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

	@Inject
	public UIInteractionHandler(final Scheduler scheduler, final CurrentTrace currentTrace, final CurrentProject currentProject) {
		this.scheduler = scheduler;
		this.currentTrace = currentTrace;
		this.lastUserInteraction = new SimpleObjectProperty<>(null);
		this.userTransitions = new ArrayList<>();
		this.timestamps = new ArrayList<>();
		this.currentProject = currentProject;
		initialize();
	}

	private void initialize() {
		currentProject.addListener((observable, from, to) -> reset());
		currentProject.currentMachineProperty().addListener((observable, from, to) -> reset());
	}

	public void handleUserInteraction(RealTimeSimulator realTimeSimulator, Transition transition) {
		if(transition == null) {
			return;
		}
		List<UIListenerConfiguration> uiListenerConfigurations = realTimeSimulator.getConfig().getUiListenerConfigurations();
		boolean anyActivated = false;
		State destinationState = transition.getDestination();
		for(UIListenerConfiguration uiListener : uiListenerConfigurations) {
			String event = uiListener.getEvent();
			// TODO: handle predicate
			List<String> activating = uiListener.getActivating();
			if(event.equals(transition.getName())) {
				// TODO: Handle parameter predicates
				for(String activatingEvent : activating) {
					realTimeSimulator.handleOperationConfiguration(destinationState,  realTimeSimulator.getActivationConfigurationMap().get(activatingEvent), new ArrayList<>(), "1=1");
					anyActivated = true;
				}
				break;
			}
		}
		if(anyActivated) {
			scheduler.runWithoutInitialisation();
		}
	}

	public void reset() {
		userTransitions.clear();
		timestamps.clear();
	}

	public void addUserInteraction(RealTimeSimulator realTimeSimulator, Transition transition) {
		if(!realTimeSimulator.isRunning()) {
			return;
		}
		String name = transition.getName();
		if ("$setup_constants".equals(name) || "$initialise_machine".equals(name)) {
			return;
		}
		lastUserInteraction.set(transition);
		userTransitions.add(transition);
		timestamps.add(realTimeSimulator.getTime());
	}

	private List<ActivationConfiguration> createUserInteractions(RealTimeSimulator realTimeSimulator) {
		List<UIListenerConfiguration> uiListeners = realTimeSimulator.getConfig().getUiListenerConfigurations();
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

		return new ActivationOperationConfiguration(id, op, String.valueOf(time), 0, null, null, fixedVariables, null, activations);
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

	public SimulationConfiguration createAutomaticSimulation(RealTimeSimulator realTimeSimulator) {
		SimulationConfiguration config = realTimeSimulator.getConfig();
		List<ActivationConfiguration> activationConfigurations = config.getActivationConfigurations();
		List<ActivationConfiguration> activationConfigurationsForResult = new ArrayList<>();

		List<ActivationConfiguration> userInteractions = createUserInteractions(realTimeSimulator);
		boolean hasSetupConstants = false;
		boolean hasInitialization = false;
		List<String> activations = new ArrayList<>();
		for(ActivationConfiguration activationConfiguration : activationConfigurations) {
			if(activationConfiguration.getId().equals("$initialise_machine")) {
				hasInitialization = true;
				ActivationOperationConfiguration initializationConfiguration = (ActivationOperationConfiguration) activationConfiguration;
				activations = new ArrayList<>(initializationConfiguration.getActivating());
				activations.addAll(userInteractions.stream().map(ActivationConfiguration::getId).collect(Collectors.toList()));
				activationConfigurations.add(new ActivationOperationConfiguration("$initialise_machine", "$initialise_machine", initializationConfiguration.getAfter(), initializationConfiguration.getPriority(), initializationConfiguration.getAdditionalGuards(), initializationConfiguration.getActivationKind(), initializationConfiguration.getFixedVariables(), initializationConfiguration.getProbabilisticVariables(), activations));
			} else if(activationConfiguration.getId().equals("$setup_constants")) {
				hasSetupConstants = true;
			} else {
				activationConfigurationsForResult.add(activationConfiguration);
			}
		}
		if(!hasInitialization) {
			activations.addAll(userInteractions.stream().map(ActivationConfiguration::getId).collect(Collectors.toList()));
			activationConfigurationsForResult.add(0, new ActivationOperationConfiguration("$initialise_machine", "$initialise_machine", null, 0, null, null, null, null, activations));
		}

		if(!hasSetupConstants) {
			PersistentTrace persistentTrace = new PersistentTrace(currentTrace.get());
			if("$setup_constants".equals(persistentTrace.getTransitionList().get(0).getOperationName())) {
				activationConfigurationsForResult.add(0, new ActivationOperationConfiguration("$setup_constants", "$setup_constants", null, 0, null, null, null, null, new ArrayList<>()));
			}
		}

		activationConfigurationsForResult.addAll(userInteractions);
		return new SimulationConfiguration(activationConfigurationsForResult, new ArrayList<>(), SimulationConfiguration.metadataBuilder(SimulationConfiguration.SimulationFileType.INTERACTION_REPLAY).withSavedNow().withUserCreator().build());
	}

	public ObjectProperty<Transition> getLastUserInteraction() {
		return lastUserInteraction;
	}

}
