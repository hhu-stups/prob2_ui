package de.prob2.ui.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.exception.ProBError;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.LoadedMachine;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;

import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.statusbar.StatusBar;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
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
import javafx.scene.layout.AnchorPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sawano.java.text.AlphanumericComparator;

@Singleton
public final class OperationsView extends AnchorPane {
	public enum SortMode {
		MODEL_ORDER, A_TO_Z, Z_TO_A
	}

	private final class OperationsCell extends ListCell<OperationItem> {
		public OperationsCell() {
			super();

			getStyleClass().add("operations-cell");

			this.setOnMouseClicked(event -> {
				if (event.getButton() == MouseButton.PRIMARY) {
					executeOperationIfPossible(this.getItem());
				}
			});

			final MenuItem showDetailsItem = new MenuItem(bundle.getString("operations.showDetails"));
			showDetailsItem.setOnAction(event -> {
				final OperationDetailsStage stage = injector.getInstance(OperationDetailsStage.class);
				stage.setItem(this.getItem());
				stage.show();
			});

			final MenuItem executeByPredicateItem = new MenuItem(bundle.getString("operations.executeByPredicate"));
			executeByPredicateItem.setOnAction(event -> {
				final ExecuteByPredicateStage stage = injector.getInstance(ExecuteByPredicateStage.class);
				stage.setItem(this.getItem());
				stage.show();
			});
			this.setContextMenu(new ContextMenu(showDetailsItem, executeByPredicateItem));
		}

		private String getPrettyName(final String name) {
			switch (name) {
			case "$setup_constants":
				return "SETUP_CONSTANTS";

			case "$initialise_machine":
				return "INITIALISATION";

			default:
				return name;
			}
		}

		private String formatOperationItem(final OperationItem item) {
			StringBuilder sb = new StringBuilder(getPrettyName(item.getName()));

			final List<String> args = new ArrayList<>();

			final List<String> paramNames = item.getParameterNames();
			final List<String> paramValues = item.getParameterValues();
			if (paramNames.isEmpty()) {
				// Parameter names not available
				args.addAll(paramValues);
			} else {
				assert paramNames.size() == paramValues.size();
				for (int i = 0; i < paramValues.size(); i++) {
					args.add(paramNames.get(i) + '=' + paramValues.get(i));
				}
			}

			item.getConstants().forEach((key, value) -> args.add(key + ":=" + value));
			item.getVariables().forEach((key, value) -> args.add(key + ":=" + value));

			if (!args.isEmpty()) {
				sb.append('(');
				sb.append(String.join(", ", args));
				sb.append(')');
			}

			final List<String> returnValues = item.getReturnValues();
			if (!returnValues.isEmpty()) {
				sb.append(" → ");
				sb.append(String.join(", ", returnValues));
			}

			return sb.toString();
		}

