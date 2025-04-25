package de.prob2.ui.animation.tracereplay;

import java.util.Arrays;

import com.google.inject.Inject;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Text;

@FXMLInjected
public final class ReplayedTraceTable extends TableView<ReplayedTraceRow> {
	private final I18n i18n;

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
		this.i18n = i18n;
		stageManager.loadFXML(this, "replayed_trace_table.fxml");
	}

	private <T> TableCell<ReplayedTraceRow, T> cellFactory(TableColumn<ReplayedTraceRow, T> column) {
		return new TableCell<>() {
			{
				getStyleClass().add("trace-diff-cell");
			}

			@Override
			protected void updateItem(T item, boolean empty) {
				if (item == getItem()) {
					return;
				}

				super.updateItem(item, empty);

				if (empty || item == null) {
					setText(null);
					setGraphic(null);
				} else if (item instanceof Node) {
					setText(null);
					setGraphic((Node) item);
				} else {
					String s = item.toString();
					Text t = getCollapsibleTextWithLimit(s, 100, true);

					setText(null);
					setGraphic(t);

					// Add ContextMenu for copying messages
					MenuItem mi = new MenuItem(i18n.translate("common.buttons.copyToClipboard"));
					mi.setOnAction(e -> {
						ClipboardContent cc = new ClipboardContent();
						cc.putString(s);
						Clipboard.getSystemClipboard().setContent(cc);
					});
					this.setContextMenu(new ContextMenu(mi));
				}

				getStyleClass().removeAll("FAULTY", "FOLLOWING");
				getStyleClass().removeAll("not_possible", "possible", "imprecise", "precise", "skip", "manual"); // interactive replay
				if (item != null) {
					TableRow<ReplayedTraceRow> row = getTableRow();
					if (row != null) {
						ReplayedTraceRow rowItem = row.getItem();
						if (rowItem != null) {
							getStyleClass().addAll(rowItem.getStyleClasses());
						}
					}
				}
			}

			private Text getCollapsibleTextWithLimit(String s, int limit, boolean collapsed) {
				Text t;
				if (s.length() > limit) {
					if (collapsed) {
						t = new Text("▶ " + s.substring(0, limit) + "...");
					} else {
						t = new Text("▼ " + s);
					}
				} else {
					t = new Text(s);
				}
				t.setOnMouseClicked(e -> setGraphic(getCollapsibleTextWithLimit(s, limit, !collapsed)));
				t.wrappingWidthProperty().bind(this.getTableColumn().widthProperty().subtract(5));
				return t;
			}
		};
	}

	@FXML
	private void initialize() {
		stepColumn.setCellValueFactory(new PropertyValueFactory<>("step"));
		stepColumn.setCellFactory(this::cellFactory);

		fileTransitionColumn.setCellValueFactory(new PropertyValueFactory<>("fileTransition"));
		fileTransitionColumn.setCellFactory(this::cellFactory);

		replayedTransitionColumn.setCellValueFactory(new PropertyValueFactory<>("replayedTransition"));
		replayedTransitionColumn.setCellFactory(this::cellFactory);

		precisionColumn.setCellValueFactory(new PropertyValueFactory<>("precision"));
		precisionColumn.setCellFactory(this::cellFactory);

		errorMessageColumn.setCellValueFactory(new PropertyValueFactory<>("errorMessage"));
		errorMessageColumn.setCellFactory(this::cellFactory);
	}

	public void disableReplayedTransitionColumns() {
		this.getColumns().removeAll(Arrays.asList(replayedTransitionColumn, precisionColumn, errorMessageColumn));
	}
}
