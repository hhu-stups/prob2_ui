package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

@Singleton
@FXMLInjected
public final class SyntaxStage extends Stage {
	@FXML
	private TextArea syntaxText;

	@Inject
	private SyntaxStage (StageManager stageManager) {
		stageManager.loadFXML(this, "syntaxStage.fxml");
	}

	void setText(String text) {
		syntaxText.setText(text);
	}
}
