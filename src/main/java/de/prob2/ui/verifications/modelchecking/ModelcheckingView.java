package de.prob2.ui.verifications.modelchecking;

import java.math.BigInteger;
import java.util.ResourceBundle;

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

@FXMLInjected
@Singleton
public final class ModelcheckingView extends ScrollPane {
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
	private TableColumn<ModelCheckingJobItem, Integer> indexColumn;

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

	private final Injector injector;
	private final I18n i18n;
	private final Modelchecker checker;
	private final CheckBox selectAll;

	@Inject
	private ModelcheckingView(final CurrentTrace currentTrace,
			final CurrentProject currentProject, final StageManager stageManager, final Injector injector,
			final I18n i18n, final Modelchecker checker) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
		this.i18n = i18n;
		this.checker = checker;
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
		cancelButton.disableProperty().bind(checker.runningProperty().not());

		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory(tvItems, selectAll));
		shouldExecuteColumn.setGraphic(selectAll);
		statusColumn.setCellFactory(col -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		descriptionColumn.setCellValueFactory(features -> Bindings.createStringBinding(() -> toUIString(features.getValue())));

		jobStatusColumn.setCellFactory(col -> new CheckedCell<>());
		jobStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		indexColumn.setCellValueFactory(new PropertyValueFactory<>("index"));
		messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));

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
	}

	private String toUIString(ModelCheckingItem item) {
		String description = item.getTaskDescription(i18n);
		if (item.getId() != null) {
			description = "[" + item.getId() + "] " + description;
		}
		return description;
	}

	private void tvItemsClicked(MouseEvent e) {
		ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
		if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() >= 2) {
			if(item.getItems().isEmpty()) {
				checker.checkItem(item, true, false);
			} else if (item.getItems()
					.stream().noneMatch(job -> job.getChecked() == Checked.SUCCESS)) {
				checker.checkItem(item, false, false);
			}
		}
	}

	private void setContextMenus() {
		tvItems.setRowFactory(table -> {
			final TableRow<ModelCheckingItem> row = new TableRow<>();
			row.setOnMouseClicked(this::tvItemsClicked);

			MenuItem checkItem = new MenuItem(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.check"));
			checkItem.setOnAction(e-> {
				ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
				checker.checkItem(item, true, false);
			});

			row.itemProperty().addListener((o, from, to) -> {
				checkItem.disableProperty().unbind();
				if (to != null) {
					checkItem.disableProperty().bind(to.itemsProperty().emptyProperty().not()
						.or(checker.runningProperty())
						.or(to.selectedProperty().not()));
				} else {
					checkItem.setDisable(true);
				}
			});

			MenuItem searchForNewErrorsItem = new MenuItem(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.searchForNewErrors"));
			searchForNewErrorsItem.setOnAction(e-> {
				ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
				checker.checkItem(item, false, false);
			});

			row.itemProperty().addListener((o, from, to) -> {
				searchForNewErrorsItem.disableProperty().unbind();
				if (to != null) {
					final BooleanExpression anySucceeded = Bindings.createBooleanBinding(() -> to.getItems().isEmpty() || to.getItems().stream().anyMatch(item -> item.getChecked() == Checked.SUCCESS), to.itemsProperty());
					searchForNewErrorsItem.disableProperty().bind(anySucceeded
						.or(checker.runningProperty())
						.or(to.selectedProperty().not()));
				} else {
					searchForNewErrorsItem.setDisable(true);
				}
			});

			MenuItem removeItem = new MenuItem(i18n.translate("verifications.modelchecking.modelcheckingView.contextMenu.remove"));
			removeItem.setOnAction(e -> removeItem());
			removeItem.disableProperty().bind(row.emptyProperty());

			row.contextMenuProperty().bind(
				Bindings.when(row.emptyProperty())
				.then((ContextMenu)null)
				.otherwise(new ContextMenu(checkItem, searchForNewErrorsItem, removeItem)));
			return row;
		});

		tvChecks.setRowFactory(table -> {
			final TableRow<ModelCheckingJobItem> row = new TableRow<>();

			MenuItem showTraceItem = new MenuItem(injector.getInstance(ResourceBundle.class).getString("verifications.modelchecking.modelcheckingView.contextMenu.showTrace"));
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
		if (!stageController.isShowing()) {
			stageController.showAndWait();
		}
	}

	private void removeItem() {
		Machine machine = currentProject.getCurrentMachine();
		ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
		machine.getModelcheckingItems().remove(item);
	}

	@FXML
	public void checkMachine() {
		currentProject.currentMachineProperty().get().getModelcheckingItems().stream()
			.filter(item -> item.getItems().isEmpty())
			.forEach(item -> checker.checkItem(item, true, true));
	}

	@FXML
	public void cancelModelcheck() {
		checker.cancelModelcheck();
	}

	public void showStats(final long timeElapsed, final StateSpaceStats stats, final BigInteger memory) {
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
}
