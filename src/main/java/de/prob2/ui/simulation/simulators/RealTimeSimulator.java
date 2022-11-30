package de.prob2.ui.simulation.simulators;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.UIInteraction;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.UIListenerConfiguration;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class RealTimeSimulator extends Simulator {

	private final Scheduler scheduler;

	private final CurrentTrace currentTrace;

	private final UIInteraction uiInteraction;

	private final ChangeListener<Transition> uiListener;

	@Inject
	public RealTimeSimulator(final CurrentTrace currentTrace, final Scheduler scheduler, final UIInteraction uiInteraction) {
		super(currentTrace);
		this.scheduler = scheduler;
		this.currentTrace = currentTrace;
		this.uiInteraction = uiInteraction;
		this.uiListener = (observable, from, to) -> {
			if(to == null) {
				return;
			}
			List<UIListenerConfiguration> uiListenerConfigurations = config.getUiListenerConfigurations();
			boolean anyActivated = false;
			for(UIListenerConfiguration uiListener : uiListenerConfigurations) {
				String event = uiListener.getEvent();
				// TODO: handle predicate
				List<String> activating = uiListener.getActivating();
				if(event.equals(to.getName())) {
					// TODO: Handle parameter predicates
					for(String activatingEvent : activating) {
						simulationEventHandler.activateOperation(to.getDestination(), (ActivationOperationConfiguration) activationConfigurationMap.get(activatingEvent), new ArrayList<>(), "1=1");
						anyActivated = true;
					}
				}
			}
			if(anyActivated) {
				scheduler.runWithoutInitialisation();
			}
		};
	}

	@Override
	public void run() {
		scheduler.run();
		uiInteraction.getUiListener().addListener(uiListener);
	}

	@FXML
	public void stop() {
		uiInteraction.getUiListener().removeListener(uiListener);
		scheduler.stop();
	}

	public void simulate() {
		scheduler.startSimulationStep();
		// Read trace and pass it through chooseOperation to avoid race condition
		Trace trace = currentTrace.get();
		Trace newTrace = simulationStep(trace);
		currentTrace.set(newTrace);
		scheduler.endSimulationStep();
	}

	public BooleanProperty runningProperty() {
		return scheduler.runningProperty();
	}

	public boolean isRunning() {
		return scheduler.isRunning();
	}

	@Override
	public void resetSimulator() {
		super.resetSimulator();
		scheduler.stop();
	}

	@Override
	public boolean endingConditionReached(Trace trace) {
		return super.endingConditionReached(trace) && config.getUiListenerConfigurations().isEmpty();
	}
}
