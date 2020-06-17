package de.prob2.ui.tracediff;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.exception.ProBError;
import de.prob.statespace.LoadedMachine;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@FXMLInjected
@Singleton
public class TraceDiff extends VBox {
	@FXML private CheckBox replayed;
	@FXML private CheckBox persistent;
	@FXML private CheckBox current;

	@FXML private ListView<String> replayedList;
	@FXML private ListView<String> persistentList;
	@FXML private ListView<String> currentList;

	@FXML private Button setReplayed;
	@FXML private Button showAlert;
	@FXML private Button savePersistent;
	@FXML private Button setCurrent;

	@FXML private VBox persistentBox;

	@FXML private HBox listBox;
	@FXML private HBox buttonBox;

	private ResourceBundle bundle;
	private CurrentTrace currentTrace;
	private Alert alert;
	private Injector injector;
	private Map<CheckBox,ListView<String>> checkBoxListViewMap = new HashMap<>();

	@Inject
	private TraceDiff(StageManager stageManager, Injector injector, CurrentTrace currentTrace) {
		this.bundle = injector.getInstance(ResourceBundle.class);
		this.currentTrace = currentTrace;
		this.injector = injector;
		stageManager.loadFXML(this,"trace_diff.fxml");
	}

	@FXML
	private void initialize() {
		this.setPadding(new Insets(5,5,5,5));
		double initialWidth = this.getWidth()/4;
		setReplayed.setPrefWidth(initialWidth);
		showAlert.setPrefWidth(initialWidth);
		savePersistent.setPrefWidth(initialWidth);
		setCurrent.setPrefWidth(initialWidth);

		this.checkBoxListViewMap.put(replayed, replayedList);
		this.checkBoxListViewMap.put(persistent, persistentList);
		this.checkBoxListViewMap.put(current, currentList);

		// Arrow key synchronicity
		ChangeListener<? super Number> replayedListCL = createChangeListenerForListView(persistent, current);
		ChangeListener<? super Number> persistentListCL = createChangeListenerForListView(replayed, current);
		ChangeListener<? super Number> currentListCL = createChangeListenerForListView(replayed, persistent);

		replayed.selectedProperty().addListener(createChangeListenerForCheckBox(replayedList, replayedListCL));
		persistent.selectedProperty().addListener(createChangeListenerForCheckBox(persistentList, persistentListCL));
		current.selectedProperty().addListener(createChangeListenerForCheckBox(currentList, currentListCL));

		// Scrollbar synchronicity
		replayed.selectedProperty().addListener(createChangeListenerForCheckBox(replayed));
		persistent.selectedProperty().addListener(createChangeListenerForCheckBox(persistent));
		current.selectedProperty().addListener(createChangeListenerForCheckBox(current));
	}

	private ScrollBar getScrollBar(ListView<String> listView) {
		return (ScrollBar) listView.lookup(".scroll-bar:vertical");
	}

	private ChangeListener<? super Boolean> createChangeListenerForCheckBox(CheckBox checkBox) {
		return (obs, o, n) -> {
			ScrollBar related = getScrollBar(checkBoxListViewMap.get(checkBox));
			if (related != null) {
				if (n) {
					for (CheckBox cb : checkBoxListViewMap.keySet()) {
						ScrollBar scrollBar = getScrollBar(checkBoxListViewMap.get(cb));
						if (cb != checkBox && scrollBar != null && cb.isSelected()) {
							related.valueProperty().bindBidirectional(scrollBar.valueProperty());
						}
					}
				} else {
					for (CheckBox cb : checkBoxListViewMap.keySet()) {
						ScrollBar scrollBar = getScrollBar(checkBoxListViewMap.get(cb));
						if (cb != checkBox && scrollBar != null) {
							related.valueProperty().unbindBidirectional(scrollBar.valueProperty());
						}
					}
				}
			}
		};
	}

