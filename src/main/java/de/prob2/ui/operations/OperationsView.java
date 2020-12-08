package de.prob2.ui.operations;

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
import de.prob2.ui.dynamic.table.ExpressionTableView;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.BackgroundUpdater;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentTrace;
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
import javafx.scene.layout.VBox;
import org.controlsfx.glyphfont.FontAwesome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sawano.java.text.AlphanumericComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@FXMLInjected
@Singleton
public final class OperationsView extends VBox {
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

			final MenuItem showDetailsItem = new MenuItem(bundle.getString("operations.operationsView.contextMenu.items.showDetails"));
			showDetailsItem.setOnAction(event -> {
				final OperationDetailsStage stage = injector.getInstance(OperationDetailsStage.class);
				stage.setItem(this.getItem());
				stage.show();
			});

			final MenuItem executeByPredicateItem = new MenuItem(bundle.getString("operations.operationsView.contextMenu.items.executeByPredicate"));
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
				setText(item.toPrettyString(unambiguousToggle.isSelected()));
				setDisable(false);
				final FontAwesome.Glyph icon;
				switch (item.getStatus()) {
				case TIMEOUT:
					icon = FontAwesome.Glyph.CLOCK_ALT;
					getStyleClass().add("timeout");
					setTooltip(new Tooltip(bundle.getString("operations.operationsView.tooltips.timeout")));
					break;

				case ENABLED:
					icon = item.isSkip() ? FontAwesome.Glyph.REPEAT : FontAwesome.Glyph.PLAY;
					getStyleClass().add("enabled");
					if (!item.isExplored()) {
						getStyleClass().add("unexplored");
						setTooltip(new Tooltip(bundle.getString("operations.operationsView.tooltips.reachesUnexplored")));
					} else if (item.isErrored()) {
						getStyleClass().add("errored");
						setTooltip(new Tooltip(bundle.getString("operations.operationsView.tooltips.reachesErrored")));
					} else if (item.isSkip()) {
						getStyleClass().add("skip");
						setTooltip(new Tooltip(bundle.getString("operations.operationsView.tooltips.reachesSame")));
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
	private Button sortButton;
	@FXML
	private ToggleButton disabledOpsToggle;
	@FXML
	private ToggleButton unambiguousToggle;
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
	private Button cancelButton;
	@FXML
	private Button btComputeUnsatCore;

	private final List<OperationItem> events = new ArrayList<>();
	private final BooleanProperty showDisabledOps;
	private final BooleanProperty showUnambiguous;
	private final ObjectProperty<OperationsView.SortMode> sortMode;
	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final ResourceBundle bundle;
	private final DisablePropertyController disablePropertyController;
	private final StageManager stageManager;
	private final Config config;
	private final Comparator<CharSequence> alphanumericComparator;
	private final BackgroundUpdater updater;
	private final ObjectProperty<Thread> randomExecutionThread;
	private final AtomicBoolean needsUpdateAfterBusy;

	@Inject
	private OperationsView(final CurrentTrace currentTrace, final Locale locale, final StageManager stageManager,
						   final Injector injector, final ResourceBundle bundle, final StatusBar statusBar, final DisablePropertyController disablePropertyController,
						   final StopActions stopActions, final Config config) {
		this.showDisabledOps = new SimpleBooleanProperty(this, "showDisabledOps", true);
		this.showUnambiguous = new SimpleBooleanProperty(this, "showUnambiguous", false);
		this.sortMode = new SimpleObjectProperty<>(this, "sortMode", OperationsView.SortMode.MODEL_ORDER);
		this.currentTrace = currentTrace;
		this.alphanumericComparator = new AlphanumericComparator(locale);
		this.injector = injector;
		this.bundle = bundle;
		this.disablePropertyController = disablePropertyController;
		this.stageManager = stageManager;
		this.config = config;
		this.updater = new BackgroundUpdater("OperationsView Updater");
		this.randomExecutionThread = new SimpleObjectProperty<>(this, "randomExecutionThread", null);
		this.needsUpdateAfterBusy = new AtomicBoolean(false);
		stopActions.add(this.updater::shutdownNow);
		statusBar.addUpdatingExpression(this.updater.runningProperty());
		disablePropertyController.addDisableExpression(this.updater.runningProperty());
		disablePropertyController.addDisableExpression(this.randomExecutionThread.isNotNull());
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

		searchBar.textProperty().addListener((o, from, to) -> opsListView.getItems().setAll(applyFilter(to)));

		randomButton.disableProperty().bind(currentTrace.isNull().or(randomExecutionThread.isNotNull()).or(disablePropertyController.disableProperty()));
		randomButton.visibleProperty().bind(randomExecutionThread.isNull());
		cancelButton.visibleProperty().bind(randomExecutionThread.isNotNull());

		randomText.textProperty().addListener((observable, from, to) -> {
			if (!NUMBER_OR_EMPTY_PATTERN.matcher(to).matches() && NUMBER_OR_EMPTY_PATTERN.matcher(from).matches()) {
				((StringProperty) observable).set(from);
			}
		});

		final ChangeListener<Trace> traceChangeListener = (observable, from, to) -> {
			boolean showUnsatCoreButton = false;
			if (to != null) {
				final Set<Transition> operations = to.getNextTransitions(true, FormulaExpand.TRUNCATE);
				if ((!to.getCurrentState().isInitialised() && operations.isEmpty()) ||
						operations.stream().map(Transition::getName).collect(Collectors.toList()).contains(Transition.PARTIAL_SETUP_CONSTANTS_NAME)) {
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
			((BindableGlyph)disabledOpsToggle.getGraphic()).setIcon(to ? FontAwesome.Glyph.EYE : FontAwesome.Glyph.EYE_SLASH);
			disabledOpsToggle.setSelected(to);
			update(currentTrace.get());
		});

		showUnambiguous.addListener((o, from, to) -> {
			((BindableGlyph)unambiguousToggle.getGraphic()).setIcon(to ? FontAwesome.Glyph.PLUS_SQUARE : FontAwesome.Glyph.MINUS_SQUARE);
			unambiguousToggle.setSelected(to);
			opsListView.refresh();
		});

		sortMode.addListener((o, from, to) -> {
			final FontAwesome.Glyph icon;
			switch (to) {
				case A_TO_Z:
					icon = FontAwesome.Glyph.SORT_ALPHA_ASC;
					break;
				
				case Z_TO_A:
					icon = FontAwesome.Glyph.SORT_ALPHA_DESC;
					break;
				
				case MODEL_ORDER:
					icon = FontAwesome.Glyph.SORT;
					break;
				
				default:
					throw new IllegalStateException("Unhandled sort mode: " + to);
			}
			((BindableGlyph)sortButton.getGraphic()).setIcon(icon);

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
		if (
			item != null
			&& item.getStatus() == OperationItem.Status.ENABLED
			&& item.getTransition().getSource().equals(trace.getCurrentState())
		) {
			Trace forward = trace.forward();
			if(forward != null && item.getTransition().equals(forward.getCurrentTransition())) {
				currentTrace.set(trace.forward());
				return;
			}
			currentTrace.set(trace.add(item.getTransition()));
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
		final Set<Transition> operations = trace.getNextTransitions(true, FormulaExpand.EXPAND);
		events.addAll(OperationItem.computeUnambiguousConstantsAndVariables(
			OperationItem.forTransitions(trace.getStateSpace(), operations)
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
			text = bundle.getString("operations.operationsView.warningLabel.maxReached");
		} else if (!trace.getCurrentState().isInitialised() && operations.isEmpty()) {
			text = bundle.getString("operations.operationsView.warningLabel.noSetupConstantsOrInit");
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
	private void handleDisabledOpsToggle() {
		this.setShowDisabledOps(disabledOpsToggle.isSelected());
	}

	@FXML
	private void handleUnambiguousToggle() {
		this.setShowUnambiguous(unambiguousToggle.isSelected());
	}

	private List<OperationItem> applyFilter(final String filter) {
		return events.stream().filter(op -> op.getPrettyName().toLowerCase().contains(filter.toLowerCase()))
				.collect(Collectors.toList());
	}

	private void doSort(final LoadedMachine loadedMachine) {
		final Comparator<OperationItem> comparator;
		switch (this.getSortMode()) {
		case MODEL_ORDER:
			comparator = this.modelOrderComparator(new ArrayList<String>(loadedMachine.getOperationNames()));
			break;

		case A_TO_Z:
			comparator = this::compareAlphanumeric;
			break;

		case Z_TO_A:
			comparator = ((Comparator<OperationItem>) this::compareAlphanumeric).reversed();
			break;

		default:
			throw new IllegalStateException("Unhandled sort mode: " + this.getSortMode());
		}

		events.sort(comparator);
	}

	@FXML
	private void handleSortButton() {
		switch (this.getSortMode()) {
		case MODEL_ORDER:
			this.setSortMode(OperationsView.SortMode.A_TO_Z);
			break;

		case A_TO_Z:
			this.setSortMode(OperationsView.SortMode.Z_TO_A);
			break;

		case Z_TO_A:
			this.setSortMode(OperationsView.SortMode.MODEL_ORDER);
			break;

		default:
			throw new IllegalStateException("Unhandled sort mode: " + this.getSortMode());
		}
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
					"operations.operationsView.alerts.invalidNumberOfOparations.header",
					"operations.operationsView.alerts.invalidNumberOfOparations.content", randomInput);
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
		
		final Thread executionThread = new Thread(() -> {
			try {
				final Trace trace = currentTrace.get();
				if (trace != null) {
					currentTrace.set(trace.randomAnimation(operationCount));
				}
			} finally {
				randomExecutionThread.set(null);
			}
		}, "Random Operation Executor");
		randomExecutionThread.set(executionThread);
		executionThread.start();
	}

	@FXML
	private void cancel() {
		currentTrace.getStateSpace().sendInterrupt();
		if(randomExecutionThread.get() != null) {
			randomExecutionThread.get().interrupt();
			randomExecutionThread.set(null);
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
		ExpressionTableView expressionTableView = injector.getInstance(ExpressionTableView.class);
		expressionTableView.selectCommand(TableVisualizationCommand.UNSAT_CORE_PROPERTIES_NAME);
		expressionTableView.show();
		expressionTableView.toFront();
	}
}
