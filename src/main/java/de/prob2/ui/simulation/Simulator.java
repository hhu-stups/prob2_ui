package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
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
	    List<OperationConfiguration> possibleOperations = config.getOperationConfigurations()
				.stream()
				.filter(config -> config.getProbability() > 0.0)
				.collect(Collectors.toList());

		List<OperationConfiguration> enabledPossibleOperations = possibleOperations.stream()
				.filter(op -> currentTrace.getCurrentState().getOutTransitions()
						.stream()
						.map(Transition::getName)
						.collect(Collectors.toSet()).contains(op.getOpName()))
				.collect(Collectors.toList());

	    double ranDouble = Math.random();
	    double minimumProbability = 0.0;
	    String chosenOperation = "";


	    for(OperationConfiguration config : enabledPossibleOperations) {
			float newProbablity = (config.getProbability()/enabledPossibleOperations.size()) * possibleOperations.size();
			minimumProbability += newProbablity;
			chosenOperation = config.getOpName();
			if(minimumProbability > ranDouble) {
				break;
			}

		}

	    if("".equals(chosenOperation)) {
	    	currentTrace.set(currentTrace.get().randomAnimation(1));
		} else {
			Transition nextTransition = currentTrace.getCurrentState().findTransition(chosenOperation, "1=1");
			Trace newTrace = currentTrace.get().add(nextTransition);
			currentTrace.set(newTrace);
		}
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
