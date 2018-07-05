package de.prob2.ui.verifications.modelchecking;

import java.util.Objects;

import javax.inject.Inject;

import com.google.inject.Injector;

import de.prob.animator.command.ComputeCoverageCommand;
import de.prob.check.IModelCheckJob;
import de.prob.check.IModelCheckingResult;
import de.prob.check.LTLOk;
import de.prob.check.ModelCheckOk;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.MachineStatusHandler;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;


public final class ModelCheckStats extends AnchorPane {
	
	@FXML private AnchorPane resultBackground;
	@FXML private Text resultText;
	@FXML private VBox statsBox;
	@FXML private Label elapsedTime;
	@FXML private Label processedStates;
	@FXML private Label totalStates;
	@FXML private Label totalTransitions;

	private Trace trace;

	
	private ModelCheckingItem item;
	
	private final Injector injector;
	
	@Inject
	public ModelCheckStats(final StageManager stageManager, final Injector injector) {
		this.injector = injector;
		stageManager.loadFXML(this, "modelchecking_stats.fxml");
	}

	@FXML
	private void initialize() {
		injector.getInstance(ModelcheckingView.class).widthProperty().addListener((observableValue, oldValue, newValue) -> {
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
		
		Platform.runLater(() -> elapsedTime.setText(String.format("%.1f",timeElapsed/1000.0) + " sec"));

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

	public void isFinished(final IModelCheckJob modelChecker, final long timeElapsed, final IModelCheckingResult result) {
		Objects.requireNonNull(modelChecker, "modelChecker");
		Objects.requireNonNull(result, "result");
		Platform.runLater(() -> elapsedTime.setText(String.format("%.3f",timeElapsed/1000.0) + " sec"));
		
		if (result instanceof ModelCheckOk || result instanceof LTLOk) {
			item.setCheckedSuccessful();
			item.setChecked(Checked.SUCCESS);
		} else if (result instanceof ITraceDescription) {
			item.setCheckedFailed();
			item.setChecked(Checked.FAIL);
		} else {
			item.setTimeout();
			item.setChecked(Checked.TIMEOUT);
		}

		item.setStats(this);
		
		Platform.runLater(() -> {
			Machine machine = injector.getInstance(CurrentProject.class).getCurrentMachine();
			injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.MODELCHECKING);
		});
		String message = result.getMessage();

		final StateSpace stateSpace = modelChecker.getStateSpace();
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
			trace = ((ITraceDescription) result).getTrace(stateSpace);
			injector.getInstance(StatsView.class).update(trace);
		}
		showResult(message);
	}
	


	private void showResult(String message) {
		resultBackground.setVisible(true);
		resultText.setText(message);
		resultText.setWrappingWidth(injector.getInstance(ModelcheckingView.class).widthProperty().doubleValue() - 60);
		switch (item.getChecked()) {
			case SUCCESS:
				resultBackground.getStyleClass().setAll("mcheckSuccess");
				resultText.setFill(Color.web("#5e945e"));
				break;

			case FAIL:
				resultBackground.getStyleClass().setAll("mcheckDanger");
				resultText.setFill(Color.web("#b95050ff"));
				break;

			case TIMEOUT:
				resultBackground.getStyleClass().setAll("mcheckWarning");
				resultText.setFill(Color.web("#96904e"));
				break;

			default:
				throw new IllegalArgumentException("Unknown result: " + item.getChecked());
		}
	}
	
	public Trace getTrace() {
		return trace;
	}
	
	public void setBackgroundOnClick(EventHandler<? super MouseEvent> eventHandler) {
		resultBackground.setOnMouseClicked(eventHandler);
	}
	
	public void updateItem(ModelCheckingItem item) {
		this.item = item;
	}
	
}
