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
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class ModelCheckStats extends AnchorPane implements IModelCheckListener {
	// @FXML
	// private Text titelText;
	// @FXML
	// private ImageView titelImage;
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

	@Inject
	public ModelCheckStats(FXMLLoader loader) {
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
			resultText.wrappingWidthProperty().bind(resultBackground.widthProperty().subtract(50.0));
		});
	}

	void addJob(String jobId, ModelChecker checker) {
		jobs.put(jobId, checker);
		statsBox.setVisible(true);
		resultBackground.setVisible(false);
	}

	@Override
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

	@Override
	public void isFinished(final String id, final long timeElapsed, final IModelCheckingResult result,
			final StateSpaceStats stats) {

		results.put(id, result);

		Platform.runLater(() -> {
			elapsedTime.setText("" + timeElapsed);
		});

		String res = result instanceof ModelCheckOk || result instanceof LTLOk ? "success"
				: result instanceof ITraceDescription ? "danger" : "warning";
		String message = result.getMessage();
		showResult(res, message);

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
		System.out.println("is finished");
	}

	private void showResult(String res, String message) {
		resultBackground.setVisible(true);
		resultText.setText(message);
		switch (res) {
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

	// private Image selectImage(String res) {
	// Image image = null;
	// switch (res) {
	// case "success":
	// image = new Image(
	// getClass().getResourceAsStream("/glyphicons_free/glyphicons/png/glyphicons-199-ok-circle.png"));
	// break;
	// case "danger":
	// image = new Image(
	// getClass().getResourceAsStream("/glyphicons_free/glyphicons/png/glyphicons-198-remove-circle.png"));
	// break;
	// case "warning":
	// image = new Image(
	// getClass().getResourceAsStream("/glyphicons_free/glyphicons/png/glyphicons-505-alert.png"));
	// break;
	// }
	// return image;
	// }

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

	@Override
	public String toString() {
		// ModelChecker modelChecker = jobs.get(id).getChecker();
		// ModelCheckingOptions options = jobs.get(id).getOptions();
		// AbstractElement main =
		// modelChecker.getStateSpace().getMainComponent();
		// List<String> optsList = new ArrayList<String>();
		// for (Options opts : options.getPrologOptions()) {
		// optsList.add(opts.getDescription());
		// if (opts.getDescription().equals("recheck existing states")) {
		// searchForNewErrors = false;
		// }
		// }
		// // Platform.runLater(() -> {
		// // String name = main == null ? "Model Check" : main.toString();
		// // if (!optsList.isEmpty()) {
		// // name += " with " + Joiner.on(", ").join(optsList);
		// // }
		// // titelText.setText(name);
		// // titelImage.setImage(selectImage(res));
		// // });
		return super.toString();
	}
}
