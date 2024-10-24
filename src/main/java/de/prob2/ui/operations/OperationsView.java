package de.prob2.ui.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.TableVisualizationCommand;
import de.prob.statespace.LoadedMachine;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.dynamic.DynamicVisualizationStage;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.internal.executor.BackgroundUpdater;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.internal.executor.FxThreadExecutor;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.interactive.UIInteractionHandler;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;
import de.prob2.ui.statusbar.StatusBar;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import org.controlsfx.glyphfont.FontAwesome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sawano.java.text.AlphanumericComparator;

@FXMLInjected
@Singleton
public final class OperationsView extends BorderPane {
	public enum SortMode {
		MODEL_ORDER, A_TO_Z, Z_TO_A
	}

	private final class OperationsCell extends ListCell<OperationItem> {
		public OperationsCell() {
			super();

			getStyleClass().add("operations-cell");

			this.setOnMouseClicked(event -> {
				if (event.getButton() == MouseButton.PRIMARY && !event.isControlDown()) {
					executeOperationIfPossible(this.getItem());
				}
			});

			final MenuItem showDetailsItem = new MenuItem(i18n.translate("operations.operationsView.contextMenu.items.showDetails"));
			showDetailsItem.setOnAction(event -> {
				final OperationDetailsStage stage = injector.getInstance(OperationDetailsStage.class);
				stage.setItem(this.getItem());
				stage.show();
			});

			final MenuItem executeByPredicateItem = new MenuItem(i18n.translate("operations.operationsView.contextMenu.items.executeByPredicate"));
			executeByPredicateItem.setOnAction(event -> {
				final ExecuteByPredicateStage stage = injector.getInstance(ExecuteByPredicateStage.class);
				stage.setItem(this.getItem());
				stage.show();
			});
			this.setContextMenu(new ContextMenu(showDetailsItem, executeByPredicateItem));
		}

