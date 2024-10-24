package de.prob2.ui.dynamic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.exceptions.BCompoundException;
import de.prob.animator.CommandInterruptedException;
import de.prob.animator.domainobjects.DotVisualizationCommand;
import de.prob.animator.domainobjects.DynamicCommandItem;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.PlantUmlVisualizationCommand;
import de.prob.animator.domainobjects.TableVisualizationCommand;
import de.prob.exception.ProBError;
import de.prob.statespace.Trace;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.ExtendedCodeArea;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.internal.executor.BackgroundUpdater;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.CheckingStatusCell;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class DynamicVisualizationStage extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(DynamicVisualizationStage.class);

	// commands
	@FXML
	private TreeView<DynamicTreeItem> tvCommandItems;
	@FXML
	private TreeItem<DynamicTreeItem> tvCommandItemsRoot;
	@FXML
	private Label lbDescription;

	// formulas
	@FXML
	private TableView<VisualizationFormulaTask> tvFormula;
	@FXML
	private TableColumn<VisualizationFormulaTask, CheckingStatus> statusColumn;
	@FXML
	private TableColumn<VisualizationFormulaTask, String> idColumn;
	@FXML
	private TableColumn<VisualizationFormulaTask, String> formulaColumn;
	@FXML
	private VBox enterFormulaBox;
	@FXML
	private ExtendedCodeArea taFormula;

	// visualizations
	@FXML
	private Label placeholderLabel;
	@FXML
	private Parent errorsView;
	@FXML
	private TextArea taErrors;

	// toolbar
	@FXML
	private Button editPreferencesButton;
	@FXML
	private Button cancelButton;
	@FXML
	private HelpButton helpButton;

	// sub-views
	@FXML
	private DynamicTableView tableView;
	@FXML
	private DynamicGraphView graphView;

	private final I18n i18n;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final Provider<EditDynamicFormulaStage> editFormulaStageProvider;
	private final Provider<DynamicPreferencesStage> preferencesStageProvider;
	private final AtomicBoolean needsUpdateAfterBusy;
	private final BackgroundUpdater updater;
	private final ObservableList<ErrorItem> errors = FXCollections.observableArrayList();

	private DynamicTreeItem lastSelected;
	private boolean ignoreCommandItemUpdates;
	private boolean ignoreFormulaUpdates;

	@Inject
	public DynamicVisualizationStage(StageManager stageManager, I18n i18n, CurrentProject currentProject, CurrentTrace currentTrace, Provider<EditDynamicFormulaStage> editFormulaStageProvider, Provider<DynamicPreferencesStage> preferencesStageProvider, StopActions stopActions) {
		this.i18n = i18n;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.editFormulaStageProvider = editFormulaStageProvider;
		this.preferencesStageProvider = preferencesStageProvider;
		this.updater = new BackgroundUpdater("Dynamic Visualization Updater");
		stopActions.add(this.updater::shutdownNow);
		this.needsUpdateAfterBusy = new AtomicBoolean();
		stageManager.loadFXML(this, "dynamic_visualization_stage.fxml");
	}

	@FXML
	private void initialize() {
		this.refresh();

		this.helpButton.setHelpContent("mainmenu.visualisations.graphVisualisation", null);

		this.showingProperty().addListener((observable, from, to) -> {
			this.interrupt();
			if (to) {
				this.refresh();
			}
		});

		this.tvCommandItems.getSelectionModel().selectedItemProperty().addListener((observable, fromTreeItem, toTreeItem) -> {
			if (!this.ignoreCommandItemUpdates) {
				this.refreshSelectedTreeItem(toTreeItem != null ? toTreeItem.getValue() : null);
			}
		});
		this.tvCommandItems.disableProperty().bind(this.updater.runningProperty().or(this.currentTrace.stateSpaceProperty().isNull()));

		this.currentTrace.addListener((observable, from, to) -> this.refreshLater());
		this.currentTrace.addStatesCalculatedListener(newOps -> this.refreshLater());
		this.currentTrace.animatorBusyProperty().addListener((o, from, to) -> {
			if (!to && this.needsUpdateAfterBusy.compareAndSet(true, false)) {
				Platform.runLater(this::refresh);
			}
		});
		this.currentProject.currentMachineProperty().addListener((o, from, to) -> this.reload());

		this.updater.runningProperty().addListener(o -> this.updatePlaceholderLabel());

		this.taFormula.getStyleClass().add("visualization-formula");
		this.taFormula.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER)) {
				if (!e.isShiftDown()) {
					this.evaluateFormulaDirect();
					e.consume();
				} else {
					this.taFormula.insertText(this.taFormula.getCaretPosition(), "\n");
				}
			}
		});

		this.statusColumn.setCellFactory(col -> new CheckingStatusCell<>());
		this.statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		this.idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
		this.formulaColumn.setCellValueFactory(new PropertyValueFactory<>("formula"));

		this.tvFormula.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if (!this.ignoreFormulaUpdates) {
				this.evaluateFormula(to);
			}
		});

		this.tvCommandItems.setCellFactory(tv -> new DynamicCommandItemCell());
		this.cancelButton.disableProperty().bind(this.updater.runningProperty().not());
		this.editPreferencesButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			DynamicCommandItem item = this.getSelectedCommandItem();
			return item == null || item.getRelevantPreferences().isEmpty();
		}, this.tvCommandItems.getSelectionModel().selectedItemProperty()));

		this.tvFormula.setRowFactory(param -> {
			TableRow<VisualizationFormulaTask> row = new TableRow<>();

			row.setOnMouseClicked(e -> {
				if (e.getClickCount() == 2) {
					this.evaluateFormula(row.getItem());
				}
			});

			MenuItem editFormula = new MenuItem(i18n.translate("common.editFormula"));
			editFormula.setOnAction(event -> this.editFormulaWithDialog(row.getItem()));

			MenuItem evaluateItem = new MenuItem(i18n.translate("dynamic.evaluateFormula"));
			evaluateItem.setOnAction(event -> this.evaluateFormula(row.getItem()));

			MenuItem dischargeItem = new MenuItem(i18n.translate("common.formula.discharge"));
			dischargeItem.setOnAction(event -> {
				VisualizationFormulaTask item = row.getItem();
				if (item == null) {
					return;
				}
				item.setStatus(CheckingStatus.SUCCESS);
			});

			MenuItem failItem = new MenuItem(this.i18n.translate("common.formula.fail"));
			failItem.setOnAction(event -> {
				VisualizationFormulaTask item = row.getItem();
				if (item == null) {
					return;
				}
				item.setStatus(CheckingStatus.FAIL);
			});

			MenuItem unknownItem = new MenuItem(this.i18n.translate("common.formula.unknown"));
			unknownItem.setOnAction(event -> {
				VisualizationFormulaTask item = row.getItem();
				if (item == null) {
					return;
				}
				item.setStatus(CheckingStatus.NOT_CHECKED);
			});

			Menu statusMenu = new Menu(this.i18n.translate("common.formula.setStatus"), null, dischargeItem, failItem, unknownItem);

			MenuItem removeItem = new MenuItem(i18n.translate("sharedviews.checking.contextMenu.remove"));
			removeItem.setOnAction(event -> {
				VisualizationFormulaTask item = row.getItem();
				if (item == null) {
					return;
				}
				this.currentProject.getCurrentMachine().removeValidationTask(item);
			});

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
							.then((ContextMenu) null)
							.otherwise(new ContextMenu(evaluateItem, editFormula, statusMenu, removeItem)));
			return row;
		});
	}

	private DynamicCommandItem getSelectedCommandItem() {
		TreeItem<DynamicTreeItem> selectedItem = this.tvCommandItems.getSelectionModel().getSelectedItem();
		if (selectedItem != null && selectedItem.getValue() instanceof CommandItem item) {
			return item.getItem();
		} else {
			return null;
		}
	}

	private void visualizeInternal(DynamicCommandItem item, List<IEvalElement> formulas) throws InterruptedException {
		Platform.runLater(this::clearContent);
		if (item instanceof TableVisualizationCommand vc) {
			this.tableView.visualize(vc, formulas);
		} else if (item instanceof DotVisualizationCommand vc) {
			this.graphView.visualize(vc, formulas);
		} else if (item instanceof PlantUmlVisualizationCommand vc) {
			this.graphView.visualize(vc, formulas);
		} else {
			throw new AssertionError("unknown dynamic visualization command class: " + item.getClass().getSimpleName());
		}
	}

	private List<DynamicCommandItem> getCommands() {
		Trace trace = this.currentTrace.get();
		Machine machine = this.currentProject.getCurrentMachine();
		if (trace == null || machine == null) {
			return List.of();
		}

		List<DynamicCommandItem> items = new ArrayList<>();
		items.addAll(TableVisualizationCommand.getAll(trace));
		items.addAll(DotVisualizationCommand.getAll(trace));
		items.addAll(PlantUmlVisualizationCommand.getAll(trace));
		return items;
	}

	private void clearContent() {
		this.taFormula.getErrors().clear();
		this.updatePlaceholderLabel();
		this.placeholderLabel.setVisible(true);
		this.errorsView.setVisible(false);
		this.taErrors.clear();
		this.errors.clear();

		this.tableView.clearContent();
		this.graphView.clearContent();
	}

	private void editFormulaWithDialog(VisualizationFormulaTask oldTask) {
		if (oldTask == null) {
			return;
		}

		DynamicCommandItem item = this.getSelectedCommandItem();
		if (item == null) {
			return;
		}

		Machine machine = currentProject.getCurrentMachine();
		EditDynamicFormulaStage stage = this.editFormulaStageProvider.get();
		stage.initOwner(this);
		stage.setInitialFormulaTask(oldTask, this.errors);
		stage.showAndWait();

		VisualizationFormulaTask newTask = stage.getResult();
		if (newTask != null) {
			if (this.currentProject.getCurrentMachine() == machine) {
				VisualizationFormulaTask added = this.currentProject.getCurrentMachine().replaceValidationTaskIfNotExist(oldTask, newTask);
				this.evaluateFormula(added);
			} else {
				LOGGER.warn("The machine has changed, discarding task changes");
			}
		}
	}

	@FXML
	private void addFormulaDirect() {
		DynamicCommandItem item = this.getSelectedCommandItem();
		if (item == null) {
			return;
		}

		VisualizationFormulaTask task = new VisualizationFormulaTask(null, item.getCommand(), this.taFormula.getText());
		VisualizationFormulaTask added = this.currentProject.getCurrentMachine().addValidationTaskIfNotExist(task);
		this.evaluateFormula(added);
	}

	@FXML
	private void reload() {
		this.interrupt();
		this.refresh();
	}

	@FXML
	private void evaluateFormulaDirect() {
		DynamicCommandItem item = this.getSelectedCommandItem();
		if (item != null) {
			this.tvFormula.getSelectionModel().clearSelection();
			this.visualize(item, this.taFormula.getText());
		}
	}

	private void evaluateFormula(VisualizationFormulaTask formula) {
		DynamicCommandItem item = this.getSelectedCommandItem();
		if (item != null && formula != null) {
			this.visualize(item, formula.getFormula());
		}
	}

	@FXML
	private void editPreferences() {
		DynamicPreferencesStage preferences = this.preferencesStageProvider.get();
		preferences.initOwner(this);
		preferences.initModality(Modality.WINDOW_MODAL);
		preferences.setToRefresh(this);
		DynamicCommandItem currentItem = this.getSelectedCommandItem();
		if (currentItem == null) {
			return;
		}

		preferences.setIncludedPreferenceNames(currentItem.getRelevantPreferences());
		preferences.setTitle(i18n.translate("dynamic.preferences.stage.title", currentItem.getName()));
		preferences.show();
	}

	@FXML
	private void cancel() {
		this.currentTrace.getStateSpace().sendInterrupt();
		this.interrupt();
	}

	private void updatePlaceholderLabel() {
		String text;
		if (this.updater.isRunning()) {
			text = this.i18n.translate("dynamic.placeholder.inProgress");
		} else if (this.currentTrace.get() == null) {
			text = this.i18n.translate("common.noModelLoaded");
		} else {
			DynamicCommandItem selectedItem = this.getSelectedCommandItem();
			if (selectedItem == null) {
				text = this.i18n.translate("dynamic.placeholder.selectVisualization");
			} else if (selectedItem.getArity() > 0) {
				text = this.i18n.translate("dynamic.enterFormula.placeholder");
			} else {
				// The placeholder label shouldn't be seen by the user in this case,
				// because the visualization content should be visible,
				// but clear the text anyway just in case.
				text = "";
			}
		}
		this.placeholderLabel.setText(text);
	}

	private void refreshLater() {
		if (this.currentTrace.isAnimatorBusy()) {
			this.needsUpdateAfterBusy.set(true);
		} else {
			Platform.runLater(this::refresh);
		}
	}

	public void refresh() {
		this.updatePlaceholderLabel();

		DynamicTreeItem currentlySelected;
		{
			TreeItem<DynamicTreeItem> currentlySelectedTreeItem = this.tvCommandItems.getSelectionModel().getSelectedItem();
			if (currentlySelectedTreeItem != null) {
				currentlySelected = currentlySelectedTreeItem.getValue();
			} else {
				currentlySelected = null;
			}
		}
		if (currentlySelected != null) {
			this.lastSelected = currentlySelected;
		} else if (this.lastSelected != null) {
			currentlySelected = this.lastSelected;
		}

		List<DynamicCommandItem> commandItems = this.getCommands();

		TreeItem<DynamicTreeItem> nextSelected = null;
		List<TreeItem<DynamicTreeItem>> result = new ArrayList<>();
		{
			List<TreeItem<DynamicTreeItem>> withoutCategory = new ArrayList<>();
			Map<String, TreeItem<DynamicTreeItem>> categoryRoots = new HashMap<>();
			for (var commandItem : commandItems) {
				String category = null;
				for (var term : commandItem.getAdditionalInfo()) {
					if (term.hasFunctor("group", 1)) {
						category = term.getArgument(1).atomToString();
					}
				}

				var treeItem = new TreeItem<DynamicTreeItem>(new CommandItem(commandItem));
				if (category != null) {
					var root = categoryRoots.computeIfAbsent(category, k -> {
						var categoryRoot = new TreeItem<DynamicTreeItem>(new Category(k));
						categoryRoot.setExpanded(true);
						result.add(categoryRoot);
						return categoryRoot;
					});
					root.getChildren().add(treeItem);
					if (nextSelected == null && currentlySelected != null && root.getValue().equals(currentlySelected)) {
						nextSelected = treeItem;
					}
				} else {
					withoutCategory.add(treeItem);
				}

				if (nextSelected == null && currentlySelected != null && treeItem.getValue().equals(currentlySelected)) {
					nextSelected = treeItem;
				}
			}
			result.addAll(withoutCategory);
		}

		this.ignoreCommandItemUpdates = true;
		try {
			this.tvCommandItemsRoot.getChildren().setAll(result);
		} finally {
			this.ignoreCommandItemUpdates = false;
		}

		this.tvCommandItems.getSelectionModel().select(nextSelected);
	}

	private void refreshSelectedTreeItem(DynamicTreeItem toItem) {
		DynamicCommandItem to = toItem instanceof CommandItem commandItem ? commandItem.getItem() : null;

		this.updatePlaceholderLabel();

		// clearSelection does not cause an update in the formula selection event handler
		// re-selection is done later
		VisualizationFormulaTask previouslySelectedFormula = this.tvFormula.getSelectionModel().getSelectedItem();
		this.tvFormula.getSelectionModel().clearSelection();
		this.tvFormula.itemsProperty().unbind();
		this.tvFormula.visibleProperty().unbind();

		if (to == null || this.currentProject.getCurrentMachine() == null || this.currentTrace.get() == null || !this.isShowing()) {
			this.lbDescription.setText("");
			this.enterFormulaBox.setVisible(false);
			this.tvFormula.setVisible(false);
			this.tvFormula.setItems(FXCollections.observableArrayList());
			this.interrupt();
			return;
		}

		if (!to.isAvailable()) {
			this.lbDescription.setText(String.join("\n\n", to.getDescription(), to.getAvailable()));
		} else {
			this.lbDescription.setText(to.getDescription());
		}

		boolean needFormula = to.getArity() > 0;
		this.enterFormulaBox.setVisible(needFormula);

		var visualizationTasks = this.currentProject.getCurrentMachine().getVisualizationFormulaTasksByCommand(to.getCommand());
		this.tvFormula.visibleProperty().bind(Bindings.isNotEmpty(visualizationTasks));
		// this should not cause any formula selection updates
		this.tvFormula.setItems(visualizationTasks);

		String previousFormula = null;
		boolean restoreVisualization = false;
		if (to.isAvailable()) {
			if (needFormula) {
				if (previouslySelectedFormula != null && this.tvFormula.getItems().contains(previouslySelectedFormula)) {
					this.ignoreFormulaUpdates = true;
					try {
						// we want to reselect the previous item if there were any,
						// but we need to defer the visualization update, so do not react to the formula update event here
						this.tvFormula.getSelectionModel().select(previouslySelectedFormula);
					} finally {
						this.ignoreFormulaUpdates = false;
					}

					previousFormula = previouslySelectedFormula.getFormula();
				} else {
					previousFormula = this.taFormula.getText();
				}

				if (previousFormula != null && !previousFormula.isEmpty()) {
					// only show visualization when we have a valid formula
					restoreVisualization = true;
				}
			} else {
				// always show visualization when it does not require a formula
				restoreVisualization = true;
			}
		}

		// Update the visualization automatically if possible.
		// If the command selection changed and the new command requires a formula,
		// clear the visualization and wait for the user to input one.
		if (restoreVisualization) {
			this.visualize(to, previousFormula);
		} else {
			this.interrupt();
		}
	}

	private void interrupt() {
		this.updater.cancel(true);
		this.clearContent();
	}

	private void visualize(DynamicCommandItem item, String formula) {
		if (!item.isAvailable()) {
			return;
		}
		this.interrupt();

		this.updater.execute(() -> {
			try {
				Trace trace = this.currentTrace.get();
				if (trace == null || (item.getArity() > 0 && (formula == null || formula.isEmpty()))) {
					return;
				}
				List<IEvalElement> formulas;
				if (item.getArity() > 0) {
					formulas = Collections.singletonList(trace.getModel().parseFormula(formula, FormulaExpand.EXPAND));
				} else {
					formulas = Collections.emptyList();
				}
				this.visualizeInternal(item, formulas);
			} catch (CommandInterruptedException | InterruptedException e) {
				LOGGER.info("Visualization interrupted", e);
				Thread.currentThread().interrupt();
			} catch (ProBError e) {
				this.handleProBError(e, item);
			} catch (Exception e) {
				if (e.getCause() instanceof ProBError) {
					this.handleProBError((ProBError) e.getCause(), item);
				} else if (e.getCause() instanceof BCompoundException) {
					this.handleProBError(new ProBError((BCompoundException) e.getCause()), item);
				} else {
					LOGGER.error("Visualization failed for {}", item, e);
					Platform.runLater(() -> {
						this.clearContent();
						this.placeholderLabel.setVisible(false);
						this.taErrors.setText(e.getMessage());
						this.errorsView.setVisible(true);
					});
				}
			}
		});
	}

	private void handleProBError(ProBError e, DynamicCommandItem item) {
		LOGGER.error("Visualization failed with ProBError for {}", item, e);
		Platform.runLater(() -> {
			this.clearContent();
			this.placeholderLabel.setVisible(false);
			this.errors.setAll(e.getErrors());
			this.taFormula.getErrors().setAll(e.getErrors());
			this.taErrors.setText(e.getMessage());
			this.errorsView.setVisible(true);
		});
	}

	public void selectCommand(String command, String formula) {
		Objects.requireNonNull(command, "command");

		TreeItem<DynamicTreeItem> choiceTreeItem = findItemInTreeView(this.tvCommandItems, item -> item instanceof CommandItem ci && command.equals(ci.getItem().getCommand()));
		if (choiceTreeItem == null) {
			throw new IllegalArgumentException("Visualization command not found: " + command);
		}

		DynamicCommandItem choice = ((CommandItem) choiceTreeItem.getValue()).getItem();
		if (formula == null || formula.isEmpty()) {
			if (choice.getArity() != 0) {
				throw new IllegalArgumentException("Visualization command needs " + choice.getArity() + " argument(s): " + command);
			}
		} else {
			if (choice.getArity() == 0) {
				throw new IllegalArgumentException("Visualization command does not take an argument: " + command);
			}
		}

		this.tvCommandItems.getSelectionModel().select(choiceTreeItem);
		this.taFormula.replaceText(formula != null ? formula : "");
		this.tvFormula.getSelectionModel().clearSelection();
		this.visualize(choice, formula);
	}

	public void selectCommand(String command) {
		this.selectCommand(command, null);
	}

	public void visualizeFormulaAsTree(String formula) {
		this.selectCommand(DotVisualizationCommand.FORMULA_TREE_NAME, formula);
	}

	public void visualizeFormulaAsGraph(String formula) {
		this.selectCommand(DotVisualizationCommand.EXPRESSION_AS_GRAPH_NAME, formula);
	}

	public void visualizeProjection(String formula) {
		this.selectCommand(DotVisualizationCommand.STATE_SPACE_PROJECTION_NAME, formula);
	}

	public void visualizeExpression(String expression) {
		this.selectCommand(TableVisualizationCommand.EXPRESSION_AS_TABLE_NAME, expression);
	}

	private static sealed abstract class DynamicTreeItem {}

	private static final class CommandItem extends DynamicVisualizationStage.DynamicTreeItem {

		private final DynamicCommandItem item;

		private CommandItem(DynamicCommandItem item) {
			this.item = item;
		}

		public DynamicCommandItem getItem() {
			return this.item;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			} else if (!(o instanceof CommandItem that)) {
				return false;
			} else {
				return Objects.equals(this.getItem(), that.getItem());
			}
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(getItem());
		}

		@Override
		public String toString() {
			return this.item.getName();
		}
	}

	private static final class Category extends DynamicVisualizationStage.DynamicTreeItem {

		private final String category;

		private Category(String category) {
			this.category = category;
		}

		public String getCategory() {
			return this.category;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			} else if (!(o instanceof Category that)) {
				return false;
			} else {
				return Objects.equals(this.getCategory(), that.getCategory());
			}
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.category);
		}

		@Override
		public String toString() {
			return this.category;
		}
	}

	private static final class DynamicCommandItemCell extends TreeCell<DynamicTreeItem> {

		private DynamicCommandItemCell() {
			this.getStyleClass().add("dynamic-command-cell");
		}

		@Override
		protected void updateItem(DynamicTreeItem item, boolean empty) {
			super.updateItem(item, empty);
			this.getStyleClass().removeAll("disabled", "category");
			if (empty || item == null) {
				this.setText(null);
				this.setGraphic(null);
			} else {
				this.setText(item.toString());
				this.setGraphic(null);
				if (item instanceof Category) {
					this.getStyleClass().add("category");
				} else if (item instanceof CommandItem ci && !ci.getItem().isAvailable()) {
					this.getStyleClass().add("disabled");
				}
			}
		}
	}

	private static <T> TreeItem<T> findItemInTreeView(TreeView<T> treeView, Predicate<T> predicate) {
		Objects.requireNonNull(treeView, "treeView");
		Objects.requireNonNull(predicate, "predicate");
		if (treeView.getRoot() == null) {
			return null;
		}

		Deque<TreeItem<T>> stack = new ArrayDeque<>();
		stack.add(treeView.getRoot());
		while (!stack.isEmpty()) {
			TreeItem<T> item = stack.removeLast();
			if (predicate.test(item.getValue())) {
				return item;
			}

			for (TreeItem<T> child : item.getChildren()) {
				stack.addLast(child);
			}
		}

		return null;
	}
}
