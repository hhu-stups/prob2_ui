package de.prob2.ui.verifications.modelchecking;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.prob.check.StateSpaceStats;
import de.prob.statespace.Trace;
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
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.CheckingExecutors;
import de.prob2.ui.verifications.CheckingStatus;
import de.prob2.ui.verifications.CheckingStatusCell;
import de.prob2.ui.verifications.ExecutionContext;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

			MenuItem continueModelCheckingItem = new MenuItem(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.searchForNewErrors"));
			continueModelCheckingItem.setOnAction(e -> continueModelChecking((ProBModelCheckingItem)this.getItem()));
			// Add "Continue Model Checking" directly after "Start/Restart Model Checking"
			int executeIndex = contextMenu.getItems().indexOf(executeMenuItem);
			assert executeIndex >= 0;
			contextMenu.getItems().add(executeIndex + 1, continueModelCheckingItem);

			this.itemProperty().addListener((o, from, to) -> {
				executeMenuItem.textProperty().unbind();
				executeMenuItem.textProperty().bind(executeTextBinding(to));
				continueModelCheckingItem.disableProperty().unbind();
				continueModelCheckingItem.disableProperty().bind(continueModelCheckingDisableBinding(to));
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
	private TableView<ModelCheckingStep> stepsTable;

	@FXML
	private TableColumn<ModelCheckingStep, CheckingStatus> stepStatusColumn;

	@FXML
	private TableColumn<ModelCheckingStep, String> stepMessageColumn;

	@FXML
	private HBox executeButtonsBox;

	@FXML
	private Button executeButton;

	@FXML
	private Button continueCheckingButton;

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
	private final StageManager stageManager;
	private final Provider<ModelcheckingStage> modelcheckingStageProvider;
	private final I18n i18n;
	private final CheckingExecutors checkingExecutors;
	private final StatsView statsView;

	@Inject
	private ModelcheckingView(final CurrentTrace currentTrace,
			final CurrentProject currentProject,
			final DisablePropertyController disablePropertyController,
			final StageManager stageManager,
			final Provider<ModelcheckingStage> modelcheckingStageProvider,
			final I18n i18n,
			final CheckingExecutors checkingExecutors,
			final StatsView statsView
	) {
		super(stageManager, i18n, disablePropertyController, currentTrace, currentProject, checkingExecutors);
		this.currentTrace = currentTrace;
		this.stageManager = stageManager;
		this.modelcheckingStageProvider = modelcheckingStageProvider;
		this.i18n = i18n;
		this.checkingExecutors = checkingExecutors;
		this.statsView = statsView;
		stageManager.loadFXML(this, "modelchecking_view.fxml");
	}

	@Override
	protected ObservableList<ModelCheckingItem> getItemsProperty(Machine machine) {
		return machine.getModelCheckingTasks();
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

		ChangeListener<CheckingStatus> showRunningStepListener = (o, from, to) -> {
			// When an item's status changes to IN_PROGRESS,
			// i. e. a new checking step was started,
			// select the newly started step so that the checking progress is visible.
			if (to == CheckingStatus.IN_PROGRESS) {
				final ModelCheckingItem item = (ModelCheckingItem) ((ReadOnlyProperty<?>)o).getBean();
				Platform.runLater(() -> {
					itemsTable.getSelectionModel().select(item);
					stepsTable.getItems().stream()
						.filter(step -> step.getStatus() == CheckingStatus.IN_PROGRESS)
						.findFirst()
						.ifPresent(stepsTable.getSelectionModel()::select);
				});
			}
		};
		itemsTable.getSelectionModel().selectedItemProperty().addListener((o, from, to) -> {
			if (from != null) {
				from.statusProperty().removeListener(showRunningStepListener);
			}

			if (to != null) {
				to.statusProperty().addListener(showRunningStepListener);
			}
		});

		stepStatusColumn.setCellFactory(col -> new CheckingStatusCell<>());
		stepStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		stepStatusColumn.setSortable(false);
		stepMessageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
		stepMessageColumn.setSortable(false);

		stepMessageColumn.setCellFactory(col -> {
			TableCell<ModelCheckingStep, String> cell = new TableCell<>();
			cell.itemProperty().addListener((obs, old, newVal) -> {
				if (newVal != null) {
					TableRow<ModelCheckingStep> row = cell.getTableRow();
					BooleanBinding buttonBinding = Bindings.createBooleanBinding(
						() -> !row.isEmpty() && !(row.getItem() == null) && !(row.getItem().getStats() == null) && row.getItem().hasTrace(),
						row.emptyProperty(), row.itemProperty());
					Node box = buildMessageCell(newVal, buttonBinding);
					cell.graphicProperty().bind(Bindings.when(cell.emptyProperty()).then((Node) null).otherwise(box));
				}
			});
			return cell;
		});

		stepsTable.disableProperty().bind(currentTrace.isNull().or(disablePropertyController.disableProperty()));

		itemsTable.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			executeButtonsBox.setVisible(to != null);
			executeButton.textProperty().unbind();
			executeButton.textProperty().bind(executeTextBinding(to));
			continueCheckingButton.disableProperty().unbind();
			continueCheckingButton.disableProperty().bind(continueModelCheckingDisableBinding(to));

			stepsTable.itemsProperty().unbind();
			if (to != null) {
				stepsTable.itemsProperty().bind(to.stepsProperty());
				if (to.getSteps().isEmpty()) {
					hideStats();
				} else {
					stepsTable.getSelectionModel().selectLast();
				}
			} else {
				// Because of the previous binding, the stepsTable items list is the same object as the steps list of one of the ModelcheckingItems.
				// This means that we can't just clear stepsTable.getItems(), because that would also clear the ModelcheckingItem's steps, which resets the item's status.
				stepsTable.setItems(FXCollections.observableArrayList());
			}
		});

		stepsTable.getSelectionModel().selectedItemProperty().addListener((observable, from, to) ->
			Platform.runLater(() -> {
				if (to != null && to.getStats() != null) {
					showStats(to.getTimeElapsed(), to.getStats(), to.getMemoryUsed());
				} else {
					hideStats();
				}
			})
		);

		stepsTable.itemsProperty().addListener(o -> {
			if (stepsTable.getSelectionModel().getSelectedIndex() == -1) {
				// Auto-select current/last model checking job if selection gets cleared.
				// This usually happens when a running job updates its progress/stats
				// (replacing an item in a list view will deselect it).
				stepsTable.getSelectionModel().selectLast();
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
			ModelCheckingStep step = stepsTable.getSelectionModel().getSelectedItem();
			currentTrace.set(step.getTrace());
		});

		button.managedProperty().bind(buttonBinding);

		container.getChildren().add(button);
		return container;
	}

	private StringExpression executeTextBinding(ModelCheckingItem item) {
		if (item != null) {
			return Bindings.when(item.stepsProperty().emptyProperty())
				.then(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.check"))
				.otherwise(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.recheck"));
		} else {
			return i18n.translateBinding("verifications.modelchecking.modelcheckingView.contextMenu.check");
		}
	}

	private BooleanExpression continueModelCheckingDisableBinding(ModelCheckingItem item) {
		if (item instanceof ProBModelCheckingItem proBItem) {
			// Enable "Continue Model Checking" only if the item has already been executed at least once, but hasn't completely finished yet.
			// TODO Continuing should also be disabled if another ModelCheckingItem has been executed after this one stopped, because ProB tracks the checking progress globally and cannot tell apart the different ModelCheckingItems.
			return this.disableItemBinding(proBItem).or(Bindings.createBooleanBinding(
				() -> proBItem.getSteps().isEmpty() || proBItem.getSteps().stream().anyMatch(step -> step.getStatus() == CheckingStatus.SUCCESS),
				proBItem.stepsProperty()
			));
		} else {
			return new SimpleBooleanProperty(true);
		}
	}
	
	@Override
	protected CompletableFuture<?> executeItemNoninteractiveImpl(ModelCheckingItem item, CheckingExecutors executors, ExecutionContext context) {
		if (item instanceof ProBModelCheckingItem proBItem) {
			statsView.updateWhileModelChecking(proBItem);
		}
		return super.executeItemNoninteractiveImpl(item, executors, context);
	}

	@Override
	protected CompletableFuture<?> executeItemImpl(ModelCheckingItem item, CheckingExecutors executors, ExecutionContext context) {
		if (item instanceof ProBModelCheckingItem proBItem) {
			statsView.updateWhileModelChecking(proBItem);
			return super.executeItemImpl(item, executors, context).thenApply(res -> {
				if (!item.getSteps().isEmpty()) {
					ModelCheckingStep lastStep = item.getSteps().get(item.getSteps().size() - 1);
					Trace trace = lastStep.getTrace();
					if (trace != null) {
						currentTrace.set(trace);
					}
				}
				return res;
			});
		} else {
			return super.executeItemImpl(item, executors, context);
		}
	}

	@FXML
	private void executeSelected() {
		ModelCheckingItem item = itemsTable.getSelectionModel().getSelectedItem();
		if (item != null) {
			this.executeItem(item);
		}
	}

	private void continueModelChecking(ProBModelCheckingItem item) {
		statsView.updateWhileModelChecking(item);
		ExecutionContext context = getCurrentExecutionContext();
		item.continueModelChecking(checkingExecutors, context).whenComplete((res, exc) -> {
			if (exc == null) {
				ModelCheckingStep lastStep = item.getSteps().get(item.getSteps().size() - 1);
				Trace trace = lastStep.getTrace();
				if (trace != null) {
					currentTrace.set(trace);
				}
			} else {
				handleCheckException(exc);
			}
		});
	}

	@FXML
	private void continueCheckingSelected() {
		ModelCheckingItem item = itemsTable.getSelectionModel().getSelectedItem();
		if (item instanceof ProBModelCheckingItem proBItem) {
			this.continueModelChecking(proBItem);
		}
	}

	private void setContextMenus() {
		itemsTable.setRowFactory(table -> new Row());

		stepsTable.setRowFactory(table -> {
			final TableRow<ModelCheckingStep> row = new TableRow<>();

			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && !(row.isEmpty() || row.getItem() == null || row.getItem().getStats() == null || !row.getItem().hasTrace())) {
					ModelCheckingStep step = stepsTable.getSelectionModel().getSelectedItem();
					currentTrace.set(step.getTrace());
				}
			});

			MenuItem showTraceItem = new MenuItem(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.showTrace"));
			showTraceItem.setOnAction(e-> {
				ModelCheckingStep step = stepsTable.getSelectionModel().getSelectedItem();
				currentTrace.set(step.getTrace());
			});
			showTraceItem.disableProperty().bind(Bindings.createBooleanBinding(
					() -> row.isEmpty() || row.getItem() == null || row.getItem().getStats() == null || !row.getItem().hasTrace(),
					row.emptyProperty(), row.itemProperty()));

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
					.then((ContextMenu)null)
					.otherwise(new ContextMenu(showTraceItem)));
			return row;
		});
	}

	@Override
	protected Optional<ModelCheckingItem> showItemDialog(final ModelCheckingItem oldItem) {
		ModelcheckingStage modelcheckingStage = modelcheckingStageProvider.get();
		modelcheckingStage.setData(oldItem);
		modelcheckingStage.showAndWait();
		return Optional.ofNullable(modelcheckingStage.getResult());
	}

	private void showStats(final long timeElapsed, final StateSpaceStats stats, final BigInteger memory) {
		elapsedTime.setText(String.format("%.1f", timeElapsed / 1000.0) + " s");
		if (stats != null) {
			progressBar.setProgress(calculateProgress(stats));
			simpleStatsView.setStats(stats);
		}
		memoryUsage.setText(memory != null ? memory.divide(MIB_FACTOR) + " MiB" : "-");
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
}
