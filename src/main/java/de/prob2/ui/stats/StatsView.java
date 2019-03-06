package de.prob2.ui.stats;

import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob.animator.command.ComputeCoverageCommand;
import de.prob.animator.command.ComputeStateSpaceStatsCommand;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.Trace;

import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableIntegerValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

@FXMLInjected
@Singleton
public class StatsView extends ScrollPane {
	@FXML
	private Label totalTransitions;
	@FXML
	private Label totalStates;
	@FXML
	private Label processedStates;
	@FXML
	private Label percentageProcessed;
	@FXML
	private GridPane stateStats;
	@FXML
	private GridPane transStats;
	@FXML
	private GridPane stateStatsHeader;
	@FXML
	private GridPane transStatsHeader;
	@FXML
	private VBox statsBox;
	@FXML
	private VBox extendedStatsBox;
	@FXML
	private Label noStatsLabel;
	@FXML
	private ToggleButton extendedStatsToggle;
	@FXML
	private AnchorPane numberOfStatesAnchorPane;
	@FXML
	private AnchorPane numberOfTransitionsAnchorPane;
	@FXML
	private HelpButton helpButton;

	private final ResourceBundle bundle;
	private final CurrentTrace currentTrace;
	private final FontSize fontSize;
	
	private final SimpleObjectProperty<StateSpaceStats> lastResult;

	@Inject
	public StatsView(final ResourceBundle bundle, final StageManager stageManager, final CurrentTrace currentTrace,
			final FontSize fontSize) {
		this.bundle = bundle;
		this.currentTrace = currentTrace;
		this.fontSize = fontSize;
		this.lastResult = new SimpleObjectProperty<>(this, "lastResult", null);
		stageManager.loadFXML(this, "stats_view.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent(this.getClass());
		extendedStatsBox.visibleProperty().bind(extendedStatsToggle.selectedProperty());
		noStatsLabel.visibleProperty().bind(currentTrace.existsProperty().not());
		statsBox.visibleProperty().bind(noStatsLabel.visibleProperty().not());
		extendedStatsBox.managedProperty().bind(extendedStatsBox.visibleProperty());
		statsBox.managedProperty().bind(statsBox.visibleProperty());
		noStatsLabel.managedProperty().bind(noStatsLabel.visibleProperty());

		this.currentTrace.addListener((observable, from, to) -> this.update(to));
		this.update(currentTrace.get());

		((FontAwesomeIconView) helpButton.getGraphic()).glyphSizeProperty().bind(fontSize.fontSizeProperty().multiply(1.2));

		numberOfStatesAnchorPane.widthProperty().addListener((observable, from, to) -> {
			stateStats.getColumnConstraints().get(1).setMinWidth(to.doubleValue());
			stateStatsHeader.getColumnConstraints().get(1).setMinWidth(to.doubleValue());
		});
		numberOfTransitionsAnchorPane.widthProperty().addListener((observable, from, to) -> {
			transStats.getColumnConstraints().get(1).setMinWidth(to.doubleValue());
			transStatsHeader.getColumnConstraints().get(1).setMinWidth(to.doubleValue());
		});

	}

	@FXML
	private void handleExtendedStatsToggle() {
		final FontAwesomeIcon icon;
		final String text;
		if (extendedStatsToggle.isSelected()) {
			this.update(currentTrace.get());

			icon = FontAwesomeIcon.CLOSE;
			text = bundle.getString("stats.statsView.hideExtendedStats");
		} else {
			icon = FontAwesomeIcon.PLUS_CIRCLE;
			text = bundle.getString("stats.statsView.showExtendedStats");
		}
		((FontAwesomeIconView)extendedStatsToggle.getGraphic()).setIcon(icon);
		extendedStatsToggle.setText(text);
		extendedStatsToggle.setTooltip(new Tooltip(text));
	}

	public void update(Trace trace) {
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
		lastResult.set(result);
		
		int nrTotalNodes = result.getNrTotalNodes();
		int nrTotalTransitions = result.getNrTotalTransitions();
		int nrProcessedNodes = result.getNrProcessedNodes();

		Platform.runLater(() -> {
			totalStates.setText(Integer.toString(nrTotalNodes));
			totalTransitions.setText(Integer.toString(nrTotalTransitions));
			processedStates.setText(Integer.toString(nrProcessedNodes));
			if (nrTotalNodes != 0) {
				percentageProcessed.setText("(" + Integer.toString(100 * nrProcessedNodes / nrTotalNodes) + "%)");
			}
		});
	}

	public void updateExtendedStats(ComputeCoverageCommand.ComputeCoverageResult result) {
		showStats(result.getNodes(), stateStats);
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
	
	public NumberBinding getProcessedStates() {
		return Bindings.createIntegerBinding(
				() -> lastResult.get() == null ? 0 : lastResult.get().getNrProcessedNodes(), lastResult);
	}

	public ObservableIntegerValue getStatesNumber() {
		return Bindings.createIntegerBinding(
				() -> lastResult.get() == null ? 0 : lastResult.get().getNrTotalNodes(), lastResult);
	}
	
}
