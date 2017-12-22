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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

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
import de.prob.statespace.Trace;

import de.prob2.ui.formula.FormulaGenerator;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.statusbar.StatusBar;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class StatesView extends VBox {
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
	private final FormulaGenerator formulaGenerator;
	private final StatusBar statusBar;
	private final StageManager stageManager;
	private final ResourceBundle bundle;

	private List<PrologASTNode> rootNodes;
	private List<PrologASTNode> filteredRootNodes;
	private final Set<IEvalElement> subscribedFormulas;
	private final Map<IEvalElement, AbstractEvalResult> currentValues;
	private final Map<IEvalElement, AbstractEvalResult> previousValues;
	private final ExecutorService updater;

	private String filter = "";

	@Inject
	private StatesView(final Injector injector, final CurrentTrace currentTrace,
			final FormulaGenerator formulaGenerator, final StatusBar statusBar, final StageManager stageManager,
			final ResourceBundle bundle, final StopActions stopActions) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.formulaGenerator = formulaGenerator;
		this.statusBar = statusBar;
		this.stageManager = stageManager;
		this.bundle = bundle;

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

		final ChangeListener<Trace> traceChangeListener = (observable, from, to) -> {
			if (to == null) {
				this.rootNodes = null;
				this.filteredRootNodes = null;
				this.currentValues.clear();
				this.previousValues.clear();
				this.tv.getRoot().getChildren().clear();
			} else {
				this.updater.execute(() -> this.updateRoot(from, to, false));
			}
		};
		traceChangeListener.changed(this.currentTrace, null, currentTrace.get());
		this.currentTrace.addListener(traceChangeListener);

		bindIconSizeToFontSize();
	}

	private void bindIconSizeToFontSize() {
		FontSize fontsize = injector.getInstance(FontSize.class);
		((FontAwesomeIconView) (searchButton.getGraphic())).glyphSizeProperty().bind(fontsize);
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

		final MenuItem visualizeExpressionItem = new MenuItem(bundle.getString("states.menu.visualizeExpression"));
		// Expression can only be shown if the row item contains an ASTFormula
		// and the current state is initialized.
		visualizeExpressionItem.disableProperty().bind(Bindings.createBooleanBinding(
				() -> row.getItem() == null || !(row.getItem().getContents() instanceof ASTFormula), row.itemProperty())
				.or(currentTrace.currentStateProperty().initializedProperty().not()));
		visualizeExpressionItem.setOnAction(event -> {
			try {
				formulaGenerator.showFormula(((ASTFormula) row.getItem().getContents()).getFormula());
			} catch (EvaluationException | ProBError e) {
				LOGGER.error("Could not visualize formula", e);
				stageManager.makeAlert(Alert.AlertType.ERROR,
						String.format(bundle.getString("states.error.couldNotVisualize"), e)).showAndWait();
			}
		});

		final MenuItem showFullValueItem = new MenuItem(bundle.getString("states.menu.showFullValue"));
		// Full value can only be shown if the row item contains an ASTFormula,
		// and the corresponding value is an EvalResult.
		showFullValueItem.disableProperty().bind(Bindings.createBooleanBinding(
				() -> row.getItem() == null || !(row.getItem().getContents() instanceof ASTFormula && this.currentValues
						.get(((ASTFormula) row.getItem().getContents()).getFormula()) instanceof EvalResult),
				row.itemProperty()));
		showFullValueItem.setOnAction(event -> this.showFullValue(row.getItem()));

		final MenuItem showErrorsItem = new MenuItem(bundle.getString("states.menu.showErrors"));
		// Errors can only be shown if the row contains an ASTFormula whose
		// value is an EvaluationErrorResult.
		showErrorsItem.disableProperty().bind(Bindings.createBooleanBinding(
				() -> row.getItem() == null || !(row.getItem().getContents() instanceof ASTFormula && this.currentValues
						.get(((ASTFormula) row.getItem().getContents()).getFormula()) instanceof EvaluationErrorResult),
				row.itemProperty()));
		showErrorsItem.setOnAction(event -> this.showError(row.getItem()));

		row.contextMenuProperty().bind(Bindings.when(row.emptyProperty()).then((ContextMenu) null)
				.otherwise(new ContextMenu(visualizeExpressionItem, showFullValueItem, showErrorsItem)));

		// Double-click on an item triggers "show full value" if allowed.
		row.setOnMouseClicked(event -> {
			if (!showFullValueItem.isDisable() && event.getButton() == MouseButton.PRIMARY
					&& event.getClickCount() == 2) {
				showFullValueItem.getOnAction().handle(null);
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

	private void subscribeFormulas(final Collection<? extends IEvalElement> formulas) {
		this.currentTrace.getStateSpace().subscribe(this, formulas);
		this.subscribedFormulas.addAll(formulas);
	}

	private void unsubscribeFormulas(final Collection<? extends IEvalElement> formulas) {
		this.currentTrace.getStateSpace().unsubscribe(this, formulas);
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

	private List<PrologASTNode> filterNodes(final List<PrologASTNode> nodes, final String filter) {
		if (filter.isEmpty()) {
			return nodes;
		}
		
		final List<PrologASTNode> filteredNodes = new ArrayList<>();
		for (final PrologASTNode node : nodes) {
			if (node instanceof ASTFormula) {
				if (((ASTFormula)node).getPrettyPrint().contains(filter)) {
					filteredNodes.add(node);
				}
			} else if (node instanceof ASTCategory) {
				final List<PrologASTNode> filteredSubnodes = filterNodes(node.getSubnodes(), filter);
				if (!filteredSubnodes.isEmpty()) {
					final ASTCategory category = (ASTCategory)node;
					filteredNodes.add(new ASTCategory(filteredSubnodes, category.getName(), category.isExpanded(), category.isPropagated()));
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
				final List<IEvalElement> formulas = getExpandedFormulas(subTreeItem.getChildren());
				if (to) {
					this.subscribeFormulas(formulas);
				} else {
					this.unsubscribeFormulas(formulas);
				}
				this.updateValueMaps(this.currentTrace.get());
			});
			buildNodes(subTreeItem, node.getSubnodes());
		}
	}

	private void updateNodes(final TreeItem<StateItem<?>> treeItem, final List<PrologASTNode> nodes) {
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
		final boolean reloadRootNodes = this.rootNodes == null || from == null || !from.getModel().equals(to.getModel());
		if (reloadRootNodes) {
			final GetMachineStructureCommand cmd = new GetMachineStructureCommand();
			to.getStateSpace().execute(cmd);
			this.rootNodes = cmd.getPrologASTList();
		}
		// If the root nodes were reloaded or the filter has changed, update the filtered node list.
		if (reloadRootNodes || filterChanged) {
			this.filteredRootNodes = filterNodes(this.rootNodes, this.filter);
			this.unsubscribeFormulas(this.subscribedFormulas);
			this.subscribeFormulas(getInitialExpandedFormulas(this.filteredRootNodes));
		}

		this.updateValueMaps(to);

		Platform.runLater(() -> {
			if (reloadRootNodes || filterChanged) {
				this.tvRootItem.getChildren().clear();
				buildNodes(this.tvRootItem, this.filteredRootNodes);
			} else {
				updateNodes(this.tvRootItem, this.filteredRootNodes);
			}
			this.tv.refresh();
			this.tv.getSelectionModel().select(selectedRow);
			this.tv.setDisable(false);
			this.statusBar.setStatesViewUpdating(false);
		});
	}

	private void showError(StateItem<?> stateItem) {
		final FullValueStage stage = injector.getInstance(FullValueStage.class);
		if (stateItem.getContents() instanceof ASTFormula) {
			final AbstractEvalResult result = this.currentValues
					.get(((ASTFormula) stateItem.getContents()).getFormula());
			if (result instanceof EvaluationErrorResult) {
				stage.setTitle(stateItem.toString());
				stage.setCurrentValue(String.join("\n", ((EvaluationErrorResult) result).getErrors()));
				stage.setFormattingEnabled(false);
				stage.show();
			} else {
				throw new IllegalArgumentException("Row item result is not an error: " + result.getClass());
			}
		} else {
			throw new IllegalArgumentException("Invalid row item type: " + stateItem.getClass());
		}
	}

	private static String getResultValue(final ASTFormula element, final State state) {
		final AbstractEvalResult result = state.eval(element.getFormula(FormulaExpand.EXPAND));
		return result instanceof EvalResult ? ((EvalResult) result).getValue() : null;
	}

	private void showFullValue(StateItem<?> stateItem) {
		final FullValueStage stage = injector.getInstance(FullValueStage.class);
		if (stateItem.getContents() instanceof ASTFormula) {
			final ASTFormula element = (ASTFormula) stateItem.getContents();
			stage.setTitle(element.getFormula().toString());
			stage.setCurrentValue(getResultValue(element, this.currentTrace.getCurrentState()));
			stage.setPreviousValue(
					this.currentTrace.canGoBack() ? getResultValue(element, this.currentTrace.get().getPreviousState())
							: null);
			stage.setFormattingEnabled(true);
		} else {
			throw new IllegalArgumentException("Invalid row item type: " + stateItem.getClass());
		}
		stage.show();
	}

	public TreeTableView<StateItem<?>> getTable() {
		return tv;
	}
}
