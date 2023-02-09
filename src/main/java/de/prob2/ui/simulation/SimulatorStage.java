package de.prob2.ui.simulation;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.animation.tracereplay.TraceSaver;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.SafeBindings;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.simulation.interactive.UIInteractionHandler;
import de.prob2.ui.simulation.interactive.UIInteractionSaver;
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.InterruptIfRunningButton;
import de.prob2.ui.simulation.choice.SimulationChoosingStage;
import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.ActivationConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;
import de.prob2.ui.simulation.simulators.Scheduler;
import de.prob2.ui.simulation.simulators.SimulationSaver;
import de.prob2.ui.simulation.simulators.check.SimulationStatsView;
import de.prob2.ui.simulation.table.SimulationChoiceDebugItem;
import de.prob2.ui.simulation.table.SimulationDebugItem;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.simulation.table.SimulationListViewDebugItem;
import de.prob2.ui.simulation.table.SimulationOperationDebugItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.visb.VisBStage;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.controlsfx.glyphfont.FontAwesome;

@FXMLInjected
@Singleton
public class SimulatorStage extends Stage {

	private final class SimulationItemRow extends TableRow<SimulationItem> {

		private final SimulatorStage simulatorStage;

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

				MenuItem checkItem = new MenuItem(i18n.translate("simulation.contextMenu.check"));
				checkItem.disableProperty().bind(configurationPath.isNull().or(simulationItemHandler.runningProperty().or(lastSimulator.isNull().or(lastSimulator.get().runningProperty()))));
				checkItem.setOnAction(e-> simulationItemHandler.checkItem(this.getItem()));

				MenuItem removeItem = new MenuItem(i18n.translate("simulation.contextMenu.remove"));
				removeItem.setOnAction(e -> simulationItemHandler.removeItem(cbSimulation.getSelectionModel().getSelectedItem(), this.getItem()));

				menuItems.add(checkItem);
				menuItems.add(removeItem);

				Menu copyMenu = new Menu(i18n.translate("simulation.contextMenu.copy"));
				copyMenu.getItems().clear();
				for(SimulationModel model : currentProject.getCurrentMachine().getSimulations()) {
					SimulationModel simulationModel = cbSimulation.getSelectionModel().getSelectedItem();
					if(simulationModel.equals(model)) {
						continue;
					}
					MenuItem menuItem = new MenuItem(model.getPath().toString());
					menuItem.setOnAction(e -> {
						if(!model.getSimulationItems().contains(item)) {
							model.getSimulationItems().add(item);
						}
					});
					copyMenu.getItems().add(menuItem);
				}
				menuItems.add(copyMenu);
				copyMenu.setDisable(copyMenu.getItems().isEmpty());

				MenuItem showTraces = new MenuItem(i18n.translate("simulation.contextMenu.showTraces"));
				showTraces.disableProperty().bind(item.tracesProperty().emptyProperty());
				showTraces.setOnAction(e -> {
					if(item.getTraces().size() == 1) {
						currentTrace.set(item.getTraces().get(0));
					} else {
						SimulationTracesView tracesView = injector.getInstance(SimulationTracesView.class);
						SimulationScenarioHandler simulationScenarioHandler = injector.getInstance(SimulationScenarioHandler.class);
						simulationScenarioHandler.setSimulatorStage(simulatorStage);
						tracesView.setItems(item, item.getTraces(), item.getTimestamps(), item.getStatuses());
						tracesView.show();
					}
				});
				menuItems.add(showTraces);

				MenuItem showStatistics = new MenuItem(i18n.translate("simulation.contextMenu.showStatistics"));
				showStatistics.disableProperty().bind(item.tracesProperty().emptyProperty());
				showStatistics.setOnAction(e -> {
					SimulationStatsView statsView = injector.getInstance(SimulationStatsView.class);
					statsView.setStats(item.getSimulationStats());
					statsView.show();
				});
				menuItems.add(showStatistics);

