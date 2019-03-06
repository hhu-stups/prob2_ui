package de.prob2.ui.verifications.modelchecking;

import com.google.inject.Injector;

import de.prob.animator.command.ComputeCoverageCommand;
import de.prob.check.IModelCheckJob;
import de.prob.check.IModelCheckingResult;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.MachineStatusHandler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import java.util.Objects;


public final class ModelCheckStats extends AnchorPane {
	
	@FXML private VBox statsBox;
	@FXML private Label elapsedTime;
	@FXML private Label processedStates;
	@FXML private Label totalStates;
	@FXML private Label totalTransitions;
	
	private final Injector injector;
	
	
	public ModelCheckStats(final StageManager stageManager, final Injector injector) {
		this.injector = injector;
		stageManager.loadFXML(this, "modelchecking_stats.fxml");
	}

	void startJob() {
		statsBox.setVisible(true);
	}

	public void updateStats(final IModelCheckJob modelChecker, final long timeElapsed, final StateSpaceStats stats) {
		Objects.requireNonNull(modelChecker, "modelChecker");
		
		Platform.runLater(() -> elapsedTime.setText(String.format("%.1f",timeElapsed/1000.0) + " s"));

		if (stats != null) {
			int nrProcessedNodes = stats.getNrProcessedNodes();
			int nrTotalNodes = stats.getNrTotalNodes();
			int nrTotalTransitions = stats.getNrTotalTransitions();
			int percent = nrProcessedNodes * 100 / nrTotalNodes;
			Platform.runLater(() -> {
				processedStates.setText(nrProcessedNodes + " (" + percent + " %)");
				totalStates.setText(String.valueOf(nrTotalNodes));
				totalTransitions.setText(String.valueOf(nrTotalTransitions));
			});
		}
		
		final StateSpace stateSpace = modelChecker.getStateSpace();
		final ComputeCoverageCommand cmd = new ComputeCoverageCommand();
		stateSpace.execute(cmd);
		if (cmd.isInterrupted()) {
			Thread.currentThread().interrupt();
			return;
		}
		final ComputeCoverageCommand.ComputeCoverageResult coverage = cmd.getResult();
		
		if (coverage != null) {
			Platform.runLater(() -> injector.getInstance(StatsView.class).updateExtendedStats(coverage));
		}
	}

	public void isFinished(final IModelCheckJob job, final long timeElapsed, final IModelCheckingResult result) {
		Objects.requireNonNull(job, "modelChecker");
		Objects.requireNonNull(result, "result");
		
		Platform.runLater(() -> {
			elapsedTime.setText(String.format("%.3f",timeElapsed/1000.0) + " s");
			Machine machine = injector.getInstance(CurrentProject.class).getCurrentMachine();
			injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.MODELCHECKING);
		});
		
		final StateSpace stateSpace = job.getStateSpace();
		final ComputeCoverageCommand cmd = new ComputeCoverageCommand();
		stateSpace.execute(cmd);
		final ComputeCoverageCommand.ComputeCoverageResult coverage = cmd.getResult();
		
		if (coverage != null) {
			Number numNodes = coverage.getTotalNumberOfNodes();
			Number numTrans = coverage.getTotalNumberOfTransitions();

			Platform.runLater(() -> {
				injector.getInstance(StatsView.class).updateExtendedStats(coverage);
				totalStates.setText(String.valueOf(numNodes));
				totalTransitions.setText(String.valueOf(numTrans));
			});
		}
		
		if (result instanceof ITraceDescription) {
			Trace trace = ((ITraceDescription) result).getTrace(stateSpace);
			Platform.runLater(() -> injector.getInstance(StatsView.class).update(trace));
		}
	}
	

}
