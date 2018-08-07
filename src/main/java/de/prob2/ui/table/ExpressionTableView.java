package de.prob2.ui.table;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.common.io.Files;

import com.google.inject.Inject;
import com.google.inject.Injector;

import de.prob.animator.command.GetAllTableCommands;
import de.prob.animator.command.GetTableForVisualizationCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.TableData;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DynamicCommandStage;
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
import javafx.stage.Stage;

import org.apache.commons.lang.StringEscapeUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExpressionTableView extends DynamicCommandStage {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionTableView.class);
	
	@FXML
	private Button saveButton;
	
	@FXML
	private HelpButton helpButton;
	
	private ObjectProperty<TableData> currentTable;
	
	
	@Inject
	public ExpressionTableView(final StageManager stageManager, final CurrentTrace currentTrace, final CurrentProject currentProject,
			final ResourceBundle bundle, final Injector injector) {
		super(stageManager, currentTrace, currentProject, bundle, injector);
		this.currentTable = new SimpleObjectProperty<>(this, "currentTable", null);
		stageManager.loadFXML(this, "table_view.fxml");
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		helpButton.setHelpContent(this.getClass());
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
				if(item.getArity() > 0) {
					formulas.add(new ClassicalB(taFormula.getText(), FormulaExpand.EXPAND));
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
					stageManager.makeExceptionAlert(bundle.getString("table.expressionTableView.alerts.visualisationNotPossible.message"), e).show();
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
		fileChooser.setTitle(bundle.getString("table.expressionTableView.fileChooser.saveAsCSV.title"));
		File file = fileChooser.showSaveDialog(new Stage());
		if(file == null || currentTable == null) {
			return;
		}
		try {
			Files.write(toCSV(currentTable.get()).getBytes(), file);
		} catch (IOException e) {
			LOGGER.error("Saving as CSV failed", e);
		}
	}
	
	private String toCSV(TableData data) {
		String csv = String.join(",", data.getHeader()) + "\n";
		csv += data.getRows()
				.stream()
				.map(column -> String.join(",", column
						.stream()
						.map(StringEscapeUtils::escapeCsv)
						.collect(Collectors.toList())))
				.collect(Collectors.joining("\n"));
		
		return csv;
	}
	
	@Override
	protected void reset() {
		currentTable.set(null);
		clearTable();
		statusBar.setText("");
	}
	
}
