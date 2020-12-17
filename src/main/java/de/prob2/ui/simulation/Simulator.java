package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.property.BooleanProperty;
import javafx.fxml.FXML;

@Singleton
public class Simulator extends ProbabilityBasedSimulator implements IRealTimeSimulator {

	private final Scheduler scheduler;

	private final CurrentTrace currentTrace;

	@Inject
	public Simulator(final Scheduler scheduler, final CurrentTrace currentTrace) {
		super();
		this.scheduler = scheduler;
		this.currentTrace = currentTrace;
	}

	@Override
	public void run() {
		scheduler.run(interval);
	}

	@FXML
	public void stop() {
		scheduler.stop();
	}

	@Override
	public void simulate() {
		scheduler.startSimulationStep();
		// Read trace and pass it through chooseOperation to avoid race condition
		Trace trace = currentTrace.get();
		Trace newTrace = simulationStep(trace);
		currentTrace.set(newTrace);
		scheduler.endSimulationStep();
	}

	@Override
	public BooleanProperty runningPropertyProperty() {
		return scheduler.runningPropertyProperty();
	}

	@Override
	public boolean isRunning() {
		return scheduler.isRunning();
	}

	public boolean isFinished() {
		return finished;
	}


	@Override
	protected void finishSimulation() {
		super.finishSimulation();
		scheduler.finish();
	}
}
