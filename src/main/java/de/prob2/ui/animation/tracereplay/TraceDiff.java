package de.prob2.ui.animation.tracereplay;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.PersistentTrace;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.statespace.Trace;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.ResourceBundle;

@FXMLInjected
@Singleton
public class TraceDiff extends GridPane {
	@FXML private Label replayed;
	@FXML private Label persistent;
	@FXML private Label current;
	@FXML private ListView<Transition> replayedList;
	@FXML private ListView<PersistentTransition> persistentList;
	@FXML private ListView<Transition> currentList;
	@FXML private Button setReplayed;
	@FXML private Button setCurrent;
	private ResourceBundle bundle;
	private CurrentTrace currentTrace;

	@Inject
	private TraceDiff(StageManager stageManager, Injector injector, CurrentTrace currentTrace) {
		this.bundle = injector.getInstance(ResourceBundle.class);
		this.currentTrace = currentTrace;
		stageManager.loadFXML(this,"trace_replay_trace_diff.fxml");
	}

	@FXML
	private void initialize() {
		replayed.setText(bundle.getString("animation.tracereplay.alerts.traceReplayError.error.traceDiff.replayed"));
		persistent.setText(bundle.getString("animation.tracereplay.alerts.traceReplayError.error.traceDiff.persistent"));
		current.setText(bundle.getString("animation.tracereplay.alerts.traceReplayError.error.traceDiff.current"));
		this.getChildren().forEach(child -> GridPane.setMargin(child, new Insets(5, 5, 5,5)));
		setReplayed.prefWidthProperty().bind(this.widthProperty().divide(3));
		setCurrent.prefWidthProperty().bind(this.widthProperty().divide(3));
	}

	void setLists(Trace replayed, PersistentTrace persistent, Trace current) {
		replayedList.setItems(FXCollections.observableList(replayed.getTransitionList()));
		persistentList.setItems(FXCollections.observableList(persistent.getTransitionList()));
		currentList.setItems(FXCollections.observableList(current.getTransitionList()));
		setReplayed.setOnAction(e -> {
			currentTrace.set(replayed);
			((Stage) this.getScene().getWindow()).close();
		});
		setCurrent.setOnAction(e -> {
			currentTrace.set(current);
			((Stage) this.getScene().getWindow()).close();
		});
	}
}