	private ChangeListener<? super Boolean> createChangeListenerForCheckBox(ListView<String> listView, ChangeListener<? super Number> listViewChangeListener) {
		return (obs, o, n) -> {
			if (n) {
				listView.getSelectionModel().selectedIndexProperty().addListener(listViewChangeListener);
			} else {
				listView.getSelectionModel().selectedIndexProperty().removeListener(listViewChangeListener);
			}
		};
	}

	private ChangeListener<? super Number> createChangeListenerForListView(CheckBox firstCB, CheckBox secondCB) {
		return (obs, o, n) -> {
			int value = n.intValue();
			scrollToEntry(firstCB, value);
			scrollToEntry(secondCB, value);
		};
	}

	private void scrollToEntry(CheckBox cb, int value) {
		if (cb.isSelected()) {
			ListView<String> lv = checkBoxListViewMap.get(cb);
			lv.getSelectionModel().select(value);
			lv.getFocusModel().focus(value);
			lv.scrollTo(value);
		}
	}

	void setLists(Trace replayedOrLost, PersistentTrace persistent, Trace current) {
		List<Transition> rTransitions = replayedOrLost.getTransitionList();
		List<PersistentTransition> pTransitions;
		// if triggered by HistoryView: No persistent trace available
		if (persistent == null) {
			pTransitions = FXCollections.emptyObservableList();
		} else {
			pTransitions = persistent.getTransitionList();
		}
		List<Transition> cTransitions = current.getTransitionList();

		int maxSize = Math.max(Math.max(rTransitions.size(), pTransitions.size()), cTransitions.size());

		translateList(rTransitions, replayedList, maxSize);
		translateList(pTransitions, persistentList, maxSize);
		translateList(cTransitions, currentList, maxSize);

		setReplayed.setOnAction(e -> {
			currentTrace.set(replayedOrLost);
			this.getScene().getWindow().hide();
		});
		showAlert.setOnAction(e -> {
			if (alert instanceof TraceReplayErrorAlert) {
				TraceChecker traceChecker = injector.getInstance(TraceChecker.class);
				traceChecker.handleAlert(alert, replayedOrLost, persistent, alert.getButtonTypes().get(0));
			} else {
				HistoryView historyView = injector.getInstance(HistoryView.class);
				historyView.handleAlert(alert, replayedOrLost, alert.getButtonTypes().get(0));
			}
		});
		savePersistent.setOnAction(e -> injector.getInstance(TraceFileHandler.class).save(persistent, injector.getInstance(CurrentProject.class).getCurrentMachine()));
		setCurrent.setOnAction(e -> {
			currentTrace.set(current);
			this.getScene().getWindow().hide();
		});
	}

	private void translateList(List<?> list, ListView<String> listView, int maxSize) {
		List<String> stringList = list.stream().map(this::getRep).collect(Collectors.toList());
		//Add "empty" entries to ensure same length (needed for synchronized scrolling)
		while (stringList.size() < maxSize) {
			stringList.add("");
		}
		listView.setItems(FXCollections.observableList(stringList));
	}

	private String getRep(Object t) {
		if (t instanceof Transition) {
			return getRep((Transition) t);
		} else if (t instanceof PersistentTransition) {
			return getRep((PersistentTransition) t);
		}
		return null;
	}

