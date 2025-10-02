package de.prob2.ui.simulation.simulators;

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
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public final class Scheduler {

	private RealTimeSimulator realTimeSimulator;

	private Timer timer;

	private final AtomicInteger runningTasks;

	private final CurrentTrace currentTrace;

	private final BooleanProperty runningProperty;

	private final BooleanProperty executingOperationProperty;

	@Inject
	public Scheduler(final CurrentTrace currentTrace, final DisablePropertyController disablePropertyController) {
		super();
		this.timer = new Timer(true);
		this.runningTasks = new AtomicInteger(0);
		this.currentTrace = currentTrace;
		this.runningProperty = new SimpleBooleanProperty(false);
		this.executingOperationProperty = new SimpleBooleanProperty(false);
		disablePropertyController.addDisableExpression(this.executingOperationProperty);
	}

	public void run() {
		runningProperty.set(true);
		Trace trace;
		if(realTimeSimulator.getTime() > 0) {
			trace = currentTrace.get();
		} else {
			trace = new Trace(currentTrace.getStateSpace());
			currentTrace.set(trace);
		}
		realTimeSimulator.setupBeforeSimulation(trace);
		startSimulationLoop();
	}

	public void runWithoutInitialisation() {
		runningProperty.set(currentTrace.getCurrentState().isInitialised());
		realTimeSimulator.updateDelay();
		startSimulationLoop();
	}

	public void startSimulationLoop() {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				if (realTimeSimulator.isRunning() && runningTasks.get() == 1) {
					realTimeSimulator.simulate();
					startSimulationLoop();
				}
				runningTasks.getAndDecrement();
				if(realTimeSimulator.hasNoActivationQueued()) {
					runningTasks.set(0);
				}
			}
		};

		try {
			timer.schedule(task, realTimeSimulator.getDelay());
			runningTasks.getAndIncrement();
		} catch (IllegalStateException ignored) {
		}
	}

	public void setSimulator(RealTimeSimulator realTimeSimulator) {
		this.realTimeSimulator = realTimeSimulator;
	}

	public void stop() {
		runningProperty.set(false);
	}

	public BooleanProperty runningProperty() {
		return runningProperty;
	}

	public boolean isRunning() {
		return runningProperty.get();
	}

	public void startSimulationStep() {
		executingOperationProperty.set(true);
	}

	public void endSimulationStep() {
		executingOperationProperty.set(false);
	}

	public void finish() {
		executingOperationProperty.set(false);
	}

	public void stopTimer() {
		stop();
		timer.cancel();
		timer = null;
	}

}
