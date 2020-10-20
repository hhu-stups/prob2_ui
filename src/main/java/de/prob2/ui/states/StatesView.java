package de.prob2.ui.states;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.animator.domainobjects.BVisual2Formula;
import de.prob.animator.domainobjects.BVisual2Value;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob.animator.domainobjects.ExpandedFormulaStructure;
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
import java.util.Iterator;
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
	private final Map<BVisual2Formula, ExpandedFormulaStructure> formulaStructureCache;
	private final Map<State, Map<BVisual2Formula, BVisual2Value>> formulaValueCache;

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
		this.formulaValueCache = new HashMap<>();

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

	private TreeTableRow<StateItem> initTableRow() {
		final TreeTableRow<StateItem> row = new TreeTableRow<>();

		row.itemProperty().addListener((observable, from, to) -> {
			row.getStyleClass().remove("changed");
			if (to != null) {
				if (!to.getCurrentValue().equals(to.getPreviousValue())) {
					row.getStyleClass().add("changed");
				}

				if (!to.getDescription().isEmpty()) {
					row.setTooltip(new Tooltip(to.getDescription()));
				}
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

	private ExpandedFormulaStructure expandFormulaWithCaching(final BVisual2Formula formula) {
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
	private void addFormulaStructuresToCache(final Collection<? extends ExpandedFormulaStructure> expandedStructures) {
		for (final ExpandedFormulaStructure expanded : expandedStructures) {
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
	 * Add the given expanded formula values (and their children, recursively) to a cache map.
	 * 
	 * @param expandedValues the expanded formula values to add
	 * @param cache the cache to which to add the expanded formula values
	 */
	private void addFormulaValuesToCache(final Collection<ExpandedFormula> expandedValues, final Map<BVisual2Formula, BVisual2Value> cache) {
		for (final ExpandedFormula expanded : expandedValues) {
			this.formulaStructureCache.putIfAbsent(expanded.getFormula(), expanded);
			cache.put(expanded.getFormula(), expanded.getValue());
			if (expanded.getChildren() != null) {
				addFormulaValuesToCache(expanded.getChildren(), cache);
			}
		}
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

	/**
	 * Evaluate the given formulas recursively and cache their values.
	 * 
	 * @param formulas the formulas for which to recursively cache the values
	 * @param state the state in which to evaluate the formulas
	 */
	private void cacheFormulaValuesRecursive(final List<BVisual2Formula> formulas, final State state) {
		addFormulaValuesToCache(BVisual2Formula.expandMultiple(formulas, state), getFormulaValueCacheForState(state));
	}

	private void addSubformulaItems(final TreeItem<StateItem> treeItem, final List<BVisual2Formula> subformulas, final State currentState, final State previousState, final String filter) {
		// Generate the tree items for treeItem's children right away.
		// This must be done even if treeItem is not expanded, because otherwise treeItem would have no child items and thus no expansion arrow, even if it actually has subformulas.
		final List<TreeItem<StateItem>> children = subformulas.stream()
			.map(f -> new TreeItem<>(new StateItem(this.expandFormulaWithCaching(f), currentState, previousState, this::evaluateFormulaWithCaching)))
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
					// TODO Don't evaluate subformulas that will be removed by the filter
					cacheMissingFormulaValues(subformulas, currentState);
					if (previousState != null) {
						cacheMissingFormulaValues(subformulas, previousState);
					}
					for (final TreeItem<StateItem> subTreeItem : children) {
						// If treeItem or subTreeItem matches the filter, don't filter subTreeItem's children.
						final String subFilter;
						if (
							(treeItem.getValue() != null && matchesFilter(filter, treeItem.getValue().getLabel()))
							|| matchesFilter(filter, subTreeItem.getValue().getLabel())
						) {
							subFilter = "";
						} else {
							subFilter = filter;
						}
						addSubformulaItems(subTreeItem, subTreeItem.getValue().getSubformulas(), currentState, previousState, subFilter);
					}
					treeItem.expandedProperty().removeListener(this);
				}
			}
		};
		treeItem.expandedProperty().addListener(generateGrandchildrenListener);

		// This listener tracks the expanded and visible states of each formula.
		// Unlike the previous listener, this one does *not* remove itself after it runs.
		final ChangeListener<Boolean> trackExpandedVisibleListener = (o, from, to) -> {
			// The root item has its value set to null. Its expanded state does not need to be stored.
			// Don't track expanded states while a filter is present, because the filtering code automatically expands items.
			if (treeItem.getValue() != null && filter.isEmpty()) {
				final BVisual2Formula formula = treeItem.getValue().getFormula();
				if (to) {
					expandedFormulas.add(formula);
				} else {
					expandedFormulas.remove(formula);
				}
			}

			if (to) {
				visibleFormulas.addAll(subformulas);
			} else {
				visibleFormulas.removeAll(subformulas);

				// When treeItem is collapsed, also collapse all of its children.
				// This will recurse automatically as the children's expanded listeners are fired.
				// JavaFX does not do this automatically by default,
				// which leads to problems with visibleFormulas containing formulas where the parent is expanded,
				// but (for example) the grandparent is collapsed.
				// In that case our code incorrectly thinks that the children are visible, even though they aren't.
				treeItem.getChildren().forEach(subTreeItem -> subTreeItem.setExpanded(false));
			}
		};
		treeItem.expandedProperty().addListener(trackExpandedVisibleListener);

		// If treeItem is already expanded, immediately fire the appropriate listeners.
		if (treeItem.isExpanded()) {
			generateGrandchildrenListener.changed(treeItem.expandedProperty(), false, true);
			trackExpandedVisibleListener.changed(treeItem.expandedProperty(), false, true);
		}

		treeItem.getChildren().setAll(children);

		// If treeItem does not match the filter, expand it (to set up its children's listeners), then filter its children.
		if (treeItem.getValue() == null || !matchesFilter(filter, treeItem.getValue().getLabel())) {
			treeItem.setExpanded(true);
			for (final Iterator<TreeItem<StateItem>> it = treeItem.getChildren().iterator(); it.hasNext();) {
				final TreeItem<StateItem> subTreeItem = it.next();
				// If subTreeItem does not match the filter, expand it.
				// This will fire the listeners to generate and filter its children.
				if (!matchesFilter(filter, subTreeItem.getValue().getLabel())) {
					subTreeItem.setExpanded(true);
					// If subTreeItem's children (which have already been filtered) are empty, remove subTreeItem,
					// because it and its children do not match the filter.
					if (subTreeItem.getChildren().isEmpty()) {
						it.remove();
					}
				}
			}
		} else {
			// Restore the previous expanded state of treeItem.
			// If this expands treeItem, this triggers the listener above.
			// The root item has its value set to null. It should always be expanded.
			treeItem.setExpanded(treeItem.getValue() == null || this.expandedFormulas.contains(treeItem.getValue().getFormula()));
		}
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
			this.visibleFormulas.clear();
			this.formulaStructureCache.clear();
			this.formulaValueCache.clear();
			return;
		}

		if (from == null || !from.getStateSpace().equals(to.getStateSpace())) {
			this.expandedFormulas.clear();
			this.visibleFormulas.clear();
			this.formulaStructureCache.clear();
			this.formulaValueCache.clear();
		} else {
			// Pre-cache the current and previous values of all visible formulas.
			// The structures have already been cached when the formulas first became visible.
			cacheMissingFormulaValues(this.visibleFormulas, to.getCurrentState());
			if (to.canGoBack()) {
				cacheMissingFormulaValues(this.visibleFormulas, to.getPreviousState());
			}
		}

		final int selectedRow = tv.getSelectionModel().getSelectedIndex();

		final List<BVisual2Formula> topLevel = BVisual2Formula.getTopLevel(to.getStateSpace());
		// If there is a filter, recursively expand and evaluate the entire tree beforehand.
		// The filtering code usually has to expand most of the tree to search for matching items, so this improves performance when filtering.
		// TODO Only expand the structure and don't evaluate the values
		// (not possible here yet, because the filtering code currently still evaluates all formulas, even if they don't match the filter)
		if (!filter.isEmpty()) {
			cacheFormulaValuesRecursive(topLevel, to.getCurrentState());
			if (to.canGoBack()) {
				cacheFormulaValuesRecursive(topLevel, to.getPreviousState());
			}
		}
		final TreeItem<StateItem> newRoot = createRootItem();
		addSubformulaItems(newRoot, topLevel, to.getCurrentState(), to.canGoBack() ? to.getPreviousState() : null, filter);
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