		@Override
		protected void updateItem(OperationItem item, boolean empty) {
			super.updateItem(item, empty);
			getStyleClass().removeAll("enabled", "timeout", "unexplored", "errored", "skip", "normal", "disabled");
			if (item != null && !empty) {
				setText(item.toPrettyString(showUnambiguous.get()));
				setDisable(false);
				final FontAwesome.Glyph icon;
				switch (item.getStatus()) {
				case TIMEOUT:
					icon = FontAwesome.Glyph.CLOCK_ALT;
					getStyleClass().add("timeout");
					setTooltip(new Tooltip(i18n.translate("operations.operationsView.tooltips.timeout")));
					break;

				case ENABLED:
					icon = item.isSkip() ? FontAwesome.Glyph.REPEAT : FontAwesome.Glyph.PLAY;
					getStyleClass().add("enabled");
					if (!item.isExplored()) {
						getStyleClass().add("unexplored");
						setTooltip(new Tooltip(i18n.translate("operations.operationsView.tooltips.reachesUnexplored")));
					} else if (item.isErrored()) {
						getStyleClass().add("errored");
						setTooltip(new Tooltip(i18n.translate("operations.operationsView.tooltips.reachesErrored")));
					} else if (item.isSkip()) {
						getStyleClass().add("skip");
						setTooltip(new Tooltip(i18n.translate("operations.operationsView.tooltips.reachesSame")));
					} else {
						getStyleClass().add("normal");
						setTooltip(null);
					}
					break;

				case DISABLED:
					icon = FontAwesome.Glyph.MINUS_CIRCLE;
					getStyleClass().add("disabled");
					setTooltip(null);
					break;

				default:
					throw new IllegalStateException("Unhandled status: " + item.getStatus());
				}
				final BindableGlyph graphic = new BindableGlyph("FontAwesome", icon);
				graphic.bindableFontSizeProperty().bind(injector.getInstance(FontSize.class).fontSizeProperty());
				setGraphic(graphic);
			} else {
				setDisable(true);
				setGraphic(null);
				setText(null);
			}
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(OperationsView.class);

	// Matches empty string or number
	private static final Pattern NUMBER_OR_EMPTY_PATTERN = Pattern.compile("^$|^\\d+$");

	@FXML
	private ListView<OperationItem> opsListView;
	@FXML
	private Label warningLabel;
	@FXML
	private CheckMenuItem sortModeAToZ;
	@FXML
	private CheckMenuItem sortModeZToA;
	@FXML
	private CheckMenuItem sortModeModelOrder;
	@FXML
	private MenuItem disabledOpsMenuItem;
	@FXML
	private MenuItem unambiguousMenuItem;
	@FXML
	private TextField searchBar;
	@FXML
	private TextField randomText;
	@FXML
	private MenuButton randomButton;
	@FXML
	private MenuItem oneRandomEvent;
	@FXML
	private MenuItem fiveRandomEvents;
	@FXML
	private MenuItem tenRandomEvents;
	@FXML
	private CustomMenuItem someRandomEvents;
	@FXML
	private HelpButton helpButton;
	@FXML
	private Button btComputeUnsatCore;
	@FXML
	private ToggleButton searchToggle;
	@FXML
	private VBox searchBox;

	private final List<OperationItem> events = new ArrayList<>();
	private final BooleanProperty showDisabledOps;
	private final BooleanProperty showUnambiguous;
	private final ObjectProperty<OperationsView.SortMode> sortMode;
	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final I18n i18n;
	private final DisablePropertyController disablePropertyController;
	private final StageManager stageManager;
	private final Config config;
	private final CliTaskExecutor cliExecutor;
	private final FxThreadExecutor fxExecutor;
	private final Comparator<CharSequence> alphanumericComparator;
	private final BackgroundUpdater updater;
	private final AtomicBoolean needsUpdateAfterBusy;

	@Inject
	private OperationsView(final CurrentTrace currentTrace, final Locale locale, final StageManager stageManager, final Injector injector, final I18n i18n, final StatusBar statusBar, final DisablePropertyController disablePropertyController, final StopActions stopActions, final Config config, final CliTaskExecutor cliExecutor, FxThreadExecutor fxExecutor) {
		this.showDisabledOps = new SimpleBooleanProperty(this, "showDisabledOps", true);
		this.showUnambiguous = new SimpleBooleanProperty(this, "showUnambiguous", false);
		this.sortMode = new SimpleObjectProperty<>(this, "sortMode", OperationsView.SortMode.MODEL_ORDER);
		this.currentTrace = currentTrace;
		this.alphanumericComparator = new AlphanumericComparator(locale);
		this.injector = injector;
		this.i18n = i18n;
		this.disablePropertyController = disablePropertyController;
		this.stageManager = stageManager;
		this.config = config;
		this.cliExecutor = cliExecutor;
		this.fxExecutor = fxExecutor;
		this.updater = new BackgroundUpdater("OperationsView Updater");
		this.needsUpdateAfterBusy = new AtomicBoolean(false);
		stopActions.add(this.updater::shutdownNow);
		statusBar.addUpdatingExpression(this.updater.runningProperty());
		disablePropertyController.addDisableExpression(this.updater.runningProperty());
		stageManager.loadFXML(this, "operations_view.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent("operations", null);
		opsListView.setCellFactory(lv -> new OperationsCell());
		opsListView.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				executeOperationIfPossible(opsListView.getSelectionModel().getSelectedItem());
			}
		});
		opsListView.disableProperty().bind(disablePropertyController.disableProperty());
		this.sortModeAToZ.setSelected(true);
		searchBox.visibleProperty().bind(searchToggle.selectedProperty());
		searchBox.managedProperty().bind(searchToggle.selectedProperty());
		searchBar.textProperty().addListener((o, from, to) -> opsListView.getItems().setAll(applyFilter(to)));

		randomButton.disableProperty().bind(currentTrace.isNull().or(cliExecutor.runningProperty()).or(disablePropertyController.disableProperty()));

		randomText.textProperty().addListener((observable, from, to) -> {
			if (!NUMBER_OR_EMPTY_PATTERN.matcher(to).matches() && NUMBER_OR_EMPTY_PATTERN.matcher(from).matches()) {
				((StringProperty) observable).set(from);
			}
		});

