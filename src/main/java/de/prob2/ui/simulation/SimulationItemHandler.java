package de.prob2.ui.simulation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.choice.SimulationCheckingType;
import de.prob2.ui.simulation.choice.SimulationType;
import de.prob2.ui.simulation.configuration.ISimulationModelConfiguration;
import de.prob2.ui.simulation.configuration.SimulationBlackBoxModelConfiguration;
import de.prob2.ui.simulation.model.SimulationModel;
import de.prob2.ui.simulation.simulators.check.ISimulationPropertyChecker;
import de.prob2.ui.simulation.simulators.check.SimulationCheckingSimulator;
import de.prob2.ui.simulation.simulators.check.SimulationEstimator;
import de.prob2.ui.simulation.simulators.check.SimulationHypothesisChecker;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.verifications.CheckingStatus;

import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Singleton
public final class SimulationItemHandler {
	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final StageManager stageManager;

	private final Injector injector;

	private Path path;

	private final ListProperty<Thread> currentJobThreads;

	private ISimulationModelConfiguration simulationModelConfiguration;

	@Inject
	private SimulationItemHandler(final CurrentProject currentProject, final CurrentTrace currentTrace, final StageManager stageManager, final Injector injector, final DisablePropertyController disablePropertyController) {
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.stageManager = stageManager;
		this.injector = injector;
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
		disablePropertyController.addDisableExpression(this.runningProperty());
	}

	public ObservableList<SimulationItem> getSimulationItems(SimulationModel simulationModel) {
		if(simulationModel.getPath().equals(Paths.get(""))) {
			return FXCollections.observableArrayList();
		}
		return this.currentProject.getCurrentMachine().getSimulationTasksByModel(simulationModel);
	}

	public void reset(SimulationModel simulationModel) {
		this.getSimulationItems(simulationModel).forEach(SimulationItem::reset);
	}

	private Map<String, Object> extractAdditionalInformation(SimulationItem item) {
		Map<String, Object> additionalInformation = new HashMap<>();

		if (item.containsField("START_AFTER_STEPS")) {
			additionalInformation.put("START_AFTER_STEPS", item.getField("START_AFTER_STEPS"));
		} else if (item.containsField("STARTING_PREDICATE")) {
			additionalInformation.put("STARTING_PREDICATE", item.getField("STARTING_PREDICATE"));
		} else if (item.containsField("STARTING_PREDICATE_ACTIVATED")) {
			additionalInformation.put("STARTING_PREDICATE_ACTIVATED", item.getField("STARTING_PREDICATE_ACTIVATED"));
		} else if (item.containsField("STARTING_TIME")) {
			additionalInformation.put("STARTING_TIME", item.getField("STARTING_TIME"));
		}

		if (item.containsField("STEPS_PER_EXECUTION")) {
			additionalInformation.put("STEPS_PER_EXECUTION", item.getField("STEPS_PER_EXECUTION"));
		} else if (item.containsField("ENDING_PREDICATE")) {
			additionalInformation.put("ENDING_PREDICATE", item.getField("ENDING_PREDICATE"));
		} else if (item.containsField("ENDING_TIME")) {
			additionalInformation.put("ENDING_TIME", item.getField("ENDING_TIME"));
		}

		return additionalInformation;
	}

	private void setResult(SimulationItem item, ISimulationPropertyChecker simulationPropertyChecker) {
		item.setTraces(simulationPropertyChecker.getResultingTraces());
		item.setTimestamps(simulationPropertyChecker.getResultingTimestamps());
		item.setStatuses(simulationPropertyChecker.getResultingStatus());
		item.setSimulationStats(simulationPropertyChecker.getStats());
		Platform.runLater(() -> {
			switch (simulationPropertyChecker.getResult()) {
				case SUCCESS:
					item.setStatus(CheckingStatus.SUCCESS);
					break;
				case FAIL:
					item.setStatus(CheckingStatus.FAIL);
					break;
				case NOT_FINISHED:
					item.setStatus(CheckingStatus.NOT_CHECKED);
					break;
				default:
					break;
			}
		});
	}

	private void runAndCheck(SimulationItem item, ISimulationPropertyChecker simulationPropertyChecker) {
		Thread thread = new Thread(() -> {
			simulationPropertyChecker.run();
			setResult(item, simulationPropertyChecker);
			currentJobThreads.remove(Thread.currentThread());
		});
		currentJobThreads.add(thread);
		thread.start();
	}

	private void handleMonteCarloSimulation(SimulationItem item) {
		int executions = item.getField("EXECUTIONS") == null ? ((SimulationBlackBoxModelConfiguration) simulationModelConfiguration).getTimedTraces().size() : (int) item.getField("EXECUTIONS");
		int maxStepsBeforeProperty = item.getField("MAX_STEPS_BEFORE_PROPERTY") == null ? 0 : (int) item.getField("MAX_STEPS_BEFORE_PROPERTY");
		Map<String, Object> additionalInformation = extractAdditionalInformation(item);
		SimulationCheckingSimulator simulationCheckingSimulator = new SimulationCheckingSimulator(injector, currentTrace, currentProject, executions, maxStepsBeforeProperty, additionalInformation);
		SimulationHelperFunctions.initSimulator(stageManager, injector.getInstance(SimulatorStage.class), simulationCheckingSimulator, currentTrace.getStateSpace().getLoadedMachine(), path);
		runAndCheck(item, simulationCheckingSimulator);
	}

