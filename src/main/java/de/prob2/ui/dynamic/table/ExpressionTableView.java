package de.prob2.ui.dynamic.table;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.animator.command.GetAllTableCommands;
import de.prob.animator.command.GetShortestTraceCommand;
import de.prob.animator.command.GetTableForVisualizationCommand;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.TableData;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob2.ui.beditor.BEditor;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.dynamic.DynamicCommandStage;
import de.prob2.ui.dynamic.DynamicPreferencesStage;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.menu.MainView;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class ExpressionTableView extends DynamicCommandStage {

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
	
	private static final Pattern NEEDS_CSV_QUOTE_PATTERN = Pattern.compile("[,\"\n\r]");

	private static final String SOURCE_COLUMN_NAME = "Source";

	private static final String STATE_ID_COLUMN_NAME = "State ID";

	private static final String VALUE_COLUMN_NAME = "VALUE";
	
	@FXML
	private Button saveButton;
	
	@FXML
	private HelpButton helpButton;

	private final Injector injector;
	
	private final FileChooserManager fileChooserManager;
	
	private ObjectProperty<TableData> currentTable;
	
	
	@Inject
	public ExpressionTableView(final Injector injector, final StageManager stageManager, final DynamicPreferencesStage preferences, final CurrentTrace currentTrace,
							   final CurrentProject currentProject, final ResourceBundle bundle, final FileChooserManager fileChooserManager) {
		super(stageManager, preferences, currentTrace, currentProject, bundle);
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
	}
	
	@Override
	protected void fillCommands() {
		super.fillCommands(new GetAllTableCommands(currentTrace.getCurrentState()));
	}
	
	public void visualizeExpression(String expression) {
		taFormula.setText(expression);
		lvChoice.getSelectionModel().selectFirst();
		visualize(lvChoice.getSelectionModel().getSelectedItem());
	}
	
	@Override
	protected void visualize(DynamicCommandItem item) {
		if(!item.isAvailable()) {
			return;
		}
		List<IEvalElement> formulas = Collections.synchronizedList(new ArrayList<>());
		interrupt();

		Thread thread = new Thread(() -> {
			Platform.runLater(() -> statusBar.setText(bundle.getString("statusbar.loadStatus.loading")));
			try {
				if(currentTrace.get() == null || (item.getArity() > 0 && taFormula.getText().isEmpty())) {
					Platform.runLater(this::reset);
					currentThread.set(null);
					return;
				}
				if(item.getArity() > 0) {
					formulas.add(currentTrace.getModel().parseFormula(taFormula.getText(), FormulaExpand.EXPAND));
				}
				State id = currentTrace.getCurrentState();
				GetTableForVisualizationCommand cmd = new GetTableForVisualizationCommand(id, item, formulas);
				currentTrace.getStateSpace().execute(cmd);
				Platform.runLater(() -> {
					reset();
					currentTable.set(cmd.getTable());
				});
				currentThread.set(null);
			} catch (ProBError | EvaluationException e) {
				LOGGER.error("Table visualization failed", e);
				currentThread.set(null);
				Platform.runLater(() -> {
					taErrors.setText(e.getMessage());
					reset();
				});
			}
		});
		currentThread.set(thread);
		thread.start();
	}
	
	private void fillTable(TableData data) {
		List<String> header = data.getHeader();
		TableView<ObservableList<String>> tableView = new TableView<>();
		tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		for (int i = 0; i < header.size(); i++) {
			final int j = i;
			final TableColumn<ObservableList<String>, String> column = new TableColumn<>(header.get(i));
			column.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().get(j)));
			tableView.getColumns().add(column);
		}
		tableView.setItems(buildData(data.getRows()));
		tableView.setRowFactory(table -> {
			final ValueItemRow row = new ValueItemRow(header);
			List<MenuItem> contextMenuItems = new ArrayList<>();
			row.itemProperty().addListener((observable, from, to) -> {

				if (header.contains(SOURCE_COLUMN_NAME)) {
					MenuItem showSourceItem = new MenuItem(bundle.getString("dynamic.tableview.showSource"));
					int indexOfSource = header.indexOf(SOURCE_COLUMN_NAME);
					if(to != null) {
						String source = to.get(indexOfSource);
						String[] sourceSplitted = source.replaceAll(" at line ", "").split(" \\- ");
						String[] start = sourceSplitted[0].split(":");
						try {
							int line = Integer.parseInt(start[0]) - 1;
							int column = Integer.parseInt(start[1]);
							showSourceItem.setOnAction(e -> {
								stageManager.getMainStage().toFront();
								injector.getInstance(MainView.class).switchTabPane("beditorTab");
								BEditor bEditor = injector.getInstance(BEditor.class);
								bEditor.requestFocus();
								bEditor.moveTo(line, column);
								bEditor.requestFollowCaret();
							});
						} catch (NumberFormatException e) {
							LOGGER.error("Source information cannot be extracted", e);
							showSourceItem.setDisable(true);
							statusBar.setText(bundle.getString("dynamic.tableview.jumpToState.notPossible"));
							statusBar.setLabelStyle("warning");
						}
					}

					contextMenuItems.add(showSourceItem);
				}

				if (header.contains(STATE_ID_COLUMN_NAME)) {
					MenuItem jumpToStateItem = new MenuItem(bundle.getString("dynamic.tableview.jumpToState"));
					int indexOfStateID = header.indexOf(STATE_ID_COLUMN_NAME);
					if(to != null) {
						String stateID = to.get(indexOfStateID);
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

				if (!contextMenuItems.isEmpty() && to != null) {
					ContextMenu contextMenu = new ContextMenu();
					contextMenu.getItems().addAll(contextMenuItems);
					row.setContextMenu(contextMenu);
				}
			});
			return row;
		});
		pane.setContent(tableView);
		taErrors.clear();
	}
	
	private void clearTable() {
		pane.setContent(new TableView<>());
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
	protected void reset() {
		currentTable.set(null);
		clearTable();
		statusBar.setText("");
		statusBar.removeLabelStyle("warning");
	}
	
	@FXML
	private void editPreferences() {
		DynamicCommandItem currentItem = lvChoice.getSelectionModel().getSelectedItem();
		preferences.setTitle(String.format(bundle.getString("dynamic.preferences.stage.title"), currentItem.getName()));
		preferences.show();
	}

	public void selectCommand(String command) {
		DynamicCommandItem commandItem = lvChoice.getItems().stream().filter(item -> item.getCommand().equals(command)).collect(Collectors.toList()).get(0);
		lvChoice.getSelectionModel().select(commandItem);
	}
	
}
