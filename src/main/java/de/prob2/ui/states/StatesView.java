package de.prob2.ui.states;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.ComposedCommand;
import de.prob.animator.command.ExpandFormulaCommand;
import de.prob.animator.command.GetTopLevelFormulasCommand;
import de.prob.animator.domainobjects.BVisual2Value;
import de.prob.animator.domainobjects.EvaluationException;
import de.prob.animator.domainobjects.ExpandedFormula;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.FormulaId;
import de.prob.animator.domainobjects.IBEvalElement;
import de.prob.exception.ProBError;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
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
import de.prob2.ui.unsatcore.UnsatCoreCalculator;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public final class StatesView extends StackPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatesView.class);

	@FXML
	private TextField filterState;
	@FXML
	private HelpButton helpButton;
	@FXML
	private Button btComputeUnsatCore;

	@FXML
	private TreeTableView<StateItem> tv;
	@FXML
	private TreeTableColumn<StateItem, ExpandedFormula> tvName;
	@FXML
	private TreeTableColumn<StateItem, ExpandedFormula> tvValue;
	@FXML
	private TreeTableColumn<StateItem, ExpandedFormula> tvPreviousValue;
	@FXML
	private TreeItem<StateItem> tvRootItem;

	private final Injector injector;
	private final CurrentTrace currentTrace;
	private final StatusBar statusBar;
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final Config config;
	private final UnsatCoreCalculator unsatCoreCalculator;

	private final ExecutorService updater;
	private List<Double> columnWidthsToRestore;

	@Inject
	private StatesView(final Injector injector, final CurrentTrace currentTrace, final StatusBar statusBar,
					   final StageManager stageManager, final ResourceBundle bundle, final StopActions stopActions, final Config config, final UnsatCoreCalculator unsatCoreCalculator) {
		this.injector = injector;
		this.currentTrace = currentTrace;
		this.statusBar = statusBar;
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.config = config;

		this.updater = Executors.newSingleThreadExecutor(r -> new Thread(r, "StatesView Updater"));
		this.unsatCoreCalculator = unsatCoreCalculator;
		stopActions.add(this.updater::shutdownNow);

		stageManager.loadFXML(this, "states_view.fxml");
	}

	@FXML
	private void initialize() {
		helpButton.setHelpContent(this.getClass());
		
		tv.setRowFactory(view -> initTableRow());

		this.tvName.setCellFactory(col -> new NameCell());
		this.tvValue.setCellFactory(col -> new ValueCell(bundle));
		this.tvPreviousValue.setCellFactory(col -> new ValueCell(bundle));

		this.tvName.setCellValueFactory(data -> Bindings.select(data.getValue().valueProperty(), "current"));
		this.tvValue.setCellValueFactory(data -> Bindings.select(data.getValue().valueProperty(), "current"));
		this.tvPreviousValue.setCellValueFactory(data -> Bindings.select(data.getValue().valueProperty(), "previous"));

		final ExpandedFormula rootPlaceholderFormula = new ExpandedFormula(
			"Machine (this root item should be invisible)",
			BVisual2Value.Inactive.INSTANCE,
			null,
			Collections.emptyList()
		);
		this.tv.getRoot().setValue(new StateItem(rootPlaceholderFormula, rootPlaceholderFormula));

		final ChangeListener<Trace> traceChangeListener = (observable, from, to) -> this.updater.execute(() -> {
			boolean showUnsatCoreButton = false;
			if (to == null) {
				this.tv.getRoot().getChildren().clear();
			} else {
				this.updateRoot(to);
				final Set<Transition> operations = to.getNextTransitions(true, FormulaExpand.TRUNCATE);
				if ((!to.getCurrentState().isInitialised() && operations.isEmpty()) || 
						operations.stream().map(trans -> trans.getName()).collect(Collectors.toList()).contains("$partial_setup_constants")) {
					showUnsatCoreButton = true;
				}
			}
			btComputeUnsatCore.setVisible(showUnsatCoreButton);
			btComputeUnsatCore.setManaged(showUnsatCoreButton);
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

	private TreeTableRow<StateItem> initTableRow() {
		final TreeTableRow<StateItem> row = new TreeTableRow<>();

		row.itemProperty().addListener((observable, from, to) -> {
			row.getStyleClass().remove("changed");
			row.getStyleClass().remove("unsatCore");
			if (to != null) {
				IBEvalElement core = unsatCoreCalculator.unsatCoreProperty().get();
				if(core != null) {
					String code = core.getCode();
					List<String> coreConjuncts = Arrays.stream(code.split("&"))
							.map(str -> str.replace(" ", ""))
							.collect(Collectors.toList());
					if (coreConjuncts.contains(to.getCurrent().getLabel().replace(" ", ""))) {
						row.getStyleClass().add("unsatCore");
						return;
					}
				}

				if (!to.getCurrent().getValue().equals(to.getPrevious().getValue())) {
					row.getStyleClass().add("changed");
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
				formulaStage.visualizeFormula(row.getItem().getCurrent().getLabel());
				formulaStage.show();
			} catch (EvaluationException | ProBError e) {
				LOGGER.error("Could not visualize formula", e);
				stageManager.makeExceptionAlert(e, "states.statesView.alerts.couldNotVisualizeFormula.content").showAndWait();
			}
		});

		final MenuItem visualizeExpressionAsTableItem = new MenuItem(
				bundle.getString("states.statesView.contextMenu.items.visualizeExpressionTable"));
		visualizeExpressionAsTableItem.disableProperty().bind(row.itemProperty().isNull());
		visualizeExpressionAsTableItem.setOnAction(event -> {
			try {
				ExpressionTableView expressionTableView = injector.getInstance(ExpressionTableView.class);
				expressionTableView.visualizeExpression(getResultValue(row.getItem().getCurrent().getValue()));
				expressionTableView.show();
			} catch (EvaluationException | ProBError e) {
				LOGGER.error("Could not visualize formula", e);
				stageManager.makeExceptionAlert(e, "states.statesView.alerts.couldNotVisualizeFormula.content")
						.showAndWait();
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

	@FXML
	private void handleSearch() {
		this.updateRoot(currentTrace.get());
	}

	private static boolean matchesFilter(final String filter, final String string) {
		return string.toLowerCase().contains(filter.toLowerCase());
	}

	private static void buildNodes(final TreeItem<StateItem> treeItem, final List<ExpandedFormula> currentFormulas, final List<ExpandedFormula> previousFormulas, final String filter) {
		Objects.requireNonNull(treeItem);
		Objects.requireNonNull(currentFormulas);
		Objects.requireNonNull(previousFormulas);

		assert treeItem.getChildren().isEmpty();
		assert previousFormulas.isEmpty() || currentFormulas.size() == previousFormulas.size();

		for (int i = 0; i < currentFormulas.size(); i++) {
			final ExpandedFormula current = currentFormulas.get(i);
			final ExpandedFormula previous;
			if (previousFormulas.isEmpty()) {
				// Previous state not available, use a placeholder formula with an inactive value.
				previous = new ExpandedFormula(current.getLabel(), BVisual2Value.Inactive.INSTANCE, current.getId(), Collections.emptyList());
			} else {
				previous = previousFormulas.get(i);
			}
			final TreeItem<StateItem> subTreeItem = new TreeItem<>(new StateItem(current, previous));

			final boolean itemMatches = matchesFilter(filter, current.getLabel());
			// If this item matches the filter, don't filter its children.
			buildNodes(subTreeItem, current.getChildren(), previous.getChildren(), itemMatches ? "" : filter);

			// Only display this item if it or any of its children match the filter.
			if (itemMatches || !subTreeItem.getChildren().isEmpty()) {
				treeItem.getChildren().add(subTreeItem);
			}
		}
	}

	private void updateRoot(final Trace to) {
		final int selectedRow = tv.getSelectionModel().getSelectedIndex();

		Platform.runLater(() -> {
			this.statusBar.setStatesViewUpdating(true);
			this.tv.setDisable(true);
		});

		final GetTopLevelFormulasCommand getTopLevelCommand = new GetTopLevelFormulasCommand();
		to.getStateSpace().execute(getTopLevelCommand);
		final List<FormulaId> topLevel = getTopLevelCommand.getFormulaIds();
		
		final List<ExpandFormulaCommand> expandCurrentCommands = topLevel.stream()
			.map(id -> new ExpandFormulaCommand(id, to.getCurrentState()))
			.collect(Collectors.toList());
		final List<ExpandFormulaCommand> expandPreviousCommands;
		if (to.canGoBack()) {
			expandPreviousCommands = topLevel.stream()
				.map(id -> new ExpandFormulaCommand(id, to.getPreviousState()))
				.collect(Collectors.toList());
		} else {
			expandPreviousCommands = Collections.emptyList();
		}
		
		final List<ExpandFormulaCommand> allCommands = new ArrayList<>(expandCurrentCommands);
		allCommands.addAll(expandPreviousCommands);
		to.getStateSpace().execute(new ComposedCommand(allCommands));
		
		final List<ExpandedFormula> currentFormulas = expandCurrentCommands.stream()
			.map(ExpandFormulaCommand::getResult)
			.collect(Collectors.toList());
		final List<ExpandedFormula> previousFormulas = expandPreviousCommands.stream()
			.map(ExpandFormulaCommand::getResult)
			.collect(Collectors.toList());

		Platform.runLater(() -> {
			this.tvRootItem.getChildren().clear();
			buildNodes(this.tvRootItem, currentFormulas, previousFormulas, filterState.getText());
			this.tv.refresh();
			this.tv.getSelectionModel().select(selectedRow);
			this.tv.setDisable(false);
			this.statusBar.setStatesViewUpdating(false);
		});
	}

	private static void handleCopyName(final TreeItem<StateItem> item) {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		final ClipboardContent content = new ClipboardContent();
		content.putString(item.getValue().getCurrent().getLabel());
		clipboard.setContent(content);
	}

	private static String getResultValue(final BVisual2Value result) {
		if (result instanceof BVisual2Value.PredicateValue) {
			return String.valueOf(((BVisual2Value.PredicateValue)result).getValue());
		} else if (result instanceof BVisual2Value.ExpressionValue) {
			return ((BVisual2Value.ExpressionValue)result).getValue();
		} else if (result instanceof BVisual2Value.Error) {
			return ((BVisual2Value.Error)result).getMessage();
		} else if (result instanceof BVisual2Value.Inactive) {
			return "";
		} else {
			throw new IllegalArgumentException("Unknown eval result type: " + result.getClass());
		}
	}

	private void showDetails(final StateItem item) {
		final FullValueStage stage = injector.getInstance(FullValueStage.class);
		stage.setTitle(item.getCurrent().getLabel());
		stage.setCurrentValue(getResultValue(item.getCurrent().getValue()));
		stage.setPreviousValue(getResultValue(item.getPrevious().getValue()));
		stage.show();
	}
	
	@FXML
	private void computeUnsatCore() {
		unsatCoreCalculator.calculate();
		btComputeUnsatCore.setVisible(false);
		btComputeUnsatCore.setManaged(false);
		tv.refresh();
	}

}