				MenuItem saveTraces = new MenuItem(i18n.translate("simulation.contextMenu.saveGeneratedTraces"));
				saveTraces.disableProperty().bind(item.tracesProperty().emptyProperty().or(
						Bindings.createBooleanBinding(() -> this.itemProperty().get() == null, this.itemProperty())));
				saveTraces.setOnAction(e -> {
					TraceFileHandler traceSaver = injector.getInstance(TraceFileHandler.class);
					if (currentTrace.get() != null) {
						traceSaver.save(item, currentProject.getCurrentMachine());
					}
				});
				menuItems.add(saveTraces);

				MenuItem saveTimedTraces = new MenuItem(i18n.translate("simulation.contextMenu.saveGeneratedTimedTraces"));
				saveTimedTraces.disableProperty().bind(item.tracesProperty().emptyProperty().or(
						Bindings.createBooleanBinding(() -> this.itemProperty().get() == null,this.itemProperty())));
				saveTimedTraces.setOnAction(e -> {
					SimulationSaver simulationSaver = injector.getInstance(SimulationSaver.class);
					simulationSaver.saveConfigurations(item);
				});
				menuItems.add(saveTimedTraces);

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
	private Button btCheckMachine;

	@FXML
	private InterruptIfRunningButton btCancel;

	@FXML
	private HelpButton helpButton;

	@FXML
	private Button btAddSimulation;

	@FXML
	private Label lbTime;

	@FXML
	private Button openVisBButton;

	@FXML
	private MenuButton saveTraceButton;

	@FXML
	private MenuItem saveTraceItem;

	@FXML
	private MenuItem saveTimedTraceItem;

	@FXML
	private MenuItem saveAutomaticSimulationItem;

	@FXML
	private TableView<SimulationItem> simulationItems;

	@FXML
	private ListView<SimulationDebugItem> simulationDebugItems;

	@FXML
	private TableColumn<SimulationItem, Checked> simulationStatusColumn;

	@FXML
	private TableColumn<SimulationItem, String> simulationIdColumn;

	@FXML
	private TableColumn<SimulationItem, String> simulationTypeColumn;

	@FXML
	private TableColumn<SimulationItem, String> simulationConfigurationColumn;

	@FXML
	private ChoiceBox<SimulationModel> cbSimulation;

	@FXML
	private Button btRemoveSimulation;

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final Injector injector;

	private final RealTimeSimulator realTimeSimulator;

	private final MachineLoader machineLoader;

	private final I18n i18n;

	private final FileChooserManager fileChooserManager;

	private final ObjectProperty<Path> configurationPath;

	private final SimulationItemHandler simulationItemHandler;

	private int time;

	private Timer timer;

	private final ObjectProperty<RealTimeSimulator> lastSimulator;

	private ChangeListener<Number> timeListener;

	@Inject
	public SimulatorStage(final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace,
						  final Injector injector, final RealTimeSimulator realTimeSimulator, final MachineLoader machineLoader,
						  final SimulationItemHandler simulationItemHandler, final I18n i18n, final FileChooserManager fileChooserManager,
						  final StopActions stopActions) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.realTimeSimulator = realTimeSimulator;
		this.machineLoader = machineLoader;
		this.simulationItemHandler = simulationItemHandler;
		this.lastSimulator = new SimpleObjectProperty<>(this, "lastSimulator", realTimeSimulator);
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.configurationPath = new SimpleObjectProperty<>(this, "configurationPath", null);
		this.time = 0;
		this.timer = new Timer(true);
		stopActions.add(this::cancelTimer);
		stageManager.loadFXML(this, "simulator_stage.fxml", this.getClass().getName());
	}

