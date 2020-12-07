package de.prob2.ui.states;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.animator.domainobjects.BVisual2Formula;
import de.prob.animator.domainobjects.BVisual2Value;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob.exception.ProBError;
import de.prob.statespace.State;
import de.prob.statespace.Trace;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.dynamic.dotty.DotView;
import de.prob2.ui.dynamic.table.ExpressionTableView;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.BackgroundUpdater;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.persistence.PersistenceUtils;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.statusbar.StatusBar;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

@FXMLInjected
@Singleton
public final class StatesView extends StackPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatesView.class);

	@FXML
	private TextField filterState;
	@FXML
	private HelpButton helpButton;

	@FXML
	private TreeTableView<StateItem> tv;
	@FXML
	private TreeTableColumn<StateItem, StateItem> tvName;
	@FXML
	private TreeTableColumn<StateItem, BVisual2Value> tvValue;
	@FXML
	private TreeTableColumn<StateItem, BVisual2Value> tvPreviousValue;

	private final Injector injector;
	private final CurrentTrace currentTrace;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final Config config;

	private final BackgroundUpdater updater;
	private final Set<BVisual2Formula> expandedFormulas;
	private final Set<BVisual2Formula> visibleFormulas;
	private final Map<BVisual2Formula, ExpandedFormula> formulaStructureCache;
	private boolean structureFullyExpanded;
	private final Map<State, Map<BVisual2Formula, BVisual2Value>> formulaValueCache;
	private final StateItem.FormulaEvaluator cachingEvaluator;

	@Inject
	private StatesView(final Injector injector, final CurrentTrace currentTrace, final StatusBar statusBar,
					   final StageManager stageManager, final ResourceBundle bundle, final StopActions stopActions, final Config config) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.config = config;

		this.updater = new BackgroundUpdater("StatesView Updater");
		stopActions.add(this.updater::shutdownNow);
		statusBar.addUpdatingExpression(this.updater.runningProperty());
		this.expandedFormulas = new HashSet<>();
		this.visibleFormulas = new HashSet<>();
		this.formulaStructureCache = new HashMap<>();
		this.structureFullyExpanded = false;
		this.formulaValueCache = new HashMap<>();
		this.cachingEvaluator = new StateItem.FormulaEvaluator() {
			@Override
			public ExpandedFormula expand(final BVisual2Formula formula) {
				return expandFormulaWithCaching(formula);
			}

			@Override
			public BVisual2Value evaluate(final BVisual2Formula formula, final State state) {
				return evaluateFormulaWithCaching(formula, state);
			}
		};

		stageManager.loadFXML(this, "states_view.fxml");
	}

	@FXML
	private void initialize() {
		helpButton.setHelpContent("mainView.stateView", null);
		
		tv.setRowFactory(view -> initTableRow());

		this.tvName.setCellFactory(col -> new NameCell());
		this.tvValue.setCellFactory(col -> new ValueCell(bundle));
		this.tvPreviousValue.setCellFactory(col -> new ValueCell(bundle));

		this.tvName.setCellValueFactory(data -> data.getValue().valueProperty());
		this.tvValue.setCellValueFactory(data -> Bindings.select(data.getValue().valueProperty(), "currentValue"));
		this.tvPreviousValue.setCellValueFactory(data -> Bindings.select(data.getValue().valueProperty(), "previousValue"));

		this.tv.setRoot(createRootItem());

		this.updater.runningProperty().addListener((o, from, to) -> Platform.runLater(() -> this.tv.setDisable(to)));

		final ChangeListener<Trace> traceChangeListener = (observable, from, to) -> this.updateRootAsync(from, to, this.filterState.getText());
		traceChangeListener.changed(this.currentTrace, null, currentTrace.get());
		this.currentTrace.addListener(traceChangeListener);

		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.statesViewColumnsWidth != null) {
					PersistenceUtils.setAbsoluteColumnWidths(tv, configData.statesViewColumnsWidth);
				}
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.statesViewColumnsWidth = PersistenceUtils.getAbsoluteColumnWidths(tv);
			}
		});

		filterState.textProperty().addListener((o, from, to) -> {
			final Trace trace = this.currentTrace.get();
			this.updateRootAsync(trace, trace, to);
		});
	}

	private Tooltip buildItemTooltip(final StateItem item) {
		final StringBuilder sb = new StringBuilder();

		final boolean hasRodinLabels = item.getRodinLabels() != null && !item.getRodinLabels().isEmpty();
		final boolean hasDescription = item.getDescription() != null && !item.getDescription().isEmpty();
		if (hasRodinLabels) {
			sb.append('@');
			sb.append(String.join(";", item.getRodinLabels()));
			if (hasDescription) {
				sb.append(": ");
			}
		}
		if (hasDescription) {
			sb.append(item.getDescription());
		}
		if (sb.length() > 0) {
			sb.append('\n');
		}

		final int childrenCount = item.getSubformulas().size();
		if (childrenCount > 0) {
			if (item.getFunctorSymbol() != null) {
				sb.append(String.format(bundle.getString("states.statesView.tooltip.formulaChildren"), item.getFunctorSymbol(), childrenCount));
			} else {
				sb.append(String.format(bundle.getString("states.statesView.tooltip.elementChildren"), childrenCount));
			}
		}

		if (sb.length() > 0) {
			return new Tooltip(sb.toString());
		} else {
			return null;
		}
	}

	private TreeTableRow<StateItem> initTableRow() {
		final TreeTableRow<StateItem> row = new TreeTableRow<>();

		row.itemProperty().addListener((observable, from, to) -> {
			row.getStyleClass().remove("changed");
			row.setTooltip(null);
			if (to != null) {
				if (!to.getCurrentValue().equals(to.getPreviousValue()) && currentTrace.getCurrentState().isInitialised()) {
					row.getStyleClass().add("changed");
				}
				row.setTooltip(this.buildItemTooltip(to));
			}
		});

		final MenuItem copyItem = new MenuItem(bundle.getString("states.statesView.contextMenu.items.copyName"));

		copyItem.setOnAction(e -> handleCopyName(row.getTreeItem()));
		copyItem.disableProperty().bind(row.itemProperty().isNull());

		this.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN), () -> {
			final TreeItem<StateItem> item = tv.getSelectionModel().getSelectedItem();
			if (item != null) {
				handleCopyName(item);
			}
		});

		final MenuItem visualizeExpressionAsGraphItem = new MenuItem(
				bundle.getString("states.statesView.contextMenu.items.visualizeExpressionGraph"));
		visualizeExpressionAsGraphItem.disableProperty().bind(row.itemProperty().isNull());
		visualizeExpressionAsGraphItem.setOnAction(event -> {
			try {
				DotView formulaStage = injector.getInstance(DotView.class);
				formulaStage.visualizeFormula(row.getItem().getLabel());
				formulaStage.show();
			} catch (EvaluationException | ProBError e) {
				LOGGER.error("Could not visualize formula", e);
				final Alert alert = stageManager.makeExceptionAlert(e, "states.statesView.alerts.couldNotVisualizeFormula.content");
				alert.initOwner(this.getScene().getWindow());
				alert.showAndWait();
			}
		});

		final MenuItem visualizeExpressionAsTableItem = new MenuItem(
				bundle.getString("states.statesView.contextMenu.items.visualizeExpressionTable"));
		visualizeExpressionAsTableItem.disableProperty().bind(row.itemProperty().isNull());
		visualizeExpressionAsTableItem.setOnAction(event -> {
			try {
				ExpressionTableView expressionTableView = injector.getInstance(ExpressionTableView.class);
				expressionTableView.visualizeExpression(row.getItem().getLabel());
				expressionTableView.show();
			} catch (EvaluationException | ProBError e) {
				LOGGER.error("Could not visualize formula", e);
				final Alert alert = stageManager.makeExceptionAlert(e, "states.statesView.alerts.couldNotVisualizeFormula.content");
				alert.initOwner(this.getScene().getWindow());
				alert.showAndWait();
			}
		});

		final MenuItem showDetailsItem = new MenuItem(bundle.getString("states.statesView.contextMenu.items.showDetails"));
		showDetailsItem.disableProperty().bind(row.itemProperty().isNull());
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

	private static boolean matchesFilter(final String filter, final String string) {
		return string.toLowerCase().contains(filter.toLowerCase());
	}

	private ExpandedFormula expandFormulaWithCaching(final BVisual2Formula formula) {
		return this.formulaStructureCache.computeIfAbsent(formula, BVisual2Formula::expandStructureNonrecursive);
	}

	private BVisual2Value evaluateFormulaWithCaching(final BVisual2Formula formula, final State state) {
		return this.formulaValueCache
			.computeIfAbsent(state, s -> new HashMap<>())
			.computeIfAbsent(formula, f -> f.evaluate(state));
	}

	private Map<BVisual2Formula, BVisual2Value> getFormulaValueCacheForState(final State state) {
		return this.formulaValueCache.computeIfAbsent(state, s -> new HashMap<>());
	}

	/**
	 * Add the given expanded formula structures (and their children, recursively) to the cache.
	 * 
	 * @param expandedStructures the expanded formula values to add
	 */
	private void addFormulaStructuresToCache(final Collection<? extends ExpandedFormula> expandedStructures) {
		for (final ExpandedFormula expanded : expandedStructures) {
			this.formulaStructureCache.put(expanded.getFormula(), expanded);
			if (expanded.getChildren() != null) {
				this.addFormulaStructuresToCache(expanded.getChildren());
			}
		}
	}

	/**
	 * <p>Expand the given formulas and cache their structures, if they are not already present in the cache.</p>
	 * <p>This is faster than expanding each formula individually via {@link StateItem} and {@link #expandFormulaWithCaching(BVisual2Formula)}, because this method internally expands all formulas in a single Prolog command, instead of one command per formula.</p>
	 *
	 * @param formulas the formulas for which to cache the values
	 */
	private void cacheMissingFormulaStructures(final Collection<BVisual2Formula> formulas) {
		final List<BVisual2Formula> notYetCached = new ArrayList<>(formulas);
		notYetCached.removeAll(this.formulaStructureCache.keySet());
		addFormulaStructuresToCache(BVisual2Formula.expandStructureNonrecursiveMultiple(notYetCached));
	}

	/**
	 * <p>Evaluate the given formulas and cache their values, if they are not already present in the cache.</p>
	 * <p>This is faster than evaluating each formula individually via {@link StateItem} and {@link #evaluateFormulaWithCaching(BVisual2Formula, State)}, because this method internally evaluates all formulas in a single Prolog command, instead of one command per formula.</p>
	 * 
	 * @param formulas the formulas for which to cache the values
	 * @param state the state in which to evaluate the formulas
	 */
	private void cacheMissingFormulaValues(final Collection<BVisual2Formula> formulas, final State state) {
		final Map<BVisual2Formula, BVisual2Value> cacheForState = getFormulaValueCacheForState(state);
		final List<BVisual2Formula> notYetCached = new ArrayList<>(formulas);
		notYetCached.removeAll(cacheForState.keySet());
		final List<BVisual2Value> values = BVisual2Formula.evaluateMultiple(notYetCached, state);
		assert notYetCached.size() == values.size();
		for (int i = 0; i < notYetCached.size(); i++) {
			cacheForState.put(notYetCached.get(i), values.get(i));
		}
	}

	private ChangeListener<Boolean> getTrackVisibleListener(final TreeItem<StateItem> treeItem) {
		return (o, from, to) -> {
			final List<BVisual2Formula> subformulas = treeItem.getChildren().stream()
				.map(TreeItem::getValue)
				.map(StateItem::getFormula)
				.collect(Collectors.toList());
			
			if (to) {
				visibleFormulas.addAll(subformulas);
			} else {
				visibleFormulas.removeAll(subformulas);
				
				// When treeItem is collapsed, also collapse all of its children.
				// This will recurse automatically as the children's listeners for tracking visibility are fired.
				// JavaFX does not do this automatically by default,
				// which leads to problems with visibleFormulas containing formulas where the parent is expanded,
				// but (for example) the grandparent is collapsed.
				// In that case our code incorrectly thinks that the children are visible, even though they aren't.
				treeItem.getChildren().forEach(subTreeItem -> subTreeItem.setExpanded(false));
			}
		};
	}

	/**
	 * <p>
	 * Generate treeItem's children from the given subformulas.
	 * The subformulas are not filtered.
	 * </p>
	 * <p>
	 * Calling this method only generates the direct children of treeItem.
	 * The next level of the tree (the children's children) is generated only once treeItem is expanded by the user.
	 * (If treeItem is already expanded once this method is called, the next level is generated immediately.)
	 * </p>
	 * 
	 * @param treeItem the tree item into which the children should be generated
	 * @param subformulas the subformulas of treeItem's formula, from which the children are generated
	 * @param currentState the current state (never null), used to display the formula's current value
	 * @param previousState the previous state (may be null if at the start of the trace), used to display the formula's previous value
	 */
	private void addSubformulaItemsUnfiltered(final TreeItem<StateItem> treeItem, final List<BVisual2Formula> subformulas, final State currentState, final State previousState) {
		// Generate the tree items for treeItem's children right away.
		// This must be done even if treeItem is not expanded, because otherwise treeItem would have no child items and thus no expansion arrow, even if it actually has subformulas.
		final List<TreeItem<StateItem>> children = subformulas.stream()
			.map(f -> new TreeItem<>(new StateItem(f, currentState, previousState, this.cachingEvaluator)))
			.collect(Collectors.toList());

		// The tree items for the children of treeItem's children are generated once treeItem is expanded.
		// This needs to be an anonymous class instead of a lambda,
		// so that the listener can remove itself after it runs once.
		final ChangeListener<Boolean> generateGrandchildrenListener = new ChangeListener<Boolean>() {
			@Override
			public void changed(final ObservableValue<? extends Boolean> o, final Boolean from, final Boolean to) {
				if (to) {
					// Pre-cache the structures and current and previous values of the newly visible formulas.
					cacheMissingFormulaStructures(subformulas);
					cacheMissingFormulaValues(subformulas, currentState);
					if (previousState != null) {
						cacheMissingFormulaValues(subformulas, previousState);
					}
					for (final TreeItem<StateItem> subTreeItem : children) {
						addSubformulaItemsUnfiltered(subTreeItem, subTreeItem.getValue().getSubformulas(), currentState, previousState);
					}
					treeItem.expandedProperty().removeListener(this);
				}
			}
		};
		treeItem.expandedProperty().addListener(generateGrandchildrenListener);

		// This listener tracks the expanded state of each formula.
		// Unlike the first listener, this one does *not* remove itself after it runs.
		final ChangeListener<Boolean> trackExpandedListener = (o, from, to) -> {
			// The root item has its value set to null. Its expanded state does not need to be stored.
			// Don't track expanded states while a filter is present, because the filtering code automatically expands items.
			if (treeItem.getValue() != null) {
				final BVisual2Formula formula = treeItem.getValue().getFormula();
				if (to) {
					expandedFormulas.add(formula);
				} else {
					expandedFormulas.remove(formula);
				}
			}
		};
		treeItem.expandedProperty().addListener(trackExpandedListener);

		// This listener tracks the visible state of each formula.
		// Unlike the first listener, this one does *not* remove itself after it runs.
		final ChangeListener<Boolean> trackVisibleListener = getTrackVisibleListener(treeItem);
		treeItem.expandedProperty().addListener(trackVisibleListener);

		// If treeItem is already expanded, immediately fire the appropriate listeners.
		if (treeItem.isExpanded()) {
			generateGrandchildrenListener.changed(treeItem.expandedProperty(), false, true);
			trackExpandedListener.changed(treeItem.expandedProperty(), false, true);
			trackVisibleListener.changed(treeItem.expandedProperty(), false, true);
		}

		treeItem.getChildren().setAll(children);

		// Restore the previous expanded state of treeItem.
		// If this expands treeItem, this triggers the listener above.
		// The root item has its value set to null. It should always be expanded.
		treeItem.setExpanded(treeItem.getValue() == null || this.expandedFormulas.contains(treeItem.getValue().getFormula()));
	}

	/**
	 * <p>
	 * Generate treeItem's children from the given subformulas.
	 * The subformulas are filtered based on the given filter string.
	 * Only formulas whose label matches the filter (and the parents of those formulas) are shown.
	 * It's assumed that treeItem's label never matches the filter
	 * (if it does, you should call {@link #addSubformulaItemsUnfiltered(TreeItem, List, State, State)} instead of this method).
	 * </p>
	 * <p>
	 * Calling this method generates all children of treeItem recursively.
	 * This is necessary because even if none of the direct children match the filter,
	 * some of the children's children might,
	 * so the entire tree has to be generated and filtered.
	 * If a (direct or indirect) child is found that matches the filter,
	 * it is generated lazily using {@link #addSubformulaItemsUnfiltered(TreeItem, List, State, State)} and not recursively,
	 * but this usually doesn't make a big performance difference.
	 * </p>
	 * 
	 * @param treeItem the tree item into which the children should be generated
	 * @param subformulas the subformulas of treeItem's formula, from which the children are generated
	 * @param currentState the current state (never null), used to display the formula's current value
	 * @param previousState the previous state (may be null if at the start of the trace), used to display the formula's previous value
	 * @param filter the string by which to filter the subformulas
	 */
	private void addSubformulaItemsFiltered(final TreeItem<StateItem> treeItem, final List<BVisual2Formula> subformulas, final State currentState, final State previousState, final String filter) {
		assert !filter.isEmpty();
		assert treeItem.getValue() == null || !matchesFilter(filter, treeItem.getValue().getLabel());

		// Pre-cache the structures of the formulas to filter.
		cacheMissingFormulaStructures(subformulas);
		final List<TreeItem<StateItem>> matchingChildren = new ArrayList<>();
		for (final BVisual2Formula subformula : subformulas) {
			final TreeItem<StateItem> subTreeItem = new TreeItem<>(new StateItem(subformula, currentState, previousState, this.cachingEvaluator));
			// If subTreeItem matches the filter, don't filter its children.
			if (matchesFilter(filter, subTreeItem.getValue().getLabel())) {
				addSubformulaItemsUnfiltered(subTreeItem, subTreeItem.getValue().getSubformulas(), currentState, previousState);
				matchingChildren.add(subTreeItem);
			} else {
				addSubformulaItemsFiltered(subTreeItem, subTreeItem.getValue().getSubformulas(), currentState, previousState, filter);
				// subTreeItem doesn't match the filter,
				// so add it only if it has any children that matched the filter.
				if (!subTreeItem.getChildren().isEmpty()) {
					matchingChildren.add(subTreeItem);
				}
			}
		}
		// Pre-cache the current and previous values of the matching subformulas, which will be immediately visible.
		final List<BVisual2Formula> matchingFormulas = matchingChildren.stream()
			.map(TreeItem::getValue)
			.map(StateItem::getFormula)
			.collect(Collectors.toList());
		cacheMissingFormulaValues(matchingFormulas, currentState);
		if (previousState != null) {
			cacheMissingFormulaValues(matchingFormulas, previousState);
		}
		treeItem.getChildren().setAll(matchingChildren);

		final ChangeListener<Boolean> trackVisibleListener = getTrackVisibleListener(treeItem);
		treeItem.expandedProperty().addListener(trackVisibleListener);

		// Always expand treeItem, because it doesn't match the filter.
		treeItem.setExpanded(true);
		trackVisibleListener.changed(treeItem.expandedProperty(), false, true);
	}

	private static TreeItem<StateItem> createRootItem() {
		final TreeItem<StateItem> rootItem = new TreeItem<>(null);
		rootItem.setExpanded(true);
		return rootItem;
	}

	private void updateRootAsync(final Trace from, final Trace to, final String filter) {
		this.updater.execute(() -> this.updateRoot(from, to, filter));
	}

	private void updateRoot(final Trace from, final Trace to, final String filter) {
		if (to == null) {
			Platform.runLater(() -> this.tv.setRoot(createRootItem()));
			this.expandedFormulas.clear();
			this.formulaStructureCache.clear();
			this.structureFullyExpanded = false;
			this.formulaValueCache.clear();
			return;
		}

		if (from == null || !from.getStateSpace().equals(to.getStateSpace())) {
			this.expandedFormulas.clear();
			this.formulaStructureCache.clear();
			this.structureFullyExpanded = false;
			this.formulaValueCache.clear();
		} else {
			// Pre-cache the current and previous values of all visible formulas.
			// The structures have already been cached when the formulas first became visible.
			cacheMissingFormulaValues(this.visibleFormulas, to.getCurrentState());
			if (to.canGoBack()) {
				cacheMissingFormulaValues(this.visibleFormulas, to.getPreviousState());
			}
		}
		// After the visible formulas have been evaluted,
		// clear the collection and let createRootItem recalculate the currently visible formulas.
		this.visibleFormulas.clear();

		final int selectedRow = tv.getSelectionModel().getSelectedIndex();

		final List<BVisual2Formula> topLevel = BVisual2Formula.getTopLevel(to.getStateSpace());
		final TreeItem<StateItem> newRoot = createRootItem();
		if (filter.isEmpty()) {
			addSubformulaItemsUnfiltered(newRoot, topLevel, to.getCurrentState(), to.canGoBack() ? to.getPreviousState() : null);
		} else {
			// If there is a filter, recursively expand the entire tree beforehand.
			// The filtering code usually has to expand most of the tree to search for matching items, so this improves performance when filtering.
			// This only needs to be done once unless the state space changes,
			// because the structure is always the same in all states.
			if (!this.structureFullyExpanded) {
				addFormulaStructuresToCache(BVisual2Formula.expandStructureMultiple(topLevel));
				this.structureFullyExpanded = true;
			}
			addSubformulaItemsFiltered(newRoot, topLevel, to.getCurrentState(), to.canGoBack() ? to.getPreviousState() : null, filter);
		}
		Platform.runLater(() -> {
			// Workaround for JavaFX bug JDK-8199324/PROB2UI-377 on Java 10 and later.
			// If the contents of a TreeTableView are changed while there is a selection,
			// JavaFX incorrectly throws an IllegalStateException with message "Not a permutation change".
			tv.getSelectionModel().clearSelection();
			this.tv.setRoot(newRoot);
			this.tv.getSelectionModel().select(selectedRow);
		});
	}

	private static void handleCopyName(final TreeItem<StateItem> item) {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		final ClipboardContent content = new ClipboardContent();
		content.putString(item.getValue().getLabel());
		clipboard.setContent(content);
	}

	private void showDetails(final StateItem item) {
		final FullValueStage stage = injector.getInstance(FullValueStage.class);
		stage.setValue(item);
		stage.show();
	}
}
