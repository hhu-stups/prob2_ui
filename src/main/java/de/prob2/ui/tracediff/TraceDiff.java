package de.prob2.ui.tracediff;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.TraceChecker;
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.history.HistoryView;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ResourceBundle;
import java.util.stream.Collectors;

@FXMLInjected
@Singleton
public class TraceDiff extends VBox {
	@FXML private HBox labelBox;
	@FXML private HBox listBox;
	@FXML private Label replayed;
	@FXML private Label persistent;
	@FXML private Label current;
	@FXML private ListView<String> replayedList;
	@FXML private ListView<String> persistentList;
	@FXML private ListView<String> currentList;
	@FXML private Button setReplayed;
	@FXML private Button showAlert;
	@FXML private Button setCurrent;
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

		VBox.setVgrow(replayedList, Priority.ALWAYS);
		VBox.setVgrow(persistentList, Priority.ALWAYS);
		VBox.setVgrow(currentList, Priority.ALWAYS);
	}

	void setLists(Trace replayedOrLost, PersistentTrace persistent, Trace current) {
		//Not fully functional yet
		replayedList.setItems(FXCollections.observableList(replayedOrLost.getTransitionList().stream().map(t -> t.getName()).collect(Collectors.toList())));
		// if triggered by HistoryView: No persistent trace available
		if (persistent != null) {
			persistentList.setItems(FXCollections.observableList(persistent.getTransitionList().stream().map(t -> t.getOperationName()).collect(Collectors.toList())));
		}
		currentList.setItems(FXCollections.observableList(current.getTransitionList().stream().map(t -> t.getName()).collect(Collectors.toList())));

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

	void setAlert(Alert alert) {
		this.alert = alert;
		// the alert is either a TraceReplayErrorAlert or triggered by trying to save a trace
		if (alert instanceof TraceReplayErrorAlert) {
			replayed.setText(bundle.getString("animation.tracereplay.alerts.traceReplayError.error.traceDiff.replayed"));

			persistent.setVisible(true);
			persistent.setMaxWidth(Double.MAX_VALUE);
			persistentList.setVisible(true);
			persistentList.setMaxWidth(Double.MAX_VALUE);
		} else {
			replayed.setText(bundle.getString("history.buttons.saveTrace.error.lost"));

			persistent.setVisible(false);
			persistent.setMaxWidth(0);
			persistentList.setVisible(false);
			persistentList.setMaxWidth(0);
		}
	}
}