	private void handleHypothesisTest(SimulationItem item) {
		int executions = item.getField("EXECUTIONS") == null ? ((SimulationBlackBoxModelConfiguration) simulationModelConfiguration).getTimedTraces().size() : (int) item.getField("EXECUTIONS");
		int maxStepsBeforeProperty = item.getField("MAX_STEPS_BEFORE_PROPERTY") == null ? 0 : (int) item.getField("MAX_STEPS_BEFORE_PROPERTY");
		Map<String, Object> additionalInformation = extractAdditionalInformation(item);
		SimulationCheckingType checkingType = (SimulationCheckingType) item.getField("CHECKING_TYPE");
		SimulationHypothesisChecker.HypothesisCheckingType hypothesisCheckingType = (SimulationHypothesisChecker.HypothesisCheckingType) item.getField("HYPOTHESIS_CHECKING_TYPE");
		double probability = (double) item.getField("PROBABILITY");
		double significance = (double) item.getField("SIGNIFICANCE");

		if (item.containsField("PROBABILITY")) {
			additionalInformation.put("PROBABILITY", probability);
		}

		if (item.containsField("SIGNIFICANCE")) {
			additionalInformation.put("SIGNIFICANCE", significance);
		}

		if (item.containsField("PREDICATE")) {
			additionalInformation.put("PREDICATE", item.getField("PREDICATE"));
		}

		if (item.containsField("TIME")) {
			additionalInformation.put("TIME", item.getField("TIME"));
		}

		SimulationHypothesisChecker hypothesisChecker = new SimulationHypothesisChecker(injector, injector.getInstance(I18n.class), hypothesisCheckingType, probability, significance);
		initializeHypothesisChecker(hypothesisChecker, executions, maxStepsBeforeProperty, checkingType, additionalInformation);
		runAndCheck(item, hypothesisChecker);
	}

	private void initializeHypothesisChecker(SimulationHypothesisChecker simulationHypothesisChecker, final int numberExecutions, final int maxStepsBeforeProperty, final SimulationCheckingType type, final Map<String, Object> additionalInformation) {
		simulationHypothesisChecker.initialize(currentTrace, currentProject, numberExecutions, maxStepsBeforeProperty, type, additionalInformation);
		SimulationHelperFunctions.initSimulator(stageManager, injector.getInstance(SimulatorStage.class), simulationHypothesisChecker.getSimulator(), currentTrace.getStateSpace().getLoadedMachine(), path);
	}

	private void handleEstimation(SimulationItem item) {
		int executions = item.getField("EXECUTIONS") == null ? ((SimulationBlackBoxModelConfiguration) simulationModelConfiguration).getTimedTraces().size() : (int) item.getField("EXECUTIONS");
		int maxStepsBeforeProperty = item.getField("MAX_STEPS_BEFORE_PROPERTY") == null ? 0 : (int) item.getField("MAX_STEPS_BEFORE_PROPERTY");
		Map<String, Object> additionalInformation = extractAdditionalInformation(item);
		SimulationCheckingType checkingType = (SimulationCheckingType) item.getField("CHECKING_TYPE");
		SimulationEstimator.EstimationType estimationType = (SimulationEstimator.EstimationType) item.getField("ESTIMATION_TYPE");
		double desiredValue = (double) item.getField("DESIRED_VALUE");
		double epsilon = (double) item.getField("EPSILON");

		if (item.containsField("DESIRED_VALUE")) {
			additionalInformation.put("DESIRED_VALUE", desiredValue);
		}

		if (item.containsField("EPSILON")) {
			additionalInformation.put("EPSILON", epsilon);
		}

		if (item.containsField("PREDICATE")) {
			additionalInformation.put("PREDICATE", item.getField("PREDICATE"));
		}

		if (item.containsField("TIME")) {
			additionalInformation.put("TIME", item.getField("TIME"));
		}

		if (item.containsField("EXPRESSION")) {
			additionalInformation.put("EXPRESSION", item.getField("EXPRESSION"));
		}

		SimulationEstimator simulationEstimator = new SimulationEstimator(injector, injector.getInstance(I18n.class), estimationType, checkingType, desiredValue, epsilon);
		initializeEstimator(simulationEstimator, executions, maxStepsBeforeProperty, checkingType, additionalInformation);
		runAndCheck(item, simulationEstimator);
	}

	private void initializeEstimator(SimulationEstimator simulationEstimator, final int numberExecutions, final int maxStepsBeforeProperty, final SimulationCheckingType type, final Map<String, Object> additionalInformation) {
		simulationEstimator.initialize(currentTrace, currentProject, numberExecutions, maxStepsBeforeProperty, type, additionalInformation);
		SimulationHelperFunctions.initSimulator(stageManager, injector.getInstance(SimulatorStage.class), simulationEstimator.getSimulator(), currentTrace.getStateSpace().getLoadedMachine(), path);
	}

	public void checkItem(SimulationItem item) {
		/*if(!item.selected()) {
			return;
		}*/
		// TODO
		SimulationType type = item.getType();
		switch (type) {
			case MONTE_CARLO_SIMULATION:
				handleMonteCarloSimulation(item);
				break;
			case HYPOTHESIS_TEST:
				handleHypothesisTest(item);
				break;
			case ESTIMATION:
				handleEstimation(item);
				break;
			default:
				break;
		}
	}

	public void handleMachine(SimulationModel simulationModel) {
		List<SimulationItem> items = this.currentProject.getCurrentMachine().getSimulationTasksByModel(simulationModel);
		Thread thread = new Thread(() -> {
			for (SimulationItem item : items) {
				this.checkItem(item);
				if (Thread.currentThread().isInterrupted()) {
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

	public void setSimulationModelConfiguration(ISimulationModelConfiguration simulationModelConfiguration) {
		this.simulationModelConfiguration = simulationModelConfiguration;
	}
}
