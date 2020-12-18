package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.configuration.OperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.configuration.VariableChoice;
import de.prob2.ui.simulation.configuration.VariableConfiguration;
import de.prob2.ui.simulation.simulators.Scheduler;
import de.prob2.ui.simulation.simulators.Simulator;
import de.prob2.ui.simulation.simulators.TraceSimulator;
import de.prob2.ui.simulation.table.SimulationDebugItem;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.simulation.table.SimulationListViewDebugItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;


@FXMLInjected
@Singleton
public class SimulatorStage extends Stage {

	private final class SimulationItemRow extends TableRow<SimulationItem> {

		private ChangeListener<Boolean> traceSimulatorRunningListener;

		private ChangeListener<Number> traceSimulatorTimeListener;

		private SimulationItemRow() {
			super();
		}

		@Override
		protected void updateItem(final SimulationItem item, final boolean empty) {
			super.updateItem(item, empty);
			if(item != null && !empty) {
				this.setContextMenu(null);
				if(item.getType() == SimulationType.TRACE_REPLAY) {
					ContextMenu contextMenu = new ContextMenu();
					List<MenuItem> menuItems = FXCollections.observableArrayList();
					MenuItem playItem = new MenuItem("Play");
					Trace trace = new Trace(currentTrace.getStateSpace());

					ReplayTrace replayTrace = (ReplayTrace) item.getSimulationConfiguration().getField("TRACE");
					TraceSimulator traceSimulator = new TraceSimulator(trace, replayTrace, injector.getInstance(Scheduler.class), currentTrace);
					traceSimulator.initSimulator(configurationPath.get().toFile());
					traceSimulator.runningPropertyProperty().addListener(schedulerRunningListener);
					traceSimulator.timeProperty().addListener(schedulerTimeListener);
					traceSimulatorRunningListener = (observable, from, to) -> {
						if(to || traceSimulator.timeProperty().get() >= 0) {
							BigDecimal seconds = new BigDecimal(traceSimulator.timeProperty().get()/1000.0f).setScale(2, RoundingMode.HALF_UP);
							Platform.runLater(() -> lbTime.setText(String.format(bundle.getString("simulation.time.second"), seconds.doubleValue())));
						} else {
							Platform.runLater(() -> lbTime.setText(""));
						}
						if(traceSimulator.isFinished()) {
							traceSimulator.runningPropertyProperty().removeListener(traceSimulatorRunningListener);
							traceSimulator.timeProperty().removeListener(traceSimulatorTimeListener);
							traceSimulator.runningPropertyProperty().removeListener(schedulerRunningListener);
							traceSimulator.timeProperty().removeListener(schedulerTimeListener);
							simulator.runningPropertyProperty().removeListener(traceSimulatorRunningListener);
							simulator.timeProperty().removeListener(traceSimulatorTimeListener);
						}
					};
					traceSimulatorTimeListener = (observable, from, to) -> {
						if(to.intValue() >= 0 || traceSimulator.runningPropertyProperty().get()) {
							BigDecimal seconds = new BigDecimal(to.intValue()/1000.0f).setScale(2, RoundingMode.HALF_UP);
							Platform.runLater(() -> lbTime.setText(String.format(bundle.getString("simulation.time.second"), seconds.doubleValue())));
						} else {
							Platform.runLater(() -> lbTime.setText(""));
						}
					};

					traceSimulator.runningPropertyProperty().addListener(traceSimulatorRunningListener);
					traceSimulator.timeProperty().addListener(traceSimulatorTimeListener);
					simulator.runningPropertyProperty().addListener(traceSimulatorRunningListener);
					simulator.timeProperty().addListener(traceSimulatorTimeListener);

					playItem.setOnAction(e -> {
						trace.setExploreStateByDefault(false);
						injector.getInstance(Scheduler.class).setSimulator(traceSimulator);
						traceSimulator.run();
						trace.setExploreStateByDefault(true);
					});

					menuItems.add(playItem);
					contextMenu.getItems().addAll(menuItems);
					this.setContextMenu(contextMenu);
				}
			}
		}
	}



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

