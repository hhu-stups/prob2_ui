package de.prob2.ui.verifications.modelchecking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob.check.ConsistencyChecker;
import de.prob.check.IModelCheckJob;
import de.prob.check.IModelCheckListener;
import de.prob.check.IModelCheckingResult;
import de.prob.check.ModelCheckingOptions;
import de.prob.check.StateSpaceStats;
import de.prob.model.representation.AbstractElement;
import de.prob.statespace.StateSpace;

import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.operations.OperationsView;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.MachineStatusHandler;
import de.prob2.ui.verifications.ShouldExecuteValueFactory;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class ModelcheckingController extends ScrollPane implements IModelCheckListener {
	private enum SearchStrategy {
		MIXED_BF_DF("verifications.modelchecking.stage.strategy.items.mixedBfDf"),
		BREADTH_FIRST("verifications.modelchecking.stage.strategy.items.breadthFirst"),
		DEPTH_FIRST("verifications.modelchecking.stage.strategy.items.depthFirst"),
		//HEURISTIC_FUNCTION("verifications.modelchecking.stage.strategy.items.heuristicFunction"),
		//HASH_RANDOM("verifications.modelchecking.stage.strategy.items.hashRandom"),
		//RANDOM("verifications.modelchecking.stage.strategy.items.random"),
		//OUT_DEGREE("verifications.modelchecking.stage.strategy.items.outDegree"),
		//DISABLED_TRANSITIONS("verifications.modelchecking.stage.strategy.items.disabledTransitions"),
		;
		
		private final String name;
		
		private SearchStrategy(final String name) {
			this.name = name;
		}
		
		public String getName() {
			return this.name;
		}
	}
	
	private final class ModelcheckingStageController extends Stage {
		@FXML
		private Button startButton;
		@FXML
		private ChoiceBox<SearchStrategy> selectSearchStrategy;
		@FXML
		private CheckBox findDeadlocks;
		@FXML
		private CheckBox findInvViolations;
		@FXML
		private CheckBox findBAViolations;
		@FXML
		private CheckBox findGoal;
		@FXML
		private CheckBox stopAtFullCoverage;

		private ModelcheckingStageController(final StageManager stageManager) {
			stageManager.loadFXML(this, "modelchecking_stage.fxml");
		}

		@FXML
		private void initialize() {
			this.initModality(Modality.APPLICATION_MODAL);
			this.selectSearchStrategy.getItems().setAll(SearchStrategy.values());
			this.selectSearchStrategy.setValue(SearchStrategy.MIXED_BF_DF);
			this.selectSearchStrategy.setConverter(new StringConverter<SearchStrategy>() {
				@Override
				public String toString(final SearchStrategy object) {
					return bundle.getString(object.getName());
				}
				
				@Override
				public SearchStrategy fromString(final String string) {
					throw new UnsupportedOperationException("Conversion from String to SearchStrategy not supported");
				}
			});
		}

		@FXML
		private void startModelCheck() {
			if (currentTrace.exists()) {
				checkItem();
			} else {
				stageManager.makeAlert(Alert.AlertType.ERROR, bundle.getString("verifications.modelchecking.stage.noMachineLoaded"))
						.showAndWait();
				this.hide();
			}
		}
		
		private void checkItem() {
			Thread currentJobThread = new Thread(() -> {
				synchronized(lock) {
					updateCurrentValues(getOptions(), currentTrace.getStateSpace(), selectSearchStrategy.getConverter(), selectSearchStrategy.getValue());
					startModelchecking();
				}
			}, "Model Check Result Waiter " + threadCounter.getAndIncrement());
			currentJobThreads.add(currentJobThread);
			currentJobThread.start();
		}
		
		private ModelCheckingOptions getOptions() {
			ModelCheckingOptions options = new ModelCheckingOptions();
			
			switch (selectSearchStrategy.getValue()) {
				case MIXED_BF_DF:
					break;
				case BREADTH_FIRST:
					options = options.breadthFirst(true);
					break;
				case DEPTH_FIRST:
					options = options.depthFirst(true);
					break;
				default:
					throw new IllegalArgumentException("Unhandled search strategy: " + selectSearchStrategy.getValue());
			}
			
			options = options.checkDeadlocks(findDeadlocks.isSelected());
			options = options.checkInvariantViolations(findInvViolations.isSelected());
			options = options.checkAssertions(findBAViolations.isSelected());
			options = options.checkGoal(findGoal.isSelected());
			options = options.stopAtFullCoverage(stopAtFullCoverage.isSelected());
			options = options.recheckExisting(true);
			return options;
		}

		@FXML
		private void cancel() {
			cancelModelcheck();
			this.hide();
		}

		private void setDisableStart(final boolean disableStart) {
			Platform.runLater(() -> this.startButton.setDisable(disableStart));
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelcheckingController.class);
	private static final AtomicInteger threadCounter = new AtomicInteger(0);

	@FXML
	private AnchorPane statsPane;

	@FXML
	private Button addModelCheckButton;
	@FXML
	private Button checkMachineButton;
	@FXML
	private Button cancelButton;
	@FXML
	private HelpButton helpButton;
	
	@FXML
	private TableView<ModelCheckingItem> tvItems;
	
	@FXML
	private TableColumn<ModelCheckingItem, FontAwesomeIconView> statusColumn;
	
	@FXML
	private TableColumn<ModelCheckingItem, String> strategyColumn;
	
	@FXML
	private TableColumn<ModelCheckingItem, String> descriptionColumn;
	
	@FXML
	private TableColumn<IExecutableItem, CheckBox> shouldExecuteColumn;

	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final StatsView statsView;
	private final ModelcheckingStageController stageController;
	private final StageManager stageManager;
	private final Injector injector;
	private final ResourceBundle bundle;

	private final Map<String, IModelCheckJob> jobs;
	private final ListProperty<IModelCheckJob> currentJobs;
	private final ListProperty<Thread> currentJobThreads;
	private ModelCheckStats currentStats;
	private ModelCheckingOptions currentOptions;
	
	private Object lock = new Object();

	@Inject
	private ModelcheckingController(final CurrentTrace currentTrace,
			final CurrentProject currentProject, final StageManager stageManager, 
			final StatsView statsView, final Injector injector, final ResourceBundle bundle) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.statsView = statsView;
		this.stageManager = stageManager;
		this.injector = injector;
		this.bundle = bundle;
		this.currentJobs = new SimpleListProperty<>(this, "currentJobs", FXCollections.observableArrayList());
		this.currentJobThreads = new SimpleListProperty<>(this, "currentJobThreads", FXCollections.observableArrayList());
		stageManager.loadFXML(this, "modelchecking_stats_view.fxml");
		this.stageController = new ModelcheckingStageController(stageManager);
		this.jobs = new HashMap<>();
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent("Verification.md.html");
		showStats(new ModelCheckStats(stageManager, this, statsView, injector));
		setBindings();
		setListeners();
		setContextMenus();
	}
	
	private void setBindings() {
		addModelCheckButton.disableProperty().bind(currentTrace.existsProperty().not().or(currentJobs.emptyProperty().not()));
		checkMachineButton.disableProperty().bind(currentTrace.existsProperty().not().or(currentJobs.emptyProperty().not()));
		cancelButton.disableProperty().bind(currentJobs.emptyProperty());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		strategyColumn.setCellValueFactory(new PropertyValueFactory<>("strategy"));
		descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		shouldExecuteColumn.setCellValueFactory(new ShouldExecuteValueFactory(CheckingType.MODELCHECKING, injector));
		CheckBox selectAll = new CheckBox();
		selectAll.setSelected(true);
		selectAll.selectedProperty().addListener((observable, from, to) -> {
			for(IExecutableItem item : tvItems.getItems()) {
				item.setShouldExecute(to);
				Machine machine = injector.getInstance(CurrentProject.class).getCurrentMachine();
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.MODELCHECKING);
				tvItems.refresh();
			}
		});
		shouldExecuteColumn.setGraphic(selectAll);
		shouldExecuteColumn.setMaxWidth(this.getPrefWidth());
		
		
		tvItems.disableProperty().bind(currentTrace.existsProperty().not().or(currentJobs.emptyProperty().not()));
		FontSize fontsize = injector.getInstance(FontSize.class);
		((FontAwesomeIconView) (addModelCheckButton.getGraphic())).glyphSizeProperty().bind(fontsize.multiply(2.0));
	}
	
	private void setListeners() {
		currentProject.currentMachineProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue != null) {
				tvItems.itemsProperty().unbind();
				tvItems.itemsProperty().bind(newValue.modelcheckingItemsProperty());
				resetView();
			} else {
				tvItems.getItems().clear();
				tvItems.itemsProperty().unbind();
			}
		});
		
		currentTrace.existsProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue) {
				checkMachineButton.disableProperty().bind(currentProject.getCurrentMachine().modelcheckingItemsProperty().emptyProperty().or(currentJobs.emptyProperty().not()));
			} else {
				checkMachineButton.disableProperty().bind(currentTrace.existsProperty().not().or(currentJobs.emptyProperty().not()));
			}
		});
		
		currentProject.addListener((observable, from, to) -> {
			if(to != from) {
				this.resetView();
			}
		});
		
		tvItems.setOnMouseClicked(e-> {
			ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
			if(item != null) {
				if(item.getStats() == null) {
					if(e.getButton() == MouseButton.PRIMARY) {
						checkItem(item);
					}
				} else {
					showStats(item.getStats());
					if (e.getClickCount() >= 2 && e.getButton() == MouseButton.PRIMARY &&
							item.getChecked() == Checked.FAIL && item.getStats().getTrace() != null) {
						currentTrace.set(item.getStats().getTrace());
						injector.getInstance(StatsView.class).update(item.getStats().getTrace());
					}
				}
			}
		});
	}
	
	private void setContextMenus() {
		tvItems.setRowFactory(table -> {
			final TableRow<ModelCheckingItem> row = new TableRow<>();
			
			final BooleanBinding disableErrorItemsBinding = Bindings.createBooleanBinding(
				() -> row.isEmpty() || row.getItem() == null || row.getItem().getStats() == null || row.getItem().getStats().getTrace() == null,
				row.emptyProperty(), row.itemProperty());
			
			MenuItem showTraceToErrorItem = new MenuItem(bundle.getString("verifications.modelchecking.menu.showTraceToError"));
			showTraceToErrorItem.setOnAction(e-> {
				ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
				currentTrace.set(item.getStats().getTrace());
				injector.getInstance(StatsView.class).update(item.getStats().getTrace());
			});
			showTraceToErrorItem.disableProperty().bind(disableErrorItemsBinding);
			
			MenuItem checkItem = new MenuItem(bundle.getString("verifications.modelchecking.menu.checkSeparately"));
			checkItem.setOnAction(e-> {
				ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
				item.setOptions(item.getOptions().recheckExisting(true));
				checkItem(item);
			});
			checkItem.disableProperty().bind(row.emptyProperty());
			
			MenuItem showFullValueItem = new MenuItem(bundle.getString("verifications.modelchecking.menu.showFullValue"));
			showFullValueItem.setOnAction(e-> {
				ModelcheckingItemFullValueStage fullValueStage = injector.getInstance(ModelcheckingItemFullValueStage.class);
				ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
				fullValueStage.setValues(item.getStrategy(), item.getDescription());
				fullValueStage.show();
			});
			showFullValueItem.disableProperty().bind(row.emptyProperty());
			
			MenuItem searchForNewErrorsItem = new MenuItem(bundle.getString("verifications.modelchecking.stage.options.searchForNewErrors"));
			searchForNewErrorsItem.setOnAction(e-> {
				ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
				item.setOptions(item.getOptions().recheckExisting(false));
				checkItem(item);
			});
			searchForNewErrorsItem.disableProperty().bind(disableErrorItemsBinding);
			
			MenuItem removeItem = new MenuItem(bundle.getString("verifications.modelchecking.menu.remove"));
			removeItem.setOnAction(e -> removeItem());
			removeItem.disableProperty().bind(row.emptyProperty());
			
			row.contextMenuProperty().bind(
				Bindings.when(row.emptyProperty())
				.then((ContextMenu)null)
				.otherwise(new ContextMenu(showTraceToErrorItem, checkItem, showFullValueItem, searchForNewErrorsItem, removeItem))
			);
			return row;
		});
	}

	@FXML
	public void addModelCheck() {
		if (!stageController.isShowing()) {
			this.stageController.showAndWait();
		}
	}
	
	private void removeItem() {
		Machine machine = currentProject.getCurrentMachine();
		ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
		machine.removeModelcheckingItem(item);
	}
	
	@FXML
	public void checkMachine() {
		currentProject.currentMachineProperty().get().getModelcheckingItems().forEach(item -> {
			item.setOptions(item.getOptions().recheckExisting(true));
			checkItem(item);
		});
	}
	
	@FXML
	public synchronized void cancelModelcheck() {
		currentJobs.forEach(job -> job.getStateSpace().sendInterrupt());
		currentJobThreads.forEach(Thread::interrupt);
	}
	
	private void checkItem(ModelCheckingItem item) {
		if(!item.shouldExecute()) {
			return;
		}
		Thread currentJobThread = new Thread(() -> {
			synchronized(lock) {
				updateCurrentValues(item.getOptions(), currentTrace.getStateSpace(), item);
				startModelchecking();
				tvItems.getSelectionModel().select(item);
			}
		}, "Model Check Result Waiter " + threadCounter.getAndIncrement());
		currentJobThreads.add(currentJobThread);
		currentJobThread.start();
	}

	private void updateCurrentValues(ModelCheckingOptions options, StateSpace stateSpace, StringConverter<SearchStrategy> converter, SearchStrategy strategy) {
		updateCurrentValues(options, stateSpace);
		ModelCheckingItem modelcheckingItem = new ModelCheckingItem(currentOptions, currentStats, converter.toString(strategy), toPrettyString(currentOptions));
		currentStats.updateItem(modelcheckingItem);
		currentProject.getCurrentMachine().addModelcheckingItem(modelcheckingItem);
		tvItems.getSelectionModel().selectLast();
	}
	
	private void updateCurrentValues(ModelCheckingOptions options, StateSpace stateSpace) {
		currentOptions = options;
		currentStats = new ModelCheckStats(stageManager, this, statsView, injector);
		IModelCheckJob job = new ConsistencyChecker(stateSpace, options, null, this);
		currentJobs.add(job);
	}
	
	private void updateCurrentValues(ModelCheckingOptions options, StateSpace stateSpace, ModelCheckingItem item) {
		updateCurrentValues(options, stateSpace);
		currentStats.updateItem(item);
	}

	private String toPrettyString(ModelCheckingOptions options) {
		int size = currentJobs.size();
		IModelCheckJob job = currentJobs.get(size - 1);
		AbstractElement main = job.getStateSpace().getMainComponent();
		List<String> optsList = options.getPrologOptions().stream().map(ModelCheckingOptions.Options::getDescription).collect(Collectors.toList());
		String name = main == null ? bundle.getString("verifications.modelchecking.machineNamePlaceholder") : main.toString();
		if (optsList.isEmpty()) {
			return name;
		} else {
			return String.format(bundle.getString("verifications.modelchecking.prettyStringWithOptions"), name, String.join(", ", optsList));
		}
	}

	private void showStats(ModelCheckStats stats) {
		statsPane.getChildren().setAll(stats);
		AnchorPane.setTopAnchor(stats, 0.0);
		AnchorPane.setRightAnchor(stats, 0.0);
		AnchorPane.setBottomAnchor(stats, 0.0);
		AnchorPane.setLeftAnchor(stats, 0.0);
	}

	public void resetView() {
		showStats(new ModelCheckStats(stageManager, this, statsView, injector));
	}

	@Override
	public void updateStats(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
		try {
			final IModelCheckJob job = jobs.get(jobId);
			if (job == null) {
				LOGGER.error("Model check job for ID {} is missing or null", jobId);
				return;
			}
			currentStats.updateStats(job, timeElapsed, stats);
		} catch (RuntimeException e) {
			LOGGER.error("Exception in updateStats", e);
			Platform.runLater(
					() -> stageManager.makeAlert(Alert.AlertType.ERROR, "Exception in updateStats:\n" + e).show());
		}
	}
	
	private void startModelchecking() {
		stageController.setDisableStart(true);
		int size = currentJobs.size();
		IModelCheckJob job = currentJobs.get(size - 1);
		jobs.put(job.getJobId(), job);
		currentStats.startJob();
		Platform.runLater(() -> showStats(currentStats));

		final IModelCheckingResult result;
		try {
			result = job.call();
		} catch (Exception e) {
			LOGGER.error("Exception while running model check job", e);
			Platform.runLater(() -> stageManager.makeAlert(Alert.AlertType.ERROR, String.format(bundle.getString("verifications.modelchecking.exceptionWhileRunningJob"), e)).show());
			return;
		} finally {
			currentJobThreads.remove(size - 1);
			stageController.setDisableStart(false);
		}
		// The consistency checker sometimes doesn't call isFinished, so
		// we call it manually here with some dummy information.
		// If the checker already called isFinished, this call won't do
		// anything - on the first call, the checker was removed from
		// the jobs map, so the second call returns right away.
		isFinished(job.getJobId(), 0, result, new StateSpaceStats(0, 0, 0));
		currentJobs.remove(size - 1);
	}

	@Override
	public void isFinished(String jobId, long timeElapsed, IModelCheckingResult result, StateSpaceStats stats) {
		final IModelCheckJob job = jobs.remove(jobId);
		if (job == null) {
			// isFinished was already called for this job
			return;
		}
		Platform.runLater(() -> {
			currentStats.isFinished(job, timeElapsed, result);
			this.stageController.hide();
			injector.getInstance(OperationsView.class).update(currentTrace.get());
			injector.getInstance(StatsView.class).update(currentTrace.get());
			tvItems.refresh();
		});
	}
	
}
