package de.prob2.ui.simulation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.ReplayTrace;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.animation.tracereplay.TraceReplayErrorAlert;
import de.prob2.ui.animation.tracereplay.TraceSaver;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.choice.SimulationChoosingStage;
import de.prob2.ui.simulation.choice.SimulationType;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.OperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.simulators.IRealTimeSimulator;
import de.prob2.ui.simulation.simulators.Scheduler;
import de.prob2.ui.simulation.simulators.Simulator;
import de.prob2.ui.simulation.simulators.TraceSimulator;
import de.prob2.ui.simulation.table.SimulationDebugItem;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.simulation.table.SimulationListViewDebugItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.visb.VisBStage;
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
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;


@FXMLInjected
@Singleton
public class SimulatorStage extends Stage {

	private final class SimulationItemRow extends TableRow<SimulationItem> {

		private SimulatorStage simulatorStage;

		private SimulationItemRow(SimulatorStage simulatorStage) {
			super();
			this.simulatorStage = simulatorStage;
		}

		@Override
		protected void updateItem(final SimulationItem item, final boolean empty) {
			super.updateItem(item, empty);

			if(item != null && !empty) {
				this.setContextMenu(null);
				ContextMenu contextMenu = new ContextMenu();

				List<MenuItem> menuItems = FXCollections.observableArrayList();

				MenuItem checkItem = new MenuItem(bundle.getString("simulation.contextMenu.check"));
				checkItem.setOnAction(e-> simulationItemHandler.checkItem(this.getItem(), false));

				MenuItem removeItem = new MenuItem(bundle.getString("simulation.contextMenu.remove"));
				removeItem.setOnAction(e -> simulationItemHandler.removeItem(currentProject.getCurrentMachine(), this.getItem()));

				menuItems.add(checkItem);
				menuItems.add(removeItem);

				this.itemProperty().addListener((observable, from, to) -> {
					if(to != null) {
						checkItem.disableProperty().bind(lastSimulator.isNull().or(lastSimulator.get().runningProperty()));
					}
				});

				MenuItem playItem = new MenuItem(bundle.getString("simulation.contextMenu.play"));

				SimulationType type = item.getType();
				switch (type) {
					case TRACE_REPLAY:
						playItem.setOnAction(e -> {
							Trace trace = new Trace(currentTrace.getStateSpace());
							ReplayTrace replayTrace = (ReplayTrace) item.getSimulationConfiguration().getField("TRACE");
							TraceSimulator traceSimulator = new TraceSimulator(currentTrace, injector.getInstance(Scheduler.class), trace, replayTrace);
							if(traceSimulator.isRunning()) {
								traceSimulator.stop();
							}
							SimulationHelperFunctions.initSimulator(stageManager, injector.getInstance(SimulatorStage.class), (IRealTimeSimulator) traceSimulator, configurationPath.get().toFile());
							trace.setExploreStateByDefault(false);
							simulate(traceSimulator);
							trace.setExploreStateByDefault(true);
						});
						break;
					default:
						playItem.setDisable(true);
						break;
				}
				menuItems.add(playItem);

				MenuItem showTraces = new MenuItem(bundle.getString("simulation.contextMenu.showTraces"));
				showTraces.disableProperty().bind(item.tracesProperty().emptyProperty());
				showTraces.setOnAction(e -> {
					if(item.getTraces().size() == 1) {
						currentTrace.set(item.getTraces().get(0));
					} else {
						SimulationTracesView tracesView = injector.getInstance(SimulationTracesView.class);
						tracesView.setSimulatorStage(simulatorStage);
						tracesView.setItems(item.getTraces());
						tracesView.show();
					}
				});
				menuItems.add(showTraces);

				MenuItem saveTraces = new MenuItem(bundle.getString("simulation.contextMenu.saveGeneratedTraces"));
				saveTraces.disableProperty().bind(item.tracesProperty().emptyProperty().or(
						Bindings.createBooleanBinding(() -> this.itemProperty().get() == null || this.itemProperty().get().getType() == SimulationType.TRACE_REPLAY,this.itemProperty())));
				saveTraces.setOnAction(e -> {
					TraceFileHandler traceSaver = injector.getInstance(TraceFileHandler.class);
					if (currentTrace.get() != null) {
						traceSaver.save(item, currentProject.getCurrentMachine());
					}
				});
				menuItems.add(saveTraces);

				contextMenu.getItems().addAll(menuItems);
				this.setContextMenu(contextMenu);
			}
		}
	}


