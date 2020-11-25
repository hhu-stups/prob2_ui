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
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

@FXMLInjected
@Singleton
public class TraceDiff extends VBox {
	@FXML private CheckBox linkScrolling;

	@FXML private Label replayed;

	@FXML private ListView<TraceDiffItem> replayedList;
	@FXML private ListView<TraceDiffItem> persistentList;
	@FXML private ListView<TraceDiffItem> currentList;

	@FXML private Button showAlert;

	@FXML private VBox persistentBox;

	@FXML private HBox listBox;
	@FXML private HBox buttonBox;

	private final ResourceBundle bundle;
	private TraceReplayErrorAlert alert;
	private ArrayList<ListView<TraceDiffItem>> listViews = new ArrayList<>();
	private List<ScrollBar> scrollBarList = new ArrayList<>();
	private static int minSize = -1;
	private int maxSize = -1;

	static class TraceDiffList extends ArrayList<TraceDiffItem> {
		TraceDiffList(List<?> list) {
			for (int i = 0; i < list.size(); i++) {
				String s = getRep(list.get(i));
				this.add(new TraceDiffItem(i, s));
			}
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

			StringBuilder stringBuilder = new StringBuilder(t.getPrettyName());

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

			if (Transition.SETUP_CONSTANTS_NAME.equals(t.getName())
					&& t.getDestination().getConstantValues(FormulaExpand.TRUNCATE) != null
					&& !t.getDestination().getConstantValues(FormulaExpand.TRUNCATE).isEmpty()) {
				t.getDestination().getConstantValues(FormulaExpand.TRUNCATE).forEach((iEvalElement, abstractEvalResult) -> args.add(iEvalElement + ":=" + abstractEvalResult));
			} else if (Transition.INITIALISE_MACHINE_NAME.equals(t.getName())
					&& t.getDestination().getVariableValues(FormulaExpand.TRUNCATE) != null
					&& !t.getDestination().getVariableValues(FormulaExpand.TRUNCATE).isEmpty()) {
				t.getDestination().getVariableValues(FormulaExpand.TRUNCATE).forEach((iEvalElement, abstractEvalResult) -> args.add(iEvalElement + ":=" + abstractEvalResult));
			}

			if (!args.isEmpty()) {
				stringBuilder.append('(');
				stringBuilder.append(String.join(",\n", args));
				stringBuilder.append(')');
			}

			if (t.getReturnValues() != null && !t.getReturnValues().isEmpty()) {
				stringBuilder.append(" → ");
				stringBuilder.append(String.join(",\n", t.getReturnValues()));
			}

			return stringBuilder.toString();
		}

		private String getRep(PersistentTransition t) {
			final StringBuilder stringBuilder = new StringBuilder(Transition.prettifyName(t.getOperationName()));
			boolean isArtificialTransition = Transition.isArtificialTransitionName(t.getOperationName());

			List<String> args = new ArrayList<>();
			if (t.getParameters()!=null && !t.getParameters().isEmpty()) {
				t.getParameters().forEach((str1, str2) -> args.add(str1 + "=" + str2));
			} else if (isArtificialTransition && t.getDestinationStateVariables() != null && !t.getDestinationStateVariables().isEmpty()) {
				t.getDestinationStateVariables().forEach((str1, str2) -> args.add(str1 + ":=" + str2));
			}

			if (!args.isEmpty()) {
				stringBuilder.append('(');
				stringBuilder.append(String.join(",\n", args));
				stringBuilder.append(')');
			}

			if (t.getOutputParameters() != null && !t.getOutputParameters().isEmpty()) {
				stringBuilder.append(" → ");
				stringBuilder.append(String.join(",\n", t.getOutputParameters().values()));
			}
			return stringBuilder.toString();
		}

		public boolean add(String s) {
			return super.add(new TraceDiffItem(this.size(), s));
		}
	}

	@Inject
	private TraceDiff(StageManager stageManager, Injector injector) {
		this.bundle = injector.getInstance(ResourceBundle.class);
		stageManager.loadFXML(this,"trace_diff.fxml");
	}