		final ChangeListener<Trace> traceChangeListener = (observable, from, to) -> {
			boolean showUnsatCoreButton = false;
			if (to != null) {
				final Set<Transition> operations = to.getNextTransitions();
				if ((!to.getCurrentState().isInitialised() && operations.isEmpty()) ||
						operations.stream().map(Transition::getName).collect(Collectors.toSet()).contains(Transition.PARTIAL_SETUP_CONSTANTS_NAME)) {
					showUnsatCoreButton = true;
				}
			}
			btComputeUnsatCore.setVisible(showUnsatCoreButton);
			btComputeUnsatCore.setManaged(showUnsatCoreButton);
			//opsListView is always visible. In the case that partial setup constants can be executed, the button for showing unsat core is still visible.
		};
		traceChangeListener.changed(this.currentTrace, null, currentTrace.get());
		this.currentTrace.addListener(traceChangeListener);

		this.update(currentTrace.get());
		currentTrace.addListener((observable, from, to) -> update(to));
		currentTrace.addStatesCalculatedListener(newOps -> update(currentTrace.get()));
		currentTrace.animatorBusyProperty().addListener((o, from, to) -> {
			if (!to && this.needsUpdateAfterBusy.getAndSet(false)) {
				update(currentTrace.get());
			}
		});

		showDisabledOps.addListener((o, from, to) -> {
			disabledOpsMenuItem.setText(to ? i18n.translate("operations.operationsView.menu.hideDisabled") : i18n.translate("operations.operationsView.menu.showDisabled"));
			showDisabledOps.set(to);
			update(currentTrace.get());
		});

		showUnambiguous.addListener((o, from, to) -> {
			unambiguousMenuItem.setText(to ? i18n.translate("operations.operationsView.menu.hideUnambiguous") : i18n.translate("operations.operationsView.menu.showUnambiguous"));
			opsListView.refresh();
		});

		sortMode.addListener((o, from, to) -> {
			this.sortModeAToZ.setSelected(false);
			this.sortModeZToA.setSelected(false);
			this.sortModeModelOrder.setSelected(false);
			switch (to) {
				case A_TO_Z:
					this.sortModeAToZ.setSelected(true);
					break;

				case Z_TO_A:
					this.sortModeZToA.setSelected(true);
					break;

				case MODEL_ORDER:
					this.sortModeModelOrder.setSelected(true);
					break;

				default:
					throw new IllegalStateException("Unhandled sort mode: " + to);
			}

			if(currentTrace.get() != null) {
				doSort(currentTrace.get().getStateSpace().getLoadedMachine());
			}
			opsListView.getItems().setAll(applyFilter(searchBar.getText()));
		});

		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				if (configData.operationsSortMode != null) {
					setSortMode(configData.operationsSortMode);
				}
				
				setShowDisabledOps(configData.operationsShowDisabled);
				setShowUnambiguous(configData.operationsShowUnambiguous);
			}
			
