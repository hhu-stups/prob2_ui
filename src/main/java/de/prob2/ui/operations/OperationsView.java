package de.prob2.ui.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;

import de.prob.model.eventb.Event;
import de.prob.model.eventb.EventParameter;
import de.prob.model.representation.AbstractElement;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.BEvent;
import de.prob.model.representation.Machine;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;

import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

//@Singleton
public final class OperationsView extends AnchorPane {
	private enum SortMode {
		MODEL_ORDER, A_TO_Z, Z_TO_A
	}

	private static final class OperationsCell extends ListCell<Operation> {
		@Override
		protected void updateItem(Operation item, boolean empty) {
			super.updateItem(item, empty);
			if (item != null && !empty) {
				setText(item.toString());
				setGraphicTextGap(10.0);
				final FontAwesomeIconView icon;
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
				setGraphic(icon);
			} else {
				setGraphic(null);
				setText(null);
				getStyleClass().clear();
			}
		}
	}
	
	private static final Logger logger = LoggerFactory.getLogger(OperationsView.class);

	@FXML private ListView<Operation> opsListView;
	@FXML private Button backButton;
	@FXML private Button forwardButton;
	@FXML private Button searchButton;
	@FXML private Button sortButton;
	@FXML private ToggleButton disabledOpsToggle;
	@FXML private TextField filterEvents;
	@FXML private TextField randomText;
	@FXML private MenuItem oneRandomEvent;
	@FXML private MenuItem fiveRandomEvents;
	@FXML private MenuItem tenRandomEvents;
	@FXML private CustomMenuItem someRandomEvents;

	private AbstractModel currentModel;
	private List<String> opNames = new ArrayList<>();
	private Map<String, List<String>> opToParams = new HashMap<>();
	private List<Operation> events = new ArrayList<>();
	private boolean showNotEnabled = true;
	private String filter = "";
	private SortMode sortMode = SortMode.MODEL_ORDER;
	private final CurrentTrace currentTrace;
	
	private final Comparator<Operation> zToA = (o1, o2) -> {
		if (o1.name.equals(o2.name)) {
			return -compareParams(o1.params, o2.params);
		} else if (o1.name.equalsIgnoreCase(o2.name)) {
			return -o1.name.compareTo(o2.name);
		} else {
			return -o1.name.compareToIgnoreCase(o2.name);
		}
	};
	
	private final Comparator<Operation> aToZ = (o1, o2) -> {
		if (o1.name.equals(o2.name)) {
			return compareParams(o1.params, o2.params);
		} else if (o1.name.equalsIgnoreCase(o2.name)) {
			return o1.name.compareTo(o2.name);
		} else {
			return o1.name.compareToIgnoreCase(o2.name);
		}
	};
	
	private final Comparator<Operation> modelOrder = (o1, o2) -> {
		if (o1.name.equals(o2.name)) {
			return compareParams(o1.params, o2.params);
		} else {
			return Integer.compare(opNames.indexOf(o1.name), opNames.indexOf(o2.name));
		}
	};

	@Inject
	private OperationsView(final CurrentTrace currentTrace, final FXMLLoader loader) {
		this.currentTrace = currentTrace;

		loader.setLocation(getClass().getResource("ops_view.fxml"));
		loader.setRoot(this);
		loader.setController(this);
		try {
			loader.load();
		} catch (IOException e) {
			logger.error("loading fxml failed", e);
		}

	}

	@FXML
	public void initialize() {
		opsListView.setCellFactory(lv -> new OperationsCell());

		opsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null && newValue.isEnabled()) {
				currentTrace.set(currentTrace.get().add(newValue.id));
				logger.debug("Selected item: " + newValue);
			}
		});

		backButton.disableProperty().bind(currentTrace.canGoBackProperty().not());
		forwardButton.disableProperty().bind(currentTrace.canGoForwardProperty().not());

		currentTrace.addListener((observable, from, to) -> update(to));
	}

	private void update(Trace trace) {
		if (trace == null) {
			currentModel = null;
			opNames = new ArrayList<>();
			Platform.runLater(opsListView.getItems()::clear);
			return;
		}

		if (trace.getModel() != currentModel) {
			updateModel(trace);
		}
		events = new ArrayList<>();
		Set<Transition> operations = trace.getNextTransitions(true);
		Set<String> notEnabled = new HashSet<>(opNames);
		Set<String> withTimeout = trace.getCurrentState().getTransitionsWithTimeout();
		for (Transition transition : operations) {
			final String name = extractPrettyName(transition.getName());
			notEnabled.remove(name);
			final boolean explored = transition.getDestination().isExplored();
			final boolean errored = explored && !transition.getDestination().isInvariantOk();
			logger.debug("{} {}", name, errored);
			Operation operation = new Operation(
				transition.getId(),
				name,
				transition.getParams(),
				transition.getReturnValues(),
				withTimeout.contains(name) ? Operation.Status.TIMEOUT : Operation.Status.ENABLED,
				explored,
				errored
			);
			events.add(operation);
		}
		if (showNotEnabled) {
			for (String s : notEnabled) {
				if (!"INITIALISATION".equals(s)) {
					events.add(new Operation(s, s, opToParams.get(s), Collections.emptyList(), Operation.Status.DISABLED, false, false));
				}
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
	
	private static int compareParams(final List<String> params1, final List<String> params2) {
		for (int i = 0; i < params1.size(); i++) {
			String p1 = stripString(params1.get(i));
			String p2 = stripString(params2.get(i));
			if (p1.compareTo(p2) != 0) {
				return p1.compareTo(p2);
			}
			
		}
		return 0;
	}

	@FXML
	private void handleDisabledOpsToggle() {
		FontAwesomeIconView icon;
		if (disabledOpsToggle.isSelected()) {
			icon = new FontAwesomeIconView(FontAwesomeIcon.EYE_SLASH);
			showNotEnabled = false;
		} else {
			icon = new FontAwesomeIconView(FontAwesomeIcon.EYE);
			showNotEnabled = true;
		}
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

	private List<Operation> applyFilter(final String filter) {
		List<Operation> newOps = new ArrayList<>();
		for (Operation op : events) {
			if (op.name.startsWith(filter)) {
				newOps.add(op);
			}
		}
		return newOps;
	}

	private void doSort() {
		final Comparator<Operation> comparator;
		switch (sortMode) {
		case MODEL_ORDER:
			comparator = modelOrder;
			break;

		case A_TO_Z:
			comparator = aToZ;
			break;

		case Z_TO_A:
			comparator = zToA;
			break;

		default:
			throw new IllegalStateException("Unhandled sort mode: " + sortMode);
		}

		Collections.sort(events, comparator);
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
				// FIXME We should not just throw an exception! I think it is
				// possible to add a validator function
				try {
					int steps = Integer.parseInt(randomText.getText());
					currentTrace.set(currentTrace.get().randomAnimation(steps));
				} catch (NumberFormatException e) {
					logger.error("invalid number", e);
				}
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
		} else if (e instanceof de.prob.model.classicalb.Operation) {
			paramList.addAll(((de.prob.model.classicalb.Operation) e).getParameters());
		}
		return paramList;
	}
}
