package de.prob2.ui.internal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.configuration.UIListenerConfiguration;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class UIInteraction {

	private final ObjectProperty<Transition> uiListener;

	private final List<ActivationConfiguration> userInteractions;

	private final CurrentTrace currentTrace;

	private int interactionCounter;

	@Inject
	public UIInteraction(final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
		this.uiListener = new SimpleObjectProperty<>(null);
		this.userInteractions = new ArrayList<>();
		this.interactionCounter = 0;
	}

	public void addUIInteraction(RealTimeSimulator realTimeSimulator, Transition transition) {
		if(!realTimeSimulator.isRunning()) {
			return;
		}
		uiListener.set(transition);
		String id = "user_interaction_" + interactionCounter;
		String op = transition.getName();
		int time = realTimeSimulator.getTime();
		Map<String, String> fixedVariables = new HashMap<>();
		for(int i = 0; i < transition.getParameterNames().size(); i++) {
			String name = transition.getParameterNames().get(i);
			String value = transition.getParameterValues().get(i);
			fixedVariables.put(name, value);
		}
		List<UIListenerConfiguration> uiListeners = realTimeSimulator.getConfig().getUiListenerConfigurations();
		List<String> activations = new ArrayList<>();
		for(UIListenerConfiguration uiListener : uiListeners) {
			// TODO: Handle predicate
			if(uiListener.getEvent().equals(op)) {
				activations = uiListener.getActivating();
				break;
			}
		}
		ActivationConfiguration activation = new ActivationOperationConfiguration(id, op, String.valueOf(time), 0, null, null, fixedVariables, null, activations);
		userInteractions.add(activation);
		interactionCounter++;
	}

	public ObjectProperty<Transition> getUiListener() {
		return uiListener;
	}

	public Transition getLastUIChange() {
		return uiListener.get();
	}

	public List<ActivationConfiguration> getUserInteractions() {
		return userInteractions;
	}

	public void clearUserInteractions() {
		userInteractions.clear();
		interactionCounter++;
	}

	public SimulationConfiguration createAutomaticSimulation(RealTimeSimulator realTimeSimulator) {
		SimulationConfiguration config = realTimeSimulator.getConfig();
		List<ActivationConfiguration> activationConfigurations = config.getActivationConfigurations();
		List<ActivationConfiguration> activationConfigurationsForResult = new ArrayList<>();

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
		return new SimulationConfiguration(activationConfigurationsForResult, new ArrayList<>(), SimulationConfiguration.metadataBuilder("Automatic_Simulation_with_User_Interaction").withSavedNow().withUserCreator().build());
	}


}
