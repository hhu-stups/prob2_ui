package de.prob2.ui.verifications.modelchecking;

import com.google.inject.Singleton;
import de.prob.animator.command.ComputeCoverageCommand;
import de.prob.check.*;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.stats.StatsView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import javax.inject.Inject;
import java.util.Objects;


@Singleton
public final class ModelCheckStats extends AnchorPane {
	public enum Result {
		SUCCESS, DANGER, WARNING
	}
	
	@FXML private AnchorPane resultBackground;
	@FXML private Text resultText;
	@FXML private VBox statsBox;
	@FXML private Label elapsedTime;
	@FXML private Label processedStates;
	@FXML private Label totalStates;
	@FXML private Label totalTransitions;

	private ModelcheckingController modelcheckingController;
	private Result result;
	private Trace trace;
	
	private final StatsView statsView;
	
	@Inject
	public ModelCheckStats(final StageManager stageManager, final ModelcheckingController modelcheckingController, final StatsView statsView) {
		this.modelcheckingController = modelcheckingController;
		this.statsView = statsView;
		stageManager.loadFXML(this, "modelchecking_stats.fxml");
	}

	@FXML
	private void initialize() {
		this.modelcheckingController.widthProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue == null) {
				resultText.setWrappingWidth(0);
				return;
			}
			resultText.setWrappingWidth(newValue.doubleValue() - 60);
		});
	}

	void startJob() {
		statsBox.setVisible(true);
		resultBackground.setVisible(false);
	}

	public void updateStats(final IModelCheckJob modelChecker, final long timeElapsed, final StateSpaceStats stats) {
		Objects.requireNonNull(modelChecker, "modelChecker");
		
		Platform.runLater(() -> elapsedTime.setText(String.valueOf(timeElapsed)));

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
			statsView.updateExtendedStats(coverage);
		}
	}

	public void isFinished(final IModelCheckJob modelChecker, final long timeElapsed, final IModelCheckingResult result) {
		Objects.requireNonNull(modelChecker, "modelChecker");
		Objects.requireNonNull(result, "result");
		
		Platform.runLater(() -> elapsedTime.setText(String.valueOf(timeElapsed)));
		
		if (result instanceof ModelCheckOk || result instanceof LTLOk) {
			this.result = Result.SUCCESS;
		} else if (result instanceof ITraceDescription) {
			this.result = Result.DANGER;
		} else {
			this.result = Result.WARNING;
		}
		String message = result.getMessage();

		final StateSpace stateSpace = modelChecker.getStateSpace();
		final ComputeCoverageCommand cmd = new ComputeCoverageCommand();
		stateSpace.execute(cmd);
		final ComputeCoverageCommand.ComputeCoverageResult coverage = cmd.getResult();
		
		if (coverage != null) {
			statsView.updateExtendedStats(coverage);
			Number numNodes = coverage.getTotalNumberOfNodes();
			Number numTrans = coverage.getTotalNumberOfTransitions();

			Platform.runLater(() -> {
				totalStates.setText(String.valueOf(numNodes));
				totalTransitions.setText(String.valueOf(numTrans));
			});
		}
		
		if (result instanceof ITraceDescription) {
			StateSpace s = modelChecker.getStateSpace();
			trace = ((ITraceDescription) result).getTrace(s);
		}
		showResult(message);
	}

	private void showResult(String message) {
		resultBackground.setVisible(true);
		resultText.setText(message);
		resultText.setWrappingWidth(this.modelcheckingController.widthProperty().doubleValue() - 60);
		switch (this.result) {
			case SUCCESS:
				resultBackground.getStyleClass().setAll("mcheckSuccess");
				resultText.setFill(Color.web("#5e945e"));
				break;

			case DANGER:
				resultBackground.getStyleClass().setAll("mcheckDanger");
				resultText.setFill(Color.web("#b95050ff"));
				break;

			case WARNING:
				resultBackground.getStyleClass().setAll("mcheckWarning");
				resultText.setFill(Color.web("#96904e"));
				break;

			default:
				throw new IllegalArgumentException("Unknown result: " + this.result);
		}
	}
	
	public Result getResult() {
		return result;
	}

	public Trace getTrace() {
		return trace;
	}
}
