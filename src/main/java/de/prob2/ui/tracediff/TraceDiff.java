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
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@FXMLInjected
@Singleton
public class TraceDiff extends HBox {
	@FXML private Label replayed;
	@FXML private Label persistent;
	@FXML private Label current;
	@FXML private ListView<String> replayedList;
	@FXML private ListView<String> persistentList;
	@FXML private ListView<String> currentList;
	@FXML private Button setReplayed;
	@FXML private Button showAlert;
	@FXML private Button setCurrent;
	@FXML private VBox persistentBox;
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
		persistentBox.setAlignment(Pos.BOTTOM_CENTER);
	}

	void setLists(Trace replayedOrLost, PersistentTrace persistent, Trace current) {
		replayedList.setItems(FXCollections.observableList(replayedOrLost.getTransitionList().stream().map(t -> getTransitionRep(t)).collect(Collectors.toList())));
		// if triggered by HistoryView: No persistent trace available
		if (persistent != null) {
			persistentList.setItems(FXCollections.observableList(persistent.getTransitionList().stream().map(t -> getPersistentTransitionRep(t)).collect(Collectors.toList())));
		}
		currentList.setItems(FXCollections.observableList(current.getTransitionList().stream().map(t -> getTransitionRep(t)).collect(Collectors.toList())));

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

	private String getTransitionRep(Transition t) {
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

		if ("$setup_constants".equals(t.getName()) && t.getDestination().getConstantValues(FormulaExpand.EXPAND) != null && !t.getDestination().getConstantValues(FormulaExpand.EXPAND).isEmpty()) {
			t.getDestination().getConstantValues(FormulaExpand.EXPAND).forEach((iEvalElement, abstractEvalResult) -> args.add(iEvalElement + ":=" + abstractEvalResult));
		} else if ("$initialise_machine".equals(t.getName()) && t.getDestination().getVariableValues(FormulaExpand.EXPAND) != null && !t.getDestination().getVariableValues(FormulaExpand.EXPAND).isEmpty()) {
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

	private String getPersistentTransitionRep(PersistentTransition t) {
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

			if (!persistentBox.getChildren().contains(persistent)) {
				persistentBox.getChildren().remove(showAlert);
				persistentBox.getChildren().addAll(persistent, persistentList, showAlert);
			}
		} else {
			//TODO: integrate "show alert again" button
			replayed.setText(bundle.getString("history.buttons.saveTrace.error.lost"));

			if (persistentBox.getChildren().contains(persistent)) {
				persistentBox.getChildren().removeAll(persistent, persistentList);
			}
		}
	}
}
