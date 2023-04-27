package de.prob2.ui.animation.symbolic.testcasegeneration;

import de.prob2.ui.internal.I18n;
import java.util.Arrays;

import com.google.inject.Inject;

import de.prob.analysis.testcasegeneration.Target;
import de.prob.analysis.testcasegeneration.TestCaseGeneratorResult;
import de.prob.analysis.testcasegeneration.testtrace.TestTrace;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.WrappedTextTableCell;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

public final class TraceInformationStage extends Stage {

	private final class TraceInformationRow extends TableRow<TestTrace> {
		private TraceInformationRow() {
			super();
			this.getStyleClass().add("trace-information-row");
			this.setOnMouseClicked(e -> {
				TestTrace item = this.getItem();
				if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && item.getTrace() != null) {
					currentTrace.set(item.getTrace());
				}
			});
			this.itemProperty().addListener((observable, from, to) -> {
				if(to != null && to.getTrace() != null) {
					this.setCursor(Cursor.HAND);
				} else {
					this.setCursor(Cursor.DEFAULT);
				}
			});
			MenuItem executeTrace = new MenuItem("Show Trace");
			executeTrace.setOnAction(e -> {
				TestTrace item = this.getItem();
				if (item.getTrace() != null) {
					currentTrace.set(item.getTrace());
				}
			});
			this.setContextMenu(new ContextMenu(executeTrace));
		}

		@Override
		protected void updateItem(TestTrace item, boolean empty) {
			super.updateItem(item, empty);

			this.getStyleClass().removeAll(Arrays.asList("replayable", "not-replayable"));

			if (!empty) {
				if (item.getTarget().getFeasible()) {
					this.getStyleClass().add("replayable");
				} else {
					this.getStyleClass().add("not-replayable");
				}
			}
		}
	}

	private static final class UncoveredOperationRow extends TableRow<Target> {
		private UncoveredOperationRow() {
			this.getStyleClass().add("trace-information-row");
		}

		@Override
		protected void updateItem(final Target item, final boolean empty) {
			super.updateItem(item, empty);

			this.getStyleClass().removeAll(Arrays.asList("replayable", "not-replayable"));

			if (!empty) {
				if (item.getFeasible()) {
					this.getStyleClass().add("replayable");
				} else {
					this.getStyleClass().add("not-replayable");
				}
			}
		}
	}

	@FXML
	private TableView<TestTrace> tvTraces;

	@FXML
	private TableColumn<TestTrace, String> depth;

	@FXML
	private TableColumn<TestTrace, String> operations;

	@FXML
	private TableColumn<TestTrace, String> coveredOperation;
	
	@FXML
	private TableColumn<TestTrace, String> guard;
	
	@FXML
	private TableColumn<TestTrace, String> enabled;
	
	@FXML
	private TableView<Target> tvUncovered;
	
	@FXML
	private TableColumn<Target, String> uncoveredOperation;
	
	@FXML
	private TableColumn<Target, String> uncoveredGuard;

	private final CurrentTrace currentTrace;

	@Inject
	private TraceInformationStage(final StageManager stageManager, final CurrentTrace currentTrace) {
		stageManager.loadFXML(this, "test_case_generation_trace_information.fxml");
		this.currentTrace = currentTrace;
	}

	public void setResult(final TestCaseGeneratorResult result) {
		this.tvTraces.getItems().setAll(result.getTestTraces());
		this.tvUncovered.getItems().setAll(result.getUncoveredTargets());
	}

	@FXML
	public void initialize() {
		tvTraces.setRowFactory(item -> new TraceInformationRow());
		depth.setCellValueFactory(new PropertyValueFactory<>("depth"));
		operations.setCellValueFactory(features -> Bindings.createStringBinding(() ->
			String.join(",\n", features.getValue().getTransitionNames())
		));
		coveredOperation.setCellValueFactory(features -> Bindings.createStringBinding(() -> 
			features.getValue().getTarget().getOperation()
		));
		guard.setCellFactory(WrappedTextTableCell::new);
		guard.setCellValueFactory(features -> Bindings.createStringBinding(() ->
			features.getValue().getTarget().getGuardString()
		));
		enabled.setCellValueFactory(features -> Bindings.createStringBinding(() ->
			String.valueOf(features.getValue().getTarget().getFeasible())
		));
		
		tvUncovered.setRowFactory(item -> new UncoveredOperationRow());
		uncoveredOperation.setCellValueFactory(new PropertyValueFactory<>("operation"));
		uncoveredGuard.setCellFactory(WrappedTextTableCell::new);
		uncoveredGuard.setCellValueFactory(new PropertyValueFactory<>("guardString"));
	}

}
