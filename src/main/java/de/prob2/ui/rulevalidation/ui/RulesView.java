package de.prob2.ui.rulevalidation.ui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.rules.AbstractOperation;
import de.be4.classicalb.core.parser.rules.ComputationOperation;
import de.be4.classicalb.core.parser.rules.RuleOperation;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.exception.ProBError;
import de.prob.model.brules.RuleResult;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.dynamic.DynamicVisualizationStage;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.rulevalidation.RulesController;
import de.prob2.ui.rulevalidation.RulesDataModel;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christoph Heinzen
 * @since 11.12.17
 */
@FXMLInjected
@Singleton
public final class RulesView extends AnchorPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(RulesView.class);

	@FXML
	private AnchorPane topBar;
	@FXML
	private ToggleButton tagListButton;
	@FXML
	private Button filterButton;

	@FXML
	public Button executeAllButton;
	@FXML
	public Button visualizeGraphButton;
	@FXML
	private Button validationReportButton;

	@FXML
	private TextField filterTextField;

	@FXML
	private Label rulesLabel;

	@FXML
	private Pane tagSelectionContainer;
	@FXML
	private ScrollPane tagSelectionScrollPane;
	@FXML
	private VBox tagSelectionBox;

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
	private Map<String, List<TreeItem<Object>>> classificationItems;
	private List<TreeItem<Object>> computationItems;
	private Set<String> currentTags;

	private final RulesDataModel dataModel;
	private final StageManager stageManager;
	private final RulesController controller;
	private final I18n i18n;
	private final FileChooserManager fileChooserManager;
	private final CurrentTrace currentTrace;
	private final Injector injector;

	@Inject
	public RulesView(final StageManager stageManager, final RulesController controller, final I18n i18n, final FileChooserManager fileChooserManager,
	                 final CurrentTrace currentTrace, final Injector injector) {
		this.controller = controller;
		this.dataModel = controller.getModel();
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.stageManager = stageManager;
		this.stageManager.loadFXML(this, "rulesView.fxml");
		this.controller.setView(this);
	}

	@FXML
	public void initialize() {
		tagListButton.visibleProperty().bind(Bindings.createBooleanBinding(() ->
			!tagSelectionBox.getChildren().isEmpty(), tagSelectionBox.getChildren()));
		tagListButton.managedProperty().bind(tagListButton.visibleProperty());
		tagSelectionContainer.managedProperty().bind(tagSelectionContainer.visibleProperty());

		tagSelectionBox.getChildren().addListener((ListChangeListener<Node>) change -> {
			while (change.next()) {
				if (change.wasAdded()) {
					for (Node node : change.getAddedSubList()) {
						if (node instanceof CheckBox checkBox) {
							checkBox.selectedProperty().addListener((obs, o, n) -> {
								if (n) {
									currentTags.add(checkBox.getText());
								} else {
									currentTags.remove(checkBox.getText());
								}
								filterTagsAndSearch();
							});
						}
					}
				}
			}
		});
		topBar.setOnMousePressed(e -> tagSelectionContainer.setVisible(false));
		treeTableView.setOnMousePressed(e -> tagSelectionContainer.setVisible(false));

		treeTableView.setRowFactory(view -> initTableRow());

		tvNameColumn.setCellFactory(column -> new NameCell(i18n));
		tvNameColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getValue()));

		tvValueColumn.setCellFactory(column -> new ValueCell(i18n));
		tvValueColumn.setCellValueFactory(param -> {
			Object item = param.getValue().getValue();
			if (item instanceof RuleOperation ruleItem) {
				return dataModel.getRuleValue(ruleItem.getName());
			} else if (item instanceof ComputationOperation compItem) {
				return dataModel.getComputationValue(compItem.getName());
			} else if (item instanceof RuleResult.CounterExample) {
				return new ReadOnlyObjectWrapper<>(item);
			} else if (item instanceof RuleResult.SuccessMessage) {
				return new ReadOnlyObjectWrapper<>(item);
			} else if (item instanceof String stringItem) {
				if (dataModel.getRuleValueMap().containsKey(stringItem)) {
					return dataModel.getRuleValue(stringItem);
				} else if (dataModel.getComputationValueMap().containsKey(stringItem)) {
					return dataModel.getComputationValue(stringItem);
				}
			}
			return null;
		} );

		tvExecuteColumn.setCellFactory(column -> new ExecutionCell(controller, i18n));
		tvExecuteColumn.setCellValueFactory(param -> {
			Object item = param.getValue().getValue();
			if (item instanceof RuleOperation ruleOperation) {
				return dataModel.getRuleValue(ruleOperation.getName());
			} else if (item instanceof ComputationOperation computationOperation) {
				return dataModel.getComputationValue(computationOperation.getName());
			}
			return null;
		});

		filterTextField.setOnKeyTyped(e -> this.handleFilterButton());
		executeAllButton.setDisable(true);
		progressBox.setVisible(false);
		currentTrace.addListener((o, oldTrace, newTrace) ->
			validationReportButton.setDisable(newTrace == null || newTrace.getCurrentState() == null || !newTrace.getCurrentState().isInitialised()));
	}

	@FXML
	public void handleTagButton(){
		tagSelectionContainer.setVisible(!tagSelectionContainer.isVisible());
	}

	@FXML
	public void handleFilterButton(){
		filterTagsAndSearch();
	}

	private void filterTagsAndSearch() {
		String filterText = filterTextField.getText();
		LOGGER.debug("Filter Operations for tags " + currentTags + " and search '" + filterText + "'");
		clearTable();
		List<TreeItem<Object>> filteredRuleItems;
		List<TreeItem<Object>> filteredComputationItems;
		if (!currentTags.isEmpty()) {
			filteredRuleItems = filterCurrentTags(ruleItems);
			filteredComputationItems = filterCurrentTags(computationItems);
		} else {
			filteredRuleItems = new ArrayList<>(ruleItems);
			filteredComputationItems = new ArrayList<>(computationItems);
		}
		if (filterText != null && !filterText.isEmpty()) {
			filterText = filterText.toLowerCase();
			filteredRuleItems.retainAll(filterOperationNames(filterText, ruleItems));
			filteredComputationItems.retainAll(filterOperationNames(filterText, computationItems));
		}
		showFilteredItems(filteredRuleItems, filteredComputationItems);
	}

	private void clearTable() {
		tvRootItem.getChildren().clear();
		tvRulesItem.getChildren().clear();
		for (TreeItem<Object> item : ruleItems) {
			item.getChildren().clear();
			if (item.getValue() instanceof String classification) {
				classification = classification.substring(0, classification.lastIndexOf("(") - 1);
				item.getChildren().addAll(classificationItems.get(classification));
			}
		}
		tvComputationsItem.getChildren().clear();
	}

	private List<TreeItem<Object>> filterCurrentTags(List<TreeItem<Object>> allItems) {
		List<TreeItem<Object>> filtered = new ArrayList<>();
		for (TreeItem<Object> item : allItems) {
			if (item.getValue() instanceof AbstractOperation abstractOperation && currentTags.stream().anyMatch(abstractOperation.getTags()::contains)) {
				filtered.add(item);
			} else if (item.getValue() instanceof String) {
				// item is classificationItem
				List<TreeItem<Object>> filteredRules = filterCurrentTags(item.getChildren());
				item.getChildren().clear();
				if (!filteredRules.isEmpty()) {
					item.getChildren().addAll(filteredRules);
					filtered.add(item);
				}
			}
		}
		return filtered;
	}

	private List<TreeItem<Object>> filterOperationNames(String filterText, List<TreeItem<Object>> allItems) {
		List<TreeItem<Object>> filtered = new ArrayList<>();
		for (TreeItem<Object> item : allItems) {
			if (item.getValue() instanceof AbstractOperation abstractOperation) {
				// RULE name or RULEID contains filterText
				if (abstractOperation.getName().toLowerCase().contains(filterText) ||
						(abstractOperation instanceof RuleOperation ruleOperation && ruleOperation.getRuleIdString() != null
							&& ruleOperation.getRuleIdString().toLowerCase().contains(filterText))) {
					filtered.add(item);
				}
			} else if (item.getValue() instanceof String classification && classification.toLowerCase().contains(filterText)) {
				// item is classificationItem and classification identifier matches search
				filtered.add(item);
			} else if (item.getValue() instanceof String) {
				// item is classificationItem
				List<TreeItem<Object>> filteredRules = filterOperationNames(filterText, item.getChildren());
				item.getChildren().clear();
				if (!filteredRules.isEmpty()) {
					item.getChildren().addAll(filteredRules);
					filtered.add(item);
				}
			}
		}
		return filtered;
	}

	private void showFilteredItems(List<TreeItem<Object>> rulesToShow, List<TreeItem<Object>> computationsToShow) {
		if (!rulesToShow.isEmpty()) {
			tvRulesItem.getChildren().setAll(rulesToShow);
			tvRootItem.getChildren().add(tvRulesItem);
		}
		if (!computationsToShow.isEmpty()) {
			tvComputationsItem.getChildren().setAll(computationsToShow);
			tvRootItem.getChildren().add(tvComputationsItem);
		}
		treeTableView.sort();
		treeTableView.refresh();
	}

	@FXML
	public void executeAll(){
		executeAllButton.setDisable(true);
		progressBox.setVisible(true);
		controller.executeAllOperations();
	}

	@FXML
	public void visualizeCompleteDependencyGraph() {
		showGraphExpression(controller.getCompleteDependencyGraphExpression());
	}

	@FXML
	public void saveValidationReport() throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("rulevalidation.view.save.title"));
		fileChooser.setInitialFileName("ValidationReport.html");
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.html", "html"));
		Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, stageManager.getCurrent());
		if (path != null) {
			controller.saveValidationReport(path, injector.getInstance(Locale.class));
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
		filterTextField.clear();

		tagSelectionBox.getChildren().clear();
		Set<String> allTags = new HashSet<>();
		for (RuleOperation op : dataModel.getRuleMap().values()) {
			allTags.addAll(op.getTags());
		}
		for (ComputationOperation op : dataModel.getComputationMap().values()) {
			allTags.addAll(op.getTags());
		}
		currentTags = new HashSet<>();
		if (allTags.isEmpty()) {
			tagSelectionContainer.setVisible(false);
		} else {
			List<CheckBox> tagCheckBoxes = new ArrayList<>();
			for (String tag : allTags) {
				CheckBox checkBox = new CheckBox(tag);
				tagCheckBoxes.add(checkBox);
			}
			tagSelectionBox.getChildren().addAll(tagCheckBoxes);
		}

		tvRootItem.getChildren().clear();
		tvRulesItem = new TreeItem<>("RULES");
		classificationItems = new HashMap<>();
		if (!dataModel.getRuleMap().isEmpty()) {
			List<TreeItem<Object>> noClassificationItem = new ArrayList<>();
			for (Map.Entry<String, RuleOperation> entry : dataModel.getRuleMap().entrySet()) {
				LOGGER.debug("Add item for rule {}   {}.", entry.getKey(), entry.getValue());
				TreeItem<Object> operationItem = new OperationItem(i18n, entry.getValue(), dataModel.getRuleValue(entry.getKey()), dataModel);
				String classification = entry.getValue().getClassification();
				if (classification != null && classificationItems.containsKey(classification)) {
					classificationItems.get(classification).add(operationItem);
				} else if (classification != null) {
					classificationItems.put(classification, new ArrayList<>());
					classificationItems.get(classification).add(operationItem);
				} else {
					noClassificationItem.add(operationItem);
				}
			}
			for (String cl : classificationItems.keySet()) {
				TreeItem<Object> classificationItem = new TreeItem<>(cl + " (" + classificationItems.get(cl).size() + ")");
				classificationItem.getChildren().addAll(classificationItems.get(cl));
				tvRulesItem.getChildren().add(classificationItem);
			}
			tvRulesItem.getChildren().addAll(noClassificationItem);
			tvRootItem.getChildren().add(tvRulesItem);
		}

		tvComputationsItem = new TreeItem<>("COMPUTATIONS");
		if (!dataModel.getComputationMap().isEmpty()) {
			for (Map.Entry<String, ComputationOperation> entry : dataModel.getComputationMap().entrySet()) {
				LOGGER.debug("Add item for computation {}.", entry.getKey());
				tvComputationsItem.getChildren()
						.add(new OperationItem(i18n, entry.getValue(), dataModel.getComputationValue(entry.getKey()), dataModel));
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
		treeTableView.sort();
		treeTableView.refresh();
	}

	private TreeTableRow<Object> initTableRow() {
		final TreeTableRow<Object> row = new TreeTableRow<>();

		final MenuItem visualizeExpressionAsGraphItem = new MenuItem(
			i18n.translate("rulevalidation.table.contextMenu.dependencyGraph"));
		visualizeExpressionAsGraphItem.setOnAction(event -> {
			try {
				if (row.getItem() instanceof AbstractOperation abstractOperation) {
					showGraphExpression(controller.getPartialDependencyGraphExpression(Set.of(abstractOperation)));
				}
			} catch (EvaluationException | ProBError e) {
				LOGGER.error("Could not visualize formula", e);
				final Alert alert = stageManager.makeExceptionAlert(e, "rulevalidation.view.dependencyGraph.error");
				alert.initOwner(this.getScene().getWindow());
				alert.showAndWait();
			}
		});

		row.itemProperty().addListener((obs, oldVal, newVal) ->
			row.setContextMenu(newVal instanceof AbstractOperation ? new ContextMenu(visualizeExpressionAsGraphItem) : null));
		return row;
	}

	private void showGraphExpression(String expression) {
		DynamicVisualizationStage dotView = injector.getInstance(DynamicVisualizationStage.class);
		dotView.show();
		dotView.toFront();
		dotView.visualizeFormulaAsGraph(expression);
	}
}
