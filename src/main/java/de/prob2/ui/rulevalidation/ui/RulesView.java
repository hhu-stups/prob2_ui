package de.prob2.ui.rulevalidation.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.be4.classicalb.core.parser.rules.AbstractOperation;
import de.be4.classicalb.core.parser.rules.ComputationOperation;
import de.be4.classicalb.core.parser.rules.RuleOperation;
import de.prob.model.brules.RuleResult;
import de.prob.model.brules.RuleResults;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.rulevalidation.RulesController;
import de.prob2.ui.rulevalidation.RulesDataModel;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static de.prob.model.brules.RuleStatus.*;


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
	private Button validationReportButton;

	@FXML
	private TextField filterTextField;

	@FXML
	private Label rulesLabel;

	@FXML
	public VBox progressBox;

	@FXML
	public Label progressLabel;
	@FXML
	public Label progressOperation;
	@FXML
	public ProgressBar progressBar;

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
	private final StageManager stageManager;
	private final RulesController controller;
	private final I18n i18n;
	private final FileChooserManager fileChooserManager;
	private final CurrentTrace currentTrace;

	@Inject
	public RulesView(final StageManager stageManager, final RulesController controller, final I18n i18n, final FileChooserManager fileChooserManager,
	                 final CurrentTrace currentTrace) {
		this.controller = controller;
		this.dataModel = controller.getModel();
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.currentTrace = currentTrace;
		this.stageManager = stageManager;
		this.stageManager.loadFXML(this, "rulesView.fxml");
		this.controller.setView(this);
	}

	@FXML
	public void initialize() {

		tvNameColumn.setCellFactory(column -> new NameCell());
		tvNameColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getValue()));

		tvValueColumn.setCellFactory(column -> new ValueCell());
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

		tvExecuteColumn.setCellFactory(column -> new ExecutionCell(controller, i18n));
		tvExecuteColumn.setCellValueFactory(param -> {
			Object item = param.getValue().getValue();
			if (item instanceof RuleOperation) {
				return dataModel.getRuleValue(((RuleOperation) item).getName());
			} else if (item instanceof ComputationOperation) {
				return dataModel.getComputationValue(((ComputationOperation) item).getName());
			}
			return null;
		});

		filterTextField.setOnKeyTyped(e -> this.handleFilterButton());
		executeAllButton.setDisable(true);
		currentTrace.addListener((o, oldTrace, newTrace) ->
			validationReportButton.setDisable(newTrace == null || newTrace.getCurrentState() == null || !newTrace.getCurrentState().isInitialised()));
	}

	@FXML
	public void handleFilterButton(){

		LOGGER.debug("Filter Operations");

		tvRootItem.getChildren().clear();
		tvRulesItem.getChildren().clear();
		tvComputationsItem.getChildren().clear();

		String filterText = filterTextField.getText();
		List<TreeItem<Object>> rulesToShow;
		List<TreeItem<Object>> computationsToShow;
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
		treeTableView.refresh();
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
		executeAllButton.setDisable(true);
		progressBox.setVisible(true);
		controller.executeAllOperations();
	}

	@FXML
	public void saveValidationReport() throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("rulevalidation.view.save.title"));
		fileChooser.setInitialFileName("ValidationReport.txt");
		fileChooser.getExtensionFilters().add(fileChooserManager.getPlainTextFilter());
		// TODO: Kind not correct
		Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.HISTORY_CHART, stageManager.getCurrent());
		if (path != null) {
			try (BufferedWriter writer = Files.newBufferedWriter(path)) {
				RuleResults ruleResults = new RuleResults(new HashSet<>(dataModel.getRuleMap().values()), currentTrace.getCurrentState(), -1);
				RuleResults.ResultSummary resultSummary = ruleResults.getSummary();

				writer.write("VALIDATION REPORT (" + currentTrace.getModel().getModelFile().getName() + ")\n-----------------------------");
				writer.write("\nTotal number of rules: " + resultSummary.numberOfRules);
				writer.write("\nNumber of checked rules: " + (resultSummary.numberOfRules - resultSummary.numberOfRulesNotChecked));
				writer.write("\nNumber of successful rules: " + resultSummary.numberOfRulesSucceeded);
				writer.write("\nNumber of failed rules: " + resultSummary.numberOfRulesFailed);
				writer.write("\nNumber of disabled rules: " + resultSummary.numberOfRulesDisabled);
				writer.write("\n-----------------------------");

				for (String ruleStr : resultSummary.status.keySet()) {
					writer.write("\n" + ruleStr + ":");

					switch (resultSummary.status.get(ruleStr)) {
						case FAIL:
							writer.write(" FAILED");
							if (resultSummary.ruleresults.containsKey(ruleStr)) {
								List<RuleResult.CounterExample> counterExamples = resultSummary.ruleresults.get(ruleStr).getCounterExamples();
								for (RuleResult.CounterExample counterExample : counterExamples) {
									writer.write("\n    - " + counterExample.getErrorType() + ", " + counterExample.getMessage());
								}
							}
							break;
						case SUCCESS:
							writer.write(" successful");
							break;
						case NOT_CHECKED:
							writer.write(" not checked");
							break;
						case DISABLED:
							writer.write(" disabled");
							break;
						default:
							writer.write(" unknown");
					}
				}
				writer.close();
			}
		}
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

		rulesLabel.setText(String.valueOf(dataModel.getRuleMap().size()));
		disabledLabel.textProperty().bind(dataModel.disabledRulesProperty());
		failLabel.textProperty().bind(dataModel.failedRulesProperty());
		notCheckedLabel.textProperty().bind(dataModel.notCheckedRulesProperty());
		successLabel.textProperty().bind(dataModel.successRulesProperty());

		executeAllButton.setDisable(false);
		treeTableView.refresh();
	}
}
