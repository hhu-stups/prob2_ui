package de.prob2.ui.modelchecking;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob.animator.command.ComputeCoverageCommand.ComputeCoverageResult;
import de.prob.check.IModelCheckingResult;
import de.prob.check.LTLOk;
import de.prob.check.ModelCheckOk;
import de.prob.check.ModelChecker;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class ModelCheckStats extends AnchorPane {
	@FXML
	private AnchorPane resultBackground;
	@FXML
	private Text resultText;
	@FXML
	private VBox statsBox;
	@FXML
	private Label elapsedTime;
	@FXML
	private Label processedNodes;
	@FXML
	private Label totalNodes;
	@FXML
	private Label totalTransitions;
	@FXML
	private GridPane nodeStats;
	@FXML
	private GridPane transStats;

	private Map<String, ModelChecker> jobs = new HashMap<>();
	private Map<String, IModelCheckingResult> results = new HashMap<>();
	private ModelcheckingController modelcheckingController;
	private String result = "warning";
	private Trace trace;
	private Logger logger = LoggerFactory.getLogger(ModelCheckStats.class);

	@Inject
	public ModelCheckStats(FXMLLoader loader, ModelcheckingController modelcheckingController) {
		this.modelcheckingController = modelcheckingController;
		try {
			loader.setLocation(getClass().getResource("modelchecking_stats.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}
	}

	@FXML
	public void initialize() {
		Platform.runLater(() -> {
			this.modelcheckingController.widthProperty().addListener((observableValue, oldValue, newValue) -> {
				if (newValue == null) {
					resultText.setWrappingWidth(0);
					return;
				}
				resultText.setWrappingWidth(newValue.doubleValue() - 60);
			});
		});
	}

	void addJob(String jobId, ModelChecker checker) {
		jobs.put(jobId, checker);
		statsBox.setVisible(true);
		resultBackground.setVisible(false);
	}

	public void updateStats(final String id, final long timeElapsed, final IModelCheckingResult result,
			final StateSpaceStats stats) {
		results.put(id, result);

		Platform.runLater(() -> {
			elapsedTime.setText(String.valueOf(timeElapsed));
		});

		if (stats != null) {
			int nrProcessedNodes = stats.getNrProcessedNodes();
			int nrTotalNodes = stats.getNrTotalNodes();
			int nrTotalTransitions = stats.getNrTotalTransitions();
			int percent = nrProcessedNodes * 100 / nrTotalNodes;
			Platform.runLater(() -> {
				processedNodes.setText(nrProcessedNodes + " (" + percent + " %)");
				totalNodes.setText(String.valueOf(nrTotalNodes));
				totalTransitions.setText(String.valueOf(nrTotalTransitions));
			});
		}
		logger.debug("updated Stats");
	}

	public void isFinished(final String id, final long timeElapsed, final IModelCheckingResult result,
			final StateSpaceStats stats) {
		results.put(id, result);

		Platform.runLater(() -> {
			elapsedTime.setText(String.valueOf(timeElapsed));
		});

		this.result = result instanceof ModelCheckOk || result instanceof LTLOk ? "success"
				: result instanceof ITraceDescription ? "danger" : "warning";
		String message = result.getMessage();

		ModelChecker modelChecker = jobs.get(id);
		ComputeCoverageResult coverage = null;

		if (modelChecker != null) {
			coverage = modelChecker.getCoverage();
		}
		jobs.remove(id);

		if (coverage != null) {
			Number numNodes = coverage.getTotalNumberOfNodes();
			Number numTrans = coverage.getTotalNumberOfTransitions();

			Platform.runLater(() -> {
				totalNodes.setText(String.valueOf(numNodes));
				totalTransitions.setText(String.valueOf(numTrans));
			});

			showStats(coverage.getNodes(), nodeStats);
			showStats(coverage.getOps(), transStats);
		}

		boolean hasTrace = result instanceof ITraceDescription;
		if (hasTrace) {
			StateSpace s = modelChecker.getStateSpace(); // FIXME is modelChecker always != null ?
			trace = ((ITraceDescription) result).getTrace(s);

		}
		showResult(message);

		logger.debug("is finished");
	}

	private void showResult(String message) {
		resultBackground.setVisible(true);
		resultText.setText(message);
		resultText.setWrappingWidth(this.modelcheckingController.widthProperty().doubleValue() - 60);
		switch (this.result) {
		case "success":
			resultBackground.getStyleClass().clear();
			resultBackground.getStyleClass().add("mcheckSuccess");
			resultText.setFill(Color.web("#5e945e"));
			break;

		case "danger":
			resultBackground.getStyleClass().clear();
			resultBackground.getStyleClass().add("mcheckDanger");
			resultText.setFill(Color.web("#b95050ff"));
			break;

		case "warning":
			resultBackground.getStyleClass().clear();
			resultBackground.getStyleClass().add("mcheckWarning");
			resultText.setFill(Color.web("#96904e"));
			break;

		default:
			throw new IllegalArgumentException("Unknown result: " + this.result);
		}
	}

	private void showStats(List<String> packedStats, GridPane grid) {
		Platform.runLater(() -> {
			grid.getChildren().clear();
		});
		for (String pStat : packedStats) {
			String woPre = pStat.startsWith("'") ? pStat.substring(1) : pStat;
			String woSuf = woPre.endsWith("'") ? woPre.substring(0, woPre.length() - 1) : woPre;
			String[] split = woSuf.split(":");
			Stat stat;
			if (split.length == 2) {
				stat = new Stat(split[0], split[1]);
			} else if (split.length == 1) {
				stat = new Stat(split[0], null);
			} else {
				throw new IllegalArgumentException(String.format(
						"Invalid number of splits (%d, should be 1 or 2) for packed stat: %s", split.length, pStat));
			}
			Node[] statFX = stat.toFX();
			Platform.runLater(() -> {
				grid.addRow(packedStats.indexOf(pStat) + 1, statFX);
			});
		}
	}

	public String getResult() {
		return result;
	}

	public Trace getTrace() {
		return trace;
	}
}
