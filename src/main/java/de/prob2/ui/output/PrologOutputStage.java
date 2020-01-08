package de.prob2.ui.output;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import javafx.stage.Stage;

@Singleton
public class PrologOutputStage extends Stage {
	@Inject
	private PrologOutputStage(StageManager stageManager) {
		stageManager.loadFXML(this, "prologOutputStage.fxml", this.getClass().getName());
	}
}
