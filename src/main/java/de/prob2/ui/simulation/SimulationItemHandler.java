package de.prob2.ui.simulation;

import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import de.prob2.ui.simulation.choice.SimulationType;
import de.prob2.ui.simulation.simulators.check.SimulationEstimator;
import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import de.prob2.ui.simulation.simulators.check.SimulationMonteCarlo;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.Checked;
import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class SimulationItemHandler {

	private final CurrentTrace currentTrace;

	private final StageManager stageManager;

	private final Injector injector;

	private Path path;

	private final ListProperty<Thread> currentJobThreads;

	@Inject
	private SimulationItemHandler(final CurrentTrace currentTrace, final StageManager stageManager, final Injector injector,
								  final DisablePropertyController disablePropertyController) {
		this.currentTrace = currentTrace;
		this.stageManager = stageManager;
		this.injector = injector;
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
		disablePropertyController.addDisableExpression(this.runningProperty());
	}

	public List<SimulationItem> getItems(final SimulationModel simulationModel) {
		if(simulationModel == null) {
			return new ArrayList<>();
		}
		return simulationModel.getSimulationItems();
	}

	public Optional<SimulationItem> addItem(final SimulationModel simulationModel, final SimulationItem item) {
		final List<SimulationItem> items = this.getItems(simulationModel);
		final Optional<SimulationItem> existingItem = items.stream().filter(item::equals).findAny();
		if(!existingItem.isPresent()) {
			items.add(item);
		}
		return existingItem;
	}

	public void removeItem(final SimulationModel simulationModel, SimulationItem item) {
		final List<SimulationItem> items = this.getItems(simulationModel);
		items.remove(item);
	}

	private Map<String, Object> extractAdditionalInformation(SimulationItem item) {
		Map<String, Object> additionalInformation = new HashMap<>();

		if(item.containsField("START_AFTER_STEPS")) {
			additionalInformation.put("START_AFTER_STEPS", item.getField("START_AFTER_STEPS"));
		} else if(item.containsField("STARTING_PREDICATE")) {
			additionalInformation.put("STARTING_PREDICATE", item.getField("STARTING_PREDICATE"));
		} else if(item.containsField("STARTING_PREDICATE_ACTIVATED")) {
			additionalInformation.put("STARTING_PREDICATE_ACTIVATED", item.getField("STARTING_PREDICATE_ACTIVATED"));
		} else if(item.containsField("STARTING_TIME")) {
			additionalInformation.put("STARTING_TIME", item.getField("STARTING_TIME"));
		}

		if(item.containsField("STEPS_PER_EXECUTION")) {
			additionalInformation.put("STEPS_PER_EXECUTION", item.getField("STEPS_PER_EXECUTION"));
		} else if(item.containsField("ENDING_PREDICATE")) {
			additionalInformation.put("ENDING_PREDICATE", item.getField("ENDING_PREDICATE"));
		} else if(item.containsField("ENDING_TIME")) {
			additionalInformation.put("ENDING_TIME", item.getField("ENDING_TIME"));
		}

		return additionalInformation;
	}

	private void setResult(SimulationItem item, SimulationMonteCarlo monteCarlo) {
		item.setTraces(monteCarlo.getResultingTraces());
		item.setTimestamps(monteCarlo.getResultingTimestamps());
		item.setSimulationStats(monteCarlo.getStats());
		Platform.runLater(() -> {
			switch (monteCarlo.getResult()) {
				case SUCCESS:
					item.setChecked(Checked.SUCCESS);
					break;
				case FAIL:
					item.setChecked(Checked.FAIL);
					break;
				case NOT_FINISHED:
					item.setChecked(Checked.NOT_CHECKED);
					break;
				default:
					break;
			}
		});
	}

	private void runAndCheck(SimulationItem item, SimulationMonteCarlo monteCarlo) {
		Thread thread = new Thread(() -> {
			monteCarlo.run();
			setResult(item, monteCarlo);
			currentJobThreads.remove(Thread.currentThread());
		});
		currentJobThreads.add(thread);
		thread.start();
	}

	private void handleMonteCarloSimulation(SimulationItem item, boolean checkAll) {
		int executions = (int) item.getField("EXECUTIONS");
		int maxStepsBeforeProperty = (int) item.getField("MAX_STEPS_BEFORE_PROPERTY");
		Map<String, Object> additionalInformation = extractAdditionalInformation(item);
		SimulationMonteCarlo monteCarlo = new SimulationMonteCarlo(injector, currentTrace, executions, maxStepsBeforeProperty, additionalInformation);
		SimulationHelperFunctions.initSimulator(stageManager, injector.getInstance(SimulatorStage.class), monteCarlo, path.toFile());
		runAndCheck(item, monteCarlo);
	}

	private void handleHypothesisTest(SimulationItem item, boolean checkAll) {
		int executions = (int) item.getField("EXECUTIONS");
		int maxStepsBeforeProperty = (int) item.getField("MAX_STEPS_BEFORE_PROPERTY");
		Map<String, Object> additionalInformation = extractAdditionalInformation(item);
		SimulationCheckingType checkingType = (SimulationCheckingType) item.getField("CHECKING_TYPE");
		SimulationHypothesisChecker.HypothesisCheckingType hypothesisCheckingType = (SimulationHypothesisChecker.HypothesisCheckingType) item.getField("HYPOTHESIS_CHECKING_TYPE");
		double probability = (double) item.getField("PROBABILITY");
		double significance = (double) item.getField("SIGNIFICANCE");

		if(item.containsField("PREDICATE")) {
			additionalInformation.put("PREDICATE", item.getField("PREDICATE"));
		}

		if(item.containsField("TIME")) {
			additionalInformation.put("TIME", item.getField("TIME"));
		}

		SimulationHypothesisChecker hypothesisChecker = new SimulationHypothesisChecker(injector, currentTrace, executions, maxStepsBeforeProperty, checkingType, hypothesisCheckingType, probability, significance, additionalInformation);
		SimulationHelperFunctions.initSimulator(stageManager, injector.getInstance(SimulatorStage.class), hypothesisChecker, path.toFile());
		runAndCheck(item, hypothesisChecker);
	}

	private void handleEstimation(SimulationItem item, boolean checkAll) {
		Trace trace = currentTrace.get();
		int executions = (int) item.getField("EXECUTIONS");
		int maxStepsBeforeProperty = (int) item.getField("MAX_STEPS_BEFORE_PROPERTY");
		Map<String, Object> additionalInformation = extractAdditionalInformation(item);
		SimulationCheckingType checkingType = (SimulationCheckingType) item.getField("CHECKING_TYPE");
		SimulationEstimator.EstimationType estimationType = (SimulationEstimator.EstimationType) item.getField("ESTIMATION_TYPE");
		double desiredValue = (double) item.getField("DESIRED_VALUE");
		double epsilon = (double) item.getField("EPSILON");

		if(item.containsField("PREDICATE")) {
			additionalInformation.put("PREDICATE", item.getField("PREDICATE"));
		}

		if(item.containsField("TIME")) {
			additionalInformation.put("TIME", item.getField("TIME"));
		}

		SimulationEstimator simulationEstimator = new SimulationEstimator(injector, currentTrace, executions, maxStepsBeforeProperty, checkingType, estimationType, desiredValue, epsilon, additionalInformation);
		SimulationHelperFunctions.initSimulator(stageManager, injector.getInstance(SimulatorStage.class), simulationEstimator, path.toFile());
		runAndCheck(item, simulationEstimator);
	}

	public void checkItem(SimulationItem item, boolean checkAll) {
		/*if(!item.selected()) {
			return;
		}*/
		// TODO
		SimulationType type = item.getType();
		switch(type) {
			case MONTE_CARLO_SIMULATION:
				handleMonteCarloSimulation(item, checkAll);
				break;
			case HYPOTHESIS_TEST:
				handleHypothesisTest(item, checkAll);
				break;
			case ESTIMATION:
				handleEstimation(item, checkAll);
				break;
			default:
				break;
		}
	}

	public void handleMachine(SimulationModel simulationModel) {
		Thread thread = new Thread(() -> {
			for (SimulationItem item : simulationModel.getSimulationItems()) {
				this.checkItem(item, true);
				if(Thread.currentThread().isInterrupted()) {
					break;
				}
			}
			currentJobThreads.remove(Thread.currentThread());
		}, "Simulation Thread");
		currentJobThreads.add(thread);
		thread.start();
	}

	public void interrupt() {
		List<Thread> removedThreads = new ArrayList<>();
		for (Thread thread : currentJobThreads) {
			thread.interrupt();
			removedThreads.add(thread);
		}
		currentTrace.getStateSpace().sendInterrupt();
		currentJobThreads.removeAll(removedThreads);
	}

	public BooleanExpression runningProperty() {
		return currentJobThreads.emptyProperty().not();
	}

	public boolean isRunning() {
		return this.runningProperty().get();
	}

	public void setPath(Path path) {
		this.path = path;
	}
}
