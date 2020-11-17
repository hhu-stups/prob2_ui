package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import javafx.stage.Stage;


@Singleton
public class SimulatorStage extends Stage {

	@Inject
	public SimulatorStage(final StageManager stageManager) {
		stageManager.loadFXML(this, "simulator_stage.fxml");
	}

}
