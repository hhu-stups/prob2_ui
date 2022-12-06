package de.prob2.ui.animation.tracereplay;

import java.util.Arrays;

import com.google.inject.Inject;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

@FXMLInjected
public class ReplayedTraceTable extends TableView<ReplayedTraceRow> {

	@FXML
	private TableColumn<ReplayedTraceRow, Integer> stepColumn;

	@FXML
	private TableColumn<ReplayedTraceRow, String> fileTransitionColumn;

	@FXML
	private TableColumn<ReplayedTraceRow, String> replayedTransitionColumn;

	@FXML
	private TableColumn<ReplayedTraceRow, String> precisionColumn;

	@FXML
	private TableColumn<ReplayedTraceRow, String> errorMessageColumn;

	@Inject
	public ReplayedTraceTable(StageManager stageManager, I18n i18n) {
		super();
		stageManager.loadFXML(this, "replayed_trace_table.fxml");
	}

	@FXML
	private void initialize() {
		stepColumn.setCellValueFactory(new PropertyValueFactory<>("step"));
		fileTransitionColumn.setCellValueFactory(new PropertyValueFactory<>("fileTransition"));
		replayedTransitionColumn.setCellValueFactory(new PropertyValueFactory<>("replayedTransition"));
		precisionColumn.setCellValueFactory(new PropertyValueFactory<>("precision"));
		errorMessageColumn.setCellValueFactory(new PropertyValueFactory<>("errorMessage"));
	}

	public void disableReplayedTransitionColumns() {
		this.getColumns().removeAll(Arrays.asList(replayedTransitionColumn, precisionColumn, errorMessageColumn));
	}
}
