package de.prob2.ui.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.model.classicalb.Operation;
import de.prob.model.eventb.Event;
import de.prob.model.eventb.EventParameter;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractFormulaElement;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.BEvent;
import de.prob.model.representation.Constant;
import de.prob.model.representation.Machine;
import de.prob.model.representation.Variable;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;

import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.statusbar.StatusBar;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;

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
				if (
					event.getButton() == MouseButton.PRIMARY
					&& this.getItem() != null
					&& this.getItem().getStatus() == OperationItem.Status.ENABLED
					&& this.getItem().getTrace().equals(currentTrace.get())
				) {
					currentTrace.set(currentTrace.get().add(this.getItem().getId()));
				}
			});
			
			final MenuItem showDetailsItem = new MenuItem("Show Details");
			showDetailsItem.setOnAction(event -> {
				final OperationDetailsStage stage = injector.getInstance(OperationDetailsStage.class);
				stage.setItem(this.getItem());
				stage.show();
			});
			this.setContextMenu(new ContextMenu(showDetailsItem));
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
			StringBuilder sb = new StringBuilder();
			
			final List<String> returnValues = item.getReturnValues();
			if (!returnValues.isEmpty()) {
				sb.append(String.join(", ", returnValues));
				sb.append(" ← ");
			}
			
			sb.append(getPrettyName(item.getName()));
			
			final List<String> params = item.getParams();
			if (!params.isEmpty()) {
				sb.append('(');
				sb.append(String.join(", ", params));
				sb.append(')');
			}
			
			return sb.toString();
		}

		@Override
		protected void updateItem(OperationItem item, boolean empty) {
			super.updateItem(item, empty);
			getStyleClass().removeAll("enabled", "timeout", "unexplored", "errored", "skip", "normal", "disabled",
					"max-reached");
			if (item != null && !empty) {
				setText(formatOperationItem(item));
				setDisable(true);
				final FontAwesomeIconView icon;
				switch (item.getStatus()) {
				case TIMEOUT:
					icon = new FontAwesomeIconView(FontAwesomeIcon.CLOCK_ALT);
					getStyleClass().add("timeout");
					setTooltip(new Tooltip("Operation with timeout"));
					break;

				case ENABLED:
					icon = new FontAwesomeIconView(item.isSkip() ? FontAwesomeIcon.REPEAT : FontAwesomeIcon.PLAY);
					setDisable(false);
					getStyleClass().add("enabled");
					if (!item.isExplored()) {
						getStyleClass().add("unexplored");
						setTooltip(new Tooltip("Reaches unexplored State"));
					} else if (item.isErrored()) {
						getStyleClass().add("errored");
						setTooltip(new Tooltip("Reaches errored State"));
					} else if (item.isSkip()) {
						getStyleClass().add("skip");
						setTooltip(new Tooltip("Does not change State"));
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

				case MAX_REACHED:
					icon = new FontAwesomeIconView(FontAwesomeIcon.ELLIPSIS_H);
					getStyleClass().add("max-reached");
					setTooltip(null);
					break;

				default:
					throw new IllegalStateException("Unhandled status: " + item.getStatus());
				}
				FontSize fontsize = injector.getInstance(FontSize.class);
				icon.glyphSizeProperty().bind(fontsize);
				setGraphic(icon);
			} else {
				setGraphic(null);
				setText(null);
			}
		}
	}

	// Matches empty string or number
	private static final Pattern NUMBER_OR_EMPTY_PATTERN = Pattern.compile("^$|^\\d+$");

	@FXML
	private ListView<OperationItem> opsListView;
	@FXML
	private Button backButton;
	@FXML
	private Button forwardButton;
	@FXML
	private Button searchButton;
	@FXML
	private Button sortButton;
	@FXML
	private ToggleButton disabledOpsToggle;
	@FXML
	private TextField filterEvents;
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

	private AbstractModel currentModel;
	private List<String> opNames = new ArrayList<>();
	private Map<String, List<String>> opToParams = new HashMap<>();
	private List<OperationItem> events = new ArrayList<>();
	private boolean showDisabledOps = true;
	private String filter = "";
	private SortMode sortMode = SortMode.MODEL_ORDER;
	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final StatusBar statusBar;
	private final Comparator<CharSequence> alphanumericComparator;
	private final ExecutorService updater;

	private static final String ICON_DARK = "icon-dark";

	@Inject
	private OperationsView(final CurrentTrace currentTrace, final Locale locale, final StageManager stageManager,
			final Injector injector, final StatusBar statusBar, final StopActions stopActions) {
		this.currentTrace = currentTrace;
		this.alphanumericComparator = new AlphanumericComparator(locale);
		this.injector = injector;
		this.statusBar = statusBar;
		this.updater = Executors.newSingleThreadExecutor(r -> new Thread(r, "OperationsView Updater"));
		stopActions.add(this.updater::shutdownNow);

		stageManager.loadFXML(this, "ops_view.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent("HelpMain.html");
		opsListView.setCellFactory(lv -> new OperationsCell());

		backButton.disableProperty().bind(currentTrace.canGoBackProperty().not());
		forwardButton.disableProperty().bind(currentTrace.canGoForwardProperty().not());
		randomButton.disableProperty().bind(currentTrace.existsProperty().not());

		randomText.textProperty().addListener((observable, from, to) -> {
			if (!NUMBER_OR_EMPTY_PATTERN.matcher(to).matches() && NUMBER_OR_EMPTY_PATTERN.matcher(from).matches()) {
				((StringProperty) observable).set(from);
			}
		});

		this.update(currentTrace.get());
		currentTrace.addListener((observable, from, to) -> update(to));

		bindIconSizeToFontSize();
	}

	private void bindIconSizeToFontSize() {
		FontSize fontsize = injector.getInstance(FontSize.class);
		((FontAwesomeIconView) (searchButton.getGraphic())).glyphSizeProperty().bind(fontsize);
		((FontAwesomeIconView) (backButton.getGraphic())).glyphSizeProperty().bind(fontsize.add(2));
		((FontAwesomeIconView) (forwardButton.getGraphic())).glyphSizeProperty().bind(fontsize.add(2));
		sortButton.graphicProperty().addListener((observable, from,
				to) -> ((FontAwesomeIconView) (sortButton.getGraphic())).glyphSizeProperty().bind(fontsize.add(2)));
		disabledOpsToggle.graphicProperty()
				.addListener((observable, from, to) -> ((FontAwesomeIconView) (disabledOpsToggle.getGraphic()))
						.glyphSizeProperty().bind(fontsize.add(2)));
		((FontAwesomeIconView) (randomButton.getGraphic())).glyphSizeProperty().bind(fontsize.add(2));
	}

	private List<String> extractParamsFromNextState(final Trace trace, final Transition transition,
			final Class<? extends AbstractFormulaElement> type) {
		// It seems that there is no way to easily find out the
		// constant/variable values which a specific $setup_constants or
		// $initialise_machine transition would set.
		// So we look at the values of all constants/variables in the
		// transition's destination state.
		final List<IEvalElement> formulas = new ArrayList<>();
		for (final AbstractFormulaElement c : trace.getStateSpace().getMainComponent().getChildrenOfType(type)) {
			formulas.add(c.getFormula());
		}

		final List<String> params = new ArrayList<>();
		final List<AbstractEvalResult> results = transition.getDestination().eval(formulas);
		for (int i = 0; i < formulas.size(); i++) {
			final AbstractEvalResult value = results.get(i);
			final String valueString;
			if (value instanceof EvalResult) {
				valueString = ((EvalResult)value).getValue();
			} else {
				// noinspection ObjectToString
				valueString = value.toString();
			}
			params.add(formulas.get(i).getCode() + '=' + valueString);
		}

		return params;
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
		final Set<Transition> operations = trace.getNextTransitions(true);
		final Set<String> disabled = new HashSet<>(opNames);
		final Set<String> withTimeout = trace.getCurrentState().getTransitionsWithTimeout();
		for (Transition transition : operations) {
			disabled.remove(transition.getName());

			final List<String> params;
			switch (transition.getName()) {
				case "$setup_constants":
					params = this.extractParamsFromNextState(trace, transition, Constant.class);
					break;
				
				case "$initialise_machine":
					params = this.extractParamsFromNextState(trace, transition, Variable.class);
					break;
				
				default:
					params = transition.getParams();
			}

			final boolean explored = transition.getDestination().isExplored();
			final boolean errored = explored && !transition.getDestination().isInvariantOk();
			final boolean skip = transition.getSource().equals(transition.getDestination());
			OperationItem operationItem = new OperationItem(trace, transition.getId(), transition.getName(), params,
					transition.getReturnValues(), OperationItem.Status.ENABLED, explored, errored, skip);
			events.add(operationItem);
		}
		showDisabledAndWithTimeout(trace, disabled, withTimeout);

		doSort();

		if (trace.getCurrentState().isMaxTransitionsCalculated()) {
			events.add(new OperationItem(trace, "-", "(possibly more - maximum operations reached)", Collections.emptyList(),
					Collections.emptyList(), OperationItem.Status.MAX_REACHED, false, false, false));
		}

		final List<OperationItem> filtered = applyFilter(filter);
		
		Platform.runLater(() -> {
			opsListView.getItems().setAll(filtered);
			this.opsListView.setDisable(false);
			this.statusBar.setOperationsViewUpdating(false);
		});
	}

	private void showDisabledAndWithTimeout(final Trace trace, final Set<String> notEnabled, final Set<String> withTimeout) {
		if (showDisabledOps) {
			for (String s : notEnabled) {
				if (!"$initialise_machine".equals(s)) {
					events.add(new OperationItem(trace, s, s, opToParams.get(s), Collections.emptyList(),
							withTimeout.contains(s) ? OperationItem.Status.TIMEOUT : OperationItem.Status.DISABLED,
							false, false, false));
				}
			}
		}
		for (String s : withTimeout) {
			if (!notEnabled.contains(s)) {
				events.add(new OperationItem(trace, s, s, Collections.emptyList(), Collections.emptyList(),
						OperationItem.Status.TIMEOUT, false, false, false));
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
			return compareParams(left.getParams(), right.getParams());
		} else {
			return alphanumericComparator.compare(left.getName(), right.getName());
		}
	}

	private int compareModelOrder(final OperationItem left, final OperationItem right) {
		if (left.getName().equals(right.getName())) {
			return compareParams(left.getParams(), right.getParams());
		} else {
			return Integer.compare(opNames.indexOf(left.getName()), opNames.indexOf(right.getName()));
		}
	}

	@FXML
	private void handleDisabledOpsToggle() {
		showDisabledOps = disabledOpsToggle.isSelected();
		FontAwesomeIconView icon = new FontAwesomeIconView(
				showDisabledOps ? FontAwesomeIcon.EYE : FontAwesomeIcon.EYE_SLASH);
		update(currentTrace.get());
		icon.setSize("15");
		icon.setStyleClass(ICON_DARK);
		disabledOpsToggle.setGraphic(icon);
	}

	@FXML
	private void handleBackButton() {
		if (currentTrace.exists()) {
			currentTrace.set(currentTrace.back());
		}
	}

	@FXML
	private void handleForwardButton() {
		if (currentTrace.exists()) {
			currentTrace.set(currentTrace.forward());
		}
	}

	@FXML
	private void handleSearchButton() {
		filter = filterEvents.getText();
		opsListView.getItems().setAll(applyFilter(filter));
	}

	private List<OperationItem> applyFilter(final String filter) {
		return events.stream().filter(op -> op.getName().startsWith(filter)).collect(Collectors.toList());
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
		FontAwesomeIconView icon;
		switch (sortMode) {
		case MODEL_ORDER:
			sortMode = SortMode.A_TO_Z;
			icon = new FontAwesomeIconView(FontAwesomeIcon.SORT_ALPHA_ASC);
			break;

		case A_TO_Z:
			sortMode = SortMode.Z_TO_A;
			icon = new FontAwesomeIconView(FontAwesomeIcon.SORT_ALPHA_DESC);
			break;

		case Z_TO_A:
			sortMode = SortMode.MODEL_ORDER;
			icon = new FontAwesomeIconView(FontAwesomeIcon.SORT);
			break;

		default:
			throw new IllegalStateException("Unhandled sort mode: " + sortMode);
		}

		doSort();
		opsListView.getItems().setAll(applyFilter(filter));
		icon.setSize("15");
		icon.setStyleClass(ICON_DARK);
		sortButton.setGraphic(icon);
	}

	@FXML
	public void random(ActionEvent event) {
		if (currentTrace.exists()) {
			if (event.getSource().equals(randomText)) {
				if (randomText.getText().isEmpty()) {
					return;
				}
				currentTrace.set(currentTrace.get().randomAnimation(Integer.parseInt(randomText.getText())));
			} else if (event.getSource().equals(oneRandomEvent)) {
				currentTrace.set(currentTrace.get().randomAnimation(1));
			} else if (event.getSource().equals(fiveRandomEvents)) {
				currentTrace.set(currentTrace.get().randomAnimation(5));
			} else if (event.getSource().equals(tenRandomEvents)) {
				currentTrace.set(currentTrace.get().randomAnimation(10));
			}
		}
	}

	private void updateModel(final Trace trace) {
		currentModel = trace.getModel();
		AbstractElement mainComponent = trace.getStateSpace().getMainComponent();
		opNames = new ArrayList<>();
		opToParams = new HashMap<>();
		if (mainComponent instanceof Machine) {
			for (BEvent e : mainComponent.getChildrenOfType(BEvent.class)) {
				opNames.add(e.getName());
				opToParams.put(e.getName(), getParams(e));
			}
		}
	}

	private List<String> getParams(BEvent e) {
		List<String> paramList = new ArrayList<>();
		if (e instanceof Event) {
			paramList.addAll(
					((Event) e).getParameters().stream().map(EventParameter::getName).collect(Collectors.toList()));
		} else if (e instanceof Operation) {
			paramList.addAll(((Operation) e).getParameters());
		}
		return paramList;
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
		FontAwesomeIconView icon;
		if (showDisabledOps) {
			icon = new FontAwesomeIconView(FontAwesomeIcon.EYE);
			disabledOpsToggle.setSelected(true);
		} else {
			icon = new FontAwesomeIconView(FontAwesomeIcon.EYE_SLASH);
			disabledOpsToggle.setSelected(false);
		}
		icon.setSize("15");
		icon.setStyleClass(ICON_DARK);
		disabledOpsToggle.setGraphic(icon);
	}

	public boolean getShowDisabledOps() {
		return showDisabledOps;
	}
}
