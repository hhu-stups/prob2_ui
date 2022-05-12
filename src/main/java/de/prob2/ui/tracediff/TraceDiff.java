package de.prob2.ui.tracediff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.domainobjects.FormulaExpand;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.exception.ProBError;
import de.prob.statespace.LoadedMachine;
import de.prob.statespace.OperationInfo;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.preferences.PreferencesStage;

import javafx.application.Platform;
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

@FXMLInjected
@Singleton
public class TraceDiff extends VBox {
	@FXML private CheckBox linkScrolling;

	@FXML private Label replayed;

	@FXML private ListView<TraceDiffItem> replayedList;
	@FXML private ListView<TraceDiffItem> persistentList;

	@FXML private Button showAlert;

	@FXML private VBox persistentBox;

	@FXML private HBox listBox;
	@FXML private HBox buttonBox;

	private final Injector injector;
	private final ResourceBundle bundle;
	private TraceReplayErrorAlert alert;
	private ArrayList<ListView<TraceDiffItem>> listViews = new ArrayList<>();
	private List<ScrollBar> scrollBarList = new ArrayList<>();
	private static int minSize = -1;
	private int maxSize = -1;
	static HashMap<Integer, Integer> indexLinesMap = new HashMap<>();
	private static String openingBrace;
	private static String argDelimiter;

	static class TraceDiffList extends ArrayList<TraceDiffItem> {
		//TODO get variables for textual diff -> TraceChecker
		TraceDiffList(List<?> list) {
			for (int i = 0; i < list.size(); i++) {
				String s = getRep(list.get(i));
				this.add(new TraceDiffItem(i, s));
				int lines = s == null? 0 : s.split("\n").length;
				if (TraceDiff.indexLinesMap.get(i) == null || TraceDiff.indexLinesMap.get(i) < lines) {
					TraceDiff.indexLinesMap.put(i,lines);
				}
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
				stringBuilder.append(openingBrace);
				stringBuilder.append(String.join(argDelimiter, args));
				stringBuilder.append(')');
			}

			if (t.getReturnValues() != null && !t.getReturnValues().isEmpty()) {
				stringBuilder.append(" → ");
				stringBuilder.append(String.join(argDelimiter, t.getReturnValues()));
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
				stringBuilder.append(openingBrace);
				stringBuilder.append(String.join(argDelimiter, args));
				stringBuilder.append(')');
			}

			if (t.getOutputParameters() != null && !t.getOutputParameters().isEmpty()) {
				stringBuilder.append(" → ");
				stringBuilder.append(String.join(argDelimiter, t.getOutputParameters().values()));
			}
			return stringBuilder.toString();
		}

		public boolean add(String s) {
			return super.add(new TraceDiffItem(this.size(), s));
		}
	}

	@Inject
	private TraceDiff(StageManager stageManager, Injector injector) {
		this.injector = injector;
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

		// Arrow key and scrollbar synchronicity
		ChangeListener<? super Number> listViewCL = createChangeListenerForListView();
		linkScrolling.selectedProperty().addListener(createChangeListenerForCheckBox(listViewCL));

		// Renew scrollbar list if stage was resized
		Platform.runLater(() -> {
			ChangeListener<? super Number> stageSizeChangelistener = (observable, oldValue, newValue) -> getScrollBars();
			this.getScene().getWindow().heightProperty().addListener(stageSizeChangelistener);
			this.getScene().getWindow().widthProperty().addListener(stageSizeChangelistener);
		});

		String typeOfTraceDiff = injector.getInstance(PreferencesStage.class).getTraceDiffType();
		switch (typeOfTraceDiff) {
			case "multipleLines":
				openingBrace = "\n(";
				argDelimiter = ",\n";
				break;
			case "singleLines":
			default:
				openingBrace = "(";
				argDelimiter = ", ";
				break;
		}
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

	final void setLists(Trace replayed, TraceJsonFile stored) {
		List<Transition> rTransitions = replayed.getTransitionList();
		List<PersistentTransition> pTransitions = stored.getTransitionList();

		maxSize = Math.max(rTransitions.size(), pTransitions.size());
		minSize = Math.min(rTransitions.size(), pTransitions.size());

		indexLinesMap.clear();
		translateLists(new TraceDiffList(rTransitions), replayedList, new TraceDiffList(pTransitions), persistentList);

		showAlert.setOnAction(e -> alert.showAlertAgain());
	}

	final void setLists(Trace lost, Trace history) {
		List<Transition> lTransitions = lost.getTransitionList();
		List<Transition> hTransitions = history.getTransitionList();

		maxSize = Math.max(lTransitions.size(), hTransitions.size());
		minSize = Math.min(lTransitions.size(), hTransitions.size());

		indexLinesMap.clear();
		translateLists(new TraceDiffList(lTransitions), replayedList, new TraceDiffList(hTransitions), persistentList);

		showAlert.setOnAction(e -> alert.showAlertAgain());
	}

	private void translateLists(TraceDiffList replayedList, ListView<TraceDiffItem> replayedView, TraceDiffList persistentList, ListView<TraceDiffItem> persistentView) {
		//Add "empty" entries to ensure same length (needed for synchronized scrolling)
		while (replayedList.size() < maxSize) {
			replayedList.add("");
		}
		while (persistentList.size() < maxSize) {
			persistentList.add("");
		}

		//Compare entries of the lists to determine colour (lists are now same length)
		for (int i = 0; i < persistentList.size(); i++) {
			TraceDiffItem replayedItem = replayedList.get(i);
			TraceDiffItem persistentItem = persistentList.get(i);
			//Blue if different size (empty entries on one side as generated above)
			if (replayedItem.getString().isEmpty()) {
				persistentItem.setStyle(TraceDiffCell.TraceDiffCellStyle.FOLLOWING);
				continue;
			}
			if (persistentItem.getString().isEmpty()) {
				replayedItem.setStyle(TraceDiffCell.TraceDiffCellStyle.FOLLOWING);
				continue;
			}
			//Red if different entries
			if (!persistentItem.getString().equals(replayedItem.getString())) {
				persistentItem.setStyle(TraceDiffCell.TraceDiffCellStyle.FAULTY);
				replayedItem.setStyle(TraceDiffCell.TraceDiffCellStyle.FAULTY);
			}
		}
		//Set Cell Factories and fill List Views
		replayedView.setCellFactory(param -> new TraceDiffCell());
		replayedView.setItems(FXCollections.observableList(replayedList));
		persistentView.setCellFactory(param -> new TraceDiffCell());
		persistentView.setItems(FXCollections.observableList(persistentList));
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
