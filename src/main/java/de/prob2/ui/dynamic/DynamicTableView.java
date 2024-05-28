package de.prob2.ui.dynamic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.prob.animator.command.GetShortestTraceCommand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.TableData;
import de.prob.animator.domainobjects.TableVisualizationCommand;
import de.prob2.ui.beditor.BEditorView;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.csv.CSVWriter;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.util.Builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
public class DynamicTableView extends BorderPane implements Builder<DynamicTableView> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTableView.class);
	private static final double MINIMUM_TABLE_COLUMN_WIDTH = 160.0;
	private static final String SOURCE_COLUMN_NAME = "Source";
	private static final String STATE_ID_COLUMN_NAME = "State ID";
	private static final String VALUE_COLUMN_NAME = "VALUE";

	@FXML
	private TableView<List<String>> tableView;
	@FXML
	private Button saveButton;

	private final StageManager stageManager;
	private final I18n i18n;
	private final FileChooserManager fileChooserManager;
	private final CurrentTrace currentTrace;
	private final Provider<BEditorView> editorViewProvider;
	private final ObjectProperty<TableData> currentTable;

	/**
	 * Store header globally so that ValueItemRow always use the current header
	 */
	private List<String> header;

	@Inject
	public DynamicTableView(StageManager stageManager, I18n i18n, FileChooserManager fileChooserManager, CurrentTrace currentTrace, Provider<BEditorView> editorViewProvider) {
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.currentTrace = currentTrace;
		this.editorViewProvider = editorViewProvider;
		this.currentTable = new SimpleObjectProperty<>(this, "currentTable", null);
		stageManager.loadFXML(this, "table_view.fxml");
	}

	@FXML
	private void initialize() {
		this.currentTable.addListener((observable, from, to) -> this.fillTable(to));
		this.saveButton.disableProperty().bind(this.currentTable.isNull());
	}

	@Override
	public DynamicTableView build() {
		return this;
	}

	@FXML
	private void save() {
		if (this.currentTable.get() == null) {
			return;
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().setAll(this.fileChooserManager.getCsvFilter());
		fileChooser.setTitle(this.i18n.translate("common.fileChooser.saveAsCSV.title"));
		Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, this.getScene().getWindow());
		if (path == null || path.getFileName() == null || path.getParent() == null) {
			return;
		}

		try {
			Files.createDirectories(path.getParent());
			try (CSVWriter csvWriter = new CSVWriter(Files.newBufferedWriter(path))) {
				csvWriter.header(this.currentTable.get().getHeader());
				for (List<String> row : this.currentTable.get().getRows()) {
					csvWriter.record(row);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to save table", e);
			Alert alert = this.stageManager.makeExceptionAlert(e, "common.alerts.couldNotSaveFile.content", path);
			alert.initOwner(this.getScene().getWindow());
			alert.showAndWait();
		}
	}

	void clearContent() {
		this.setVisible(false);
		this.currentTable.set(null);
	}

	void visualize(TableVisualizationCommand command, List<IEvalElement> formulas) {
		final TableData table = command.visualize(formulas);
		Platform.runLater(() -> {
			this.currentTable.set(table);
			this.setVisible(true);
		});
	}

	private void fillTable(TableData data) {
		this.tableView.setItems(FXCollections.observableArrayList());
		this.tableView.getColumns().clear();
		this.header = null;
		if (data == null) {
			return;
		}

		// Update header when table is updated
		this.header = data.getHeader();

		// All table columns get the same size initially (but they can be resized by the user).
		// Make the table columns fill the current width of the table view exactly,
		// unless that would make the columns too narrow.
		final double columnWidth = Math.max(this.tableView.getWidth() / this.header.size(), MINIMUM_TABLE_COLUMN_WIDTH);
		for (int i = 0; i < this.header.size(); i++) {
			final TableColumn<List<String>, String> column = new TableColumn<>(this.header.get(i));
			final int columnIndex = i;
			column.setCellValueFactory(param -> {
				List<String> row = param.getValue();
				if (columnIndex >= row.size()) {
					return new SimpleStringProperty("");
				}

				return new SimpleStringProperty(row.get(columnIndex));
			});
			column.setPrefWidth(columnWidth);
			column.getStyleClass().add("expression-table-view-column");
			column.getStyleClass().add("alignment");
			this.tableView.getColumns().add(column);
		}
		ObservableList<List<String>> value = this.buildData(data.getRows());
		this.tableView.setItems(value);

		// Do not provide header to row factory as this would lead to the bug that the row factory always use the oldest headers
		// Instead use header as a variable in ExpressionTableView that can be accessed by ValueItemRow
		this.tableView.setRowFactory(table -> new ValueItemRow());
	}

	private void handleSource(List<String> header, List<String> item, List<MenuItem> contextMenuItems) {
		MenuItem showSourceItem = new MenuItem(this.i18n.translate("dynamic.tableview.showSource"));
		int indexOfSource = header.indexOf(SOURCE_COLUMN_NAME);
		if (item != null) {
			String source = item.get(indexOfSource);
			String[] start;
			final int line;
			final int column;
			final Path path;
			if (source.isEmpty()) {
				// No source location - just disable the item and don't attempt to parse anything.
				showSourceItem.setDisable(true);
				line = -1;
				column = -1;
				path = null;
			} else if (source.startsWith(" at line ")) {
				String[] sourceSplitted = source.replaceFirst(" at line ", "").split(" \\- ");
				start = sourceSplitted[0].split(":");
				line = Integer.parseInt(start[0]) - 1;
				column = Integer.parseInt(start[1]);
				path = this.currentTrace.getModel().getModelFile().toPath();
			} else if (source.startsWith("Line: ")) {
				source = source.replaceFirst("Line: ", "");
				int nextWhiteSpaceIndex = source.indexOf(" ");
				line = Integer.parseInt(source.substring(0, nextWhiteSpaceIndex)) - 1;
				source = source.substring(nextWhiteSpaceIndex + 1);
				source = source.replaceFirst("Column: ", "");
				nextWhiteSpaceIndex = source.indexOf(" ");
				column = Integer.parseInt(source.substring(0, nextWhiteSpaceIndex));
				int fileIndex = source.indexOf("file: ");
				String pathAsString = source.substring(fileIndex + 6);
				path = Paths.get(pathAsString);
			} else {
				showSourceItem.setDisable(true);
				line = -1;
				column = -1;
				path = null;
				LOGGER.warn("Could not parse source location from string in table visualization source column - Show Source will be disabled for this row: {}", source);
			}

			showSourceItem.setOnAction(e -> {
				assert line != -1;
				assert column != -1;
				assert path != null;
				this.editorViewProvider.get().jumpToSource(path, line, column);
			});
		}

		contextMenuItems.add(showSourceItem);
	}

	private void handleStateID(List<String> header, List<String> item, List<MenuItem> contextMenuItems) {
		MenuItem jumpToStateItem = new MenuItem(i18n.translate("dynamic.tableview.jumpToState"));
		int indexOfStateID = header.indexOf(STATE_ID_COLUMN_NAME);
		if (item != null) {
			String stateID = item.get(indexOfStateID);
			jumpToStateItem.setOnAction(e -> {
				GetShortestTraceCommand cmd = new GetShortestTraceCommand(this.currentTrace.getStateSpace(), stateID);
				this.currentTrace.getStateSpace().execute(cmd);
				this.currentTrace.set(cmd.getTrace(this.currentTrace.getStateSpace()));
			});
			if ("none".equals(stateID)) {
				jumpToStateItem.setDisable(true);
			}
		}
		contextMenuItems.add(jumpToStateItem);
	}

	private ObservableList<List<String>> buildData(List<List<String>> list) {
		ObservableList<List<String>> data = FXCollections.observableArrayList();
		for (List<String> row : list) {
			data.add(List.copyOf(row));
		}
		return data;
	}

	private final class ValueItemRow extends TableRow<List<String>> {

		private ValueItemRow() {
			super();
			this.getStyleClass().add("expression-table-view-row");
		}

		@Override
		protected void updateItem(List<String> item, boolean empty) {
			super.updateItem(item, empty);
			this.getStyleClass().removeAll("true-val", "false-val");
			if (item != null && !empty) {
				List<MenuItem> contextMenuItems = new ArrayList<>();
				this.setContextMenu(null);
				if (DynamicTableView.this.header.contains(SOURCE_COLUMN_NAME)) {
					DynamicTableView.this.handleSource(DynamicTableView.this.header, item, contextMenuItems);
				}
				if (DynamicTableView.this.header.contains(STATE_ID_COLUMN_NAME)) {
					DynamicTableView.this.handleStateID(DynamicTableView.this.header, item, contextMenuItems);
				}
				if (!contextMenuItems.isEmpty()) {
					ContextMenu contextMenu = new ContextMenu();
					contextMenu.getItems().addAll(contextMenuItems);
					this.setContextMenu(contextMenu);
				}
				if (DynamicTableView.this.header.contains(VALUE_COLUMN_NAME)) {
					int indexOfValue = DynamicTableView.this.header.indexOf(VALUE_COLUMN_NAME);
					String value = item.get(indexOfValue);
					switch (value) {
						case "TRUE":
							this.getStyleClass().add("true-val");
							break;
						case "FALSE":
							this.getStyleClass().add("false-val");
							break;
						default:
							break;
					}
				}
			}
		}
	}
}
