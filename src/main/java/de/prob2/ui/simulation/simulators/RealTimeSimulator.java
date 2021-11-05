package de.prob2.ui.simulation.simulators;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;

@Singleton
public class RealTimeSimulator extends Simulator {

	private final Scheduler scheduler;

	private final CurrentTrace currentTrace;

	@Inject
	public RealTimeSimulator(final CurrentTrace currentTrace, final Scheduler scheduler) {
		super(currentTrace);
		this.scheduler = scheduler;
		this.currentTrace = currentTrace;
	}

	@Override
	public void run() {
		scheduler.run();
	}

	@FXML
	public void stop() {
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

	public void resetSimulator() {
		super.resetSimulator();
		scheduler.stop();
	}
}