			@Override
			public void saveConfig(final ConfigData configData) {
				configData.operationsSortMode = getSortMode();
				configData.operationsShowDisabled = getShowDisabledOps();
				configData.operationsShowUnambiguous = getShowUnambiguous();
			}
		});
	}

	private void executeOperationIfPossible(final OperationItem item) {
		final Trace trace = currentTrace.get();
		RealTimeSimulator realTimeSimulator = injector.getInstance(RealTimeSimulator.class);
		if (
			item != null
			&& item.getStatus() == OperationItem.Status.ENABLED
			&& item.getTransition().getSource().equals(trace.getCurrentState())
		) {
			UIInteractionHandler uiInteraction = injector.getInstance(UIInteractionHandler.class);
			// Use the CLI executor for executing the operation to avoid blocking the UI thread.
			// Executing the operation itself isn't slow (because the transition is already known),
			// but changing the current trace can be slow,
			// because the destination state may not be explored yet.
			// TODO This might be better solved by moving the state exploring out of CurrentTrace.
			cliExecutor.execute(() -> {
				Trace forward = trace.forward();
				if (forward != null && item.getTransition().equals(forward.getCurrentTransition())) {
					currentTrace.set(forward);
					uiInteraction.addUserInteraction(realTimeSimulator, forward.getCurrentTransition());
				} else {
					currentTrace.set(trace.add(item.getTransition()));
					uiInteraction.addUserInteraction(realTimeSimulator, item.getTransition());
				}
			});
		}
	}

	private void update(final Trace trace) {
		if (trace == null) {
			opsListView.getItems().clear();
		} else if (currentTrace.isAnimatorBusy()) {
			// If the animator is currently busy,
			// delay updating the operations view until it is no longer busy,
			// to avoid hangs or constant reloading when long sequences of commands are executing.
			this.needsUpdateAfterBusy.set(true);
		} else {
			this.updater.execute(() -> this.updateBG(trace));
		}
	}

	private synchronized void updateBG(final Trace trace) {
		events.clear();
		final Set<Transition> operations = trace.getNextTransitions();
		trace.getStateSpace().evaluateTransitions(operations, FormulaExpand.EXPAND);
		events.addAll(OperationItem.computeUnambiguousConstantsAndVariables(
			OperationItem.forTransitions(trace.getStateSpace(), operations).values()
		));
		
		final LoadedMachine loadedMachine = trace.getStateSpace().getLoadedMachine();
		final Set<String> disabled = loadedMachine.getOperationNames().stream()
			.map(loadedMachine::getMachineOperationInfo)
			.filter(OperationInfo::isTopLevel)
			.map(OperationInfo::getOperationName)
			.collect(Collectors.toSet());
		disabled.removeAll(operations.stream().map(Transition::getName).collect(Collectors.toSet()));
		final Set<String> withTimeout = trace.getCurrentState().getTransitionsWithTimeout();
		showDisabledAndWithTimeout(loadedMachine, disabled, withTimeout);

		doSort(loadedMachine);

		final String text;
		if (trace.getCurrentState().isMaxTransitionsCalculated()) {
			if (!trace.getCurrentState().isInitialised()){
				text = i18n.translate("operations.operationsView.warningLabel.maxInitialisationsReached");
			} else {
				text = i18n.translate("operations.operationsView.warningLabel.maxOperationsReached");
			}
		} else if (trace.getCurrentState().isTimeoutOccurred()) {
			text = i18n.translate("operations.operationsView.warningLabel.timeoutOccurred");
		} else if (!trace.getCurrentState().isInitialised() && operations.isEmpty()) {
			text = i18n.translate("operations.operationsView.warningLabel.noSetupConstantsOrInit");
		} else {
			text = "";
		}
		Platform.runLater(() -> warningLabel.setText(text));

		final List<OperationItem> filtered = applyFilter(searchBar.getText());

		Platform.runLater(() -> opsListView.getItems().setAll(filtered));
	}

	private void showDisabledAndWithTimeout(final LoadedMachine loadedMachine, final Set<String> notEnabled, final Set<String> withTimeout) {
		if (this.getShowDisabledOps()) {
			for (String s : notEnabled) {
				if (!Transition.INITIALISE_MACHINE_NAME.equals(s)) {
					events.add(OperationItem.forDisabled(
						s, withTimeout.contains(s) ? OperationItem.Status.TIMEOUT : OperationItem.Status.DISABLED, loadedMachine.getMachineOperationInfo(s).getParameterNames()
					));
				}
			}
		}
		for (String s : withTimeout) {
			if (!notEnabled.contains(s)) {
				events.add(OperationItem.forDisabled(s, OperationItem.Status.TIMEOUT, Collections.emptyList()));
			}
		}
	}

	private int compareParams(final List<String> left, final List<String> right) {
		int minSize = Math.min(left.size(), right.size());
		for (int i = 0; i < minSize; i++) {
			int cmp = alphanumericComparator.compare(left.get(i), right.get(i));
			if (cmp != 0) {
				return cmp;
			}
		}

		// All elements are equal up to the end of the smaller list, order based
		// on which one has more elements.
		return Integer.compare(left.size(), right.size());
	}

	private int compareAlphanumeric(final OperationItem left, final OperationItem right) {
		if (left.getName().equals(right.getName())) {
			return compareParams(left.getParameterValues(), right.getParameterValues());
		} else {
			return alphanumericComparator.compare(left.getName(), right.getName());
		}
	}

	private Comparator<OperationItem> modelOrderComparator(final List<String> orderedOperationNames) {
		return (left, right) -> {
			if (left.getName().equals(right.getName())) {
				return compareParams(left.getParameterValues(), right.getParameterValues());
			} else {
				final int leftIndex = orderedOperationNames.indexOf(left.getName());
				final int rightIndex = orderedOperationNames.indexOf(right.getName());
				if (leftIndex == -1 && rightIndex == -1) {
					return left.getName().compareTo(right.getName());
				} else {
					return Integer.compare(leftIndex, rightIndex);
				}
			}
		};
	}

	@FXML
	private void handleDisabledOpsMenuItem() {
		this.setShowDisabledOps(!this.showDisabledOps.get());
	}

	@FXML
	private void handleUnambiguousMenuItem() {
		this.setShowUnambiguous(!this.showUnambiguous.get());
	}

	private List<OperationItem> applyFilter(final String filter) {
		return events.stream().filter(op -> op.getPrettyName().toLowerCase().contains(filter.toLowerCase()))
				.collect(Collectors.toList());
	}

	private void doSort(final LoadedMachine loadedMachine) {
		final Comparator<OperationItem> comparator = switch (this.getSortMode()) {
			case MODEL_ORDER -> this.modelOrderComparator(new ArrayList<>(loadedMachine.getOperationNames()));
			case A_TO_Z -> this::compareAlphanumeric;
			case Z_TO_A -> ((Comparator<OperationItem>) this::compareAlphanumeric).reversed();
		};

		events.sort(comparator);
	}

	@FXML
	private void setSortModeAToZ(){
		this.setSortMode(OperationsView.SortMode.A_TO_Z);
	}

	@FXML
	private void setSortModeZToA(){
		this.setSortMode(OperationsView.SortMode.Z_TO_A);
	}

	@FXML
	private void setSortModeToModelOrder(){
		this.setSortMode(OperationsView.SortMode.MODEL_ORDER);
	}

	@FXML
	public void random(ActionEvent event) {
		final int operationCount;
		if (event.getSource().equals(randomText)) {
			final String randomInput = randomText.getText();
			if (randomInput.isEmpty()) {
				return;
			}
			try {
				operationCount = Integer.parseInt(randomInput);
			} catch (NumberFormatException e) {
				LOGGER.error("Invalid input for executing random number of events",e);
				final Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING,
						"operations.operationsView.alerts.invalidNumberOfOperations.header",
						"operations.operationsView.alerts.invalidNumberOfOperations.content", randomInput);
				alert.initOwner(this.getScene().getWindow());
				alert.showAndWait();
				return;
			}
		} else if (event.getSource().equals(oneRandomEvent)) {
			operationCount = 1;
		} else if (event.getSource().equals(fiveRandomEvents)) {
			operationCount = 5;
		} else if (event.getSource().equals(tenRandomEvents)) {
			operationCount = 10;
		} else {
			throw new AssertionError("Unhandled random animation event source: " + event.getSource());
		}

		Trace currentTrace = this.currentTrace.get();
		if (currentTrace != null) {
			this.cliExecutor.submit(() -> currentTrace.randomAnimation(operationCount)).whenCompleteAsync((res, exc) -> {
				if (exc != null) {
					if (!(exc instanceof CancellationException)) {
						LOGGER.error("error while randomly animating", exc);
						this.stageManager.showUnhandledExceptionAlert(exc, this.getScene().getWindow());
					}
				} else if (res != null) {
					this.currentTrace.set(res);
				}
			}, this.fxExecutor);
		}
	}

	private OperationsView.SortMode getSortMode() {
		return this.sortMode.get();
	}

	private void setSortMode(final OperationsView.SortMode sortMode) {
		this.sortMode.set(sortMode);
	}
	
	private boolean getShowDisabledOps() {
		return this.showDisabledOps.get();
	}

	private void setShowDisabledOps(boolean showDisabledOps) {
		this.showDisabledOps.set(showDisabledOps);
	}
	
	private boolean getShowUnambiguous() {
		return this.showUnambiguous.get();
	}

	private void setShowUnambiguous(final boolean showUnambiguous) {
		this.showUnambiguous.set(showUnambiguous);
	}

	@FXML
	private void computeUnsatCore() {
		DynamicVisualizationStage expressionTableView = injector.getInstance(DynamicVisualizationStage.class);
		expressionTableView.show();
		expressionTableView.toFront();
		expressionTableView.selectCommand(TableVisualizationCommand.UNSAT_CORE_PROPERTIES_NAME);
	}

	@FXML
	private void clearSearchbarWhenHidden(){
		if (!searchToggle.isSelected()){
			searchBar.clear();
		}
	}
}
