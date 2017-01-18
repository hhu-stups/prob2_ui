package de.prob2.ui.stats;

import java.util.List;

import com.google.inject.Inject;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.animator.command.ComputeCoverageCommand;
import de.prob.animator.command.ComputeStateSpaceStatsCommand;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class StatsView extends ScrollPane {
	@FXML
	private Label totalTransitions;
	@FXML
	private Label totalNodes;
	@FXML
	private Label processedNodes;
	@FXML
	private Label percentageProcessed;
	@FXML
	private GridPane nodeStats;
	@FXML
	private GridPane transStats;
	@FXML
	private VBox statsBox;
	@FXML
	private VBox extendedStatsBox;
	@FXML
	private Label noStatsLabel;
	@FXML
	private ToggleButton extendedStatsToggle;

	private final CurrentTrace currentTrace;
	private final ChangeListener<Trace> traceChangeListener = (observable, from, to) -> computeStats(to);

	@Inject
	public StatsView(final StageManager stageManager, final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;

		stageManager.loadFXML(this, "stats_view.fxml");
	}

	@FXML
	public void initialize() {
		extendedStatsBox.visibleProperty().bind(extendedStatsToggle.selectedProperty());
		noStatsLabel.visibleProperty().bind(currentTrace.existsProperty().not());
		statsBox.visibleProperty().bind(noStatsLabel.visibleProperty().not());
		extendedStatsBox.managedProperty().bind(extendedStatsBox.visibleProperty());
		statsBox.managedProperty().bind(statsBox.visibleProperty());
		noStatsLabel.managedProperty().bind(noStatsLabel.visibleProperty());

		this.currentTrace.addListener(traceChangeListener);
		traceChangeListener.changed(this.currentTrace, null, currentTrace.get());

		this.setMinSize(200, 100);
	}

	@FXML
	private void handleExtendedStatsToggle() {
		FontAwesomeIconView icon;
		Tooltip tooltip;
		if (extendedStatsToggle.isSelected()) {
			traceChangeListener.changed(this.currentTrace, null, currentTrace.get());

			icon = new FontAwesomeIconView(FontAwesomeIcon.CLOSE);
			tooltip = new Tooltip("Close Extended Stats");
			extendedStatsToggle.setText("");
		} else {
			icon = new FontAwesomeIconView(FontAwesomeIcon.PLUS_CIRCLE);
			tooltip = new Tooltip("Show Extended Stats");
			extendedStatsToggle.setText("Show Extended Stats");
		}
		icon.setSize("16");
		icon.setStyle("-fx-fill: -prob-grey;");
		extendedStatsToggle.setGraphic(icon);
		extendedStatsToggle.setTooltip(tooltip);
	}

	private void computeStats(Trace trace) {
		if (trace != null) {
			final ComputeStateSpaceStatsCommand stateSpaceStatsCmd = new ComputeStateSpaceStatsCommand();
			trace.getStateSpace().execute(stateSpaceStatsCmd);
			updateSimpleStats(stateSpaceStatsCmd.getResult());

			if (extendedStatsToggle.isSelected()) {
				final ComputeCoverageCommand coverageCmd = new ComputeCoverageCommand();
				trace.getStateSpace().execute(coverageCmd);
				updateExtendedStats(coverageCmd.getResult());
			}
		}
	}

	private void updateSimpleStats(StateSpaceStats result) {
		int nrTotalNodes = result.getNrTotalNodes();
		int nrTotalTransitions = result.getNrTotalTransitions();
		int nrProcessedNodes = result.getNrProcessedNodes();

		Platform.runLater(() -> {
			totalNodes.setText(Integer.toString(nrTotalNodes));
			totalTransitions.setText(Integer.toString(nrTotalTransitions));
			processedNodes.setText(Integer.toString(nrProcessedNodes));
			if (nrTotalNodes != 0) {
				percentageProcessed.setText("("+Integer.toString(100 * nrProcessedNodes / nrTotalNodes)+"%)");
			}
		});
	}

	public void updateExtendedStats(ComputeCoverageCommand.ComputeCoverageResult result) {
		showStats(result.getNodes(), nodeStats);
		showStats(result.getOps(), transStats);
	}

	private static void showStats(List<String> packedStats, GridPane grid) {
		final Node[][] stats = new Node[packedStats.size()][];

		for (int i = 0; i < packedStats.size(); i++) {
			final String pStat = packedStats.get(i);
			final String woPre = pStat.startsWith("'") ? pStat.substring(1) : pStat;
			final String woSuf = woPre.endsWith("'") ? woPre.substring(0, woPre.length() - 1) : woPre;
			final String[] split = woSuf.split(":");
			final Stat stat;
			if (split.length == 2) {
				stat = new Stat(split[0], split[1]);
			} else if (split.length == 1) {
				stat = new Stat(split[0]);
			} else {
				throw new IllegalArgumentException(String.format(
						"Invalid number of splits (%d, should be 1 or 2) for packed stat: %s", split.length, pStat));
			}
			stats[i] = stat.toFX();
		}

		Platform.runLater(() -> {
			grid.getChildren().clear();
			for (int i = 0; i < stats.length; i++) {
				grid.addRow(i + 1, stats[i]);
			}
		});
	}
}