	@FXML
	private Button btLoadConfiguration;

	@FXML
	private Button btSimulate;

	@FXML
	private Button btAddSimulation;

	@FXML
	private Label lbTime;

	@FXML
	private Button openVisBButton;

	@FXML
	private Button saveTraceButton;

	@FXML
	private TableView<SimulationItem> simulationItems;

	@FXML
	private ListView<SimulationDebugItem> simulationDebugItems;

	@FXML
	private TableColumn<SimulationItem, Checked> simulationStatusColumn;

	@FXML
	private TableColumn<SimulationItem, String> simulationTypeColumn;

	@FXML
	private TableColumn<SimulationItem, String> simulationConfigurationColumn;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final Injector injector;

	private final Simulator simulator;

	private final MachineLoader machineLoader;

	private final ResourceBundle bundle;

	private final FileChooserManager fileChooserManager;

    private final ObjectProperty<Path> configurationPath;

    private final SimulationItemHandler simulationItemHandler;

	private int time;

	private Timer timer;

	private ObjectProperty<IRealTimeSimulator> lastSimulator;

	@Inject
	public SimulatorStage(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace,
						  final Injector injector, final Simulator simulator, final MachineLoader machineLoader,
						  final SimulationItemHandler simulationItemHandler, final ResourceBundle bundle, final FileChooserManager fileChooserManager) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.injector = injector;
	    this.simulator = simulator;
	    this.machineLoader = machineLoader;
	    this.simulationItemHandler = simulationItemHandler;
	    this.lastSimulator = new SimpleObjectProperty<>(this, "lastSimulator", simulator);
		this.bundle = bundle;
		this.fileChooserManager = fileChooserManager;
        this.configurationPath = new SimpleObjectProperty<>(this, "configurationPath", null);
        this.time = 0;
        this.timer = new Timer();
		stageManager.loadFXML(this, "simulator_stage.fxml", this.getClass().getName());
	}

	@FXML
	public void initialize() {
		simulator.runningProperty().addListener((observable, from, to) -> {
			if(to) {
				Platform.runLater(() -> {
					btSimulate.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.PAUSE));
					btSimulate.setTooltip(new Tooltip(bundle.getString("simulation.button.stop")));
				});
			} else {
				Platform.runLater(() -> {
					btSimulate.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.PLAY));
					btSimulate.setTooltip(new Tooltip(bundle.getString("simulation.button.start")));
				});
			}
		});
		btLoadConfiguration.disableProperty().bind(simulator.runningProperty());
		btSimulate.disableProperty().bind(configurationPath.isNull());
		this.titleProperty().bind(Bindings.createStringBinding(() -> configurationPath.isNull().get() ? bundle.getString("simulation.stage.title") : String.format(bundle.getString("simulation.currentSimulation"), currentProject.getLocation().relativize(configurationPath.get()).toString()), configurationPath));
		btAddSimulation.disableProperty().bind(currentTrace.isNull().or(injector.getInstance(DisablePropertyController.class).disableProperty()).or(configurationPath.isNull()).or(simulator.runningProperty()));
		this.simulationDebugItems.setCellFactory(lv -> new SimulationListViewDebugItem(stageManager, currentTrace, bundle));

		machineLoader.loadingProperty().addListener((observable, from, to) -> {
			if(to) {
				stopSimulator(lastSimulator.get());
			}
			resetSimulator();
		});

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
		simulationTypeColumn.setCellValueFactory(new PropertyValueFactory<>("typeAsName"));
		simulationConfigurationColumn.setCellValueFactory(new PropertyValueFactory<>("configuration"));

		simulationItems.setRowFactory(table -> new SimulationItemRow(this));

		this.currentTrace.addListener((observable, from, to) -> simulationDebugItems.refresh());
		this.currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			configurationPath.set(null);
			simulationDebugItems.getItems().clear();
			simulationDebugItems.refresh();
		});
	}


	@FXML
	public void simulate() {
		this.simulate(simulator);
	}

	public void simulate(IRealTimeSimulator simulator) {
		if(!simulator.isRunning()) {
			runSimulator(simulator);
		} else {
			stopSimulator(simulator);
		}
	}

	private void runSimulator(IRealTimeSimulator simulator) {
		Path path = configurationPath.get();
		if(path != null) {
			injector.getInstance(Scheduler.class).setSimulator(simulator);
			if (lastSimulator.isNull().get() || !lastSimulator.get().equals(simulator)) {
				this.time = 0;
				SimulationHelperFunctions.initSimulator(stageManager, this, simulator, configurationPath.get().toFile());
			}
			simulator.run();
			startTimer(simulator);
		}
	}

	public void initSimulator(IRealTimeSimulator simulator) {
		SimulationHelperFunctions.initSimulator(stageManager, this, simulator, configurationPath.get().toFile());
	}

	private void stopSimulator(IRealTimeSimulator simulator) {
		Path path = configurationPath.get();
		if (path != null) {
			simulator.updateRemainingTime(time - simulator.timeProperty().get());
			simulator.updateDelay();
		}
		simulator.stop();
		cancelTimer();
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
			lbTime.setText("");
			this.time = 0;
			File configFile = path.toFile();;
			SimulationHelperFunctions.initSimulator(stageManager, this, (IRealTimeSimulator) simulator, configurationPath.get().toFile());
			loadSimulationItems();
		}
    }

    private void resetSimulator() {
		lbTime.setText("");
		this.time = 0;
		simulator.resetSimulator();
	}

    private void loadSimulationItems() {
		SimulationConfiguration config = simulator.getConfig();
		ObservableList<SimulationDebugItem> observableList = FXCollections.observableArrayList();

		for(OperationConfiguration opConfig : config.getOperationConfigurations()) {
			String opName = opConfig.getOpName();
			String additionalGuards = opConfig.getAdditionalGuards();
			List<ActivationConfiguration> activationConfiguration = opConfig.getActivation();
			OperationConfiguration.ActivationKind activationKind = opConfig.getActivationKind();
			String priority = String.valueOf(opConfig.getPriority());
			Map<String, String> destState = opConfig.getDestState();
			observableList.add(new SimulationDebugItem(opName, activationConfiguration, activationKind, additionalGuards, priority, destState));
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

	private void startTimer(IRealTimeSimulator simulator) {
		cancelTimer();
		lastSimulator.set(simulator);
		List<Boolean> firstStart = new ArrayList<>(Collections.singletonList(true));
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				if(firstStart.get(0)) {
					time = simulator.timeProperty().get();
					firstStart.set(0, false);
				} else if(!simulator.isFinished()) {
					if(time + 100 < simulator.getTime() + simulator.getDelay()) {
						time += 100;
						BigDecimal seconds = new BigDecimal(time / 1000.0f).setScale(1, RoundingMode.HALF_DOWN);
						Platform.runLater(() -> lbTime.setText(String.format(bundle.getString("simulation.time.second"), seconds.doubleValue())));
					}
				}
			}
		};
		simulator.timeProperty().addListener((observable, from, to) -> {
			if(!simulator.isFinished()) {
				time = to.intValue();
				if(time == 0) {
					Platform.runLater(() -> lbTime.setText(""));
				} else {
					BigDecimal seconds = new BigDecimal(time / 1000.0f).setScale(1, RoundingMode.HALF_DOWN);
					Platform.runLater(() -> lbTime.setText(String.format(bundle.getString("simulation.time.second"), seconds.doubleValue())));
				}
			}
		});
		this.timer = new Timer();
		timer.scheduleAtFixedRate(task, 100, 100);
	}

	private void cancelTimer() {
		if(timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	@FXML
	private void openVisB() {
		VisBStage visBStage = injector.getInstance(VisBStage.class);
		visBStage.show();
		visBStage.toFront();
	}

	@FXML
	public void saveTrace() {
		injector.getInstance(TraceSaver.class).saveTrace(this.getScene().getWindow(), TraceReplayErrorAlert.Trigger.TRIGGER_SIMULATOR);
	}

}
