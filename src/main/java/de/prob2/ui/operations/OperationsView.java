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
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.animator.domainobjects.AbstractEvalResult;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.model.classicalb.Operation;
import de.prob.model.eventb.Event;
import de.prob.model.eventb.EventParameter;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.BEvent;
import de.prob.model.representation.Constant;
import de.prob.model.representation.Machine;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import se.sawano.java.text.AlphanumericComparator;

public final class OperationsView extends AnchorPane {
	public enum SortMode {
		MODEL_ORDER, A_TO_Z, Z_TO_A
	}

	private static final class OperationsCell extends ListCell<OperationItem> {
		@Override
		protected void updateItem(OperationItem item, boolean empty) {
			super.updateItem(item, empty);
			if (item != null && !empty) {
				setText(item.toString());
				setGraphicTextGap(10.0);
				final FontAwesomeIconView icon;
				if (item.isTimeOut()) {
					icon = new FontAwesomeIconView(FontAwesomeIcon.CLOCK_ALT);
					icon.setFill(Color.ORANGE);
					setDisable(true);
					getStyleClass().clear();
					getStyleClass().add("normal");
				} else {
					if (item.isEnabled()) {
						icon = new FontAwesomeIconView(FontAwesomeIcon.PLAY);
						icon.setFill(Color.LIMEGREEN);
						setDisable(false);
						if (!item.explored) {
							getStyleClass().clear();
							getStyleClass().add("unexplored");
						} else if (item.errored) {
							getStyleClass().clear();
							getStyleClass().add("errored");
						} else {
							getStyleClass().clear();
							getStyleClass().add("normal");
						}
					} else {
						icon = new FontAwesomeIconView(FontAwesomeIcon.MINUS_CIRCLE);
						icon.setFill(Color.RED);
						setDisable(true);
						getStyleClass().clear();
						getStyleClass().add("normal");
					}
				}
				setGraphic(icon);
			} else {
				setGraphic(null);
				setText(null);
				getStyleClass().clear();
			}
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(OperationsView.class);
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

	private AbstractModel currentModel;
	private List<String> opNames = new ArrayList<>();
	private Map<String, List<String>> opToParams = new HashMap<>();
	private List<OperationItem> events = new ArrayList<>();
	private boolean showDisabledOps = true;
	private String filter = "";
	private SortMode sortMode = SortMode.MODEL_ORDER;
	private final CurrentTrace currentTrace;

	private final Comparator<CharSequence> alphanumericComparator;

	@Inject
	private OperationsView(final CurrentTrace currentTrace, final Locale locale, final StageManager stageManager) {
		this.currentTrace = currentTrace;
		this.alphanumericComparator = new AlphanumericComparator(locale);

		stageManager.loadFXML(this, "ops_view.fxml");
	}

	@FXML
	public void initialize() {
		opsListView.setCellFactory(lv -> new OperationsCell());

		opsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null && newValue.isEnabled()) {
				// Disable the operations list until the trace change is
				// finished, the update method reenables it later
				opsListView.setDisable(true);
				currentTrace.set(currentTrace.get().add(newValue.id));
			}
		});

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
	}

	private List<String> extractSetupConstantsParams(final Trace trace, final Transition transition) {
		// It seems that there is no way to easily find out the constant values
		// behind a $setup_constants transition.
		// So we look at the values of all constants in the state after each
		// transition.
		final Set<IEvalElement> constantsFormulas = new HashSet<>();
		for (final Constant c : trace.getStateSpace().getMainComponent().getChildrenOfType(Constant.class)) {
			constantsFormulas.add(c.getFormula());
		}

		for (final IEvalElement ee : constantsFormulas) {
			trace.getStateSpace().subscribe(this, ee);
		}

		final List<String> params = new ArrayList<>();
		final Map<IEvalElement, AbstractEvalResult> values = transition.getDestination().getValues();
		for (final Map.Entry<IEvalElement, AbstractEvalResult> entry : values.entrySet()) {
			if (constantsFormulas.contains(entry.getKey())) {
				params.add(entry.getKey() + "=" + entry.getValue());
			}
		}

		for (final IEvalElement ee : constantsFormulas) {
			trace.getStateSpace().unsubscribe(this, ee);
		}

		return params;
	}

	private void update(final Trace trace) {
		if (trace == null) {
			this.opsListView.setDisable(true);
			currentModel = null;
			opNames = new ArrayList<>();
			Platform.runLater(opsListView.getItems()::clear);
			return;
		}

		this.opsListView.setDisable(false);

		if (trace.getModel() != currentModel) {
			updateModel(trace);
		}
		events = new ArrayList<>();
		final Set<Transition> operations = trace.getNextTransitions(true);
		final Set<String> notEnabled = new HashSet<>(opNames);
		final Set<String> withTimeout = trace.getCurrentState().getTransitionsWithTimeout();
		for (Transition transition : operations) {
			final String name = extractPrettyName(transition.getName());
			notEnabled.remove(name);

			final List<String> params;
			if ("SETUP_CONSTANTS".equals(name)) {
				params = this.extractSetupConstantsParams(trace, transition);
			} else {
				params = transition.getParams();
			}

			final boolean explored = transition.getDestination().isExplored();
			final boolean errored = explored && !transition.getDestination().isInvariantOk();
			logger.trace("{} {}", name, errored);
			OperationItem operationItem = new OperationItem(
					transition.getId(),
					name,
					params,
					transition.getReturnValues(),
					OperationItem.Status.ENABLED,
					explored,
					errored);
			events.add(operationItem);
		}
		if (showDisabledOps) {
			for (String s : notEnabled) {
				if (!"INITIALISATION".equals(s)) {
					events.add(new OperationItem(s, s, opToParams.get(s), Collections.emptyList(),
							withTimeout.contains(s) ? OperationItem.Status.TIMEOUT : OperationItem.Status.DISABLED, false, false));
				}
			}
		}
		for (String s : withTimeout) {
			if (!notEnabled.contains(s)) {
				events.add(new OperationItem(s, s, opToParams.get(s), Collections.emptyList(),
						OperationItem.Status.TIMEOUT, false, false));
			
			}
		}
		
		doSort();

		Platform.runLater(() -> opsListView.getItems().setAll(applyFilter(filter)));
	}

	private static String extractPrettyName(final String name) {
		if ("$setup_constants".equals(name)) {
			return "SETUP_CONSTANTS";
		}
		if ("$initialise_machine".equals(name)) {
			return "INITIALISATION";
		}
		return name;
	}

	private static String stripString(final String param) {
		return param.replaceAll("\\{", "").replaceAll("\\}", "");
	}

	private int compareParams(final List<String> left, final List<String> right) {
		int minSize = left.size() > right.size() ? left.size() : right.size();
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
		if (left.name.equals(right.name)) {
			return compareParams(left.params, right.params);
		} else {
			return alphanumericComparator.compare(left.name, right.name);
		}
	}

	private int compareModelOrder(final OperationItem left, final OperationItem right) {
		if (left.name.equals(right.name)) {
			return compareParams(left.params, right.params);
		} else {
			return Integer.compare(opNames.indexOf(left.name), opNames.indexOf(right.name));
		}
	}

	@FXML
	private void handleDisabledOpsToggle() {
		showDisabledOps = disabledOpsToggle.isSelected();
		FontAwesomeIconView icon = new FontAwesomeIconView(
				showDisabledOps ? FontAwesomeIcon.EYE : FontAwesomeIcon.EYE_SLASH);
		update(currentTrace.get());
		icon.setSize("15");
		icon.setStyleClass("icon-dark");
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
		List<OperationItem> newOps = new ArrayList<>();
		for (OperationItem op : events) {
			if (op.name.startsWith(filter)) {
				newOps.add(op);
			}
		}
		return newOps;
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
		icon.setStyleClass("icon-dark");
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
			for (EventParameter eParam : ((Event) e).getParameters()) {
				paramList.add(eParam.getName());
			}
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
		icon.setStyleClass("icon-dark");
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
		icon.setStyleClass("icon-dark");
		disabledOpsToggle.setGraphic(icon);
	}

	public boolean getShowDisabledOps() {
		return showDisabledOps;
	}
}
