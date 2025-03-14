package de.prob2.ui.simulation;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import com.google.common.io.MoreFiles;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.SafeBindings;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.simulation.choice.SimulationChoosingStage;
import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.ActivationKind;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.DiagramConfiguration;
import de.prob2.ui.simulation.configuration.ISimulationModelConfiguration;
import de.prob2.ui.simulation.configuration.SimulationBlackBoxModelConfiguration;
import de.prob2.ui.simulation.configuration.SimulationExternalConfiguration;
import de.prob2.ui.simulation.configuration.SimulationFileHandler;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.configuration.TransitionSelection;
import de.prob2.ui.simulation.configuration.UIListenerConfiguration;
import de.prob2.ui.simulation.interactive.UIInteractionHandler;
import de.prob2.ui.simulation.model.SimulationModel;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;
import de.prob2.ui.simulation.simulators.Scheduler;
import de.prob2.ui.simulation.simulators.check.SimulationStatsView;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.CheckingStatusCell;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public final class SimulatorStage extends Stage {
	private final class SimulationItemRow extends TableRow<SimulationItem> {

		private final SimulatorStage simulatorStage;

		private SimulationItemRow(SimulatorStage simulatorStage) {
			super();
			this.simulatorStage = simulatorStage;
		}

		@Override
		protected void updateItem(final SimulationItem item, final boolean empty) {
			super.updateItem(item, empty);

			if (item != null && !empty) {
				this.setContextMenu(null);
				ContextMenu contextMenu = new ContextMenu();

				List<MenuItem> menuItems = FXCollections.observableArrayList();

				MenuItem checkItem = new MenuItem(i18n.translate("simulation.contextMenu.check"));
				checkItem.disableProperty().bind(configurationPath.isNull().or(disablePropertyController.disableProperty().or(lastSimulator.isNull().or(lastSimulator.get().runningProperty()))));
				checkItem.setOnAction(e -> simulationItemHandler.checkItem(this.getItem()));

				MenuItem editItem = new MenuItem(i18n.translate("simulation.contextMenu.edit"));
				editItem.setOnAction(e -> editSimulation(this.getItem()));

				MenuItem removeItem = new MenuItem(i18n.translate("simulation.contextMenu.remove"));
				removeItem.setOnAction(e -> currentProject.getCurrentMachine().removeValidationTask(this.getItem()));

				menuItems.add(checkItem);
				menuItems.add(editItem);
				menuItems.add(removeItem);

				Menu copyMenu = new Menu(i18n.translate("simulation.contextMenu.copy"));
				{
					copyMenu.getItems().clear();
					SimulationModel sourceModel = cbSimulation.getSelectionModel().getSelectedItem();
					for (SimulationModel targetModel : currentProject.getCurrentMachine().getSimulations()) {
						if (sourceModel.equals(targetModel)) {
							continue;
						}

						MenuItem menuItem = new MenuItem(targetModel.getPath().toString());
						menuItem.setOnAction(e -> {
							ISimulationModelConfiguration config = realTimeSimulator.getConfig();
							int size = item.getInformation().get("EXECUTIONS") == null ? ((SimulationBlackBoxModelConfiguration) config).getTimedTraces().size() : (int) item.getInformation().get("EXECUTIONS");
							currentProject.getCurrentMachine().addValidationTaskIfNotExist(item.withSimulationPath(size, targetModel.getPath()));
						});
						copyMenu.getItems().add(menuItem);
					}
					copyMenu.setDisable(copyMenu.getItems().isEmpty());
				}
				menuItems.add(copyMenu);

				BooleanExpression itemHasNoSimulationResult = Bindings.createBooleanBinding(
					() -> !(item.getResult() instanceof SimulationItem.Result simulationResult) || simulationResult.getTraces().isEmpty(),
					item.resultProperty()
				);
				// For whatever reason, this does not work properly with map -
				// the initial state will be incorrect and the context menu options will be enabled from the start,
				// even though they should be disabled until the items have been executed.
				//var itemHasNoSimulationResult = item.resultProperty().map(res -> {
				//	return !(res instanceof SimulationItem.Result simulationResult) || simulationResult.getTraces().isEmpty();
				//});

				MenuItem showTraces = new MenuItem(i18n.translate("simulation.contextMenu.showTraces"));
				showTraces.disableProperty().bind(itemHasNoSimulationResult);
				showTraces.setOnAction(e -> {
					if (!(item.getResult() instanceof SimulationItem.Result simulationResult)) {
						return;
					}

					if (simulationResult.getTraces().size() == 1) {
						currentTrace.set(simulationResult.getTraces().get(0));
					} else {
						SimulationTracesView tracesView = injector.getInstance(SimulationTracesView.class);
						SimulationScenarioHandler simulationScenarioHandler = injector.getInstance(SimulationScenarioHandler.class);
						simulationScenarioHandler.setSimulatorStage(simulatorStage);
						tracesView.setFromItem(item);
						tracesView.show();
					}
				});
				menuItems.add(showTraces);

				MenuItem showStatistics = new MenuItem(i18n.translate("simulation.contextMenu.showStatistics"));
				showStatistics.disableProperty().bind(itemHasNoSimulationResult);
				showStatistics.setOnAction(e -> {
					if (!(item.getResult() instanceof SimulationItem.Result simulationResult)) {
						return;
					}

					SimulationStatsView statsView = injector.getInstance(SimulationStatsView.class);
					statsView.setStats(simulationResult.getStats());
					statsView.show();
				});
				menuItems.add(showStatistics);

				MenuItem saveTraces = new MenuItem(i18n.translate("simulation.contextMenu.saveGeneratedTraces"));
				saveTraces.disableProperty().bind(itemHasNoSimulationResult);
				saveTraces.setOnAction(e -> {
					if (currentTrace.get() != null) {
						traceFileHandler.save(item, currentProject.getCurrentMachine());
					}
				});
				menuItems.add(saveTraces);

				MenuItem saveTimedTraces = new MenuItem(i18n.translate("simulation.contextMenu.saveGeneratedTimedTraces"));
				saveTimedTraces.disableProperty().bind(itemHasNoSimulationResult);
				saveTimedTraces.setOnAction(e -> simulationFileHandler.saveTimedTracesForSimulationItem(item));
				menuItems.add(saveTimedTraces);

				contextMenu.getItems().addAll(menuItems);
				this.setContextMenu(contextMenu);
			}
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(SimulatorStage.class);

	@FXML
	private MenuBar menuBar;

	@FXML
	private MenuItem loadSimBModelMenuItem;

	@FXML
	private MenuItem loadSimBTracesMenuItem;

	@FXML
	private MenuItem loadExternalSimulationMenuItem;

	@FXML
	private MenuItem saveMenuItem;

	@FXML
	private MenuItem saveAsMenuItem;

	@FXML
	private MenuItem saveTraceMenuItem;

	@FXML
	private MenuItem saveTimedTraceMenuItem;

	@FXML
	private MenuItem saveAutomaticSimulationMenuItem;

	@FXML
	private MenuButton btLoadConfiguration;

	@FXML
	private Button btSimulate;

	@FXML
	private Button btCheckMachine;

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
	private MenuItem saveItem;

	@FXML
	private MenuItem saveAsItem;

	@FXML
	private MenuItem saveTraceItem;

	@FXML
	private MenuItem saveTimedTraceItem;

	@FXML
	private MenuItem saveAutomaticSimulationItem;

	@FXML
	private TableView<SimulationItem> simulationItems;

	@FXML
	private ListView<DiagramConfiguration> simulationDiagramItems;

	@FXML
	private TableColumn<SimulationItem, CheckingStatus> simulationStatusColumn;

	@FXML
	private TableColumn<SimulationItem, SimulationItem> simulationConfigurationColumn;

	@FXML
	private ChoiceBox<SimulationModel> cbSimulation;

	@FXML
	private Button btRemoveSimulation;

	@FXML
	private MenuButton btAddDiagramElement;

	@FXML
	private Button btRemoveDiagramElement;

	@FXML
	private MenuItem advancedItem;

	@FXML
	private Label lbSimulationStats;

	@FXML
	private ProgressBar progressBar;

	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final Injector injector;
	private final RealTimeSimulator realTimeSimulator;
	private final MachineLoader machineLoader;
	private final I18n i18n;
	private final FileChooserManager fileChooserManager;
	private final TraceFileHandler traceFileHandler;
	private final SimulationFileHandler simulationFileHandler;
	private final DisablePropertyController disablePropertyController;
	private final ObjectProperty<Path> configurationPath;
	private final BooleanProperty savedProperty;
	private final SimulationItemHandler simulationItemHandler;
	private final SimulationMode simulationMode;

	private int time;

	private Timer timer;

	private final ObjectProperty<RealTimeSimulator> lastSimulator;

	private final InvalidationListener simulationModelsListener;
	private ChangeListener<Number> timeListener;

	@Inject
	public SimulatorStage(
			final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace,
			final Injector injector, final RealTimeSimulator realTimeSimulator, final MachineLoader machineLoader,
			final SimulationItemHandler simulationItemHandler, final SimulationMode simulationMode,
			final I18n i18n, final FileChooserManager fileChooserManager,
			final TraceFileHandler traceFileHandler, final DisablePropertyController disablePropertyController,
			final StopActions stopActions, SimulationFileHandler simulationFileHandler
	) {
		super();
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.realTimeSimulator = realTimeSimulator;
		this.machineLoader = machineLoader;
		this.simulationItemHandler = simulationItemHandler;
		this.simulationMode = simulationMode;
		this.lastSimulator = new SimpleObjectProperty<>(this, "lastSimulator", realTimeSimulator);
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.traceFileHandler = traceFileHandler;
		this.disablePropertyController = disablePropertyController;
		this.simulationFileHandler = simulationFileHandler;
		this.configurationPath = new SimpleObjectProperty<>(this, "configurationPath", null);
		this.savedProperty = new SimpleBooleanProperty(this, "savedProperty", true);
		this.time = 0;
		this.timer = new Timer(true);
		stopActions.add(this::cancelTimer);

		this.simulationModelsListener = o -> {
			Machine machine = currentProject.getCurrentMachine();
			// Show the simulation models saved for the machine, or the default simulation if none are saved.
			if (machine.getSimulations().isEmpty()) {
				cbSimulation.getItems().setAll(new SimulationModel(Paths.get("")));
			} else {
				cbSimulation.getItems().setAll(machine.getSimulations());
			}

			// If the last selected simulation disappears, select a different one if possible.
			// Note: it's important to check the selected index and not the selected item!
			// When items are removed from the list,
			// the selection model can get into a state where the selected index is -1,
			// but the selected item is not null and still points to the last selected item.
			// This may be a bug in JavaFX (last checked with JavaFX 22.0.2).
			if (cbSimulation.getSelectionModel().getSelectedIndex() == -1 && !cbSimulation.getItems().isEmpty()) {
				cbSimulation.getSelectionModel().selectFirst();
			}
		};

		stageManager.loadFXML(this, "simulator_stage.fxml", this.getClass().getName());
	}

	@FXML
	public void initialize() {
		stageManager.setMacMenuBar(this, menuBar);

		realTimeSimulator.runningProperty().addListener((observable, from, to) -> {
			if (to) {
				Platform.runLater(() -> {
					((Glyph)btSimulate.getGraphic()).setIcon(FontAwesome.Glyph.PAUSE);
					btSimulate.setTooltip(new Tooltip(i18n.translate("simulation.button.stop")));
				});
			} else {
				Platform.runLater(() -> {
					((Glyph)btSimulate.getGraphic()).setIcon(FontAwesome.Glyph.PLAY);
					btSimulate.setTooltip(new Tooltip(i18n.translate("simulation.button.start")));
				});
			}
		});

		BooleanExpression disableOpenProperty = realTimeSimulator.runningProperty().or(currentProject.currentMachineProperty().isNull());
		loadSimBModelMenuItem.disableProperty().bind(disableOpenProperty);
		loadSimBTracesMenuItem.disableProperty().bind(disableOpenProperty);
		loadExternalSimulationMenuItem.disableProperty().bind(disableOpenProperty);
		btLoadConfiguration.disableProperty().bind(disableOpenProperty);
		btSimulate.disableProperty().bind(configurationPath.isNull().or(currentProject.currentMachineProperty().isNull()));

		final BooleanProperty noSimulations = new SimpleBooleanProperty();
		noSimulations.bind(SafeBindings.wrappedBooleanBinding(List::isEmpty, simulationItems.itemsProperty()));

		btCheckMachine.disableProperty().bind(configurationPath.isNull().or(currentTrace.isNull().or(noSimulations.or(disablePropertyController.disableProperty()))));
		this.titleProperty().bind(
			Bindings.when(configurationPath.isNull())
				.then(i18n.translateBinding("simulation.stage.title"))
				.otherwise(i18n.translateBinding("simulation.currentSimulation",
					SafeBindings.createSafeStringBinding(
						() -> configurationPath.get().toString().isEmpty() ? i18n.translate("simulation.defaultSimulation") : currentProject.getLocation().relativize(configurationPath.get()) + (savedProperty.get() ? "": "*"),
						currentProject, configurationPath, savedProperty
					)
				))
		);

		// The items list is set once here and then always updated in-place.
		// setItems should never be called again after this.
		cbSimulation.setItems(FXCollections.observableArrayList());
		cbSimulation.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			checkIfSimulationShouldBeSaved();
			configurationPath.set(null);
			simulationMode.setMode(to == null ? null :
					currentProject.getLocation().resolve(to.getPath()).toFile().isDirectory() ? SimulationMode.Mode.BLACK_BOX :
					SimulationMode.Mode.MONTE_CARLO);
			injector.getInstance(SimulationChoosingStage.class).setSimulation(to);
			simulationDiagramItems.getItems().clear();
			simulationItems.itemsProperty().unbind();

			if (to != null) {
				simulationItems.setItems(simulationItemHandler.getSimulationItems(to));
			} else {
				simulationItems.setItems(FXCollections.observableArrayList());
			}

			UIInteractionHandler uiInteractionHandler = injector.getInstance(UIInteractionHandler.class);
			uiInteractionHandler.reset();
			this.loadSimulationIntoSimulator(to);
			uiInteractionHandler.loadUIListenersIntoSimulator(realTimeSimulator);
		});
		cbSimulation.disableProperty().bind(currentTrace.isNull().or(realTimeSimulator.runningProperty()).or(currentProject.currentMachineProperty().isNull()));

		btAddSimulation.disableProperty().bind(currentTrace.isNull().or(disablePropertyController.disableProperty()).or(configurationPath.isNull()).or(realTimeSimulator.runningProperty()).or(currentProject.currentMachineProperty().isNull()));

		BooleanExpression disableSaveTraceProperty = currentProject.currentMachineProperty().isNull().or(currentTrace.isNull());
		saveTraceMenuItem.disableProperty().bind(disableSaveTraceProperty);
		saveTimedTraceMenuItem.disableProperty().bind(disableSaveTraceProperty);
		saveAutomaticSimulationMenuItem.disableProperty().bind(disableSaveTraceProperty);
		saveTraceButton.disableProperty().bind(disableSaveTraceProperty);
		saveAutomaticSimulationItem.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			ISimulationModelConfiguration config = realTimeSimulator.getConfig();
			return config == null || config instanceof SimulationExternalConfiguration;
		}, configurationPath, cbSimulation.itemsProperty(), cbSimulation.getSelectionModel().selectedItemProperty()));
		helpButton.setHelpContent("mainmenu.advanced.simB", null);

		BooleanExpression disableSaveProperty = Bindings.createBooleanBinding(() -> configurationPath.get() == null || !configurationPath.get().toString().isEmpty() && !configurationPath.get().toString().endsWith(".json"), configurationPath);

		saveMenuItem.disableProperty().bind(disableSaveProperty);
		saveItem.disableProperty().bind(disableSaveProperty);

		saveAsMenuItem.disableProperty().bind(disableSaveProperty);
		saveAsItem.disableProperty().bind(disableSaveProperty);

		this.simulationDiagramItems.setCellFactory(lv -> new DiagramConfigurationListCell(stageManager, i18n, savedProperty, realTimeSimulator.runningProperty()));

		machineLoader.loadingProperty().addListener((observable, from, to) -> {
			if (to) {
				stopSimulator(lastSimulator.get());
			}
			resetSimulator();
		});

		final ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
			if(to == null) {
				return;
			}
			checkIfSimulationShouldBeSaved();
			configurationPath.set(null);
			simulationDiagramItems.getItems().clear();
			simulationItems.itemsProperty().unbind();
			loadSimulationsFromMachine(from, to);
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());

		simulationStatusColumn.setCellFactory(col -> new CheckingStatusCell<>());
		simulationStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		simulationConfigurationColumn.setCellFactory(lv -> new SimulationItemTableCell(stageManager, i18n));
		simulationConfigurationColumn.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue()));


		simulationItems.setRowFactory(table -> new SimulationItemRow(this));

		simulationItems.setOnMouseClicked(e -> {
			SimulationItem item = simulationItems.getSelectionModel().getSelectedItem();
			if (
				e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && item != null && currentTrace.get() != null &&
				configurationPath.get() != null && !disablePropertyController.disableProperty().get() && lastSimulator.get() != null &&
				!lastSimulator.get().isRunning()
			) {
				simulationItemHandler.checkItem(item);
			}
		});

		btRemoveSimulation.disableProperty().bind(cbSimulation.getSelectionModel().selectedItemProperty().isNull());
		btAddDiagramElement.disableProperty().bind(Bindings.createBooleanBinding(() ->
				cbSimulation.getSelectionModel().selectedItemProperty().get() == null || configurationPath.get() == null || !configurationPath.get().toString().endsWith(".json"),
				cbSimulation.getSelectionModel().selectedItemProperty()));

		btRemoveDiagramElement.disableProperty().bind(simulationDiagramItems.getSelectionModel().selectedItemProperty().isNull());

		setOnCloseRequest(e -> {
			if (realTimeSimulator.isRunning())
				stopSimulator(realTimeSimulator);
		});
	}

	@FXML
	public void simulate() {
		this.simulate(realTimeSimulator);
	}

	public void simulate(RealTimeSimulator realTimeSimulator) {
		if (!realTimeSimulator.isRunning()) {
			runSimulator(realTimeSimulator);
		} else {
			stopSimulator(realTimeSimulator);
		}
	}

	private void runSimulator(RealTimeSimulator realTimeSimulator) {
		Path path = configurationPath.get();
		if (path != null) {
			ISimulationModelConfiguration config = realTimeSimulator.getConfig();
			Path configPath;
			if (config instanceof SimulationModelConfiguration || config instanceof SimulationExternalConfiguration) {
				configPath = configurationPath.get();
				injector.getInstance(Scheduler.class).setSimulator(realTimeSimulator);
				if (lastSimulator.isNull().get() || !lastSimulator.get().equals(realTimeSimulator)) {
					this.time = 0;
					this.simulationFileHandler.initSimulator(this, realTimeSimulator, currentTrace.getStateSpace().getLoadedMachine(), configPath);
				}
				realTimeSimulator.run();
				startTimer(realTimeSimulator);
			} else { //  SimulationBlackBoxModelConfiguration
				List<Path> timedTraces = ((SimulationBlackBoxModelConfiguration) config).getTimedTraces();
				configPath = timedTraces.get((int) (Math.random() * timedTraces.size()));
				injector.getInstance(Scheduler.class).setSimulator(realTimeSimulator);
				this.time = 0;
				this.simulationFileHandler.initSimulator(this, realTimeSimulator, currentTrace.getStateSpace().getLoadedMachine(), configPath);
				Trace trace = new Trace(currentTrace.getStateSpace());
				currentTrace.set(trace);
				realTimeSimulator.setupBeforeSimulation(trace);
				trace.setExploreStateByDefault(false);
				realTimeSimulator.run();
				startTimer(realTimeSimulator);
				trace.setExploreStateByDefault(true);
			}
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
		if (timeListener != null) {
			realTimeSimulator.timeProperty().removeListener(timeListener);
		}
	}

	@FXML
	public void loadSimBModel() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("simulation.stage.filechooser.title"));
		fileChooser.getExtensionFilters().addAll(
			fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.simulation", "json")
		);
		Path path = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.SIMULATION, stageManager.getCurrent());
		if (path != null) {
			Path resolvedPath = currentProject.getLocation().relativize(path);
			currentProject.getCurrentMachine().getSimulations().add(new SimulationModel(resolvedPath));
		}
	}

	@FXML
	public void loadSimBTraces() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(i18n.translate("simulation.stage.filechooser.title"));
		Path path = fileChooserManager.showDirectoryChooser(directoryChooser, FileChooserManager.Kind.SIMULATION, stageManager.getCurrent());
		if (path != null) {
			Path resolvedPath = currentProject.getLocation().relativize(path);
			currentProject.getCurrentMachine().getSimulations().add(new SimulationModel(resolvedPath));
		}
	}

	@FXML
	public void loadExternal() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("simulation.stage.filechooser.title"));
		fileChooser.getExtensionFilters().addAll(
			fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.simulation", "py")
		);
		Path path = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.SIMULATION, stageManager.getCurrent());
		if (path != null) {
			Path resolvedPath = currentProject.getLocation().relativize(path);
			currentProject.getCurrentMachine().getSimulations().add(new SimulationModel(resolvedPath));
		}
	}

	private void resetSimulator() {
		lbTime.setText("");
		this.time = 0;
		realTimeSimulator.resetSimulator();
	}

	private void loadSimulationItems() {
		ISimulationModelConfiguration config = realTimeSimulator.getConfig();

		ObservableList<DiagramConfiguration> observableList = FXCollections.observableArrayList();
		if (config != null) {
			if (config instanceof SimulationModelConfiguration modelConfig) {
				observableList.addAll(modelConfig.getActivations());
				observableList.addAll(modelConfig.getListeners());
			}
		}

		simulationDiagramItems.getItems().clear();
		simulationDiagramItems.setItems(observableList);
	}

	@FXML
	public void addSimulation() {
		SimulationChoosingStage choosingStage = injector.getInstance(SimulationChoosingStage.class);
		choosingStage.reset();
		choosingStage.showAndWait();
		SimulationItem newItem = choosingStage.getResult();
		if (newItem != null) {
			SimulationItem toCheck = currentProject.getCurrentMachine().addValidationTaskIfNotExist(newItem);
			simulationItemHandler.checkItem(toCheck);
		}
	}

	public void editSimulation(SimulationItem oldItem) {
		Machine machine = this.currentProject.getCurrentMachine();
		SimulationChoosingStage choosingStage = injector.getInstance(SimulationChoosingStage.class);
		choosingStage.reset();
		choosingStage.setData(oldItem);
		choosingStage.showAndWait();
		SimulationItem newItem = choosingStage.getResult();
		if (newItem != null) {
			if (this.currentProject.getCurrentMachine() == machine) {
				this.currentProject.getCurrentMachine().replaceValidationTaskIfNotExist(oldItem, newItem);
				this.simulationItems.refresh();
			} else {
				LOGGER.warn("The machine has changed, discarding task changes");
			}
		}
	}

	private void startTimer(RealTimeSimulator realTimeSimulator) {
		cancelTimer();
		lastSimulator.set(realTimeSimulator);
		List<Boolean> firstStart = new ArrayList<>(Collections.singletonList(true));
		timeListener = (observable, from, to) -> {
			if (!realTimeSimulator.endingConditionReached(currentTrace.get())) {
				time = to.intValue();
				if (time == 0) {
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
				if (firstStart.get(0)) {
					time = realTimeSimulator.timeProperty().get();
					firstStart.set(0, false);
				} else if (!realTimeSimulator.endingConditionReached(currentTrace.get())) {
					if (currentTrace.getCurrentState() != null && currentTrace.getCurrentState().isInitialised() && time + 100 < realTimeSimulator.getTime() + realTimeSimulator.getDelay()) {
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
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	@FXML
	private void checkMachine() {
		simulationItemHandler.handleMachine(cbSimulation.getSelectionModel().getSelectedItem());
	}

	private void loadSimulationsFromMachine(Machine prevMachine, Machine machine) {
		if (prevMachine != null) {
			prevMachine.getSimulations().removeListener(this.simulationModelsListener);
		}
		cbSimulation.getItems().clear();
		if (machine != null) {
			machine.getSimulations().addListener(this.simulationModelsListener);
			this.simulationModelsListener.invalidated(null);
			cbSimulation.getSelectionModel().clearSelection();
		}
	}

	public void loadSimulationIntoSimulator(SimulationModel simulation) {
		configurationPath.set(simulation == null ? null :
				simulation.getPath().equals(SimulationFileHandler.DEFAULT_SIMULATION_PATH) ? simulation.getPath() : currentProject.getLocation().resolve(simulation.getPath()));
		StateSpace stateSpace = currentTrace.getStateSpace();
		if (simulation != null && stateSpace != null) {
			simulationItemHandler.setPath(configurationPath.get());
			lbTime.setText("");
			this.time = 0;
			simulationItemHandler.reset(simulation);
			this.simulationFileHandler.initSimulator(this, realTimeSimulator, stateSpace.getLoadedMachine(), configurationPath.get());
			simulationItemHandler.setSimulationModelConfiguration(realTimeSimulator.getConfig());
			loadSimulationItems();
		}
	}

	@FXML
	private void removeSimulation() {
		SimulationModel simulationModel = cbSimulation.getSelectionModel().getSelectedItem();
		if (simulationModel == null) {
			return;
		}
		currentProject.getCurrentMachine().getSimulations().remove(simulationModel);
	}

	private SimulationModelConfiguration buildSimulationModel() {
		Map<String, String> variables = new HashMap<>();
		List<DiagramConfiguration.NonUi> activations = new ArrayList<>();
		List<UIListenerConfiguration> listeners = new ArrayList<>();
		for(var diagramConfiguration : simulationDiagramItems.getItems()) {
			if(diagramConfiguration instanceof DiagramConfiguration.NonUi nonUi) {
				activations.add(nonUi);
			} else if (diagramConfiguration instanceof UIListenerConfiguration ui) {
				listeners.add(ui);
			} else {
				throw new RuntimeException("Unknown diagram configuration type: " + diagramConfiguration);
			}
		}

		return new SimulationModelConfiguration(variables, activations, listeners, SimulationModelConfiguration.metadataBuilder().build());
	}

	private void checkIfSimulationShouldBeSaved() {
		if(!savedProperty.get()) {
			Optional<ButtonType> selected = stageManager.makeAlert(Alert.AlertType.WARNING, Arrays.asList(ButtonType.YES, ButtonType.NO), "simulation.file.unsaved.title", "simulation.file.unsaved.text").showAndWait();
			if(selected.isPresent() && selected.get() == ButtonType.YES) {
				saveSimulation();
			}
		}
	}

	@FXML
	private void saveSimulation() {
		if(configurationPath.get().toString().isEmpty()) {
			saveSimulationAs();
			return;
		}

		try {
			this.simulationFileHandler.saveConfiguration(buildSimulationModel(), currentProject.getLocation().resolve(configurationPath.get()));
			savedProperty.set(true);
		} catch (IOException ex) {
			stageManager.makeExceptionAlert(ex, "simulation.save.error").showAndWait();
		}
	}

	@FXML
	private void saveSimulationAs() {
		final FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().addAll(fileChooserManager.getSimBFilter());
		chooser.setInitialFileName(MoreFiles.getNameWithoutExtension(currentProject.getLocation()) + ".json");
		Path path = fileChooserManager.showSaveFileChooser(chooser, FileChooserManager.Kind.SIMULATION, this);
		if(path == null) {
			return;
		}
		try {
			this.simulationFileHandler.saveConfiguration(buildSimulationModel(), path);
			savedProperty.set(true);
			Path previousPath = configurationPath.get();
			Path relativePath = currentProject.getLocation().relativize(path);
			SimulationModel simulationModel = new SimulationModel(relativePath);
			Machine currentMachine = currentProject.getCurrentMachine();
			List<SimulationItem> simulationTasks = simulationItems.getItems();
			ISimulationModelConfiguration config = realTimeSimulator.getConfig();
			simulationTasks.forEach(task -> {
				int size = task.getInformation().get("EXECUTIONS") == null ? ((SimulationBlackBoxModelConfiguration) config).getTimedTraces().size() : (int) task.getInformation().get("EXECUTIONS");
				currentMachine.addValidationTaskIfNotExist(task.withSimulationPath(size, relativePath));
			});
			if (currentMachine.getSimulations().contains(simulationModel)) {
				cbSimulation.getSelectionModel().select(simulationModel);
			} else {
				currentMachine.getSimulations().add(simulationModel);
				cbSimulation.getSelectionModel().selectLast();
			}
		} catch (IOException ex) {
			stageManager.makeExceptionAlert(ex, "simulation.save.error").showAndWait();
		}
	}

	@FXML
	private void addDirectActivation() {
		simulationDiagramItems.getItems().add(new ActivationOperationConfiguration(
				i18n.translate("simulation.item.newDirectActivation"),
				"Event",
				"0",
				0,
				null,
				ActivationKind.MULTI,
				null,
				null,
				TransitionSelection.FIRST,
				null,
				true,
				null,
				null,
				""
		));
	}

	@FXML
	private void addChoiceActivation() {
		simulationDiagramItems.getItems().add(new ActivationChoiceConfiguration(i18n.translate("simulation.item.newChoiceActivation"), Map.of(), ""));
	}

	@FXML
	private void addUiListener() {
		simulationDiagramItems.getItems().add(new UIListenerConfiguration(i18n.translate("simulation.item.newUiListener"), "Event", null, List.of(), ""));
	}

	@FXML
	private void removeDiagramElement() {
		DiagramConfiguration diagramConfiguration = simulationDiagramItems.getSelectionModel().getSelectedItem();
		simulationDiagramItems.getItems().remove(diagramConfiguration);
	}

	@FXML
	private void saveTrace() {
		try {
			traceFileHandler.save(currentTrace.get(), currentProject.getCurrentMachine());
		} catch (IOException | RuntimeException exc) {
			traceFileHandler.showSaveError(exc);
		}
	}

	@FXML
	private void saveTimedTrace() {
		try {
			this.simulationFileHandler.saveTimedTrace(currentTrace.get(), realTimeSimulator.getTimestamps(), "Real-Time Simulation");
		} catch (IOException exception) {
			stageManager.makeExceptionAlert(exception, "simulation.save.error").showAndWait();
		}
	}

	@FXML
	private void saveAutomaticSimulation() {
		try {
			this.simulationFileHandler.saveUIInteractions();
		} catch (IOException exception) {
			stageManager.makeExceptionAlert(exception, "simulation.save.ui.error").showAndWait();
		}
	}

	public void updateSimulationStatistics(int numberExecuted, int numberTotal) {
		Platform.runLater(() -> {
			progressBar.setVisible(true);
			progressBar.setProgress((double) numberExecuted / (double) numberTotal);
			lbSimulationStats.setText(String.format("%s/%s", numberExecuted, numberTotal));
		});
	}

	public void resetSimulationStatistics() {
		Platform.runLater(() -> {
			progressBar.setVisible(false);
			progressBar.setProgress(0);
			lbSimulationStats.setText("");
		});
	}

}
