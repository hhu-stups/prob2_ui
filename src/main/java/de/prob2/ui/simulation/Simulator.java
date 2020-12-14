package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

import java.util.Timer;
import java.util.TimerTask;

@Singleton
public class Simulator extends AbstractSimulator {

	private ChangeListener<Trace> listener;

	private TimerTask task;

	private Timer timer;

	private final CurrentTrace currentTrace;

	private final BooleanProperty runningProperty;

	private final BooleanProperty executingOperationProperty;

	@Inject
	public Simulator(final CurrentTrace currentTrace, final DisablePropertyController disablePropertyController) {
		super();
		this.currentTrace = currentTrace;
		this.runningProperty = new SimpleBooleanProperty(false);
		this.executingOperationProperty = new SimpleBooleanProperty(false);
		this.listener = (observable, from, to) -> {
			if(to != null) {
				if (!to.getCurrentState().isInitialised()) {
					Trace newTrace = setupBeforeSimulation(to);
					currentTrace.set(newTrace);
				}
			}
		};
        disablePropertyController.addDisableExpression(this.executingOperationProperty);
	}

	@Override
	public void run() {
		this.timer = new Timer();
		runningProperty.set(true);
		Trace trace = currentTrace.get();
		currentTrace.set(setupBeforeSimulation(trace));
		currentTrace.addListener(listener);

		this.task = new TimerTask() {
			@Override
			public void run() {
				simulate();
			}
		};
		timer.scheduleAtFixedRate(task, interval, interval);
	}

	public void simulate() {
		executingOperationProperty.set(true);
		// Read trace and pass it through chooseOperation to avoid race condition
		Trace trace = currentTrace.get();
		Trace newTrace = simulationStep(trace);
		currentTrace.set(newTrace);
		executingOperationProperty.set(false);
	}


	public void stop() {
		if(timer != null) {
			timer.cancel();
			timer = null;
		}
		currentTrace.removeListener(listener);
		runningProperty.set(false);
	}

	public BooleanProperty runningPropertyProperty() {
		return runningProperty;
	}

	public boolean isRunning() {
		return runningProperty.get();
	}

	@Override
	protected void finishSimulation() {
		super.finishSimulation();
		task.cancel();
		currentTrace.removeListener(listener);
		executingOperationProperty.set(false);
	}
}
