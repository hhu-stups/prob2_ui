package de.prob2.ui.rulevalidation.ui;


import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.be4.classicalb.core.parser.rules.AbstractOperation;
import de.be4.classicalb.core.parser.rules.ComputationOperation;
import de.be4.classicalb.core.parser.rules.RuleOperation;
import de.prob.model.brules.RuleResult;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.rulevalidation.RulesController;
import de.prob2.ui.rulevalidation.RulesDataModel;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Description of class
 *
 * @author Christoph Heinzen
 * @version 0.1.0
 * @since 11.12.17
 */
@FXMLInjected
@Singleton
public class RulesView extends AnchorPane{

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesView.class);

	@FXML
	private Button filterButton;

	@FXML
	private Button executeAllButton;

	@FXML
	private TextField filterTextField;

	@FXML
	private Label rulesLabel;

	@FXML
	private Label notCheckedLabel;

	@FXML
	private Label successLabel;

	@FXML
	private Label failLabel;

	@FXML
	private Label disabledLabel;

	@FXML
	private TreeTableView<Object> treeTableView;

	@FXML
	private TreeTableColumn<Object, Object> tvNameColumn;
	@FXML
	private TreeTableColumn<Object, Object> tvValueColumn;
	@FXML
	private TreeTableColumn<Object, Object> tvExecuteColumn;

	@FXML
	private TreeItem<Object> tvRootItem;
	private TreeItem<Object> tvRulesItem;
	private TreeItem<Object> tvComputationsItem;

	private List<TreeItem<Object>> ruleItems;
	private List<TreeItem<Object>> computationItems;

	private final RulesDataModel dataModel;
	private final RulesController controller;

	@Inject
	public RulesView(final StageManager stageManager, final RulesController controller) {
		this.controller = controller;
		this.dataModel = controller.getModel();
		stageManager.loadFXML(this, "rulesView.fxml");
		this.controller.setView(this);
	}

	@FXML
	public void initialize() {

		tvNameColumn.setCellFactory(column -> new NameCell());
		tvNameColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getValue()));

		tvValueColumn.setCellFactory(column -> new ValueCell(dataModel));
		tvValueColumn.setCellValueFactory(param -> {
			Object item = param.getValue().getValue();
			if (item instanceof RuleOperation) {
				return dataModel.getRuleValue(((RuleOperation) item).getName());
			} else if (item instanceof ComputationOperation) {
				return dataModel.getComputationValue(((ComputationOperation) item).getName());
			} else if (item instanceof RuleResult.CounterExample) {
				return new ReadOnlyObjectWrapper<>(item);
			} else if (item instanceof String) {
				if (dataModel.getRuleValueMap().containsKey(item)) {
					return dataModel.getRuleValue((String)item);
				} else if (dataModel.getComputationValueMap().containsKey(item)) {
					return dataModel.getComputationValue((String) item);
				}
			}
			return null;
		} );

		tvExecuteColumn.setCellFactory(column -> new ExecutionCell(controller));
		tvExecuteColumn.setCellValueFactory(param -> {
			Object item = param.getValue().getValue();
			if (item instanceof RuleOperation) {
				return dataModel.getRuleValue(((RuleOperation) item).getName());
			} else if (item instanceof ComputationOperation) {
				return dataModel.getComputationValue(((ComputationOperation) item).getName());
			}
			return null;
		});

		//filterButton.setFont(new Font(fontsize.getFontSize()));
		
		executeAllButton.setDisable(true);
	}

	@FXML
	public void handleFilterButton(){

		LOGGER.debug("Filter Operations");

		tvRootItem.getChildren().clear();
		tvRulesItem.getChildren().clear();
		tvComputationsItem.getChildren().clear();

		String filterText = filterTextField.getText();
		List<TreeItem<Object>> rulesToShow = null;
		List<TreeItem<Object>> computationsToShow = null;
		if (filterText != null && !filterText.isEmpty()) {
			//filter
			filterText = filterText.toLowerCase();
			rulesToShow = filterItems(filterText, ruleItems);
			computationsToShow = filterItems(filterText, computationItems);
		} else {
			//don't filter, show all
			rulesToShow = ruleItems;
			computationsToShow = computationItems;
		}
		if (!rulesToShow.isEmpty()) {
			tvRulesItem.getChildren().addAll(rulesToShow);
			tvRootItem.getChildren().add(tvRulesItem);
		}
		if (!computationsToShow.isEmpty()) {
			tvComputationsItem.getChildren().addAll(computationsToShow);
			tvRootItem.getChildren().add(tvComputationsItem);
		}
	}

	private List<TreeItem<Object>> filterItems(String filterText, List<TreeItem<Object>> allItems) {
		List<TreeItem<Object>> filtered = new ArrayList<>();
		for (TreeItem<Object> item : allItems) {
			String itemName = ((AbstractOperation) item.getValue()).getName().toLowerCase();
			if (itemName.contains(filterText)) {
				filtered.add(item);
			}
		}
		return filtered;
	}

	@FXML
	public void executeAll(){
		controller.executeAllOperations();
	}

	public void clear(){
		LOGGER.debug("Clear RulesView!");

		tvRootItem.getChildren().clear();
		filterTextField.setText("");
		rulesLabel.setText("-");

		executeAllButton.setDisable(true);
	}

	public void build() {

		LOGGER.debug("Build RulesView!");
		tvRootItem.getChildren().clear();
		tvRulesItem = new TreeItem<>("RULES");
		if (!dataModel.getRuleMap().isEmpty()) {
			for (Map.Entry<String, RuleOperation> entry : dataModel.getRuleMap().entrySet()) {
				LOGGER.debug("Add item for rule {}   {}.", entry.getKey(), entry.getValue());
				tvRulesItem.getChildren()
						.add(new OperationItem(entry.getValue(), dataModel.getRuleValue(entry.getKey()), dataModel));
			}
			tvRootItem.getChildren().add(tvRulesItem);

		}
		tvComputationsItem = new TreeItem<>("COMPUTATIONS");
		if (!dataModel.getComputationMap().isEmpty()) {
			for (Map.Entry<String, ComputationOperation> entry : dataModel.getComputationMap().entrySet()) {
				LOGGER.debug("Add item for computation {}.", entry.getKey());
				tvComputationsItem.getChildren()
						.add(new OperationItem(entry.getValue(), dataModel.getComputationValue(entry.getKey()), dataModel));
			}
			tvRootItem.getChildren().add(tvComputationsItem);
		}

		ruleItems = new ArrayList<>(tvRulesItem.getChildren());
		computationItems = new ArrayList<>(tvComputationsItem.getChildren());

		rulesLabel.setText(dataModel.getRuleMap().size() + "");
		disabledLabel.textProperty().bind(dataModel.disabledRulesProperty());
		failLabel.textProperty().bind(dataModel.failedRulesProperty());
		notCheckedLabel.textProperty().bind(dataModel.notCheckedRulesProperty());
		successLabel.textProperty().bind(dataModel.successRulesProperty());

		executeAllButton.setDisable(false);
	}
}
