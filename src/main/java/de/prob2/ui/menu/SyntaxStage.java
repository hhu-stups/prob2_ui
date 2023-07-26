package de.prob2.ui.menu;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.ExtendedCodeArea;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.stage.Stage;

@Singleton
@FXMLInjected
public final class SyntaxStage extends Stage {
	@FXML
	private ExtendedCodeArea syntaxText;

	@Inject
	private SyntaxStage (StageManager stageManager) {
		stageManager.loadFXML(this, "syntaxStage.fxml");
	}

	void setText(String text) {
		this.syntaxText.replaceText(text);
		this.syntaxText.moveTo(0);
		this.syntaxText.requestFollowCaret();
	}
}
