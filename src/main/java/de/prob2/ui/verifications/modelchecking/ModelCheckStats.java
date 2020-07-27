package de.prob2.ui.verifications.modelchecking;

import java.util.Objects;

import com.google.inject.Injector;

import de.prob.animator.command.ComputeCoverageCommand;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.StateSpace;
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

	public void updateStats(final StateSpace stateSpace, final long timeElapsed, final StateSpaceStats stats) {
		Objects.requireNonNull(stateSpace, "stateSpace");
		
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
				injector.getInstance(StatsView.class).updateSimpleStats(stats);
			});
		}
		
		final ComputeCoverageCommand cmd = new ComputeCoverageCommand();
		stateSpace.execute(cmd);
		final ComputeCoverageCommand.ComputeCoverageResult coverage = cmd.getResult();

		if (coverage != null) {
			Platform.runLater(() -> injector.getInstance(StatsView.class).updateExtendedStats(coverage));
		}
	}
}
