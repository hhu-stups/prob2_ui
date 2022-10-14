package de.prob2.ui.verifications.modelchecking;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.check.StateSpaceStats;
import de.prob.statespace.ITraceDescription;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.SimpleStatsView;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ItemSelectedFactory;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public final class ModelcheckingView extends ScrollPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(ModelcheckingView.class);
	private static final BigInteger MIB_FACTOR = new BigInteger("1024").pow(2);

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
	private TableColumn<IExecutableItem, CheckBox> shouldExecuteColumn;

	@FXML
	private TableColumn<ModelCheckingItem, Checked> statusColumn;

	@FXML
	private TableColumn<ModelCheckingItem, String> strategyColumn;

	@FXML
	private TableColumn<ModelCheckingItem, String> descriptionColumn;

	@FXML
	private TableView<ModelCheckingJobItem> tvChecks;

	@FXML
	private TableColumn<ModelCheckingJobItem, Checked> jobStatusColumn;

	@FXML
	private TableColumn<ModelCheckingJobItem, String> messageColumn;

	@FXML
	private VBox statsBox;

	@FXML
	private Label elapsedTime;

	@FXML
	private ProgressBar progressBar;

	@FXML
	private SimpleStatsView simpleStatsView;

	@FXML
	private Label memoryUsage;

	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final Injector injector;
	private final I18n i18n;
	private final Modelchecker checker;
	private CliTaskExecutor cliExecutor;
	private final CheckBox selectAll;

	@Inject
	private ModelcheckingView(final CurrentTrace currentTrace,
			final CurrentProject currentProject, final StageManager stageManager, final Injector injector,
			final I18n i18n, final Modelchecker checker, final CliTaskExecutor cliExecutor) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.injector = injector;
		this.i18n = i18n;
		this.checker = checker;
		this.cliExecutor = cliExecutor;
		this.selectAll = new CheckBox();
		stageManager.loadFXML(this, "modelchecking_view.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent("verification", "Model");
		setBindings();
		setContextMenus();
	}

	private void setBindings() {
		addModelCheckButton.disableProperty().bind(currentTrace.isNull().or(injector.getInstance(DisablePropertyController.class).disableProperty()));
		final BooleanProperty noModelcheckingItems = new SimpleBooleanProperty();
		final ChangeListener<Machine> machineChangeListener = (o, from, to) -> {
			tvItems.itemsProperty().unbind();
			noModelcheckingItems.unbind();
			if (to != null) {
				tvItems.itemsProperty().bind(to.modelcheckingItemsProperty());
				noModelcheckingItems.bind(to.modelcheckingItemsProperty().emptyProperty());
			} else {
				tvItems.setItems(FXCollections.observableArrayList());
				noModelcheckingItems.set(true);
			}
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());
		checkMachineButton.disableProperty().bind(currentTrace.isNull().or(noModelcheckingItems.or(selectAll.selectedProperty().not().or(injector.getInstance(DisablePropertyController.class).disableProperty()))));
		cancelButton.disableProperty().bind(cliExecutor.runningProperty().not());

		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory(tvItems, selectAll));
		shouldExecuteColumn.setGraphic(selectAll);
		statusColumn.setCellFactory(col -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		descriptionColumn.setCellValueFactory(features -> Bindings.createStringBinding(() -> toUIString(features.getValue())));

		jobStatusColumn.setCellFactory(col -> new CheckedCell<>());
		jobStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		jobStatusColumn.setSortable(false);
		messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
		messageColumn.setSortable(false);

		tvItems.disableProperty().bind(currentTrace.isNull().or(injector.getInstance(DisablePropertyController.class).disableProperty()));
		tvChecks.disableProperty().bind(currentTrace.isNull().or(injector.getInstance(DisablePropertyController.class).disableProperty()));


		tvItems.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			tvChecks.itemsProperty().unbind();
			if (to != null) {
				tvChecks.itemsProperty().bind(to.itemsProperty());
				if(to.getItems().isEmpty()) {
					hideStats();
				} else {
					tvChecks.getSelectionModel().selectLast();
				}
			} else {
				// Because of the previous binding, the tvChecks items list is the same object as the job items list of one of the ModelcheckingItems.
				// This means that we can't just clear tvChecks.getItems(), because that would also clear the ModelcheckingItem's job items, which resets the item's status.
				tvChecks.setItems(FXCollections.observableArrayList());
			}
		});

		tvChecks.getSelectionModel().selectedItemProperty().addListener((observable, from, to) ->
			Platform.runLater(() -> {
				if(to != null && to.getStats() != null) {
					showStats(to.getTimeElapsed(), to.getStats(), to.getMemoryUsed());
				} else {
					hideStats();
				}
			})
		);

		tvChecks.itemsProperty().addListener(o -> {
			if (tvChecks.getSelectionModel().getSelectedIndex() == -1) {
				// Auto-select current/last model checking job if selection gets cleared.
				// This usually happens when a running job updates its progress/stats
				// (replacing an item in a list view will deselect it).
				tvChecks.getSelectionModel().selectLast();
			}
		});
	}

	private String toUIString(ModelCheckingItem item) {
		String description = item.getTaskDescription(i18n);
		if (item.getId() != null) {
			description = "[" + item.getId() + "] " + description;
		}
		return description;
	}

	private void showModelCheckException(final Throwable t) {
		if (t instanceof CancellationException) {
			LOGGER.debug("Model checking interrupted by user (this is not an error)", t);
		} else {
			LOGGER.error("Exception while running model check job", t);
			Platform.runLater(() -> stageManager.makeExceptionAlert(t, "verifications.modelchecking.modelchecker.alerts.exceptionWhileRunningJob.content").show());
		}
	}

	private void checkSingleItem(final ModelCheckingItem item) {
		if(!item.selected()) {
			return;
		}

		final CompletableFuture<ModelCheckingJobItem> future = checker.startNextCheckStep(item);
		future.whenComplete((r, t) -> {
			if (t == null) {
				if (r.getResult() instanceof ITraceDescription) {
					currentTrace.set(r.getTrace());
				}
			} else {
				showModelCheckException(t);
			}
		});
	}

	private void tvItemsClicked(MouseEvent e) {
		ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
		if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() >= 2) {
			if (item.getItems().stream().noneMatch(job -> job.getChecked() == Checked.SUCCESS)) {
				this.checkSingleItem(item);
			}
		}
	}

	private void setContextMenus() {
		tvItems.setRowFactory(table -> {
			final TableRow<ModelCheckingItem> row = new TableRow<>();
			row.setOnMouseClicked(this::tvItemsClicked);

			MenuItem checkItem = new MenuItem(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.check"));
			checkItem.setOnAction(e -> this.checkSingleItem(tvItems.getSelectionModel().getSelectedItem()));

			MenuItem openEditor = new MenuItem(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.openInEditor"));
			openEditor.setOnAction(e->showCurrentItemDialog(row.getItem()));

			row.itemProperty().addListener((o, from, to) -> {
				checkItem.textProperty().unbind();
				checkItem.disableProperty().unbind();
				if (to != null) {
					checkItem.textProperty().bind(Bindings.when(to.itemsProperty().emptyProperty())
						.then(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.check"))
						.otherwise(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.searchForNewErrors")));
					final BooleanExpression anySucceeded = Bindings.createBooleanBinding(() -> to.getItems().stream().anyMatch(item -> item.getChecked() == Checked.SUCCESS), to.itemsProperty());
					checkItem.disableProperty().bind(anySucceeded
						.or(cliExecutor.runningProperty())
						.or(to.selectedProperty().not()));
				} else {
					checkItem.setText(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.check"));
					checkItem.setDisable(true);
				}
			});

			MenuItem removeItem = new MenuItem(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.remove"));
			removeItem.setOnAction(e -> removeItem());
			removeItem.disableProperty().bind(row.emptyProperty());

			row.contextMenuProperty().bind(
				Bindings.when(row.emptyProperty())
				.then((ContextMenu)null)
				.otherwise(new ContextMenu(checkItem, removeItem, openEditor)));
			return row;
		});

		tvChecks.setRowFactory(table -> {
			final TableRow<ModelCheckingJobItem> row = new TableRow<>();

			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (!(row.isEmpty() || row.getItem() == null || row.getItem().getStats() == null || !(row.getItem().getResult() instanceof ITraceDescription)))){
					ModelCheckingJobItem item = tvChecks.getSelectionModel().getSelectedItem();
					injector.getInstance(CurrentTrace.class).set(item.getTrace());
				}
			});

			MenuItem showTraceItem = new MenuItem(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.showTrace"));
			showTraceItem.setOnAction(e-> {
				ModelCheckingJobItem item = tvChecks.getSelectionModel().getSelectedItem();
				injector.getInstance(CurrentTrace.class).set(item.getTrace());
			});
			showTraceItem.disableProperty().bind(Bindings.createBooleanBinding(
					() -> row.isEmpty() || row.getItem() == null || row.getItem().getStats() == null || !(row.getItem().getResult() instanceof ITraceDescription),
					row.emptyProperty(), row.itemProperty()));

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
					.then((ContextMenu)null)
					.otherwise(new ContextMenu(showTraceItem)));
			return row;
		});
	}

	@FXML
	public void addModelCheck() {
		ModelcheckingStage stageController = injector.getInstance(ModelcheckingStage.class);
		stageController.showAndWait();
		final ModelCheckingItem newItem = stageController.getResult();
		if (newItem == null) {
			// User cancelled/closed the window
			return;
		}
		final Optional<ModelCheckingItem> existingItem = currentProject.getCurrentMachine().getModelcheckingItems().stream().filter(newItem::settingsEqual).findAny();
		final ModelCheckingItem toCheck;
		if (existingItem.isPresent()) {
			// Identical existing configuration found - reuse it instead of creating another one
			toCheck = existingItem.get();
		} else {
			currentProject.getCurrentMachine().getModelcheckingItems().add(newItem);
			toCheck = newItem;
		}
		if (toCheck.getItems().isEmpty()) {
			// Start checking with this configuration
			// (unless it's an existing configuration that has already been run)
			this.checkSingleItem(toCheck);
		}
	}

	private void removeItem() {
		Machine machine = currentProject.getCurrentMachine();
		ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
		machine.getModelcheckingItems().remove(item);
	}

	@FXML
	public void checkMachine() {
		for (ModelCheckingItem item : currentProject.currentMachineProperty().get().getModelcheckingItems()) {
			if (!item.selected()) {
				continue;
			}

			final CompletableFuture<ModelCheckingJobItem> future = checker.startCheckIfNeeded(item);
			future.exceptionally(t -> {
				showModelCheckException(t);
				return null;
			});
		}
	}

	@FXML
	public void cancelModelcheck() {
		checker.cancelModelcheck();
	}

	private void showStats(final long timeElapsed, final StateSpaceStats stats, final BigInteger memory) {
		elapsedTime.setText(String.format("%.1f", timeElapsed / 1000.0) + " s");
		if (stats != null) {
			progressBar.setProgress(calculateProgress(stats));
			simpleStatsView.setStats(stats);
		}
		if (memory != null) {
			memoryUsage.setText(memory.divide(MIB_FACTOR) + " MiB");
		}
		statsBox.setVisible(true);
	}

	private double calculateProgress(StateSpaceStats stats) {
		double processedStates = stats.getNrProcessedNodes() - 1;
		int distinctStates = stats.getNrTotalNodes() - 1;
		return Math.pow(processedStates / distinctStates, 6);
	}

	public void hideStats() {
		statsBox.setVisible(false);
	}

	public void selectItem(ModelCheckingItem item) {
		tvItems.getSelectionModel().select(item);
	}

	public void selectJobItem(ModelCheckingJobItem item) {
		tvChecks.getSelectionModel().select(item);
	}

	private void showCurrentItemDialog(ModelCheckingItem oldItem) {
		ModelcheckingStage modelcheckingStage = injector.getInstance(ModelcheckingStage.class);
		modelcheckingStage.setData(oldItem);
		modelcheckingStage.showAndWait();
		final ModelCheckingItem changedItem = modelcheckingStage.getResult();
		Machine machine = currentProject.getCurrentMachine();
		if(machine.getModelcheckingItems().stream().noneMatch(existing -> !oldItem.settingsEqual(existing) && changedItem.settingsEqual(existing))) {
			machine.getModelcheckingItems().set(machine.getModelcheckingItems().indexOf(oldItem), changedItem);
			// This is a new configuration and so should have no history of previous checks
			assert changedItem.getItems().isEmpty();
			this.checkSingleItem(changedItem);
		}
	}

}
