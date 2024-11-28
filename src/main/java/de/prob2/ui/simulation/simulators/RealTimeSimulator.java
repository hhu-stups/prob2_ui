package de.prob2.ui.simulation.simulators;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.interactive.UIInteractionHandler;
import de.prob2.ui.simulation.simulators.check.ISimulationPropertyChecker;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;

@Singleton
public final class RealTimeSimulator extends Simulator {
	private final Scheduler scheduler;

	private final CurrentTrace currentTrace;

	private final UIInteractionHandler uiInteractionHandler;

	private final ChangeListener<Transition> uiListener;

	@Inject
	public RealTimeSimulator(final CurrentTrace currentTrace, final CurrentProject currentProject, final Scheduler scheduler, final UIInteractionHandler uiInteractionHandler) {
		super(currentTrace, currentProject);
		this.scheduler = scheduler;
		this.currentTrace = currentTrace;
		this.uiInteractionHandler = uiInteractionHandler;
		this.uiListener = (observable, from, to) -> uiInteractionHandler.handleUserInteraction(this, to);
	}

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
		try {
			Trace newTrace = simulationStep(trace);
			currentTrace.set(mergeUserInteractions(trace.getTransitionList().size(), currentTrace.get(), newTrace));
		} catch (Exception e) {
			scheduler.endSimulationStep();
			throw e;
		}
		scheduler.endSimulationStep();
	}

	private Trace mergeUserInteractions(int index, Trace traceWithUserInteractions, Trace simulatedTrace) {
		Trace trace = traceWithUserInteractions;
		for(int i = index; i < simulatedTrace.getTransitionList().size(); i++) {
			trace = traceWithUserInteractions.add(simulatedTrace.getTransitionList().get(i));
		}
		return trace;
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
		boolean endingConditionReached = super.endingConditionReached(trace);
		if(config instanceof SimulationModelConfiguration) {
			return endingConditionReached && ((SimulationModelConfiguration) config).getUiListenerConfigurations().isEmpty();
		}
		return endingConditionReached;
	}

	@Override
	public void run(ISimulationPropertyChecker simulationPropertyChecker) {
		throw new UnsupportedOperationException();
	}
}
