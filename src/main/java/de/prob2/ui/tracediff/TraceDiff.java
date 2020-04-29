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
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
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
import java.util.List;
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
	@FXML private Button setCurrent;

	@FXML private VBox persistentBox;

	@FXML private HBox listBox;

	private ResourceBundle bundle;
	private CurrentTrace currentTrace;
	private Alert alert;
	private Injector injector;

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
		double initialWidth = this.getWidth()/3;
		setReplayed.setPrefWidth(initialWidth);
		showAlert.setPrefWidth(initialWidth);
		setCurrent.setPrefWidth(initialWidth);

		// Arrow key synchronicity
		ChangeListener<? super Number> replayedListCL = createChangeListener(persistent, persistentList, current, currentList);
		ChangeListener<? super Number> persistentListCL = createChangeListener(replayed, replayedList, current, currentList);
		ChangeListener<? super Number> currentListCL = createChangeListener(replayed, replayedList, persistent, persistentList);

		replayed.selectedProperty().addListener(((observable, oldValue, newValue) -> {
			if (newValue) {
				replayedList.getSelectionModel().selectedIndexProperty().addListener(replayedListCL);
			} else {
				replayedList.getSelectionModel().selectedIndexProperty().removeListener(replayedListCL);
			}
		}));

		persistent.selectedProperty().addListener(((observable, oldValue, newValue) -> {
			if (newValue) {
				persistentList.getSelectionModel().selectedIndexProperty().addListener(persistentListCL);
			} else {
				persistentList.getSelectionModel().selectedIndexProperty().removeListener(persistentListCL);
			}
		}));

		current.selectedProperty().addListener(((observable, oldValue, newValue) -> {
			if (newValue) {
				currentList.getSelectionModel().selectedIndexProperty().addListener(currentListCL);
			} else {
				currentList.getSelectionModel().selectedIndexProperty().removeListener(currentListCL);
			}
		}));

		// Scrollbar synchronicity
		replayed.selectedProperty().addListener((observable, oldValue, newValue) -> {
			ScrollBar rsc = (ScrollBar) replayedList.lookup(".scroll-bar:vertical");
			ScrollBar psc = (ScrollBar) persistentList.lookup(".scroll-bar:vertical");
			ScrollBar csc = (ScrollBar) currentList.lookup(".scroll-bar:vertical");
			if (rsc != null) {
				if (newValue) {
					if (persistent.isSelected() && psc != null) {
						rsc.valueProperty().bindBidirectional(psc.valueProperty());
					}
					if (current.isSelected() && csc != null) {
						rsc.valueProperty().bindBidirectional(csc.valueProperty());
					}
				} else {
					rsc.valueProperty().unbindBidirectional(psc.valueProperty());
					rsc.valueProperty().unbindBidirectional(csc.valueProperty());
				}
			}
		});

		persistent.selectedProperty().addListener((observable, oldValue, newValue) -> {
			ScrollBar rsc = (ScrollBar) replayedList.lookup(".scroll-bar:vertical");
			ScrollBar psc = (ScrollBar) persistentList.lookup(".scroll-bar:vertical");
			ScrollBar csc = (ScrollBar) currentList.lookup(".scroll-bar:vertical");
			if (psc != null) {
				if (newValue) {
					if (replayed.isSelected() && rsc != null) {
						psc.valueProperty().bindBidirectional(rsc.valueProperty());
					}
					if (current.isSelected() && csc != null) {
						psc.valueProperty().bindBidirectional(csc.valueProperty());
					}
				} else {
					psc.valueProperty().unbindBidirectional(rsc.valueProperty());
					psc.valueProperty().unbindBidirectional(csc.valueProperty());
				}
			}
		});

		current.selectedProperty().addListener((observable, oldValue, newValue) -> {
			ScrollBar rsc = (ScrollBar) replayedList.lookup(".scroll-bar:vertical");
			ScrollBar psc = (ScrollBar) persistentList.lookup(".scroll-bar:vertical");
			ScrollBar csc = (ScrollBar) currentList.lookup(".scroll-bar:vertical");
			if (csc != null) {
				if (newValue) {
					if (replayed.isSelected() && rsc != null) {
						csc.valueProperty().bindBidirectional(rsc.valueProperty());
					}
					if (persistent.isSelected() && psc != null) {
						csc.valueProperty().bindBidirectional(psc.valueProperty());
					}
				} else {
					csc.valueProperty().unbindBidirectional(rsc.valueProperty());
					csc.valueProperty().unbindBidirectional(psc.valueProperty());
				}
			}
		});
	}

	private ChangeListener<? super Number> createChangeListener(CheckBox firstCB, ListView firstLV, CheckBox secondCB, ListView secondLV) {
		return (obs, o, n) -> {
			if (firstCB.isSelected()) {
				firstLV.getSelectionModel().select(n.intValue());
				firstLV.getFocusModel().focus(n.intValue());
				firstLV.scrollTo(n.intValue());
			}

			if (secondCB.isSelected()) {
				secondLV.getSelectionModel().select(n.intValue());
				secondLV.getFocusModel().focus(n.intValue());
				secondLV.scrollTo(n.intValue());
			}
		};
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
		} else {
			replayed.setText(bundle.getString("history.buttons.saveTrace.error.lost"));
			if (listBox.getChildren().contains(persistentBox)) {
				persistent.setSelected(false);
				listBox.getChildren().remove(persistentBox);
			}
		}
	}
}
