package de.prob2.ui.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.prob.animator.command.GetAllTableCommands;
import de.prob.animator.command.GetTableForVisualizationCommand;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.statespace.State;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class ExpressionTableView extends Stage {
	
	private final class ExpressionTableCommandCell extends ListCell<DynamicCommandItem> {

		public ExpressionTableCommandCell() {
			super();
			getStyleClass().add("expression-table-command-cell");
		}

		@Override
		protected void updateItem(DynamicCommandItem item, boolean empty) {
			super.updateItem(item, empty);
			this.getStyleClass().removeAll(Arrays.asList("tablecommandenabled", "tablecommanddisabled"));
			if (item != null && !empty) {
				setText(item.getName());
				if (item.isAvailable()) {
					getStyleClass().add("tablecommandenabled");
				} else {
					getStyleClass().add("tablecommanddisabled");
				}
			}
		}
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionTableView.class);
	
	@FXML
	private ListView<DynamicCommandItem> lvChoice;

	@FXML
	private TextArea taFormula;

	@FXML
	private VBox enterFormulaBox;

	@FXML
	private Label lbDescription;

	@FXML
	private Label lbAvailable;

	@FXML
	private CheckBox cbContinuous;

	@FXML
	private ScrollPane pane;
	
	@FXML
	private GridPane gpVisualisation;
	
	private CurrentTrace currentTrace;
	
	private final ResourceBundle bundle;
	
	
	
	@Inject
	public ExpressionTableView(final StageManager stageManager, final CurrentTrace currentTrace, final ResourceBundle bundle) {
		this.currentTrace = currentTrace;
		this.bundle = bundle;
		stageManager.loadFXML(this, "table_view.fxml");
	}
	
	@FXML
	private void initialize() {
		fillCommands();
		lvChoice.getSelectionModel().selectFirst();
		lvChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if (to == null) {
				return;
			}
			if (!to.isAvailable()) {
				lbAvailable.setText(String.join("\n", bundle.getString("dotview.notavailable"), to.getAvailable()));
			} else {
				lbAvailable.setText("");
			}
			lbDescription.setText(to.getDescription());
		});
		
		taFormula.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				if (!e.isShiftDown()) {
					DynamicCommandItem item = lvChoice.getSelectionModel().getSelectedItem();
					if (item == null) {
						return;
					}
					visualize(item);
				} else {
					taFormula.insertText(taFormula.getCaretPosition(), "\n");
				}
			}
		});
		
		lvChoice.setCellFactory(item -> new ExpressionTableCommandCell());
	}
	
	private void fillCommands() {
		try {
			lvChoice.getItems().clear();
			State id = currentTrace.getCurrentState();
			GetAllTableCommands cmd = new GetAllTableCommands(id);
			currentTrace.getStateSpace().execute(cmd);
			for (DynamicCommandItem item : cmd.getCommands()) {
				lvChoice.getItems().add(item);
			}
		} catch (Exception e) {
			LOGGER.error("Extract all expression table commands failed", e);
		}
	}
	
	private void visualize(DynamicCommandItem item) {
		gpVisualisation.getChildren().clear();
		List<IEvalElement> formulas = Collections.synchronizedList(new ArrayList<>());
		formulas.add(new ClassicalB(taFormula.getText()));
		State id = currentTrace.getCurrentState();
		GetTableForVisualizationCommand cmd = new GetTableForVisualizationCommand(id, item, formulas);
		currentTrace.getStateSpace().execute(cmd);
		fillTable(cmd.getTable());
	}
	
	private void fillTable(TableData data) {
		List<String> header = data.getHeader();
		for(int i = 0; i < header.size(); i++) {
			Text text = new Text(header.get(i));
			text.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
			gpVisualisation.add(text, i, 0);
		}
		
		List<List<String>> rows = data.getRows();
		for(int i = 0; i < rows.size(); i++) {
			for(int j = 0; j < rows.get(i).size(); j++) {
				gpVisualisation.add(new Label(rows.get(i).get(j)), j, i+1);
			}
		}
	}
	
	
	
}
