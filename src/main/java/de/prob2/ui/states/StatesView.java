package de.prob2.ui.states;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.GetMachineStructureCommand;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EvaluationErrorResult;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.prologast.ASTCategory;
import de.prob.animator.prologast.ASTFormula;
import de.prob.animator.prologast.PrologASTNode;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.dynamic.dotty.DotView;
import de.prob2.ui.dynamic.table.ExpressionTableView;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.persistence.TableUtils;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.statusbar.StatusBar;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public final class StatesView extends StackPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatesView.class);
	private static final TreeItem<StateItem<?>> LOADING_ITEM;

	static {
		LOADING_ITEM = new TreeItem<>(new StateItem<>("Loading...", false));
		LOADING_ITEM.getChildren().add(new TreeItem<>(new StateItem<>("Loading", false)));
	}

	@FXML
	private Button searchButton;
	@FXML
	private TextField filterState;
	@FXML
	private HelpButton helpButton;

	@FXML
	private TreeTableView<StateItem<?>> tv;
	@FXML
	private TreeTableColumn<StateItem<?>, StateItem<?>> tvName;
	@FXML
	private TreeTableColumn<StateItem<?>, StateItem<?>> tvValue;
	@FXML
	private TreeTableColumn<StateItem<?>, StateItem<?>> tvPreviousValue;
	@FXML
	private TreeItem<StateItem<?>> tvRootItem;

	private final Injector injector;
	private final CurrentTrace currentTrace;
	private final StatusBar statusBar;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final Config config;

	private List<PrologASTNode> rootNodes;
	private List<PrologASTNode> filteredRootNodes;
	private final Set<IEvalElement> subscribedFormulas;
	private final Map<IEvalElement, AbstractEvalResult> currentValues;
	private final Map<IEvalElement, AbstractEvalResult> previousValues;
	private final ExecutorService updater;
	private List<Double> columnWidthsToRestore;

	private String filter = "";

	@Inject
	private StatesView(final Injector injector, final CurrentTrace currentTrace, final StatusBar statusBar,
			final StageManager stageManager, final ResourceBundle bundle, final StopActions stopActions, final Config config) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.statusBar = statusBar;
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.config = config;

		this.rootNodes = null;
		this.filteredRootNodes = null;
		this.subscribedFormulas = new HashSet<>();
		this.currentValues = new HashMap<>();
		this.previousValues = new HashMap<>();
		this.updater = Executors.newSingleThreadExecutor(r -> new Thread(r, "StatesView Updater"));
		stopActions.add(this.updater::shutdownNow);

		stageManager.loadFXML(this, "states_view.fxml");
	}

	@FXML
	private void initialize() {
		helpButton.setHelpContent(this.getClass());
		
		tv.setRowFactory(view -> initTableRow());

		this.tvName.setCellFactory(col -> new NameCell());
		this.tvValue.setCellFactory(col -> new ValueCell(bundle, this.currentValues, true));
		this.tvPreviousValue.setCellFactory(col -> new ValueCell(bundle, this.previousValues, false));

		final Callback<TreeTableColumn.CellDataFeatures<StateItem<?>, StateItem<?>>, ObservableValue<StateItem<?>>> cellValueFactory = data -> Bindings
				.createObjectBinding(data.getValue()::getValue, this.currentTrace);
		this.tvName.setCellValueFactory(cellValueFactory);
		this.tvValue.setCellValueFactory(cellValueFactory);
		this.tvPreviousValue.setCellValueFactory(cellValueFactory);

		this.tv.getRoot().setValue(new StateItem<>("Machine (this root item should be invisible)", false));

		final ChangeListener<Trace> traceChangeListener = (observable, from, to) -> this.updater.execute(() -> {
			if (to == null) {
				this.rootNodes = null;
				this.filteredRootNodes = null;
				this.currentValues.clear();
				this.previousValues.clear();
				this.tv.getRoot().getChildren().clear();
			} else {
				this.updateRoot(from, to, false);
			}
		});
		traceChangeListener.changed(this.currentTrace, null, currentTrace.get());
		this.currentTrace.addListener(traceChangeListener);

		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.statesViewColumnsWidth != null) {
					// The table columns cannot be resized until the table view is shown on screen (before then, the resizing always fails).
					// So we can't restore the column widths yet - that is done later using the restoreColumnWidths() method, which is called by the UI startup code once the main stage is visible.
					columnWidthsToRestore = configData.statesViewColumnsWidth;
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.statesViewColumnsWidth = TableUtils.getAbsoluteColumnWidths(tv.getColumns());
			}
		});
	}

	public void restoreColumnWidths() {
		if (columnWidthsToRestore != null) {
			TableUtils.setAbsoluteColumnWidths(tv, tv.getColumns(), columnWidthsToRestore);
		}
	}

	private TreeTableRow<StateItem<?>> initTableRow() {
		final TreeTableRow<StateItem<?>> row = new TreeTableRow<>();

		row.itemProperty().addListener((observable, from, to) -> {
			row.getStyleClass().remove("changed");
			if (to != null && to.getContents() instanceof ASTFormula) {
				final IEvalElement formula = ((ASTFormula) to.getContents()).getFormula();
				final AbstractEvalResult current = this.currentValues.get(formula);
				final AbstractEvalResult previous = this.previousValues.get(formula);

				if (current != null && previous != null
						&& (!current.getClass().equals(previous.getClass()) || current instanceof EvalResult
								&& !((EvalResult) current).getValue().equals(((EvalResult) previous).getValue()))) {
					row.getStyleClass().add("changed");
				}
			}
		});

		final MenuItem copyItem = new MenuItem(bundle.getString("common.contextMenu.copy"));

		copyItem.setOnAction(e -> {
			final Clipboard clipboard = Clipboard.getSystemClipboard();
			final ClipboardContent content = new ClipboardContent();
			content.putString(((ASTFormula) row.getItem().getContents()).getFormula().getCode());
			clipboard.setContent(content);
		});

		copyItem.disableProperty().bind(Bindings.createBooleanBinding(
				() -> row.getItem() == null || !(row.getItem().getContents() instanceof ASTFormula), row.itemProperty())
				.or(currentTrace.currentStateProperty().initializedProperty().not()));

		this.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN), () -> {
			if (tv.getSelectionModel().getSelectedItem() == null) {
				return;
			}
			Object formula = tv.getSelectionModel().getSelectedItem().getValue().getContents();
			if (formula instanceof ASTFormula) {
				final Clipboard clipboard = Clipboard.getSystemClipboard();
				final ClipboardContent content = new ClipboardContent();
				content.putString(((ASTFormula) formula).getFormula().getCode());
				clipboard.setContent(content);
			}
		});

		final MenuItem visualizeExpressionAsGraphItem = new MenuItem(
				bundle.getString("states.statesView.contextMenu.items.visualizeExpressionGraph"));
		// Expression can only be shown if the row item contains an ASTFormula
		// and the current state is initialized.
		visualizeExpressionAsGraphItem.disableProperty().bind(Bindings.createBooleanBinding(
				() -> row.getItem() == null || !(row.getItem().getContents() instanceof ASTFormula), row.itemProperty())
				.or(currentTrace.currentStateProperty().initializedProperty().not()));
		visualizeExpressionAsGraphItem.setOnAction(event -> {
			try {
				DotView formulaStage = injector.getInstance(DotView.class);
				formulaStage.visualizeFormula((IEvalElement) ((ASTFormula) row.getItem().getContents()).getFormula());
				formulaStage.show();
			} catch (EvaluationException | ProBError e) {
				LOGGER.error("Could not visualize formula", e);
				stageManager.makeExceptionAlert(e, "states.statesView.alerts.couldNotVisualizeFormula.content").showAndWait();
			}
		});

		final MenuItem visualizeExpressionAsTableItem = new MenuItem(
				bundle.getString("states.statesView.contextMenu.items.visualizeExpressionTable"));
		// Expression can only be shown if the row item contains an ASTFormula
		// and the current state is initialized.
		visualizeExpressionAsTableItem.disableProperty().bind(Bindings.createBooleanBinding(
				() -> row.getItem() == null || !(row.getItem().getContents() instanceof ASTFormula), row.itemProperty())
				.or(currentTrace.currentStateProperty().initializedProperty().not()));
		visualizeExpressionAsTableItem.setOnAction(event -> {
			try {
				ExpressionTableView expressionTableView = injector.getInstance(ExpressionTableView.class);
				expressionTableView.visualizeExpression(
						getResultValue((ASTFormula) row.getItem().getContents(), this.currentTrace.getCurrentState()));
				expressionTableView.show();
			} catch (EvaluationException | ProBError e) {
				LOGGER.error("Could not visualize formula", e);
				stageManager.makeExceptionAlert(e, "states.statesView.alerts.couldNotVisualizeFormula.content")
						.showAndWait();
			}
		});

		final MenuItem showDetailsItem = new MenuItem(bundle.getString("states.statesView.contextMenu.items.showDetails"));
		// Details can only be shown if the row item contains an ASTFormula,
		// and the corresponding value is an EvalResult or EvaluationErrorResult.
		showDetailsItem.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			if (row.getItem() == null) {
				return true;
			}
			if (row.getItem().getContents() instanceof ASTFormula) {
				final AbstractEvalResult currentValue = this.currentValues.get(((ASTFormula)row.getItem().getContents()).getFormula());
				return !(currentValue instanceof EvalResult || currentValue instanceof EvaluationErrorResult);
			} else {
				return true;
			}
		}, row.itemProperty()));
		showDetailsItem.setOnAction(event -> this.showDetails(row.getItem()));

		row.contextMenuProperty().bind(Bindings.when(row.emptyProperty())
			.then((ContextMenu) null)
			.otherwise(new ContextMenu(
				copyItem,
				visualizeExpressionAsGraphItem,
				visualizeExpressionAsTableItem,
				showDetailsItem
			))
		);

		// Double-click on an item triggers "show full value" if allowed.
		row.setOnMouseClicked(event -> {
			if (!showDetailsItem.isDisable() && event.getButton() == MouseButton.PRIMARY
					&& event.getClickCount() == 2) {
				this.showDetails(row.getItem());
			}
		});
		return row;
	}

	private static void getInitialExpandedFormulas(final List<PrologASTNode> nodes, final List<IEvalElement> formulas) {
		for (final PrologASTNode node : nodes) {
			if (node instanceof ASTFormula) {
				formulas.add(((ASTFormula) node).getFormula());
			}
			if (node instanceof ASTCategory && ((ASTCategory) node).isExpanded()) {
				getInitialExpandedFormulas(node.getSubnodes(), formulas);
			}
		}
	}

	private static List<IEvalElement> getInitialExpandedFormulas(final List<PrologASTNode> nodes) {
		final List<IEvalElement> formulas = new ArrayList<>();
		getInitialExpandedFormulas(nodes, formulas);
		return formulas;
	}

	private static void getExpandedFormulas(final List<TreeItem<StateItem<?>>> treeItems,
			final List<IEvalElement> formulas) {
		for (final TreeItem<StateItem<?>> ti : treeItems) {
			if (ti.getValue().getContents() instanceof ASTFormula) {
				formulas.add(((ASTFormula) ti.getValue().getContents()).getFormula());
			}
			if (ti.isExpanded()) {
				getExpandedFormulas(ti.getChildren(), formulas);
			}
		}
	}

	private static List<IEvalElement> getExpandedFormulas(final List<TreeItem<StateItem<?>>> treeItems) {
		final List<IEvalElement> formulas = new ArrayList<>();
		getExpandedFormulas(treeItems, formulas);
		return formulas;
	}

	private void subscribeFormulas(final StateSpace stateSpace, final Collection<? extends IEvalElement> formulas) {
		stateSpace.subscribe(this, formulas);
		this.subscribedFormulas.addAll(formulas);
	}

	private void unsubscribeFormulas(final StateSpace stateSpace, final Collection<? extends IEvalElement> formulas) {
		stateSpace.unsubscribe(this, formulas);
		this.subscribedFormulas.removeAll(formulas);
	}

	private void updateValueMaps(final Trace trace) {
		this.currentValues.clear();
		this.currentValues.putAll(trace.getCurrentState().getValues());
		this.previousValues.clear();
		if (trace.canGoBack()) {
			this.previousValues.putAll(trace.getPreviousState().getValues());
		}
	}

	@FXML
	private void handleSearchButton() {
		filter = filterState.getText();
		this.updateRoot(currentTrace.get(), currentTrace.get(), true);
	}

	private static boolean matchesFilter(final String filter, final String string) {
		return string.toLowerCase().contains(filter.toLowerCase());
	}

	private static List<PrologASTNode> filterNodes(final List<PrologASTNode> nodes, final String filter) {
		if (filter.isEmpty()) {
			return nodes;
		}

		final List<PrologASTNode> filteredNodes = new ArrayList<>();
		for (final PrologASTNode node : nodes) {
			if (node instanceof ASTFormula) {
				if (matchesFilter(filter, ((ASTFormula) node).getPrettyPrint())) {
					filteredNodes.add(node);
				}
			} else if (node instanceof ASTCategory) {
				final List<PrologASTNode> filteredSubnodes = filterNodes(node.getSubnodes(), filter);
				if (!filteredSubnodes.isEmpty()) {
					final ASTCategory category = (ASTCategory) node;
					filteredNodes.add(new ASTCategory(filteredSubnodes, category.getName(), category.isExpanded(),
							category.isPropagated()));
				}
			} else {
				throw new IllegalArgumentException("Unknown node type: " + node.getClass());
			}
		}
		return filteredNodes;
	}

	private void buildNodes(final TreeItem<StateItem<?>> treeItem, final List<PrologASTNode> nodes) {
		Objects.requireNonNull(treeItem);
		Objects.requireNonNull(nodes);

		assert treeItem.getChildren().isEmpty();

		for (final PrologASTNode node : nodes) {
			final TreeItem<StateItem<?>> subTreeItem = new TreeItem<>();
			if (node instanceof ASTCategory && ((ASTCategory) node).isExpanded()) {
				subTreeItem.setExpanded(true);
			}

			treeItem.getChildren().add(subTreeItem);
			subTreeItem.setValue(new StateItem<>(node, false));
			subTreeItem.expandedProperty().addListener((o, from, to) -> {
				final Trace trace = this.currentTrace.get();
				final List<IEvalElement> formulas = getExpandedFormulas(subTreeItem.getChildren());
				if (to) {
					this.subscribeFormulas(trace.getStateSpace(), formulas);
				} else {
					this.unsubscribeFormulas(trace.getStateSpace(), formulas);
				}
				this.updateValueMaps(trace);
			});
			buildNodes(subTreeItem, node.getSubnodes());
		}
	}

	private static void updateNodes(final TreeItem<StateItem<?>> treeItem, final List<PrologASTNode> nodes) {
		Objects.requireNonNull(treeItem);
		Objects.requireNonNull(nodes);

		assert treeItem.getChildren().size() == nodes.size();

		for (int i = 0; i < nodes.size(); i++) {
			final TreeItem<StateItem<?>> subTreeItem = treeItem.getChildren().get(i);
			final PrologASTNode node = nodes.get(i);
			subTreeItem.setValue(new StateItem<>(node, false));
			updateNodes(subTreeItem, node.getSubnodes());
		}
	}

	private void updateRoot(final Trace from, final Trace to, final boolean filterChanged) {
		final int selectedRow = tv.getSelectionModel().getSelectedIndex();

		Platform.runLater(() -> {
			this.statusBar.setStatesViewUpdating(true);
			this.tv.setDisable(true);
		});

		// If the model has changed or the machine structure hasn't been loaded
		// yet, update it and rebuild the tree view.
		final boolean reloadRootNodes = this.rootNodes == null || from == null
				|| !from.getModel().equals(to.getModel());
		if (reloadRootNodes) {
			final GetMachineStructureCommand cmd = new GetMachineStructureCommand();
			to.getStateSpace().execute(cmd);
			this.rootNodes = cmd.getPrologASTList();
		}
		// If the root nodes were reloaded or the filter has changed, update the
		// filtered node list.
		if (reloadRootNodes || filterChanged) {
			this.filteredRootNodes = filterNodes(this.rootNodes, this.filter);
			this.unsubscribeFormulas(to.getStateSpace(), this.subscribedFormulas);
			this.subscribeFormulas(to.getStateSpace(), getInitialExpandedFormulas(this.filteredRootNodes));
		}

		this.updateValueMaps(to);

		// The nodes are stored in a local variable to avoid a race condition
		// where another updateRoot call reassigns the filteredRootNodes field
		// before the Platform.runLater block runs.
		final List<PrologASTNode> nodes = this.filteredRootNodes;
		Platform.runLater(() -> {
			if (reloadRootNodes || filterChanged) {
				this.tvRootItem.getChildren().clear();
				buildNodes(this.tvRootItem, nodes);
			} else {
				updateNodes(this.tvRootItem, nodes);
			}
			this.tv.refresh();
			this.tv.getSelectionModel().select(selectedRow);
			this.tv.setDisable(false);
			this.statusBar.setStatesViewUpdating(false);
		});
	}

	private static String getResultValue(final ASTFormula element, final State state) {
		final AbstractEvalResult result = state.eval(element.getFormula(FormulaExpand.EXPAND));
		if (result instanceof EvalResult) {
			return ((EvalResult)result).getValue();
		} else if (result instanceof EvaluationErrorResult) {
			return String.join("\n", ((EvaluationErrorResult)result).getErrors());
		} else {
			throw new IllegalArgumentException("Unknown eval result type: " + result.getClass());
		}
	}

	private void showDetails(StateItem<?> stateItem) {
		final FullValueStage stage = injector.getInstance(FullValueStage.class);
		if (stateItem.getContents() instanceof ASTFormula) {
			final ASTFormula element = (ASTFormula) stateItem.getContents();
			stage.setTitle(element.getFormula().toString());
			stage.setCurrentValue(getResultValue(element, this.currentTrace.getCurrentState()));
			if (this.currentTrace.canGoBack()) {
				stage.setPreviousValue(getResultValue(element, this.currentTrace.get().getPreviousState()));
			}
			stage.show();
		} else {
			throw new IllegalArgumentException("Invalid row item type: " + stateItem.getClass());
		}
		stage.show();
	}
}
