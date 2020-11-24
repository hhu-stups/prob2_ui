package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.ResourceBundle;


@FXMLInjected
@Singleton
public class SimulatorStage extends Stage {

	@FXML
	private Button btSimulate;

	private final StageManager stageManager;

	private final Simulator simulator;

	private final ResourceBundle bundle;

	private final FileChooserManager fileChooserManager;

    private final ObjectProperty<Path> configurationPath;

	@Inject
	public SimulatorStage(final StageManager stageManager, final Simulator simulator, final ResourceBundle bundle, final FileChooserManager fileChooserManager) {
		super();
		this.stageManager = stageManager;
	    this.simulator = simulator;
		this.bundle = bundle;
		this.fileChooserManager = fileChooserManager;
        this.configurationPath = new SimpleObjectProperty<>(this, "configurationPath", null);
		stageManager.loadFXML(this, "simulator_stage.fxml", this.getClass().getName());
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
		btSimulate.disableProperty().bind(configurationPath.isNull());
	}


	@FXML
	public void simulate() {
		if(!simulator.isRunning()) {
			simulator.run();
		} else {
			simulator.stop();
		}
	}

	@FXML
    public void loadConfiguration() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(bundle.getString("simulation.stage.filechooser.title"));
        fileChooser.getExtensionFilters().addAll(
                fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.simulation", "json")
        );
        Path path = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.SIMULATION, stageManager.getCurrent());
        if(path != null) {
            clear();
            configurationPath.set(path);
            File configFile = path.toFile();
            simulator.initSimulator(configFile);
        }
    }

    public void clear() {

    }

}
