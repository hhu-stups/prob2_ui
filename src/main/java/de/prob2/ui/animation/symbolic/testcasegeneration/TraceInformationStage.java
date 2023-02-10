package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.WrappedTextTableCell;

import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

public final class TraceInformationStage extends Stage {

	private final class TraceInformationRow extends TableRow<TraceInformationItem> {
		private TraceInformationRow() {
			super();
			this.getStyleClass().add("trace-information-row");
			this.setOnMouseClicked(e -> {
				TraceInformationItem item = this.getItem();
				if(e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && item != null && item.getTrace() != null) {
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
		}

		@Override
		protected void updateItem(TraceInformationItem item, boolean empty) {
			super.updateItem(item, empty);
			this.getStyleClass().removeAll(Arrays.asList("replayable", "not-replayable"));
			if(item != null) {
				if (item.isEnabled()) {
					this.getStyleClass().add("replayable");
				} else {
					this.getStyleClass().add("not-replayable");
				}
			}
		}
	}

	@FXML
	private TableView<TraceInformationItem> tvTraces;

	@FXML
	private TableColumn<TraceInformationItem, String> depth;

	@FXML
	private TableColumn<TraceInformationItem, String> operations;

	@FXML
	private TableColumn<TraceInformationItem, String> coveredOperation;
	
	@FXML
	private TableColumn<TraceInformationItem, String> guard;
	
	@FXML
	private TableColumn<TraceInformationItem, String> enabled;
	
	@FXML
	private TableView<TraceInformationItem> tvUncovered;
	
	@FXML
	private TableColumn<TraceInformationItem, String> uncoveredOperation;
	
	@FXML
	private TableColumn<TraceInformationItem, String> uncoveredGuard;

	private final CurrentTrace currentTrace;

	@Inject
	private TraceInformationStage(final StageManager stageManager, final CurrentTrace currentTrace) {
		stageManager.loadFXML(this, "test_case_generation_trace_information.fxml");
		this.currentTrace = currentTrace;
	}

	public void setTraces(List<TraceInformationItem> traces) {
		this.tvTraces.getItems().setAll(traces);
	}
	
	public void setUncoveredOperations(List<TraceInformationItem> uncoveredOperations) {
		this.tvUncovered.getItems().setAll(uncoveredOperations);
	}

	@FXML
	public void initialize() {
		tvTraces.setRowFactory(item -> new TraceInformationRow());
		depth.setCellValueFactory(new PropertyValueFactory<>("depth"));
		operations.setCellValueFactory(new PropertyValueFactory<>("operations"));
		coveredOperation.setCellValueFactory(new PropertyValueFactory<>("operation"));
		guard.setCellFactory(WrappedTextTableCell<TraceInformationItem>::new);
		guard.setCellValueFactory(new PropertyValueFactory<>("guard"));
		
		enabled.setCellValueFactory(new PropertyValueFactory<>("enabled"));
		
		tvUncovered.setRowFactory(item -> new TraceInformationRow());
		uncoveredOperation.setCellValueFactory(new PropertyValueFactory<>("operation"));
		uncoveredGuard.setCellFactory(WrappedTextTableCell<TraceInformationItem>::new);
		uncoveredGuard.setCellValueFactory(new PropertyValueFactory<>("guard"));
	}

}
