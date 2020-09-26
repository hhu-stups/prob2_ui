package de.prob2.ui.output;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

@Singleton
public class PrologOutputStage extends Stage {
	@FXML
	private Button clearButton;

	@Inject
	private PrologOutputStage(StageManager stageManager, Injector injector) {
		stageManager.loadFXML(this, "prologOutputStage.fxml", this.getClass().getName());
		clearButton.setOnAction(e -> injector.getInstance(PrologOutput.class).clear());
	}
}