	@FXML
	private TableColumn<SimulationItem, Checked> simulationStatusColumn;

	@FXML
	private TableColumn<SimulationItem, String> simulationTypeColumn;

	@FXML
	private TableColumn<SimulationItem, String> simulationDescriptionColumn;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final Injector injector;

	private final Simulator simulator;

	private final ResourceBundle bundle;

	private final FileChooserManager fileChooserManager;

    private final ObjectProperty<Path> configurationPath;

    private ChangeListener<Boolean> schedulerRunningListener;

    private ChangeListener<Number> schedulerTimeListener;

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
				Platform.runLater(() -> btSimulate.setText(bundle.getString("simulation.button.stop")));
			} else {
				Platform.runLater(() -> btSimulate.setText(bundle.getString("simulation.button.start")));
			}
		});
		btSimulate.disableProperty().bind(configurationPath.isNull());
		this.titleProperty().bind(Bindings.createStringBinding(() -> configurationPath.isNull().get() ? bundle.getString("simulation.stage.title") : String.format(bundle.getString("simulation.currentSimulation"), currentProject.getLocation().relativize(configurationPath.get()).toString()), configurationPath));
		btAddSimulation.disableProperty().bind(currentTrace.isNull().or(injector.getInstance(DisablePropertyController.class).disableProperty()).or(configurationPath.isNull()));
		this.simulationDebugItems.setCellFactory(lv -> new SimulationListViewDebugItem(stageManager, currentTrace, simulator, bundle));

		final ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
			if(to != null) {
				simulationItems.itemsProperty().bind(to.simulationItemsProperty());
			} else {
				simulationItems.getItems().clear();
				simulationItems.itemsProperty().unbind();
			}
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());

		simulationStatusColumn.setCellFactory(col -> new CheckedCell<>());
		simulationStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		simulationTypeColumn.setCellValueFactory(new PropertyValueFactory<>("configuration"));
		simulationDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

		simulationItems.setRowFactory(table -> new SimulationItemRow());

		this.currentTrace.addListener((observable, from, to) -> simulationDebugItems.refresh());
		this.currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			configurationPath.set(null);
			simulationDebugItems.getItems().clear();
			simulationDebugItems.refresh();
		});

		schedulerRunningListener = (observable, from, to) -> {
			if(to || simulator.timeProperty().get() >= 0) {
				BigDecimal seconds = new BigDecimal(simulator.timeProperty().get()/1000.0f).setScale(2, RoundingMode.HALF_UP);
				Platform.runLater(() -> lbTime.setText(String.format(bundle.getString("simulation.time.second"), seconds.doubleValue())));
			} else {
				Platform.runLater(() -> lbTime.setText(""));
			}
		};

		schedulerTimeListener = (observable, from, to) -> {
			if(to.intValue() >= 0 || this.simulator.runningPropertyProperty().get()) {
				BigDecimal seconds = new BigDecimal(to.intValue()/1000.0f).setScale(2, RoundingMode.HALF_UP);
				Platform.runLater(() -> lbTime.setText(String.format(bundle.getString("simulation.time.second"), seconds.doubleValue())));
			} else {
				Platform.runLater(() -> lbTime.setText(""));
			}
		};

		this.simulator.runningPropertyProperty().addListener(schedulerRunningListener);
		this.simulator.timeProperty().addListener(schedulerTimeListener);
	}


	@FXML
	public void simulate() {
		if(!simulator.isRunning()) {
			injector.getInstance(Scheduler.class).setSimulator(simulator);
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
		SimulationChoosingStage choosingStage = injector.getInstance(SimulationChoosingStage.class);
		choosingStage.reset();
		choosingStage.setPath(configurationPath.get());
		choosingStage.showAndWait();
	}

}
