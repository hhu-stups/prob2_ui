package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.Timer;
import java.util.TimerTask;

@Singleton
public class Simulator {

	private Timer timer;

	private int interval; // in ms

	private final CurrentTrace currentTrace;

	private final BooleanProperty runningProperty;

	@Inject
	public Simulator(final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;
		this.runningProperty = new SimpleBooleanProperty(false);
	}

	public void initSimulator(int interval) {
		this.interval = interval;
		this.timer = new Timer();
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public void run() {
		runningProperty.set(true);
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				Trace trace = currentTrace.get().randomAnimation(1);
				currentTrace.set(trace);
			}
		};
		timer.schedule(task, interval,1000);
	}

	public void stop() {
		timer.cancel();
		runningProperty.set(false);
	}

	public BooleanProperty runningPropertyProperty() {
		return runningProperty;
	}

	public boolean isRunning() {
		return runningProperty.get();
	}
}
