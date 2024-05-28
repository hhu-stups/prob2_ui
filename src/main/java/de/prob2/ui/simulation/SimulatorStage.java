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

import de.prob.statespace.LoadedMachine;
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
import de.prob2.ui.sharedviews.InterruptIfRunningButton;
import de.prob2.ui.simulation.choice.SimulationChoosingStage;
import de.prob2.ui.simulation.configuration.ActivationChoiceConfiguration;
import de.prob2.ui.simulation.configuration.ActivationOperationConfiguration;
import de.prob2.ui.simulation.configuration.DiagramConfiguration;
import de.prob2.ui.simulation.configuration.ISimulationModelConfiguration;
import de.prob2.ui.simulation.configuration.SimulationBlackBoxModelConfiguration;
import de.prob2.ui.simulation.configuration.SimulationExternalConfiguration;
import de.prob2.ui.simulation.configuration.SimulationFileHandler;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.configuration.UIListenerConfiguration;
import de.prob2.ui.simulation.interactive.UIInteractionHandler;
import de.prob2.ui.simulation.interactive.UIInteractionSaver;
import de.prob2.ui.simulation.model.SimulationModel;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;
import de.prob2.ui.simulation.simulators.Scheduler;
import de.prob2.ui.simulation.simulators.SimulationSaver;
import de.prob2.ui.simulation.simulators.check.SimulationStatsView;
import de.prob2.ui.simulation.table.SimulationItem;
import de.prob2.ui.simulation.table.SimulationListViewDiagramItem;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;

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

			if (item != null && !empty) {
				this.setContextMenu(null);
				ContextMenu contextMenu = new ContextMenu();

				List<MenuItem> menuItems = FXCollections.observableArrayList();

				MenuItem checkItem = new MenuItem(i18n.translate("simulation.contextMenu.check"));
				checkItem.disableProperty().bind(configurationPath.isNull().or(simulationItemHandler.runningProperty().or(lastSimulator.isNull().or(lastSimulator.get().runningProperty()))));
				checkItem.setOnAction(e -> simulationItemHandler.checkItem(this.getItem()));

				MenuItem editItem = new MenuItem(i18n.translate("simulation.contextMenu.edit"));
				editItem.setOnAction(e -> editSimulation(this.getItem()));

				MenuItem removeItem = new MenuItem(i18n.translate("simulation.contextMenu.remove"));
				removeItem.setOnAction(e -> simulationItemHandler.removeItem(this.getItem()));

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
						menuItem.setOnAction(e -> simulationItemHandler.addItem(item.withSimulationPath(targetModel.getPath())));
						copyMenu.getItems().add(menuItem);
					}
					copyMenu.setDisable(copyMenu.getItems().isEmpty());
				}
				menuItems.add(copyMenu);

				MenuItem showTraces = new MenuItem(i18n.translate("simulation.contextMenu.showTraces"));
				showTraces.disableProperty().bind(item.tracesProperty().emptyProperty());
				showTraces.setOnAction(e -> {
					if (item.getTraces().size() == 1) {
						currentTrace.set(item.getTraces().get(0));
					} else {
						SimulationTracesView tracesView = injector.getInstance(SimulationTracesView.class);
						SimulationScenarioHandler simulationScenarioHandler = injector.getInstance(SimulationScenarioHandler.class);
						simulationScenarioHandler.setSimulatorStage(simulatorStage);
						tracesView.setItems(item, item.getTraces(), item.getTimestamps(), item.getStatuses(), item.getSimulationStats().getEstimatedValues());
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
					if (currentTrace.get() != null) {
						traceFileHandler.save(item, currentProject.getCurrentMachine());
					}
				});
				menuItems.add(saveTraces);

				MenuItem saveTimedTraces = new MenuItem(i18n.translate("simulation.contextMenu.saveGeneratedTimedTraces"));
				saveTimedTraces.disableProperty().bind(item.tracesProperty().emptyProperty().or(
					Bindings.createBooleanBinding(() -> this.itemProperty().get() == null, this.itemProperty())));
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
	private TableColumn<SimulationItem, Checked> simulationStatusColumn;

	@FXML
	private TableColumn<SimulationItem, String> simulationConfigurationColumn;

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

	private final StageManager stageManager;

	private final CurrentProject currentProject;

	private final CurrentTrace currentTrace;

	private final Injector injector;

	private final RealTimeSimulator realTimeSimulator;

	private final MachineLoader machineLoader;

	private final I18n i18n;

	private final FileChooserManager fileChooserManager;

	private final SimulationSaver simulationSaver;

	private final TraceFileHandler traceFileHandler;

	private final ObjectProperty<Path> configurationPath;

	private final BooleanProperty savedProperty;

	private final SimulationItemHandler simulationItemHandler;

	private final SimulationMode simulationMode;

	private int time;

	private Timer timer;

	private final ObjectProperty<RealTimeSimulator> lastSimulator;

	private ChangeListener<Number> timeListener;

	@Inject
	public SimulatorStage(
		final StageManager stageManager, final CurrentProject currentProject, final CurrentTrace currentTrace,
		final Injector injector, final RealTimeSimulator realTimeSimulator, final MachineLoader machineLoader,
		final SimulationItemHandler simulationItemHandler, final SimulationMode simulationMode,
		final I18n i18n, final FileChooserManager fileChooserManager,
		final SimulationSaver simulationSaver, final TraceFileHandler traceFileHandler,
		final StopActions stopActions
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
		this.simulationSaver = simulationSaver;
		this.traceFileHandler = traceFileHandler;
		this.configurationPath = new SimpleObjectProperty<>(this, "configurationPath", null);
		this.savedProperty = new SimpleBooleanProperty(this, "savedProperty", true);
		this.time = 0;
		this.timer = new Timer(true);
		stopActions.add(this::cancelTimer);
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
						() -> configurationPath.get().toString().isEmpty() ? i18n.translate("simulation.defaultSimulation") : currentProject.getLocation().relativize(configurationPath.get()) + (savedProperty.get() ? "": "*"),
						currentProject, configurationPath, savedProperty
					)
				))
		);

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

		btAddSimulation.disableProperty().bind(currentTrace.isNull().or(injector.getInstance(DisablePropertyController.class).disableProperty()).or(configurationPath.isNull()).or(realTimeSimulator.runningProperty()).or(currentProject.currentMachineProperty().isNull()));

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

		this.simulationDiagramItems.setCellFactory(lv -> new SimulationListViewDiagramItem(stageManager, i18n, savedProperty));

		machineLoader.loadingProperty().addListener((observable, from, to) -> {
			if (to) {
				stopSimulator(lastSimulator.get());
			}
			resetSimulator();
		});

		this.addEventFilter(WindowEvent.WINDOW_SHOWING, event -> loadSimulationsFromMachine(currentProject.getCurrentMachine()));

		final ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
			checkIfSimulationShouldBeSaved();
			configurationPath.set(null);
			simulationDiagramItems.getItems().clear();
			simulationItems.itemsProperty().unbind();
			loadSimulationsFromMachine(to);
		};

		currentProject.addListener((observable, from, to) -> machineChangeListener.changed(null, null, null));

		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());

		simulationStatusColumn.setCellFactory(col -> new CheckedCell<>());
		simulationStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		simulationConfigurationColumn.setCellFactory(lv -> new SimulationTaskItem(stageManager, i18n));
		simulationConfigurationColumn.setCellValueFactory(features -> new SimpleStringProperty(""));


		simulationItems.setRowFactory(table -> new SimulationItemRow(this));

		this.currentTrace.addListener((observable, from, to) -> simulationDiagramItems.refresh());

		simulationItems.setOnMouseClicked(e -> {
			SimulationItem item = simulationItems.getSelectionModel().getSelectedItem();
			if (
				e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY && item != null && currentTrace.get() != null &&
				configurationPath.get() != null && !simulationItemHandler.runningProperty().get() && lastSimulator.get() != null &&
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
					SimulationHelperFunctions.initSimulator(stageManager, this, realTimeSimulator, currentTrace.getStateSpace().getLoadedMachine(), configPath);
				}
				realTimeSimulator.run();
				startTimer(realTimeSimulator);
			} else { //  SimulationBlackBoxModelConfiguration
				List<Path> timedTraces = ((SimulationBlackBoxModelConfiguration) config).getTimedTraces();
				configPath = timedTraces.get((int) (Math.random() * timedTraces.size()));
				injector.getInstance(Scheduler.class).setSimulator(realTimeSimulator);
				try {
					this.time = 0;
					ISimulationModelConfiguration modelConfiguration = SimulationFileHandler.constructConfiguration(configPath, currentTrace.getStateSpace().getLoadedMachine());
					realTimeSimulator.initSimulator(modelConfiguration);
					Trace trace = new Trace(currentTrace.getStateSpace());
					currentTrace.set(trace);
					realTimeSimulator.setupBeforeSimulation(trace);
					trace.setExploreStateByDefault(false);
					realTimeSimulator.run();
					startTimer(realTimeSimulator);
					trace.setExploreStateByDefault(true);
				} catch (IOException e) {
					final Alert alert = stageManager.makeExceptionAlert(e, "simulation.error.header.fileNotFound", "simulation.error.body.fileNotFound");
					alert.initOwner(this);
					alert.showAndWait();
				}
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
				observableList.addAll(modelConfig.getActivationConfigurations());
				observableList.addAll(modelConfig.getUiListenerConfigurations());
			}
		}

		simulationDiagramItems.getItems().clear();
		simulationDiagramItems.setItems(observableList);
		simulationDiagramItems.refresh();
	}

	@FXML
	public void addSimulation() {
		SimulationChoosingStage choosingStage = injector.getInstance(SimulationChoosingStage.class);
		choosingStage.reset();
		choosingStage.setModifying(false, null);
		choosingStage.showAndWait();
		simulationItems.refresh();
	}

	public void editSimulation(SimulationItem item) {
		SimulationChoosingStage choosingStage = injector.getInstance(SimulationChoosingStage.class);
		choosingStage.reset();
		choosingStage.setData(item);
		choosingStage.setModifying(true, item);
		choosingStage.showAndWait();
		simulationItems.refresh();
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
					if (currentTrace.getCurrentState().isInitialised() && time + 100 < realTimeSimulator.getTime() + realTimeSimulator.getDelay()) {
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

	public void loadSimulationsFromMachine(Machine machine) {
		cbSimulation.itemsProperty().unbind();
		if (machine != null) {
			cbSimulation.setItems(machine.getSimulations());
			if(cbSimulation.getItems().isEmpty()) {
				cbSimulation.getItems().add(new SimulationModel(Paths.get("")));
			}
			cbSimulation.getSelectionModel().clearSelection();
			cbSimulation.getSelectionModel().select(0);
		} else {
			cbSimulation.setItems(FXCollections.observableArrayList());
		}
	}

	public void loadSimulationIntoSimulator(SimulationModel simulation) {
		configurationPath.set(simulation == null ? null :
				simulation.getPath().equals(Paths.get("")) ? simulation.getPath() : currentProject.getLocation().resolve(simulation.getPath()));
		LoadedMachine loadedMachine = currentTrace.getStateSpace() == null ? null : currentTrace.getStateSpace().getLoadedMachine();
		if (simulation != null && loadedMachine != null) {
			injector.getInstance(SimulationChoosingStage.class).setPath(configurationPath.get());
			lbTime.setText("");
			this.time = 0;
			simulationItemHandler.reset(simulation);
			SimulationHelperFunctions.initSimulator(stageManager, this, realTimeSimulator, loadedMachine, configurationPath.get());
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
		if(cbSimulation.getItems().isEmpty()) {
			cbSimulation.getItems().add(new SimulationModel(Paths.get("")));
		}
		cbSimulation.getSelectionModel().clearSelection();
		cbSimulation.getSelectionModel().select(0);
	}

	private SimulationModelConfiguration buildSimulationModel() {
		Map<String, String> variables = new HashMap<>();
		List<DiagramConfiguration> activations = new ArrayList<>();
		List<UIListenerConfiguration> listeners = new ArrayList<>();

		for(DiagramConfiguration diagramConfiguration : simulationDiagramItems.getItems()) {
			if(diagramConfiguration instanceof ActivationChoiceConfiguration || diagramConfiguration instanceof ActivationOperationConfiguration) {
				activations.add(diagramConfiguration);
			} else {
				listeners.add((UIListenerConfiguration) diagramConfiguration);
			}
		}

		return new SimulationModelConfiguration(variables, activations, listeners, SimulationModelConfiguration.metadataBuilder(SimulationModelConfiguration.SimulationFileType.SIMULATION)
				.build());
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
			savedProperty.set(true);
			simulationSaver.saveConfiguration(buildSimulationModel(), currentProject.getLocation().resolve(configurationPath.get()));
		} catch (IOException ex) {
			injector.getInstance(StageManager.class).makeExceptionAlert(ex, "simulation.save.error").showAndWait();
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
			savedProperty.set(true);
			simulationSaver.saveConfiguration(buildSimulationModel(), path);
			Path previousPath = configurationPath.get();
			Path relativePath = currentProject.getLocation().relativize(path);
			SimulationModel simulationModel = new SimulationModel(relativePath);
			Machine currentMachine = currentProject.getCurrentMachine();
			List<SimulationItem> simulationTasks = simulationItems.getItems();
			simulationTasks.forEach(task -> currentMachine.addValidationTaskIfNotExist(task.withSimulationPath(relativePath)));
			if (currentMachine.getSimulations().contains(simulationModel)) {
				cbSimulation.getSelectionModel().select(simulationModel);
			} else {
				if(previousPath.toString().isEmpty()) {
					cbSimulation.getItems().remove(new SimulationModel(Paths.get("")));
				}
				currentMachine.getSimulations().add(simulationModel);
				cbSimulation.getSelectionModel().selectLast();
			}
		} catch (IOException ex) {
			injector.getInstance(StageManager.class).makeExceptionAlert(ex, "simulation.save.error").showAndWait();
		}
	}

	@FXML
	private void addDirectActivation() {
		simulationDiagramItems.getItems().add(new ActivationOperationConfiguration(i18n.translate("simulation.item.newDirectActivation"), "Event", "0", 0, null, ActivationOperationConfiguration.ActivationKind.MULTI,
		null, null, null, true, null, null));
	}

	@FXML
	private void addChoiceActivation() {
		simulationDiagramItems.getItems().add(new ActivationChoiceConfiguration(i18n.translate("simulation.item.newChoiceActivation"), new HashMap<>()));
	}

	@FXML
	private void addUiListener() {
		simulationDiagramItems.getItems().add(new UIListenerConfiguration(i18n.translate("simulation.item.newUiListener"), "Event", null, new ArrayList<>()));
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
			injector.getInstance(SimulationSaver.class).saveConfiguration(currentTrace.get(), realTimeSimulator.getTimestamps(), "Real-Time Simulation");
		} catch (IOException exception) {
			injector.getInstance(StageManager.class).makeExceptionAlert(exception, "simulation.save.error").showAndWait();
		}
	}

	@FXML
	private void saveAutomaticSimulation() {
		try {
			injector.getInstance(UIInteractionSaver.class).saveUIInteractions();
		} catch (IOException exception) {
			injector.getInstance(StageManager.class).makeExceptionAlert(exception, "simulation.save.ui.error").showAndWait();
		}
	}
}
