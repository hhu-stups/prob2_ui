package de.prob2.ui.dynamic.table;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.GetAllTableCommands;
import de.prob.animator.command.GetTableForVisualizationCommand;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.TableData;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.dynamic.DynamicCommandStage;
import de.prob2.ui.dynamic.DynamicPreferencesStage;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ExpressionTableView extends DynamicCommandStage {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionTableView.class);
	
	private static final Pattern NEEDS_CSV_QUOTE_PATTERN = Pattern.compile("[,\"\n\r]");
	
	@FXML
	private Button saveButton;
	
	@FXML
	private HelpButton helpButton;
	
	private final FileChooserManager fileChooserManager;
	
	private ObjectProperty<TableData> currentTable;
	
	
	@Inject
	public ExpressionTableView(final StageManager stageManager, final DynamicPreferencesStage preferences, final CurrentTrace currentTrace, 
			final CurrentProject currentProject, final ResourceBundle bundle, final FileChooserManager fileChooserManager) {
		super(stageManager, preferences, currentTrace, currentProject, bundle);
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
					Platform.runLater(() -> {
						reset();
						statusBar.setText("");
					});
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
					statusBar.setText("");
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
	}
	
	@FXML
	private void editPreferences() {
		DynamicCommandItem currentItem = lvChoice.getSelectionModel().getSelectedItem();
		preferences.setTitle(String.format(bundle.getString("dynamic.preferences.stage.title"), currentItem.getName()));
		preferences.show();
	}
	
}
