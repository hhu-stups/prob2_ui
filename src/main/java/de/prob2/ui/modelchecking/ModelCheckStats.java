package de.prob2.ui.modelchecking;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

import de.prob.animator.command.ComputeCoverageCommand.ComputeCoverageResult;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.LTLOk;
import de.prob.check.ModelCheckOk;
import de.prob.check.ModelChecker;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.ITraceDescription;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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

	private Map<String, ModelChecker> jobs = new HashMap<String, ModelChecker>();
	private Map<String, IModelCheckingResult> results = new HashMap<String, IModelCheckingResult>();
	private ModelcheckingController modelcheckingController;
	private String result = "warning";

	@Inject
	public ModelCheckStats(FXMLLoader loader, ModelcheckingController modelcheckingController) {
		this.modelcheckingController = modelcheckingController;
		try {
			loader.setLocation(getClass().getResource("modelchecking_stats.fxml"));
			loader.setRoot(this);
			loader.setController(this);
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	public void initialize() {
		Platform.runLater(() -> {
			this.modelcheckingController.widthProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observableValue, Number oldValue,
						Number newValue) {
					if (newValue == null) {
						resultText.setWrappingWidth(0);
						return;
					}
					resultText.setWrappingWidth(newValue.doubleValue() - 60);
				}
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
			elapsedTime.setText("" + timeElapsed);
		});

		if (stats != null) {
			int nrProcessedNodes = stats.getNrProcessedNodes();
			int nrTotalNodes = stats.getNrTotalNodes();
			int nrTotalTransitions = stats.getNrTotalTransitions();
			int percent = nrProcessedNodes * 100 / nrTotalNodes;
			Platform.runLater(() -> {
				processedNodes.setText("" + nrProcessedNodes + " (" + percent + " %)");
				totalNodes.setText("" + nrTotalNodes);
				totalTransitions.setText("" + nrTotalTransitions);
			});
		}
		System.out.println("updated Stats");
	}

	public void isFinished(final String id, final long timeElapsed, final IModelCheckingResult result,
			final StateSpaceStats stats) {
		results.put(id, result);

		Platform.runLater(() -> {
			elapsedTime.setText("" + timeElapsed);
		});

		this.result = result instanceof ModelCheckOk || result instanceof LTLOk ? "success"
				: result instanceof ITraceDescription ? "danger" : "warning";
		String message = result.getMessage();

		boolean hasTrace = result instanceof ITraceDescription;
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
				totalNodes.setText("" + numNodes);
				totalTransitions.setText("" + numTrans);
			});

			showStats(coverage.getNodes(), nodeStats);
			showStats(coverage.getOps(), transStats);

			// List<String> uncovered = coverage.getUncovered();
			// for (String transition : uncovered) {
			// transStats.add(WebUtils.wrap("name", transition, "value", "0"));
			// }
			// String transitionStats = WebUtils.toJson(transStats);
		}
		showResult(message);
		System.out.println("is finished");
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
			resultText.setFill(Color.web("#b95050"));
			break;
		case "warning":
			resultBackground.getStyleClass().clear();
			resultBackground.getStyleClass().add("mcheckWarning");
			resultText.setFill(Color.web("#96904e"));
			break;
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
			Stat stat = null;
			if (split.length == 2) {
				stat = new Stat(split[0], split[1]);
			} else if (split.length == 1) {
				stat = new Stat(split[0], null);
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
}