	@FXML
	private void initialize() {
		this.setPadding(new Insets(5,5,5,5));
		double initialWidth = this.getWidth();
		showAlert.setPrefWidth(initialWidth);

		this.listViews.add(replayedList);
		this.listViews.add(persistentList);
		this.listViews.add(currentList);

		// Arrow key and scrollbar synchronicity
		ChangeListener<? super Number> listViewCL = createChangeListenerForListView();
		linkScrolling.selectedProperty().addListener(createChangeListenerForCheckBox(listViewCL));
	}

	private void getScrollBars() {
		scrollBarList.clear();
		listViews.forEach(lv -> {
			ScrollBar sb = (ScrollBar) lv.lookup(".scroll-bar:vertical");
			if (sb != null) {
				scrollBarList.add(sb);
			}
		});
	}

	private ChangeListener<? super Boolean> createChangeListenerForCheckBox(ChangeListener<? super Number> listViewChangeListener) {
		return (obs, o, n) -> {
			if (n) {
				// Arrow key synchronicity
				listViews.forEach(lv -> lv.getSelectionModel().selectedIndexProperty().addListener(listViewChangeListener));
				// Scrollbar synchronicity
				getScrollBars();
				for (int i = 0; i < scrollBarList.size()-1; i++) {
					scrollBarList.get(i).valueProperty().bindBidirectional(scrollBarList.get(i+1).valueProperty());
				}
			} else {
				listViews.forEach(lv -> lv.getSelectionModel().selectedIndexProperty().removeListener(listViewChangeListener));
				for (int i = 0; i < scrollBarList.size()-1; i++) {
					scrollBarList.get(i).valueProperty().unbindBidirectional(scrollBarList.get(i+1).valueProperty());
				}
			}
		};
	}

	private ChangeListener<? super Number> createChangeListenerForListView() {
		return (obs, o, n) -> {
			int value = n.intValue();
			listViews.forEach(lv -> scrollToEntry(lv, value));
		};
	}

	private void scrollToEntry(ListView<TraceDiffItem> lv, int value) {
		if (linkScrolling.isSelected()) {
			lv.getSelectionModel().select(value);
			lv.getFocusModel().focus(value);
			lv.scrollTo(value);
		}
	}

	final void setLists(Trace replayedOrLost, PersistentTrace persistent, Trace current) {
		List<Transition> rTransitions = replayedOrLost.getTransitionList();
		List<PersistentTransition> pTransitions;
		// if triggered by HistoryView: No persistent trace available
		if (persistent == null) {
			pTransitions = FXCollections.emptyObservableList();
		} else {
			pTransitions = persistent.getTransitionList();
		}
		List<Transition> cTransitions = current.getTransitionList();

		maxSize = Math.max(rTransitions.size(), pTransitions.size());
		minSize = Math.min(rTransitions.size(), pTransitions.size());

		translateList(new TraceDiffList(rTransitions), replayedList);
		translateList(new TraceDiffList(pTransitions), persistentList);
		translateList(new TraceDiffList(cTransitions), currentList);

		showAlert.setOnAction(e -> alert.showAlertAgain());
	}

	private void translateList(TraceDiffList stringList, ListView<TraceDiffItem> listView) {
		//Add "empty" entries to ensure same length (needed for synchronized scrolling)
		while (stringList.size() < maxSize) {
			stringList.add("");
		}
		// Mark faulty operation index red and following operation indices blue -> TraceDiffCell
		listView.setCellFactory(param -> new TraceDiffCell());
		listView.setItems(FXCollections.observableList(stringList));
	}

	void setAlert(TraceReplayErrorAlert alert) {
		this.alert = alert;
		if (alert.getTrigger().equals(TraceReplayErrorAlert.Trigger.TRIGGER_HISTORY_VIEW)) {
			replayed.setText(bundle.getString("history.buttons.saveTrace.error.lost"));
			if (listBox.getChildren().contains(persistentBox)) {
				listBox.getChildren().remove(persistentBox);
			}
		} else {
			replayed.setText(bundle.getString("animation.tracereplay.alerts.traceReplayError.error.traceDiff.replayed"));
			if (!listBox.getChildren().contains(persistentBox)) {
				listBox.getChildren().add(persistentBox);
			}
		}
	}

	static int getMinSize() {
		return minSize;
	}
}
