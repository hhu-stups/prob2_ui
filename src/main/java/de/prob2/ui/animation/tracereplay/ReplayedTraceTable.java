package de.prob2.ui.animation.tracereplay;

import com.google.inject.Inject;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Arrays;

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

	private static <T> TableCell<ReplayedTraceRow, T> cellFactory(TableColumn<ReplayedTraceRow, T> column) {
		return new TableCell<ReplayedTraceRow, T>() {
			{
				getStyleClass().add("trace-diff-cell");
			}

			@Override
			protected void updateItem(T item, boolean empty) {
				if (item == getItem()) {
					return;
				}

				super.updateItem(item, empty);

				if (item == null) {
					setText(null);
					setGraphic(null);
				} else if (item instanceof Node) {
					setText(null);
					setGraphic((Node) item);
				} else {
					setText(item.toString());
					setGraphic(null);
				}

				getStyleClass().removeAll("FAULTY", "FOLLOWING");
				if (item != null) {
					TableRow<ReplayedTraceRow> row = getTableRow();
					ReplayedTraceRow rowItem = row.getItem();

					getStyleClass().addAll(rowItem.getStyleClasses());
				}
			}
		};
	}

	@FXML
	private void initialize() {
		stepColumn.setCellValueFactory(new PropertyValueFactory<>("step"));
		stepColumn.setCellFactory(ReplayedTraceTable::cellFactory);

		fileTransitionColumn.setCellValueFactory(new PropertyValueFactory<>("fileTransition"));
		fileTransitionColumn.setCellFactory(ReplayedTraceTable::cellFactory);

		replayedTransitionColumn.setCellValueFactory(new PropertyValueFactory<>("replayedTransition"));
		replayedTransitionColumn.setCellFactory(ReplayedTraceTable::cellFactory);

		precisionColumn.setCellValueFactory(new PropertyValueFactory<>("precision"));
		precisionColumn.setCellFactory(ReplayedTraceTable::cellFactory);

		errorMessageColumn.setCellValueFactory(new PropertyValueFactory<>("errorMessage"));
		errorMessageColumn.setCellFactory(ReplayedTraceTable::cellFactory);
	}

	public void disableReplayedTransitionColumns() {
		this.getColumns().removeAll(Arrays.asList(replayedTransitionColumn, precisionColumn, errorMessageColumn));
	}
}
