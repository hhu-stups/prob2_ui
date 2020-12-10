package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.simulation.table.SimulationDebugItem;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.simulation.table.SimulationListViewDebugItem;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.Map;
import java.util.ResourceBundle;


@FXMLInjected
@Singleton
public class SimulatorStage extends Stage {

	@FXML
	private Button btSimulate;

	@FXML
	private Button btAddSimulation;

	@FXML
	private Label lbTime;

	@FXML
	private TableView<SimulationItem> simulationItems;

	@FXML
	private ListView<SimulationDebugItem> simulationDebugItems;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final Injector injector;

	private final Simulator simulator;

	private final ResourceBundle bundle;

	private final FileChooserManager fileChooserManager;

    private final ObjectProperty<Path> configurationPath;

	@Inject
	public SimulatorStage(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace,
						  final Injector injector, final Simulator simulator, final ResourceBundle bundle, final FileChooserManager fileChooserManager) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.injector = injector;
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
				btSimulate.setText(bundle.getString("simulation.button.stop"));
			} else {
				btSimulate.setText(bundle.getString("simulation.button.start"));
			}
		});
		btSimulate.disableProperty().bind(configurationPath.isNull());
		this.titleProperty().bind(Bindings.createStringBinding(() -> configurationPath.isNull().get() ? bundle.getString("simulation.stage.title") : String.format(bundle.getString("simulation.currentSimulation"), currentProject.getLocation().relativize(configurationPath.get()).toString()), configurationPath));
		btAddSimulation.disableProperty().bind(currentTrace.isNull().or(injector.getInstance(DisablePropertyController.class).disableProperty()).or(configurationPath.isNull()));
		this.simulationDebugItems.setCellFactory(lv -> new SimulationListViewDebugItem(stageManager, currentTrace, simulator, bundle));
		this.currentTrace.addListener((observable, from, to) -> simulationDebugItems.refresh());
		this.currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			configurationPath.set(null);
			simulationDebugItems.getItems().clear();
			simulationDebugItems.refresh();
		});
		this.simulator.runningPropertyProperty().addListener((observable, from, to) -> {
			if(to || simulator.timeProperty().get() >= 0) {
				BigDecimal seconds = new BigDecimal(simulator.timeProperty().get()/1000.0f).setScale(2, RoundingMode.HALF_UP);
				Platform.runLater(() -> lbTime.setText(String.format(bundle.getString("simulation.time.second"), seconds.doubleValue())));
			} else {
				Platform.runLater(() -> lbTime.setText(""));
			}
		});
		this.simulator.timeProperty().addListener((observable, from, to) -> {
			if(to.intValue() >= 0 || this.simulator.runningPropertyProperty().get()) {
				BigDecimal seconds = new BigDecimal(to.intValue()/1000.0f).setScale(2, RoundingMode.HALF_UP);
				Platform.runLater(() -> lbTime.setText(String.format(bundle.getString("simulation.time.second"), seconds.doubleValue())));
			} else {
				Platform.runLater(() -> lbTime.setText(""));
			}
		});
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
            configurationPath.set(path);
            File configFile = path.toFile();
			lbTime.setText("");
            simulator.initSimulator(configFile);
            loadSimulationItems();
        }
    }

    private void loadSimulationItems() {
		SimulationConfiguration config = simulator.getConfig();
		ObservableList<SimulationDebugItem> observableList = FXCollections.observableArrayList();

		if(config.getSetupConfigurations() != null) {
			int setupID = 0;
			for(VariableChoice choice : config.getSetupConfigurations()) {
				for(VariableConfiguration variableConfiguration : choice.getChoice()) {
					observableList.add(new SimulationDebugItem("SETUP_CONSTANTS", "", null, "", "", String.valueOf(setupID), variableConfiguration.getValues(), variableConfiguration.getProbability()));
				}
				setupID++;
			}
		}

		if(config.getInitialisationConfigurations() != null) {
			int initialisationID = 0;
			for(VariableChoice choice : config.getInitialisationConfigurations()) {
				for(VariableConfiguration variableConfiguration : choice.getChoice()) {
					observableList.add(new SimulationDebugItem("INITIALISATION", "", null, "", "", String.valueOf(initialisationID), variableConfiguration.getValues(), variableConfiguration.getProbability()));
				}
				initialisationID++;
			}
		}


		for(OperationConfiguration opConfig : config.getOperationConfigurations()) {
			String opName = opConfig.getOpName();
			String time = String.valueOf(opConfig.getTime());
			Map<String, Integer> delay = opConfig.getDelay();
			String priority = String.valueOf(opConfig.getPriority());
			String probability = opConfig.getProbability();
			if(opConfig.getVariableChoices() == null) {
				observableList.add(new SimulationDebugItem(opName, time, delay, priority, probability, "", null, ""));
			} else {
				int variableID = 0;
				for(VariableChoice choice : opConfig.getVariableChoices()) {
					for(VariableConfiguration variableConfiguration : choice.getChoice()) {
						observableList.add(new SimulationDebugItem(opName, time, delay, priority, probability, String.valueOf(variableID), variableConfiguration.getValues(), variableConfiguration.getProbability()));
					}
				}
			}
		}

		simulationDebugItems.setItems(observableList);
	}

	@FXML
	public void addSimulation() {
		injector.getInstance(SimulationChoosingStage.class).reset();
		injector.getInstance(SimulationChoosingStage.class).showAndWait();
	}

}