		@Override
		protected void updateItem(OperationItem item, boolean empty) {
			super.updateItem(item, empty);
			getStyleClass().removeAll("enabled", "timeout", "unexplored", "errored", "skip", "normal", "disabled");
			if (item != null && !empty) {
				setText(formatOperationItem(item));
				setDisable(true);
				final FontAwesomeIconView icon;
				switch (item.getStatus()) {
				case TIMEOUT:
					icon = new FontAwesomeIconView(FontAwesomeIcon.CLOCK_ALT);
					getStyleClass().add("timeout");
					setTooltip(new Tooltip(bundle.getString("operations.tooltip.timeout")));
					break;

				case ENABLED:
					icon = new FontAwesomeIconView(item.isSkip() ? FontAwesomeIcon.REPEAT : FontAwesomeIcon.PLAY);
					setDisable(false);
					getStyleClass().add("enabled");
					if (!item.isExplored()) {
						getStyleClass().add("unexplored");
						setTooltip(new Tooltip(bundle.getString("operations.tooltip.reachesUnexplored")));
					} else if (item.isErrored()) {
						getStyleClass().add("errored");
						setTooltip(new Tooltip(bundle.getString("operations.tooltip.reachesErrored")));
					} else if (item.isSkip()) {
						getStyleClass().add("skip");
						setTooltip(new Tooltip(bundle.getString("operations.tooltip.reachesSame")));
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
	private List<String> opNames = new ArrayList<>();
	private Map<String, List<String>> opToParams = new HashMap<>();
	private List<OperationItem> events = new ArrayList<>();
	private boolean showDisabledOps = true;
	private SortMode sortMode = SortMode.MODEL_ORDER;
	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final ResourceBundle bundle;
	private final StatusBar statusBar;
	private final StageManager stageManager;
	private final Comparator<CharSequence> alphanumericComparator;
	private final ExecutorService updater;
	private final ObjectProperty<Thread> randomExecutionThread;

	private static final String ICON_DARK = "icon-dark";

	@Inject
	private OperationsView(final CurrentTrace currentTrace, final Locale locale, final StageManager stageManager,
						   final Injector injector, final ResourceBundle bundle, final StatusBar statusBar,
						   final StopActions stopActions) {
		this.currentTrace = currentTrace;
		this.alphanumericComparator = new AlphanumericComparator(locale);
		this.injector = injector;
		this.bundle = bundle;
		this.statusBar = statusBar;
		this.stageManager = stageManager;
		this.updater = Executors.newSingleThreadExecutor(r -> new Thread(r, "OperationsView Updater"));
		this.randomExecutionThread = new SimpleObjectProperty<>(this, "randomExecutionThread", null);
		stopActions.add(this.updater::shutdownNow);

		stageManager.loadFXML(this, "ops_view.fxml");
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
	}

	private void executeOperationIfPossible(final OperationItem item) {
		if (
			item != null
			&& item.getStatus() == OperationItem.Status.ENABLED
			&& item.getTrace().equals(currentTrace.get())
		) {
			currentTrace.set(currentTrace.get().add(item.getTransition().getId()));
		}
	}

	private LinkedHashMap<String, String> getNextStateValues(Transition transition, List<IEvalElement> formulas) {
		// It seems that there is no way to easily find out the
		// constant/variable values which a specific $setup_constants or
		// $initialise_machine transition would set.
		// So we look at the values of all constants/variables in the
		// transition's destination state.
		final LinkedHashMap<String, String> values = new LinkedHashMap<>();
		final List<AbstractEvalResult> results = transition.getDestination().eval(formulas);
		for (int i = 0; i < formulas.size(); i++) {
			final AbstractEvalResult value = results.get(i);
			final String valueString;
			if (value instanceof EvalResult) {
				valueString = ((EvalResult) value).getValue();
			} else {
				// noinspection ObjectToString
				valueString = value.toString();
			}
			values.put(formulas.get(i).getCode(), valueString);
		}

		return values;
	}

	public void update(final Trace trace) {
		if (trace == null) {
			currentModel = null;
			opNames = new ArrayList<>();
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

		events = new ArrayList<>();
		final Set<Transition> operations = trace.getNextTransitions(true, FormulaExpand.TRUNCATE);
		final Set<String> disabled = new HashSet<>(opNames);
		final Set<String> withTimeout = trace.getCurrentState().getTransitionsWithTimeout();
		for (Transition transition : operations) {
			disabled.remove(transition.getName());

			final LoadedMachine loadedMachine = trace.getStateSpace().getLoadedMachine();
			OperationInfo opInfo;
			try {
				opInfo = loadedMachine.getMachineOperationInfo(transition.getName());
			} catch (ProBError e) {
				// fallback solution if getMachineOperationInfo throws a ProBError
				opInfo = null;
			}

			final List<String> paramValues;
			final Map<String, String> constants;
			final Map<String, String> variables;
			switch (transition.getName()) {
			case "$setup_constants":
				paramValues = Collections.emptyList();
				constants = this.getNextStateValues(transition, loadedMachine.getConstantEvalElements());
				variables = Collections.emptyMap();
				break;

			case "$initialise_machine":
				paramValues = Collections.emptyList();
				variables = this.getNextStateValues(transition, loadedMachine.getVariableEvalElements());
				constants = Collections.emptyMap();
				break;

			default:
				paramValues = transition.getParameterValues();
				constants = Collections.emptyMap();
				if (opInfo == null) {
					variables = null;
				} else {
					variables = this.getNextStateValues(transition, opInfo.getNonDetWrittenVariables().stream()
						.map(var -> trace.getStateSpace().getModel().parseFormula(var, FormulaExpand.TRUNCATE))
						.collect(Collectors.toList()));
				}
			}

			final List<String> paramNames = opInfo == null ? Collections.emptyList() : opInfo.getParameterNames();
			final List<String> outputNames = opInfo == null ? Collections.emptyList() : opInfo.getOutputParameterNames();
			events.add(new OperationItem(trace, transition, transition.getName(),
				paramValues, transition.getReturnValues(), OperationItem.Status.ENABLED,
				paramNames, outputNames, constants, variables));
		}
		showDisabledAndWithTimeout(trace, disabled, withTimeout);

		doSort();

		final String text;
		if (trace.getCurrentState().isMaxTransitionsCalculated()) {
			text = bundle.getString("operations.maxReached");
		} else if (!trace.getCurrentState().isInitialised() && operations.isEmpty()) {
			text = bundle.getString("operations.noSetupConstantsOrInit");
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

	private void showDisabledAndWithTimeout(final Trace trace, final Set<String> notEnabled,
			final Set<String> withTimeout) {
		if (showDisabledOps) {
			for (String s : notEnabled) {
				if (!"$initialise_machine".equals(s)) {
					events.add(new OperationItem(trace, null, s, opToParams.get(s), Collections.emptyList(),
							withTimeout.contains(s) ? OperationItem.Status.TIMEOUT : OperationItem.Status.DISABLED,
						Collections.emptyList(), Collections.emptyList(),
							Collections.emptyMap(), Collections.emptyMap()));
				}
			}
		}
		for (String s : withTimeout) {
			if (!notEnabled.contains(s)) {
				events.add(new OperationItem(trace, null, s, Collections.emptyList(), Collections.emptyList(),
						OperationItem.Status.TIMEOUT, Collections.emptyList(),
						Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap()));
			}
		}
	}

	private static String stripString(final String param) {
		return param.replaceAll("\\{", "").replaceAll("\\}", "");
	}

	private int compareParams(final List<String> left, final List<String> right) {
		int minSize = left.size() < right.size() ? left.size() : right.size();
		for (int i = 0; i < minSize; i++) {
			int cmp = alphanumericComparator.compare(stripString(left.get(i)), stripString(right.get(i)));
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
			return Integer.compare(opNames.indexOf(left.getName()), opNames.indexOf(right.getName()));
		}
	}

	@FXML
	private void handleDisabledOpsToggle() {
		showDisabledOps = disabledOpsToggle.isSelected();
		final FontAwesomeIcon icon = showDisabledOps ? FontAwesomeIcon.EYE : FontAwesomeIcon.EYE_SLASH;
		update(currentTrace.get());
		((FontAwesomeIconView)disabledOpsToggle.getGraphic()).setIcon(icon);
	}

	private List<OperationItem> applyFilter(final String filter) {
		return events.stream().filter(op -> op.getName().toLowerCase().contains(filter.toLowerCase()))
				.collect(Collectors.toList());
	}

	private void doSort() {
		final Comparator<OperationItem> comparator;
		switch (sortMode) {
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
			throw new IllegalStateException("Unhandled sort mode: " + sortMode);
		}

		events.sort(comparator);
	}

	@FXML
	private void handleSortButton() {
		final FontAwesomeIcon icon;
		switch (sortMode) {
		case MODEL_ORDER:
			sortMode = SortMode.A_TO_Z;
			icon = FontAwesomeIcon.SORT_ALPHA_ASC;
			break;

		case A_TO_Z:
			sortMode = SortMode.Z_TO_A;
			icon = FontAwesomeIcon.SORT_ALPHA_DESC;
			break;

		case Z_TO_A:
			sortMode = SortMode.MODEL_ORDER;
			icon = FontAwesomeIcon.SORT;
			break;

		default:
			throw new IllegalStateException("Unhandled sort mode: " + sortMode);
		}

		doSort();
		opsListView.getItems().setAll(applyFilter(searchBar.getText()));
		((FontAwesomeIconView)sortButton.getGraphic()).setIcon(icon);
	}

	@FXML
	public void random(ActionEvent event) {
		if (currentTrace.exists()) {
			Thread executionThread = new Thread(() -> {
				try {
					Trace newTrace = null;
					if (event.getSource().equals(randomText)) {
						if (randomText.getText().isEmpty()) {
							return;
						}
						newTrace = currentTrace.get().randomAnimation(Integer.parseInt(randomText.getText()));
					} else if (event.getSource().equals(oneRandomEvent)) {
						newTrace = currentTrace.get().randomAnimation(1);
					} else if (event.getSource().equals(fiveRandomEvents)) {
						newTrace = currentTrace.get().randomAnimation(5);
					} else if (event.getSource().equals(tenRandomEvents)) {
						newTrace = currentTrace.get().randomAnimation(10);
					}
					currentTrace.set(newTrace);
					randomExecutionThread.set(null);
				} catch (Exception e) {
					LOGGER.error("Invalid input for executing random number of events",e);
					Platform.runLater(() -> {
						Alert alert = stageManager.makeAlert(Alert.AlertType.WARNING, e.getMessage());
						alert.setHeaderText(bundle.getString("operations.random.error"));
						alert.showAndWait();
					});
					randomExecutionThread.set(null);
				}
			});
			randomExecutionThread.set(executionThread);
			executionThread.start();
		}
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
		opNames = new ArrayList<>();
		opToParams = new HashMap<>();
		LoadedMachine loadedMachine = trace.getStateSpace().getLoadedMachine();
		for (String opName : loadedMachine.getOperationNames()) {
			OperationInfo machineOperationInfo = loadedMachine.getMachineOperationInfo(opName);
			opNames.add(opName);
			opToParams.put(opName, machineOperationInfo.getParameterNames());

		}
	}
	

	public void setSortMode(OperationsView.SortMode mode) {
		sortMode = mode;
		FontAwesomeIconView icon;
		switch (sortMode) {
		case A_TO_Z:
			icon = new FontAwesomeIconView(FontAwesomeIcon.SORT_ALPHA_ASC);
			break;

		case Z_TO_A:
			icon = new FontAwesomeIconView(FontAwesomeIcon.SORT_ALPHA_DESC);
			break;

		case MODEL_ORDER:
			icon = new FontAwesomeIconView(FontAwesomeIcon.SORT);
			break;

		default:
			throw new IllegalStateException("Unhandled sort mode: " + sortMode);
		}
		icon.setSize("15");
		icon.setStyleClass(ICON_DARK);
		sortButton.setGraphic(icon);
	}

	public SortMode getSortMode() {
		return sortMode;
	}

	public void setShowDisabledOps(boolean showDisabledOps) {
		this.showDisabledOps = showDisabledOps;
		final FontAwesomeIcon icon;
		if (showDisabledOps) {
			icon = FontAwesomeIcon.EYE;
			disabledOpsToggle.setSelected(true);
		} else {
			icon = FontAwesomeIcon.EYE_SLASH;
			disabledOpsToggle.setSelected(false);
		}
		((FontAwesomeIconView)disabledOpsToggle.getGraphic()).setIcon(icon);
	}

	public boolean getShowDisabledOps() {
		return showDisabledOps;
	}
}
