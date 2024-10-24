package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;
import de.prob2.ui.simulation.simulators.SimulationCreator;
import de.prob2.ui.simulation.simulators.SimulationSaver;

import java.io.IOException;

@Singleton
public final class SimulationScenarioHandler {
	private final StageManager stageManager;

	private final CurrentTrace currentTrace;

	private final CurrentProject currentProject;

	private final Injector injector;

	private SimulatorStage simulatorStage;

	@Inject
	public SimulationScenarioHandler(final StageManager stageManager, final CurrentTrace currentTrace, final CurrentProject currentProject,
									final Injector injector) {
		this.stageManager = stageManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
	}


	public void loadTrace(SimulationTracesView.SimulationTraceItem item) {
		if (item == null) {
			return;
		}
		this.currentTrace.set(item.getTrace());
	}

	public void playTrace(SimulationTracesView.SimulationTraceItem traceItem) {
		Trace trace = new Trace(currentTrace.getStateSpace());
		currentTrace.set(trace);
		SimulationModelConfiguration config = SimulationCreator.createConfiguration(traceItem.getTrace(), traceItem.getTimestamps(), false, SimulationModelConfiguration.metadataBuilder(SimulationModelConfiguration.SimulationFileType.TIMED_TRACE).build());
		RealTimeSimulator realTimeSimulator = injector.getInstance(RealTimeSimulator.class);

		realTimeSimulator.initSimulator(config);
		realTimeSimulator.setupBeforeSimulation(trace);
		trace.setExploreStateByDefault(false);
		simulatorStage.simulate();
		trace.setExploreStateByDefault(true);
	}

	public void saveTrace(SimulationTracesView.SimulationTraceItem item) {
		if (item == null) {
			return;
		}
		TraceFileHandler traceSaver = injector.getInstance(TraceFileHandler.class);
		if (currentTrace.get() != null) {
			Trace trace = item.getTrace();
			try {
				traceSaver.save(trace, currentProject.getCurrentMachine());
			} catch (IOException e) {
				stageManager.makeExceptionAlert(e, "animation.tracereplay.alerts.saveError").showAndWait();
			}
		}
	}

	public void saveTimedTrace(SimulationTracesView.SimulationTraceItem item) {
		if (item == null) {
			return;
		}
		SimulationSaver simulationSaver = injector.getInstance(SimulationSaver.class);
		try {
			String createdBy = "Simulation: " + item.getParent().getTypeAsName() + "; " + item.getParent().getConfiguration();
			simulationSaver.saveConfiguration(item.getTrace(), item.getTimestamps(), createdBy);
		} catch (IOException e) {
			stageManager.makeExceptionAlert(e, "simulation.save.error").showAndWait();
		}
	}

	public void setSimulatorStage(final SimulatorStage simulatorStage) {
		this.simulatorStage = simulatorStage;
	}

}
