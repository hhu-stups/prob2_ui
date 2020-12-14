package de.prob2.ui.dynamic.table;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.animator.command.GetShortestTraceCommand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.TableData;
import de.prob.animator.domainobjects.TableVisualizationCommand;
import de.prob.statespace.State;
import de.prob2.ui.beditor.BEditorView;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.dynamic.DynamicCommandStage;
import de.prob2.ui.dynamic.DynamicPreferencesStage;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class ExpressionTableView extends DynamicCommandStage<TableVisualizationCommand> {

	private static final class ValueItemRow extends TableRow<ObservableList<String>> {

		private final List<String> header;

		private ValueItemRow(final List<String> header) {
			super();
			this.header = header;
			getStyleClass().add("expression-table-view-row");
		}

		@Override
		protected void updateItem(final ObservableList<String> item, final boolean empty) {
			super.updateItem(item, empty);
			this.getStyleClass().removeAll("true-val", "false-val");
			if(item != null && !empty) {
				if (header.contains(VALUE_COLUMN_NAME)) {
					int indexOfValue = header.indexOf(VALUE_COLUMN_NAME);
					String value = item.get(indexOfValue);
					switch (value) {
						case "TRUE":
							getStyleClass().add("true-val");
							break;
						case "FALSE":
							getStyleClass().add("false-val");
							break;
						default:
							break;
					}
				}
			}
		}
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionTableView.class);

	private static final double TABLE_DEFAULT_WIDTH = 800.0;
	
	private static final Pattern NEEDS_CSV_QUOTE_PATTERN = Pattern.compile("[,\"\n\r]");

	private static final String SOURCE_COLUMN_NAME = "Source";

	private static final String STATE_ID_COLUMN_NAME = "State ID";

	private static final String VALUE_COLUMN_NAME = "VALUE";
	
	@FXML
	private TableView<ObservableList<String>> tableView;
	
	@FXML
	private Button saveButton;
	
	@FXML
	private HelpButton helpButton;

	private final Injector injector;
	
	private final FileChooserManager fileChooserManager;
	
	private ObjectProperty<TableData> currentTable;
	
	
	@Inject
	public ExpressionTableView(final Injector injector, final StageManager stageManager, final DynamicPreferencesStage preferences, final CurrentTrace currentTrace,
							   final CurrentProject currentProject, final ResourceBundle bundle, final FileChooserManager fileChooserManager, final StopActions stopActions) {
		super(preferences, currentTrace, currentProject, bundle, stopActions, "Expression Table Visualizer");
		this.injector = injector;
		this.fileChooserManager = fileChooserManager;
		this.currentTable = new SimpleObjectProperty<>(this, "currentTable", null);
		stageManager.loadFXML(this, "table_view.fxml");
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		helpButton.setHelpContent("mainView.formulaTableVisualisation", null);
		currentTable.addListener((observable, from, to) -> {
			if(to != null) {
				fillTable(to);
			}
		});
		saveButton.disableProperty().bind(currentTable.isNull());
		taFormula.focusedProperty().addListener((observable, from, to) -> {
			if(to) {
				if(taFormula.getText().equals(bundle.getString("table.expressionTableView.enterFormula.placeholder"))) {
					taFormula.clear();
				}
			} else {
				if(taFormula.getText().isEmpty()) {
					taFormula.setText(bundle.getString("table.expressionTableView.enterFormula.placeholder"));
				}
			}
		});
	}
	
	@Override
	protected List<TableVisualizationCommand> getCommandsInState(final State state) {
		return TableVisualizationCommand.getAll(state);
	}
	
	public void visualizeExpression(String expression) {
		this.selectCommand(TableVisualizationCommand.EXPRESSION_AS_TABLE_NAME, expression);
	}
	
	@Override
	protected void visualizeInternal(final TableVisualizationCommand item, final List<IEvalElement> formulas) {
		final TableData table = item.visualize(formulas);
		Platform.runLater(() -> {
			this.clearLoadingStatus();
			currentTable.set(table);
		});
	}
	
	private void fillTable(TableData data) {
		List<String> header = data.getHeader();
		tableView.getColumns().clear();
		for (int i = 0; i < header.size(); i++) {
			final int j = i;
			final TableColumn<ObservableList<String>, String> column = new TableColumn<>(header.get(i));
			column.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().get(j)));
			tableView.getColumns().add(column);
			column.setPrefWidth(TABLE_DEFAULT_WIDTH/header.size());
			column.getStyleClass().add("expression-table-view-column");
			column.getStyleClass().add("alignment");
		}
		tableView.setItems(buildData(data.getRows()));
		tableView.setRowFactory(table -> {
			final ValueItemRow row = new ValueItemRow(header);
			List<MenuItem> contextMenuItems = new ArrayList<>();
			row.itemProperty().addListener((observable, from, to) -> {
				row.setContextMenu(null);
				if (header.contains(SOURCE_COLUMN_NAME)) {
					handleSource(header, to, contextMenuItems);
				}
				if (header.contains(STATE_ID_COLUMN_NAME)) {
					handleStateID(header, to, contextMenuItems);
				}
				if (!contextMenuItems.isEmpty() && to != null) {
					ContextMenu contextMenu = new ContextMenu();
					contextMenu.getItems().addAll(contextMenuItems);
					row.setContextMenu(contextMenu);
				}
			});
			return row;
		});
		taErrors.clear();
	}

	private void handleSource(List<String> header, List<String> item, List<MenuItem> contextMenuItems) {
		MenuItem showSourceItem = new MenuItem(bundle.getString("dynamic.tableview.showSource"));
		int indexOfSource = header.indexOf(SOURCE_COLUMN_NAME);
		if(item != null) {
			String source = item.get(indexOfSource);
			String[] start;
			List<Integer> line = new ArrayList<>();
			List<Integer> column = new ArrayList<>();
			List<Path> path = new ArrayList<>();
			if(source.startsWith(" at line ")) {
				String[] sourceSplitted = source.replaceFirst(" at line ", "").split(" \\- ");
				start = sourceSplitted[0].split(":");
				line.add(Integer.parseInt(start[0]) - 1);
				column.add(Integer.parseInt(start[1]));
				path.add(currentTrace.getModel().getModelFile().toPath());
			} else if(source.startsWith("Line: ")) {
				source = source.replaceFirst("Line: ", "");
				int nextWhiteSpaceIndex = source.indexOf(" ");
				line.add(Integer.parseInt(source.substring(0, nextWhiteSpaceIndex)) - 1);
				source = source.substring(nextWhiteSpaceIndex + 1);
				source = source.replaceFirst("Column: ", "");
				nextWhiteSpaceIndex = source.indexOf(" ");
				column.add(Integer.parseInt(source.substring(0, nextWhiteSpaceIndex)));
				int fileIndex = source.indexOf("file: ");
				String pathAsString = source.substring(fileIndex + 6);
				path.add(Paths.get(new File(pathAsString).toURI()));
			} else {
				showSourceItem.setDisable(true);
				statusBar.setText(bundle.getString("dynamic.tableview.jumpToState.notPossible"));
				statusBar.setLabelStyle("warning");
			}

			showSourceItem.setOnAction(e -> {
				if(!line.isEmpty() && !column.isEmpty() && !path.isEmpty()) {
					injector.getInstance(BEditorView.class).jumpToSource(path.get(0), line.get(0), column.get(0));
				}
			});
		}

		contextMenuItems.add(showSourceItem);
	}

	private void handleStateID(List<String> header, List<String> item, List<MenuItem> contextMenuItems) {
		MenuItem jumpToStateItem = new MenuItem(bundle.getString("dynamic.tableview.jumpToState"));
		int indexOfStateID = header.indexOf(STATE_ID_COLUMN_NAME);
		if(item != null) {
			String stateID = item.get(indexOfStateID);
			jumpToStateItem.setOnAction(e -> {
				GetShortestTraceCommand cmd = new GetShortestTraceCommand(currentTrace.getStateSpace(), stateID);
				currentTrace.getStateSpace().execute(cmd);
				currentTrace.set(cmd.getTrace(currentTrace.getStateSpace()));
			});
			if ("none".equals(stateID)) {
				jumpToStateItem.setDisable(true);
			}
		}
		contextMenuItems.add(jumpToStateItem);
	}
	
	private ObservableList<ObservableList<String>> buildData(List<List<String>> list) {
		ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
		for (List<String> row : list) {
			data.add(FXCollections.observableArrayList(row));
		}
		return data;
	}
	
	@FXML
	private void save() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.csv", "csv"));
		fileChooser.setTitle(bundle.getString("common.fileChooser.saveAsCSV.title"));
		Path path = fileChooserManager.showSaveFileChooser(fileChooser, null, this);
		if(path == null || currentTable == null) {
			return;
		}
		if(!path.endsWith(".csv")) {
			path = path.resolveSibling(path.getFileName() + ".csv");
		}
		try {
			Files.write(path, toCSV(currentTable.get()));
		} catch (IOException e) {
			LOGGER.error("Saving as CSV failed", e);
		}
	}
	
	private static String escapeCSV(final String toEscape) {
		if (NEEDS_CSV_QUOTE_PATTERN.matcher(toEscape).find()) {
			return '"' + toEscape.replace("\"", "\"\"") + '"';
		} else {
			return toEscape;
		}
	}
	
	private static List<String> toCSV(TableData data) {
		final List<String> csv = new ArrayList<>();
		csv.add(String.join(",", data.getHeader()));
		data.getRows()
			.stream()
			.map(column -> column.stream().map(ExpressionTableView::escapeCSV).collect(Collectors.joining(",")))
			.collect(Collectors.toCollection(() -> csv));
		return csv;
	}
	
	@Override
	protected void clearContent() {
		currentTable.set(null);
		tableView.getItems().clear();
		tableView.getColumns().clear();
	}
	
	@FXML
	private void editPreferences() {
		TableVisualizationCommand currentItem = lvChoice.getSelectionModel().getSelectedItem();
		preferences.setTitle(String.format(bundle.getString("dynamic.preferences.stage.title"), currentItem.getName()));
		preferences.show();
	}
}
