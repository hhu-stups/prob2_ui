package de.prob2.ui.visb.help;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

@Singleton
public final class UserManualStage extends Stage {
	private final StageManager stageManager;

	@FXML
	private WebView userManualWebView;

	@Inject
	public UserManualStage(final StageManager stageManager) {
		super();

		this.stageManager = stageManager;

		stageManager.loadFXML(this, "user_manual_stage.fxml");
	}

	@FXML
	public void initialize() {
		stageManager.initWebView(this.userManualWebView);
		this.userManualWebView.getEngine().load(this.getClass().getResource("user_manual.html").toExternalForm());
	}
}
