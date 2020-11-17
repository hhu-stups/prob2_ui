package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;



@Singleton
public class SimulatorStage extends Stage {

	@FXML
	private TextField tfTimer;

	@FXML
	private Button btSimulate;

	private final Simulator simulator;

	@Inject
	public SimulatorStage(final StageManager stageManager, final Simulator simulator) {
		this.simulator = simulator;
		stageManager.loadFXML(this, "simulator_stage.fxml");
	}

	@FXML
	public void initialize() {
		simulator.runningPropertyProperty().addListener((observable, from, to) -> {
			if(to) {
				btSimulate.setText("Stop");
			} else {
				btSimulate.setText("Start");
			}
		});
	}


	@FXML
	public void simulate() {
		if(!simulator.isRunning()) {
			int timer = Integer.parseInt(tfTimer.getText());
			simulator.initSimulator(timer);
			simulator.run();
		} else {
			simulator.stop();
		}
	}

}
