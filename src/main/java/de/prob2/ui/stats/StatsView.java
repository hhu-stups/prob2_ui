package de.prob2.ui.stats;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.animator.command.ComputeCoverageCommand;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
	private static final Logger logger = LoggerFactory.getLogger(StatsView.class);

	@Inject
	public StatsView(final FXMLLoader loader, final CurrentTrace currentTrace) {
		this.currentTrace = currentTrace;

		loader.setLocation(getClass().getResource("stats_view.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
	}

	@FXML
	public void initialize() {
		extendedStatsBox.visibleProperty().bind(extendedStatsToggle.selectedProperty());
		statsBox.visibleProperty().bind(currentTrace.existsProperty());
		noStatsLabel.visibleProperty().bind(statsBox.visibleProperty().not());
		
//		final ChangeListener<Trace> traceChangeListener = (observable, from, to) -> {
//			final ComputeCoverageCommand cmd = new ComputeCoverageCommand();
//			if (currentTrace.exists()) {
//				this.currentTrace.getStateSpace().execute(cmd);
//				update(cmd.getResult());
//			} else {
//				update(cmd.new ComputeCoverageResult(new IntegerPrologTerm(0), new IntegerPrologTerm(0),
//						new ListPrologTerm(), new ListPrologTerm(), new ListPrologTerm()));
//			}
//		};
//		traceChangeListener.changed(this.currentTrace, null, currentTrace.get());
//		this.currentTrace.addListener(traceChangeListener);
	}
	
	@FXML
	private void handleExtendedStatsToggle() {
		FontAwesomeIconView icon;
		Tooltip tooltip;
		if(extendedStatsToggle.isSelected()) {
			icon = new FontAwesomeIconView(FontAwesomeIcon.CLOSE);
			tooltip = new Tooltip("Close Extended Stats");
		} else {
			icon = new FontAwesomeIconView(FontAwesomeIcon.PLUS_CIRCLE);
			tooltip = new Tooltip("Show Extended Stats");
		}
		icon.setSize("16");
		icon.setStyle("-fx-fill: -prob-grey");
		extendedStatsToggle.setGraphic(icon);
		extendedStatsToggle.setTooltip(tooltip);
	}

	public void update(ComputeCoverageCommand.ComputeCoverageResult result) {
//		if (result.getNodes().isEmpty() && result.getOps().isEmpty()
//				&& result.getTotalNumberOfNodes().intValue() == 0) {
//			statsBox.setVisible(false);
//			noStatsLabel.setVisible(true);
//		} else {
//			statsBox.setVisible(true);
//			noStatsLabel.setVisible(false);
//
//				Number numTrans = result.getTotalNumberOfTransitions();
//				Number numNodes = result.getTotalNumberOfNodes();
//
//				Platform.runLater(() -> {
//					totalTransitions.setText(String.valueOf(numTrans));
//					totalNodes.setText(String.valueOf(numNodes));
//				});
//
//			showStats(result.getNodes(), nodeStats);
//			showStats(result.getOps(), transStats);
//		}
	}

//	private static void showStats(List<String> packedStats, GridPane grid) {
//		final Node[][] stats = new Node[packedStats.size()][];
//
//		for (int i = 0; i < packedStats.size(); i++) {
//			final String pStat = packedStats.get(i);
//			final String woPre = pStat.startsWith("'") ? pStat.substring(1) : pStat;
//			final String woSuf = woPre.endsWith("'") ? woPre.substring(0, woPre.length() - 1) : woPre;
//			final String[] split = woSuf.split(":");
//			final Stat stat;
//			if (split.length == 2) {
//				stat = new Stat(split[0], split[1]);
//			} else if (split.length == 1) {
//				stat = new Stat(split[0]);
//			} else {
//				throw new IllegalArgumentException(String.format(
//						"Invalid number of splits (%d, should be 1 or 2) for packed stat: %s", split.length, pStat));
//			}
//			stats[i] = stat.toFX();
//		}
//
//		Platform.runLater(() -> {
//			grid.getChildren().clear();
//			for (int i = 0; i < stats.length; i++) {
//				grid.addRow(i + 1, stats[i]);
//			}
//		});
//	}
}
