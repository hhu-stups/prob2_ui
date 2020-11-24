package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

@Singleton
public class Simulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Simulator.class);

    private SimulationConfiguration config;

	private Timer timer;

	private final CurrentTrace currentTrace;

	private final BooleanProperty runningProperty;

	@Inject
	public Simulator(final CurrentTrace currentTrace) {
	    this.config = null;
		this.currentTrace = currentTrace;
		this.runningProperty = new SimpleBooleanProperty(false);
	}

	public void initSimulator(File configFile) {
	    this.config = null;
	    try {
            this.config = SimulationFileHandler.constructConfigurationFromJSON(configFile);
        } catch (IOException e) {
            LOGGER.debug("Tried to load simulation configuration file");
            //TODO: Implement alert
            return;
        }
		this.timer = new Timer();
	}

	public void run() {
		runningProperty.set(true);
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
                chooseOperation();
			}
		};
		timer.schedule(task, config.getTime(),1000);
	}

	public void chooseOperation() {
	    Set<String> enabledOperations = currentTrace.getCurrentState().getOutTransitions()
                .stream()
                .map(trans -> trans.getName())
                .collect(Collectors.toSet());
	    // TODO:
	    double ranDouble = Math.random();
        Trace trace = currentTrace.get().randomAnimation(1);
        currentTrace.set(trace);
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