	private String getRep(Transition t) {
		LoadedMachine loadedMachine = t.getStateSpace().getLoadedMachine();
		OperationInfo opInfo;
		try {
			opInfo = loadedMachine.getMachineOperationInfo(t.getName());
		} catch (ProBError e) {
			opInfo = null;
		}
		List<String> paramNames = opInfo == null ? Collections.emptyList() : opInfo.getParameterNames();

		StringBuilder stringBuilder;
		if ("$setup_constants".equals(t.getName())) {
			stringBuilder = new StringBuilder("SETUP_CONSTANTS");
		} else if ("$initialise_machine".equals(t.getName())){
			stringBuilder = new StringBuilder("INITIALISATION");
		} else {
			stringBuilder = new StringBuilder(t.getName());
		}

		List<String> args = new ArrayList<>();
		List<String> paramValues = t.getParameterValues();
		if (paramNames.isEmpty()) {
			args.addAll(paramValues);
		} else if (paramValues.isEmpty()) {
			args.addAll(paramNames);
		} else if (paramNames.size() == paramValues.size()){
			for (int i = 0; i<paramNames.size(); i++) {
				args.add(paramNames.get(i) + '=' + paramValues.get(i));
			}
		}

		if ("$setup_constants".equals(t.getName())
				&& t.getDestination().getConstantValues(FormulaExpand.EXPAND) != null
				&& !t.getDestination().getConstantValues(FormulaExpand.EXPAND).isEmpty()) {
			t.getDestination().getConstantValues(FormulaExpand.EXPAND).forEach((iEvalElement, abstractEvalResult) -> args.add(iEvalElement + ":=" + abstractEvalResult));
		} else if ("$initialise_machine".equals(t.getName())
				&& t.getDestination().getVariableValues(FormulaExpand.EXPAND) != null
				&& !t.getDestination().getVariableValues(FormulaExpand.EXPAND).isEmpty()) {
			t.getDestination().getVariableValues(FormulaExpand.EXPAND).forEach((iEvalElement, abstractEvalResult) -> args.add(iEvalElement + ":=" + abstractEvalResult));
		}

		if (!args.isEmpty()) {
			stringBuilder.append('(');
			stringBuilder.append(String.join(", ", args));
			stringBuilder.append(')');
		}

		if (t.getReturnValues() != null && !t.getReturnValues().isEmpty()) {
			stringBuilder.append(" → ");
			stringBuilder.append(String.join(", ", t.getReturnValues()));
		}

		return stringBuilder.toString();
	}

	private String getRep(PersistentTransition t) {
		StringBuilder stringBuilder;
		boolean isArtificialTransition = false;
		if ("$setup_constants".equals(t.getOperationName())) {
			stringBuilder = new StringBuilder("SETUP_CONSTANTS");
			isArtificialTransition = true;
		} else if ("$initialise_machine".equals(t.getOperationName())){
			stringBuilder = new StringBuilder("INITIALISATION");
			isArtificialTransition = true;
		} else {
			stringBuilder = new StringBuilder(t.getOperationName());
		}

		List<String> args = new ArrayList<>();
		if (t.getParameters()!=null && !t.getParameters().isEmpty()) {
			t.getParameters().forEach((str1, str2) -> args.add(str1 + "=" + str2));
		} else if (isArtificialTransition && t.getDestinationStateVariables() != null && !t.getDestinationStateVariables().isEmpty()) {
			t.getDestinationStateVariables().forEach((str1, str2) -> args.add(str1 + ":=" + str2));
		}

		if (!args.isEmpty()) {
			stringBuilder.append('(');
			stringBuilder.append(String.join(", ", args));
			stringBuilder.append(')');
		}

		if (t.getOuputParameters() != null && !t.getOuputParameters().isEmpty()) {
			stringBuilder.append(" → ");
			stringBuilder.append(String.join(", ", t.getOuputParameters().values()));
		}
		return stringBuilder.toString();
	}

	void setAlert(Alert alert) {
		this.alert = alert;
		// the alert is either a TraceReplayErrorAlert or triggered by trying to save a trace
		if (alert instanceof TraceReplayErrorAlert) {
			replayed.setText(bundle.getString("animation.tracereplay.alerts.traceReplayError.error.traceDiff.replayed"));
			if (!listBox.getChildren().contains(persistentBox)) {
			 	listBox.getChildren().add(persistentBox);
			}
			if (!buttonBox.getChildren().contains(savePersistent)) {
				buttonBox.getChildren().add(savePersistent);
			}
		} else {
			replayed.setText(bundle.getString("history.buttons.saveTrace.error.lost"));
			if (listBox.getChildren().contains(persistentBox)) {
				persistent.setSelected(false);
				listBox.getChildren().remove(persistentBox);
			}
			if (buttonBox.getChildren().contains(savePersistent)) {
				buttonBox.getChildren().remove(savePersistent);
			}
		}
	}
}
