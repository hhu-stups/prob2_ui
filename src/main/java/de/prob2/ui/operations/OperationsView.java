package de.prob2.ui.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.LoadedMachine;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.statusbar.StatusBar;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sawano.java.text.AlphanumericComparator;

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
				setText(item.toPrettyString());
				setDisable(true);
				final FontAwesomeIconView icon;
				switch (item.getStatus()) {
				case TIMEOUT:
					icon = new FontAwesomeIconView(FontAwesomeIcon.CLOCK_ALT);
					getStyleClass().add("timeout");
					setTooltip(new Tooltip(bundle.getString("operations.operationsView.tooltips.timeout")));
					break;

				case ENABLED:
					icon = new FontAwesomeIconView(item.isSkip() ? FontAwesomeIcon.REPEAT : FontAwesomeIcon.PLAY);
					setDisable(false);
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
					icon = new FontAwesomeIconView(FontAwesomeIcon.MINUS_CIRCLE);
					getStyleClass().add("disabled");
					setTooltip(null);
					break;

				default:
					throw new IllegalStateException("Unhandled status: " + item.getStatus());
				}
				FontSize fontsize = injector.getInstance(FontSize.class);
				icon.glyphSizeProperty().bind(fontsize.fontSizeProperty());
				setGraphic(icon);
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

	private AbstractModel currentModel;
	private final List<String> opNames = new ArrayList<>();
	private final Map<String, List<String>> opToParams = new HashMap<>();
	private final List<OperationItem> events = new ArrayList<>();
	private final BooleanProperty showDisabledOps;
	private final BooleanProperty showUnambiguous;
	private final ObjectProperty<OperationsView.SortMode> sortMode;
	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final ResourceBundle bundle;
	private final StatusBar statusBar;
	private final StageManager stageManager;
	private final Config config;
	private final Comparator<CharSequence> alphanumericComparator;
	private final ExecutorService updater;
	private final ObjectProperty<Thread> randomExecutionThread;

	@Inject
	private OperationsView(final CurrentTrace currentTrace, final Locale locale, final StageManager stageManager,
						   final Injector injector, final ResourceBundle bundle, final StatusBar statusBar,
						   final StopActions stopActions, final Config config) {
		this.showDisabledOps = new SimpleBooleanProperty(this, "showDisabledOps", true);
		this.showUnambiguous = new SimpleBooleanProperty(this, "showUnambiguous", false);
		this.sortMode = new SimpleObjectProperty<>(this, "sortMode", OperationsView.SortMode.MODEL_ORDER);
		this.currentTrace = currentTrace;
		this.alphanumericComparator = new AlphanumericComparator(locale);
		this.injector = injector;
		this.bundle = bundle;
		this.statusBar = statusBar;
		this.stageManager = stageManager;
		this.config = config;
		this.updater = Executors.newSingleThreadExecutor(r -> new Thread(r, "OperationsView Updater"));
		this.randomExecutionThread = new SimpleObjectProperty<>(this, "randomExecutionThread", null);
		stopActions.add(this.updater::shutdownNow);

		stageManager.loadFXML(this, "operations_view.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent(this.getClass());
		opsListView.setCellFactory(lv -> new OperationsCell());
		opsListView.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER) {
				executeOperationIfPossible(opsListView.getSelectionModel().getSelectedItem());
			}
		});
		searchBar.textProperty().addListener((o, from, to) -> opsListView.getItems().setAll(applyFilter(to)));

		randomButton.disableProperty().bind(Bindings.or(currentTrace.existsProperty().not(), randomExecutionThread.isNotNull()));

		randomText.textProperty().addListener((observable, from, to) -> {
			if (!NUMBER_OR_EMPTY_PATTERN.matcher(to).matches() && NUMBER_OR_EMPTY_PATTERN.matcher(from).matches()) {
				((StringProperty) observable).set(from);
			}
		});

		this.update(currentTrace.get());
		currentTrace.addListener((observable, from, to) -> update(to));
		cancelButton.disableProperty().bind(randomExecutionThread.isNull());

		showDisabledOps.addListener((o, from, to) -> {
			((FontAwesomeIconView)disabledOpsToggle.getGraphic()).setIcon(to ? FontAwesomeIcon.EYE : FontAwesomeIcon.EYE_SLASH);
			disabledOpsToggle.setSelected(to);
			update(currentTrace.get());
		});

		showUnambiguous.addListener((o, from, to) -> {
			((FontAwesomeIconView)unambiguousToggle.getGraphic()).setIcon(to ? FontAwesomeIcon.PLUS_SQUARE : FontAwesomeIcon.MINUS_SQUARE);
			unambiguousToggle.setSelected(to);
			update(currentTrace.get());
		});

		sortMode.addListener((o, from, to) -> {
			final FontAwesomeIcon icon;
			switch (to) {
				case A_TO_Z:
					icon = FontAwesomeIcon.SORT_ALPHA_ASC;
					break;
				
				case Z_TO_A:
					icon = FontAwesomeIcon.SORT_ALPHA_DESC;
					break;
				
				case MODEL_ORDER:
					icon = FontAwesomeIcon.SORT;
					break;
				
				default:
					throw new IllegalStateException("Unhandled sort mode: " + to);
			}
			((FontAwesomeIconView)sortButton.getGraphic()).setIcon(icon);
			
			doSort();
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
		if (
			item != null
			&& item.getStatus() == OperationItem.Status.ENABLED
			&& item.getTransition().getSource().equals(currentTrace.getCurrentState())
		) {
			Trace forward = currentTrace.forward();
			if(forward != null && item.getTransition().equals(forward.getCurrentTransition())) {
				currentTrace.set(currentTrace.forward());
				return;
			}
			currentTrace.set(currentTrace.get().add(item.getTransition().getId()));
		}
	}

	public void update(final Trace trace) {
		if (trace == null) {
			currentModel = null;
			opNames.clear();
			opToParams.clear();
			opsListView.getItems().clear();
		} else {
			this.updater.execute(() -> this.updateBG(trace));
		}
	}

	private void updateBG(final Trace trace) {
		Platform.runLater(() -> {
			this.statusBar.setOperationsViewUpdating(true);
			this.opsListView.setDisable(true);
		});

		if (!trace.getModel().equals(currentModel)) {
			updateModel(trace);
		}

		events.clear();
		final Set<Transition> operations = trace.getNextTransitions(true, FormulaExpand.TRUNCATE);
		Collection<OperationItem> operationItems = OperationItem.forTransitions(trace.getStateSpace(), operations);
		if (!unambiguousToggle.isSelected()) {
			operationItems = OperationItem.removeUnambiguousConstantsAndVariables(operationItems);
		}
		events.addAll(operationItems);
		
		final Set<String> disabled = new HashSet<>(opNames);
		disabled.removeAll(operations.stream().map(Transition::getName).collect(Collectors.toSet()));
		final Set<String> withTimeout = trace.getCurrentState().getTransitionsWithTimeout();
		showDisabledAndWithTimeout(disabled, withTimeout);

		doSort();

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

		Platform.runLater(() -> {
			opsListView.getItems().setAll(filtered);
			this.opsListView.setDisable(false);
			this.statusBar.setOperationsViewUpdating(false);
		});
	}

	private void showDisabledAndWithTimeout(final Set<String> notEnabled, final Set<String> withTimeout) {
		if (this.getShowDisabledOps()) {
			for (String s : notEnabled) {
				if (!"$initialise_machine".equals(s)) {
					events.add(OperationItem.forDisabled(
						s, withTimeout.contains(s) ? OperationItem.Status.TIMEOUT : OperationItem.Status.DISABLED, opToParams.get(s)
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
		int minSize = left.size() < right.size() ? left.size() : right.size();
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

	private int compareModelOrder(final OperationItem left, final OperationItem right) {
		if (left.getName().equals(right.getName())) {
			return compareParams(left.getParameterValues(), right.getParameterValues());
		} else {
			final int leftIndex = opNames.indexOf(left.getName());
			final int rightIndex = opNames.indexOf(right.getName());
			if (leftIndex == -1 && rightIndex == -1) {
				return left.getName().compareTo(right.getName());
			} else {
				return Integer.compare(leftIndex, rightIndex);
			}
		}
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
		return events.stream().filter(op -> op.getName().toLowerCase().contains(filter.toLowerCase()))
				.collect(Collectors.toList());
	}

	private void doSort() {
		final Comparator<OperationItem> comparator;
		switch (this.getSortMode()) {
		case MODEL_ORDER:
			comparator = this::compareModelOrder;
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
				stageManager.makeAlert(Alert.AlertType.WARNING,
					"operations.operationsView.alerts.invalidNumberOfOparations.header",
					"operations.operationsView.alerts.invalidNumberOfOparations.content", randomInput)
					.showAndWait();
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


	private void updateModel(final Trace trace) {
		currentModel = trace.getModel();
		opNames.clear();
		opToParams.clear();
		LoadedMachine loadedMachine = trace.getStateSpace().getLoadedMachine();
		for (String opName : loadedMachine.getOperationNames()) {
			OperationInfo machineOperationInfo = loadedMachine.getMachineOperationInfo(opName);
			opNames.add(opName);
			opToParams.put(opName, machineOperationInfo.getParameterNames());

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
}
