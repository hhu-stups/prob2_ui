package de.prob2.ui.simulation.simulators;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.simulation.interactive.UIInteractionHandler;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;

@Singleton
public class RealTimeSimulator extends Simulator {

	private final Scheduler scheduler;

	private final CurrentTrace currentTrace;

	private final UIInteractionHandler uiInteractionHandler;

	private final ChangeListener<Transition> uiListener;

	@Inject
	public RealTimeSimulator(final CurrentTrace currentTrace, final Scheduler scheduler, final UIInteractionHandler uiInteractionHandler) {
		super(currentTrace);
		this.scheduler = scheduler;
		this.currentTrace = currentTrace;
		this.uiInteractionHandler = uiInteractionHandler;
		this.uiListener = (observable, from, to) -> uiInteractionHandler.handleUserInteraction(this, to);
	}

	@Override
	public void run() {
		scheduler.run();
		uiInteractionHandler.getLastUserInteraction().addListener(uiListener);
	}

	@FXML
	public void stop() {
		uiInteractionHandler.getLastUserInteraction().removeListener(uiListener);
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