	@FXML
	public void initialize() {
		realTimeSimulator.runningProperty().addListener((observable, from, to) -> {
			if(to) {
				Platform.runLater(() -> {
					btSimulate.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.PAUSE));
					btSimulate.setTooltip(new Tooltip(i18n.translate("simulation.button.stop")));
				});
			} else {
				Platform.runLater(() -> {
					btSimulate.setGraphic(new BindableGlyph("FontAwesome", FontAwesome.Glyph.PLAY));
					btSimulate.setTooltip(new Tooltip(i18n.translate("simulation.button.start")));
				});
			}
		});
		btLoadConfiguration.disableProperty().bind(realTimeSimulator.runningProperty().or(currentProject.currentMachineProperty().isNull()));
		btSimulate.disableProperty().bind(configurationPath.isNull().or(currentProject.currentMachineProperty().isNull()));
		final BooleanProperty noSimulations = new SimpleBooleanProperty();

		btCheckMachine.disableProperty().bind(configurationPath.isNull().or(currentTrace.isNull().or(simulationItemHandler.runningProperty().or(noSimulations.or(injector.getInstance(DisablePropertyController.class).disableProperty())))));
		btCancel.runningProperty().bind(simulationItemHandler.runningProperty());
		btCancel.getInterruptButton().setOnAction(e -> {
			simulationItemHandler.interrupt();
			currentTrace.getStateSpace().sendInterrupt();
		});
		this.titleProperty().bind(
				Bindings.when(configurationPath.isNull())
						.then(i18n.translateBinding("simulation.stage.title"))
						.otherwise(i18n.translateBinding("simulation.currentSimulation",
								SafeBindings.createSafeStringBinding(
										() -> currentProject.getLocation().relativize(configurationPath.get()).toString(),
										currentProject, configurationPath
								)
						))
		);
		btAddSimulation.disableProperty().bind(currentTrace.isNull().or(injector.getInstance(DisablePropertyController.class).disableProperty()).or(configurationPath.isNull()).or(realTimeSimulator.runningProperty()).or(currentProject.currentMachineProperty().isNull()));
		saveTraceButton.disableProperty().bind(currentProject.currentMachineProperty().isNull().or(currentTrace.isNull()));
		helpButton.setHelpContent("mainmenu.advanced.simB", null);
		saveTraceItem.setOnAction(e -> injector.getInstance(TraceSaver.class).saveTrace(this.getScene().getWindow()));
		saveTimedTraceItem.setOnAction(e -> {
			try {
				injector.getInstance(SimulationSaver.class).saveConfiguration(currentTrace.get(), realTimeSimulator.getTimestamps(), "Real-Time Simulation");
			} catch (IOException exception) {
				exception.printStackTrace();
				//TODO: Handle error
			}
		});
		saveAutomaticSimulationItem.setOnAction(e -> {
			try {
				injector.getInstance(UIInteractionSaver.class).saveUIInteractions();
			} catch (IOException exception) {
				exception.printStackTrace();
				//TODO: Handle error
			}
		});

		this.simulationDebugItems.setCellFactory(lv -> new SimulationListViewDebugItem(stageManager, i18n));

		machineLoader.loadingProperty().addListener((observable, from, to) -> {
			if (to) {
				stopSimulator(lastSimulator.get());
			}
			resetSimulator();
		});

		this.addEventFilter(WindowEvent.WINDOW_SHOWING, event -> loadSimulationsFromMachine(currentProject.getCurrentMachine()));

		final ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
			configurationPath.set(null);
			simulationDebugItems.getItems().clear();
			simulationItems.itemsProperty().unbind();
			noSimulations.unbind();
			loadSimulationsFromMachine(to);
		};

		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());

		simulationStatusColumn.setCellFactory(col -> new CheckedCell<>());
		simulationStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		simulationIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
		simulationTypeColumn.setCellValueFactory(new PropertyValueFactory<>("typeAsName"));
		simulationConfigurationColumn.setCellValueFactory(new PropertyValueFactory<>("configuration"));

		simulationItems.setRowFactory(table -> new SimulationItemRow(this));

		this.currentTrace.addListener((observable, from, to) -> simulationDebugItems.refresh());

		simulationItems.setOnMouseClicked(e-> {
			SimulationItem item = simulationItems.getSelectionModel().getSelectedItem();
			if(e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && item != null && currentTrace.get() != null &&
				configurationPath.get() != null && !simulationItemHandler.runningProperty().get() && lastSimulator.get() != null &&
				!lastSimulator.get().isRunning()) {
				simulationItemHandler.checkItem(item);
			}
		});

		cbSimulation.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			configurationPath.set(null);
			injector.getInstance(SimulationChoosingStage.class).setSimulation(to);
			simulationDebugItems.getItems().clear();
			simulationItems.itemsProperty().unbind();
			noSimulations.unbind();
			injector.getInstance(UIInteractionHandler.class).reset();

			this.loadSimulationIntoSimulator(to);
			if(to != null) {
				noSimulations.bind(to.simulationItemsProperty().emptyProperty());
				simulationItems.itemsProperty().bind(to.simulationItemsProperty());
			} else {
				noSimulations.set(true);
				simulationItems.setItems(FXCollections.observableArrayList());
			}
		});

		btRemoveSimulation.disableProperty().bind(cbSimulation.getSelectionModel().selectedItemProperty().isNull());
	}


	@FXML
	public void simulate() {
		this.simulate(realTimeSimulator);
	}

	public void simulate(RealTimeSimulator realTimeSimulator) {
		if(!realTimeSimulator.isRunning()) {
			runSimulator(realTimeSimulator);
		} else {
			stopSimulator(realTimeSimulator);
		}
	}

	private void runSimulator(RealTimeSimulator realTimeSimulator) {
		Path path = configurationPath.get();
		if(path != null) {
			injector.getInstance(Scheduler.class).setSimulator(realTimeSimulator);
			if (lastSimulator.isNull().get() || !lastSimulator.get().equals(realTimeSimulator)) {
				this.time = 0;
				SimulationHelperFunctions.initSimulator(stageManager, this, realTimeSimulator, configurationPath.get());
			}
			realTimeSimulator.run();
			startTimer(realTimeSimulator);
		}
	}

	private void stopSimulator(RealTimeSimulator realTimeSimulator) {
		Path path = configurationPath.get();
		if (path != null) {
			realTimeSimulator.updateRemainingTime(time - realTimeSimulator.timeProperty().get());
			realTimeSimulator.updateDelay();
		}
		realTimeSimulator.stop();
		cancelTimer();
		if(timeListener != null) {
			realTimeSimulator.timeProperty().removeListener(timeListener);
		}
	}

	@FXML
	public void loadConfiguration() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("simulation.stage.filechooser.title"));
		fileChooser.getExtensionFilters().addAll(
				fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.simulation", "json")
		);
		Path path = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.SIMULATION, stageManager.getCurrent());
		if(path != null) {
			Path resolvedPath = currentProject.getLocation().relativize(path);
			currentProject.getCurrentMachine().simulationsProperty().add(new SimulationModel(resolvedPath, Collections.emptyList()));
		}
	}

	private void resetSimulator() {
		lbTime.setText("");
		this.time = 0;
		realTimeSimulator.resetSimulator();
	}

	private SimulationOperationDebugItem createOperationDebugItem(ActivationConfiguration activationConfig) {
		ActivationOperationConfiguration opConfig = (ActivationOperationConfiguration) activationConfig;
		String id = opConfig.getId();
		String opName = opConfig.getOpName();
		String time = opConfig.getAfter();
		String priority = String.valueOf(opConfig.getPriority());
		List<String> activations = opConfig.getActivating();
		ActivationOperationConfiguration.ActivationKind activationKind = opConfig.getActivationKind();
		String additionalGuards = opConfig.getAdditionalGuards();
		Map<String, String> fixedVariables = opConfig.getFixedVariables();
		Object probabilisticVariables = opConfig.getProbabilisticVariables();
		return new SimulationOperationDebugItem(id, opName, time, priority, activations, activationKind,
				additionalGuards, fixedVariables, probabilisticVariables);
	}

	private SimulationChoiceDebugItem createChoiceDebugItem(ActivationConfiguration activationConfig) {
		ActivationChoiceConfiguration choiceConfig = (ActivationChoiceConfiguration) activationConfig;
		String id = choiceConfig.getId();
		Map<String, String> activations = choiceConfig.getActivations();
		return new SimulationChoiceDebugItem(id, activations);
	}

	private void loadSimulationItems() {
		SimulationConfiguration config = realTimeSimulator.getConfig();
		ObservableList<SimulationDebugItem> observableList = FXCollections.observableArrayList();
		if(config != null) {
			for(ActivationConfiguration activationConfig : config.getActivationConfigurations()) {
				if(activationConfig instanceof ActivationOperationConfiguration) {
					observableList.add(createOperationDebugItem(activationConfig));
				} else {
					observableList.add(createChoiceDebugItem(activationConfig));
				}
			}
		}

		simulationDebugItems.setItems(observableList);
		simulationDebugItems.refresh();
	}

	@FXML
	public void addSimulation() {
		SimulationChoosingStage choosingStage = injector.getInstance(SimulationChoosingStage.class);
		choosingStage.showAndWait();
	}

	private void startTimer(RealTimeSimulator realTimeSimulator) {
		cancelTimer();
		lastSimulator.set(realTimeSimulator);
		List<Boolean> firstStart = new ArrayList<>(Collections.singletonList(true));
		timeListener = (observable, from, to) -> {
			if(!realTimeSimulator.endingConditionReached(currentTrace.get())) {
				time = to.intValue();
				if(time == 0) {
					Platform.runLater(() -> lbTime.setText(""));
				} else {
					BigDecimal seconds = new BigDecimal(time / 1000.0f).setScale(1, RoundingMode.HALF_DOWN);
					Platform.runLater(() -> lbTime.setText(i18n.translate("simulation.time.second", seconds.doubleValue())));
				}
			}
		};
		realTimeSimulator.timeProperty().addListener(timeListener);
		this.timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if(firstStart.get(0)) {
					time = realTimeSimulator.timeProperty().get();
					firstStart.set(0, false);
				} else if(!realTimeSimulator.endingConditionReached(currentTrace.get())) {
					if(currentTrace.getCurrentState().isInitialised() && time + 100 < realTimeSimulator.getTime() + realTimeSimulator.getDelay()) {
						time += 100;
						BigDecimal seconds = new BigDecimal(time / 1000.0f).setScale(1, RoundingMode.HALF_DOWN);
						Platform.runLater(() -> lbTime.setText(i18n.translate("simulation.time.second", seconds.doubleValue())));
					}
				} else {
					Platform.runLater(() -> {
						stopSimulator(realTimeSimulator);
						loadSimulationIntoSimulator(cbSimulation.getSelectionModel().getSelectedItem());
					});
				}
			}
		}, 100, 100);
	}

	private void cancelTimer() {
		if(timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	@FXML
	private void checkMachine() {
		simulationItemHandler.handleMachine(cbSimulation.getSelectionModel().getSelectedItem());
	}

	@FXML
	private void openVisB() {
		VisBStage visBStage = injector.getInstance(VisBStage.class);
		visBStage.show();
		visBStage.toFront();
	}

	public void loadSimulationsFromMachine(Machine machine) {
		if(machine == null) {
			return;
		}
		cbSimulation.itemsProperty().unbind();
		cbSimulation.itemsProperty().bind(machine.simulationsProperty());
	}

	public void loadSimulationIntoSimulator(SimulationModel simulation) {
		configurationPath.set(null);
		configurationPath.set(simulation == null ? null : currentProject.getLocation().resolve(simulation.getPath()));
		if(simulation != null) {
			injector.getInstance(SimulationChoosingStage.class).setPath(configurationPath.get());
			lbTime.setText("");
			this.time = 0;
			simulation.reset();
			SimulationHelperFunctions.initSimulator(stageManager, this, realTimeSimulator, configurationPath.get());
			loadSimulationItems();
		}
	}

	@FXML
	private void removeSimulation() {
		SimulationModel simulationModel = cbSimulation.getSelectionModel().getSelectedItem();
		if(simulationModel == null) {
			return;
		}
		currentProject.getCurrentMachine().getSimulations().remove(simulationModel);
	}
}
