package de.prob2.ui.verifications.tracereplay;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;

@Singleton
public class TraceReplayView extends ScrollPane {
	
	@Inject
	private TraceReplayView(final StageManager stageManager) {
		stageManager.loadFXML(this, "trace_replay_view.fxml");
	}
	
	@FXML
	private void initialize() {
	}
}
