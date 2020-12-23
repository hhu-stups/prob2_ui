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
import de.prob2.ui.simulation.simulators.AbstractSimulator;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;


@FXMLInjected
@Singleton
public class SimulatorStage extends Stage {

	private int time;

	private Timer timer;

	private AbstractSimulator lastSimulator;

	private final class SimulationItemRow extends TableRow<SimulationItem> {

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

					playItem.setOnAction(e -> {
						trace.setExploreStateByDefault(false);
						injector.getInstance(Scheduler.class).setSimulator(traceSimulator);
						traceSimulator.run();
						startTimer(traceSimulator);
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

	@Inject
	public SimulatorStage(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace,
						  final Injector injector, final Simulator simulator, final ResourceBundle bundle, final FileChooserManager fileChooserManager) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.injector = injector;
	    this.simulator = simulator;
	    this.lastSimulator = simulator;
		this.bundle = bundle;
		this.fileChooserManager = fileChooserManager;
        this.configurationPath = new SimpleObjectProperty<>(this, "configurationPath", null);
        this.time = 0;
        this.timer = new Timer();
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
	}


	@FXML
	public void simulate() {
		if(!simulator.isRunning()) {
			injector.getInstance(Scheduler.class).setSimulator(simulator);
			if(lastSimulator == null || !lastSimulator.equals(simulator)) {
				this.time = 0;
				simulator.initSimulator(configurationPath.get().toFile());
			}
			simulator.run();
			startTimer(simulator);
		} else {
			cancelTimer();
			simulator.updateRemainingTime(time - simulator.timeProperty().get());
			simulator.updateDelay();
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
			this.time = 0;
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

	private void startTimer(AbstractSimulator simulator) {
		cancelTimer();
		lastSimulator = simulator;
		List<Boolean> firstStart = Arrays.asList(true);
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				if(firstStart.get(0)) {
					time = simulator.timeProperty().get();
					firstStart.set(0, false);
				} else if(!simulator.isFinished()) {
					time += 100;
					BigDecimal seconds = new BigDecimal(time / 1000.0f).setScale(2, RoundingMode.HALF_UP);
					Platform.runLater(() -> lbTime.setText(String.format(bundle.getString("simulation.time.second"), seconds.doubleValue())));
				}
			}
		};
		this.timer = new Timer();
		timer.scheduleAtFixedRate(task, 100, 100);
	}

	private void cancelTimer() {
		if(timer != null) {
			timer.cancel();
			timer = null;
		}
	}

}
