package de.prob2.ui.dynamic.table;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import de.prob.animator.command.GetShortestTraceCommand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.TableData;
import de.prob.animator.domainobjects.TableVisualizationCommand;
import de.prob.statespace.State;
import de.prob2.ui.beditor.BEditorView;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.dynamic.DynamicCommandFormulaItem;
import de.prob2.ui.dynamic.DynamicCommandStage;
import de.prob2.ui.dynamic.DynamicPreferencesStage;
import de.prob2.ui.dynamic.EditFormulaDialog;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.internal.csv.CSVWriter;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class ExpressionTableView extends DynamicCommandStage<TableVisualizationCommand> {

	private final class ValueItemRow extends TableRow<ObservableList<String>> {

		private ValueItemRow() {
			super();
			getStyleClass().add("expression-table-view-row");
		}

		@Override
		protected void updateItem(final ObservableList<String> item, final boolean empty) {
			super.updateItem(item, empty);
			this.getStyleClass().removeAll("true-val", "false-val");
			if(item != null && !empty) {
				List<MenuItem> contextMenuItems = new ArrayList<>();
				this.setContextMenu(null);
				if (header.contains(SOURCE_COLUMN_NAME)) {
					handleSource(header, item, contextMenuItems);
				}
				if (header.contains(STATE_ID_COLUMN_NAME)) {
					handleStateID(header, item, contextMenuItems);
				}
				if (!contextMenuItems.isEmpty()) {
					ContextMenu contextMenu = new ContextMenu();
					contextMenu.getItems().addAll(contextMenuItems);
					this.setContextMenu(contextMenu);
				}
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

	private static final double MINIMUM_TABLE_COLUMN_WIDTH = 160.0;

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

	private final ObjectProperty<TableData> currentTable;

	// Store header globally so that ValueItemRow always use the current header
	private List<String> header;


	@Inject
	public ExpressionTableView(final Injector injector, final StageManager stageManager, final Provider<DynamicPreferencesStage> preferencesStageProvider, final CurrentTrace currentTrace,
	                           final CurrentProject currentProject, final I18n i18n, final FileChooserManager fileChooserManager, final StopActions stopActions) {
		super(preferencesStageProvider, stageManager, currentTrace, currentProject, i18n, stopActions, "Expression Table Visualizer");
		this.injector = injector;
		this.fileChooserManager = fileChooserManager;
		this.currentTable = new SimpleObjectProperty<>(this, "currentTable", null);
		stageManager.loadFXML(this, "table_view.fxml");
	}

	@Override
	protected void initialize() {
		super.initialize();
		helpButton.setHelpContent("mainmenu.visualisations.formulaTableVisualisation", null);
		currentTable.addListener((observable, from, to) -> {
			if(to != null) {
				fillTable(to);
			}
		});
		saveButton.disableProperty().bind(currentTable.isNull());

		lvChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			Machine machine = currentProject.getCurrentMachine();
			tvFormula.itemsProperty().unbind();
			if(machine == null || to == null) {
				return;
			}
			Map<String, ListProperty<DynamicCommandFormulaItem>> items = machine.getTableVisualizationItems();
			if(!items.containsKey(to.getCommand())) {
				machine.addTableVisualizationListProperty(to.getCommand());
			}
			tvFormula.itemsProperty().bind(items.get(to.getCommand()));
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
			currentTable.set(table);
			placeholderLabel.setVisible(false);
			taErrors.clear();
			errorsView.setVisible(false);
			tableView.setVisible(true);
			errors.clear();
		});
	}

	private void fillTable(TableData data) {
		// Update header when table is updated
		this.header = data.getHeader();
		tableView.getColumns().clear();
		// All table columns get the same size initially (but they can be resized by the user).
		// Make the table columns fill the current width of the table view exactly,
		// unless that would make the columns too narrow.
		final double columnWidth = Math.max(tableView.getWidth() / header.size(), MINIMUM_TABLE_COLUMN_WIDTH);
		for (int i = 0; i < header.size(); i++) {
			final int columnIndex = i;
			final TableColumn<ObservableList<String>, String> column = new TableColumn<>(header.get(i));
			column.setCellValueFactory(param -> {
				ObservableList<String> row = param.getValue();
				if (columnIndex >= row.size()) {
					return new ReadOnlyObjectWrapper<>("");
				}

				return new ReadOnlyObjectWrapper<>(row.get(columnIndex));
			});
			tableView.getColumns().add(column);
			column.setPrefWidth(columnWidth);
			column.getStyleClass().add("expression-table-view-column");
			column.getStyleClass().add("alignment");
		}
		tableView.setItems(buildData(data.getRows()));

		// Do not provide header to row factory as this would lead to the bug that the row factory always use the oldest headers
		// Instead use header as a variable in ExpressionTableView that can be accessed by ValueItemRow
		tableView.setRowFactory(table -> new ValueItemRow());
	}

	private void handleSource(List<String> header, List<String> item, List<MenuItem> contextMenuItems) {
		MenuItem showSourceItem = new MenuItem(i18n.translate("dynamic.tableview.showSource"));
		int indexOfSource = header.indexOf(SOURCE_COLUMN_NAME);
		if(item != null) {
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
				path = currentTrace.getModel().getModelFile().toPath();
			} else if(source.startsWith("Line: ")) {
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
				injector.getInstance(BEditorView.class).jumpToSource(path, line, column);
			});
		}

		contextMenuItems.add(showSourceItem);
	}

	private void handleStateID(List<String> header, List<String> item, List<MenuItem> contextMenuItems) {
		MenuItem jumpToStateItem = new MenuItem(i18n.translate("dynamic.tableview.jumpToState"));
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
		if (currentTable == null || currentTable.get() == null) {
			return;
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(fileChooserManager.getCsvFilter());
		fileChooser.setTitle(i18n.translate("common.fileChooser.saveAsCSV.title"));
		Path path = fileChooserManager.showSaveFileChooser(fileChooser, null, this);
		if (path == null) {
			return;
		}

		if (!path.endsWith(".csv")) {
			path = path.resolveSibling(path.getFileName() + ".csv");
		}

		try (CSVWriter csvWriter = new CSVWriter(Files.newBufferedWriter(path))) {
			csvWriter.header(currentTable.get().getHeader());
			for (List<String> row : currentTable.get().getRows()) {
				csvWriter.record(row);
			}
		} catch (IOException e) {
			LOGGER.error("Saving as CSV failed", e);
			final Alert alert = injector.getInstance(StageManager.class).makeExceptionAlert(e, "common.alerts.couldNotSaveFile.content", path);
			alert.initOwner(this);
			alert.showAndWait();
		}
	}

	@Override
	protected void clearContent() {
		currentTable.set(null);
		tableView.getItems().clear();
		tableView.getColumns().clear();
		tableView.setVisible(false);
		placeholderLabel.setVisible(true);
	}

	@Override
	protected void editFormula(TableRow<DynamicCommandFormulaItem> row){
		final EditFormulaDialog dialog = injector.getInstance(EditFormulaDialog.class);
		dialog.initOwner(this);
		Optional<DynamicCommandFormulaItem> item = row == null ? dialog.addAndShow(currentProject, lastItem.getCommand()) : dialog.editAndShow(currentProject, row, errors);
		Machine machine = currentProject.getCurrentMachine();
		if(item != null && item.isPresent()) {
			if (row == null){
				machine.addTableVisualizationItem(lastItem.getCommand(), item.get());
				this.tvFormula.edit(this.tvFormula.getItems().size() - 1, formulaColumn);
			} else {
				ListProperty<DynamicCommandFormulaItem> tableVisualisationItem = machine.getTableVisualizationItems().get(lastItem.getCommand());
				tableVisualisationItem.set(tableVisualisationItem.indexOf(item.get()), item.get());
				machine.setChanged(true);
			}
			tvFormula.refresh();
		}
	}

	@Override
	protected void removeFormula() {
		if(this.tvFormula.getSelectionModel().getSelectedIndex() < 0) {
			return;
		}
		DynamicCommandFormulaItem formulaItem = this.tvFormula.getItems().get(this.tvFormula.getSelectionModel().getSelectedIndex());
		Machine machine = currentProject.getCurrentMachine();
		machine.removeTableVisualizationItem(lastItem.getCommand(), formulaItem);
	}
}
