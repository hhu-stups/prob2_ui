package de.prob2.ui.verifications.modelchecking;

import com.google.inject.Injector;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.animator.command.ComputeCoverageCommand;
import de.prob.check.IModelCheckJob;
import de.prob.check.IModelCheckingResult;
import de.prob.check.LTLOk;
import de.prob.check.ModelCheckOk;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.StateSpaceStats;
import de.prob.statespace.ITraceDescription;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.MachineStatusHandler;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.ResourceBundle;


public final class ModelCheckStats extends AnchorPane {
	
	@FXML private TableView<ModelCheckingJobItem> tvChecks;
	@FXML private TableColumn<ModelCheckingJobItem, FontAwesomeIconView> statusColumn;
	@FXML private TableColumn<ModelCheckingJobItem, Integer> indexColumn;
	@FXML private TableColumn<ModelCheckingJobItem, String> messageColumn;
	
	@FXML private VBox statsBox;
	@FXML private Label elapsedTime;
	@FXML private Label processedStates;
	@FXML private Label totalStates;
	@FXML private Label totalTransitions;

	private ModelCheckingItem item;
	
	private final Injector injector;
	
	
	public ModelCheckStats(final StageManager stageManager, final Injector injector) {
		this.injector = injector;
		stageManager.loadFXML(this, "modelchecking_stats.fxml");
	}
	
	@FXML
	private void initialize() {
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		indexColumn.setCellValueFactory(new PropertyValueFactory<>("index"));
		messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
		
		tvChecks.setRowFactory(table -> {
			final TableRow<ModelCheckingJobItem> row = new TableRow<>();
			final BooleanBinding disableErrorItemsBinding = Bindings.createBooleanBinding(
					() -> row.isEmpty() || row.getItem() == null || row.getItem().getStats() == null || row.getItem().getTrace() == null,
					row.emptyProperty(), row.itemProperty());
			
			MenuItem showTraceToErrorItem = new MenuItem(injector.getInstance(ResourceBundle.class).getString("verifications.modelchecking.modelcheckingView.contextMenu.showTraceToError"));
			showTraceToErrorItem.setOnAction(e-> {
				ModelCheckingJobItem item = tvChecks.getSelectionModel().getSelectedItem();
				injector.getInstance(CurrentTrace.class).set(item.getTrace());
				injector.getInstance(StatsView.class).update(item.getTrace());
			});
			showTraceToErrorItem.disableProperty().bind(disableErrorItemsBinding);
			
			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
					.then((ContextMenu)null)
					.otherwise(new ContextMenu(showTraceToErrorItem)));
			return row;
		});
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

	public void isFinished(final IModelCheckJob modelChecker, final long timeElapsed, final IModelCheckingResult result) {
		Objects.requireNonNull(modelChecker, "modelChecker");
		Objects.requireNonNull(result, "result");
		
		Platform.runLater(() -> {
			elapsedTime.setText(String.format("%.3f",timeElapsed/1000.0) + " s");
			Machine machine = injector.getInstance(CurrentProject.class).getCurrentMachine();
			injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.MODELCHECKING);
		});
		
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
		
		showResult(result, stateSpace);
		
		boolean failed = tvChecks.getItems()
			.stream()
			.map(item -> item.getChecked())
			.anyMatch(checked -> checked == Checked.FAIL);
		boolean success = !failed & tvChecks.getItems()
				.stream()
				.map(item -> item.getChecked())
				.anyMatch(checked -> checked == Checked.SUCCESS);
		if (success) {
			item.setCheckedSuccessful();
			item.setChecked(Checked.SUCCESS);
		} else if (failed) {
			item.setCheckedFailed();
			item.setChecked(Checked.FAIL);
		} else {
			item.setTimeout();
			item.setChecked(Checked.TIMEOUT);
		}

		item.setStats(this);
		
		if (result instanceof ITraceDescription) {
			Trace trace = ((ITraceDescription) result).getTrace(stateSpace);
			injector.getInstance(StatsView.class).update(trace);
		}
	}
	


	private void showResult(IModelCheckingResult result, StateSpace stateSpace) {
		ModelCheckingJobItem jobItem = new ModelCheckingJobItem(this, tvChecks.getItems().size() + 1, result.getMessage());
		if (result instanceof ModelCheckOk || result instanceof LTLOk) {
			jobItem.setCheckedSuccessful();
			jobItem.setChecked(Checked.SUCCESS);
		} else if (result instanceof ITraceDescription) {
			jobItem.setCheckedFailed();
			jobItem.setChecked(Checked.FAIL);
		} else {
			jobItem.setTimeout();
			jobItem.setChecked(Checked.TIMEOUT);
		}
		tvChecks.getItems().add(jobItem);
		if (result instanceof ITraceDescription) {
			Trace trace = ((ITraceDescription) result).getTrace(stateSpace);
			jobItem.setTrace(trace);
		}
	}
	
	public void updateItem(ModelCheckingItem item) {
		this.item = item;
	}
	
}
