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
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.CheckingViewBase;
import de.prob2.ui.sharedviews.SimpleStatsView;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public final class ModelcheckingView extends CheckingViewBase<ModelCheckingItem> {
	private final class Row extends RowBase {
		private Row() {
			executeMenuItem.setText(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.check"));

			this.itemProperty().addListener((o, from, to) -> {
				executeMenuItem.textProperty().unbind();
				if (to != null) {
					executeMenuItem.textProperty().bind(Bindings.when(to.itemsProperty().emptyProperty())
						.then(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.check"))
						.otherwise(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.searchForNewErrors")));
				} else {
					executeMenuItem.setText(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.check"));
				}
			});
		}
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ModelcheckingView.class);
	private static final BigInteger MIB_FACTOR = new BigInteger("1024").pow(2);

	@FXML
	private Button addModelCheckButton;

	@FXML
	private HelpButton helpButton;

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

	@Inject
	private ModelcheckingView(final CurrentTrace currentTrace,
			final CurrentProject currentProject,
			final DisablePropertyController disablePropertyController,
			final StageManager stageManager, final Injector injector,
			final I18n i18n, final Modelchecker checker) {
		super(i18n, disablePropertyController);
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.injector = injector;
		this.i18n = i18n;
		this.checker = checker;
		stageManager.loadFXML(this, "modelchecking_view.fxml");
	}

	@Override
	@FXML
	public void initialize() {
		super.initialize();
		helpButton.setHelpContent("verification", "Model");
		setBindings();
		setContextMenus();
	}

	private void setBindings() {
		addModelCheckButton.disableProperty().bind(currentTrace.isNull().or(disablePropertyController.disableProperty()));
		final ChangeListener<Machine> machineChangeListener = (o, from, to) -> {
			items.unbind();
			if (to != null) {
				items.bind(to.modelcheckingItemsProperty());
			} else {
				items.set(FXCollections.observableArrayList());
			}
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());

		jobStatusColumn.setCellFactory(col -> new CheckedCell<>());
		jobStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		jobStatusColumn.setSortable(false);
		messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
		messageColumn.setSortable(false);

		messageColumn.setCellFactory(col -> {
			TableCell<ModelCheckingJobItem, String> cell = new TableCell<>();
			cell.itemProperty().addListener((obs, old, newVal) -> {
				if (newVal != null) {
					TableRow<ModelCheckingJobItem> row = cell.getTableRow();
					BooleanBinding buttonBinding = Bindings.createBooleanBinding(
						() -> !row.isEmpty() && !(row.getItem() == null) && !(row.getItem().getStats() == null) && row.getItem().getResult() instanceof ITraceDescription,
						row.emptyProperty(), row.itemProperty());
					Node box = buildMessageCell(newVal, buttonBinding);
					cell.graphicProperty().bind(Bindings.when(cell.emptyProperty()).then((Node) null).otherwise(box));
				}
			});
			return cell;
		});

		tvChecks.disableProperty().bind(currentTrace.isNull().or(disablePropertyController.disableProperty()));

		itemsTable.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
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

	private Node buildMessageCell(String text, BooleanBinding buttonBinding){
		HBox container = new HBox();
		container.setAlignment(Pos.CENTER_LEFT);
		container.getChildren().add(new Label(text));

		container.setSpacing(5);
		Button button = new Button(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.showTrace"));
		button.getStyleClass().add("button-blue");
		button.setOnAction(actionEvent -> {
			ModelCheckingJobItem item = tvChecks.getSelectionModel().getSelectedItem();
			injector.getInstance(CurrentTrace.class).set(item.getTrace());
		});

		button.managedProperty().bind(buttonBinding);

		container.getChildren().add(button);
		return container;
	}

	@Override
	protected String configurationForItem(final ModelCheckingItem item) {
		return item.getTaskDescription(i18n);
	}

	private void showModelCheckException(final Throwable t) {
		if (t instanceof CancellationException) {
			LOGGER.debug("Model checking interrupted by user (this is not an error)", t);
		} else {
			LOGGER.error("Exception while running model check job", t);
			Platform.runLater(() -> stageManager.makeExceptionAlert(t, "verifications.modelchecking.modelchecker.alerts.exceptionWhileRunningJob.content").show());
		}
	}
	
	@Override
	protected BooleanExpression disableItemBinding(final ModelCheckingItem item) {
		return super.disableItemBinding(item).or(Bindings.createBooleanBinding(
			() -> item.getItems().stream().anyMatch(jobItem -> jobItem.getChecked() == Checked.SUCCESS),
			item.itemsProperty()
		));
	}
	
	@Override
	protected void executeItem(final ModelCheckingItem item) {
		if (item.getItems().stream().anyMatch(job -> job.getChecked() == Checked.SUCCESS)) {
			return;
		}

		this.checkSingleItem(item);
	}

	private void checkSingleItem(final ModelCheckingItem item) {
		if(!item.selected()) {
			return;
		}

		try {
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
		catch (IllegalArgumentException e){
			stageManager.makeExceptionAlert(e, "verifications.modelchecking.modelchecker.alerts.exceptionWhileRunningJob.content").show();
		}

	}

	private void setContextMenus() {
		itemsTable.setRowFactory(table -> new Row());

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

	@Override
	protected Optional<ModelCheckingItem> editItem(final ModelCheckingItem oldItem) {
		ModelcheckingStage modelcheckingStage = injector.getInstance(ModelcheckingStage.class);
		modelcheckingStage.setData(oldItem);
		modelcheckingStage.showAndWait();
		return Optional.ofNullable(modelcheckingStage.getResult());
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
		itemsTable.getSelectionModel().select(item);
	}

	public void selectJobItem(ModelCheckingJobItem item) {
		tvChecks.getSelectionModel().select(item);
	}
}
