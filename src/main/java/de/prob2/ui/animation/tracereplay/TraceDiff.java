package de.prob2.ui.animation.tracereplay;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.PersistentTransition;
import de.prob.statespace.Transition;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;

import java.util.List;
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
	private ResourceBundle bundle;

	@Inject
	private TraceDiff(StageManager stageManager, Injector injector) {
		this.bundle = injector.getInstance(ResourceBundle.class);
		stageManager.loadFXML(this,"trace_replay_trace_diff.fxml");
	}

	@FXML
	private void initialize() {
		replayed.setText(bundle.getString("animation.tracereplay.alerts.traceReplayError.error.traceDiff.replayed"));
		persistent.setText(bundle.getString("animation.tracereplay.alerts.traceReplayError.error.traceDiff.persistent"));
		current.setText(bundle.getString("animation.tracereplay.alerts.traceReplayError.error.traceDiff.current"));
	}

	void setLists(List<Transition> replayed, List<PersistentTransition> persistent, List<Transition> current) {
		replayedList.setItems(FXCollections.observableList(replayed));
		persistentList.setItems(FXCollections.observableList(persistent));
		currentList.setItems(FXCollections.observableList(current));
	}
}
