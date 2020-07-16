package de.prob2.ui.verifications.modelchecking;

import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.check.ModelCheckingOptions;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.BooleanCell;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ISelectableCheckingView;
import de.prob2.ui.verifications.ItemSelectedFactory;
import de.prob2.ui.verifications.MachineStatusHandler;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;

@FXMLInjected
@Singleton
public final class ModelcheckingView extends ScrollPane implements ISelectableCheckingView {
	
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
	private TableColumn<ModelCheckingItem, Checked> statusColumn;
	
	@FXML
	private TableColumn<ModelCheckingItem, String> strategyColumn;
	
	@FXML
	private TableColumn<ModelCheckingItem, Boolean> deadlockColumn;
	
	@FXML
	private TableColumn<ModelCheckingItem, Boolean> invariantsViolationsColumn;

	@FXML
	private TableColumn<ModelCheckingItem, Boolean> assertionViolationsColumn;

	@FXML
	private TableColumn<ModelCheckingItem, Boolean> otherErrorsColumn;
	
	@FXML
	private TableColumn<ModelCheckingItem, Boolean> goalsColumn;
	
	@FXML
	private TableColumn<ModelCheckingItem, Boolean> stopAtFullCoverageColumn;
	
	@FXML
	private TableColumn<IExecutableItem, CheckBox> shouldExecuteColumn;

	@FXML
	private TableView<ModelCheckingJobItem> tvChecks;

	@FXML
	private TableColumn<ModelCheckingJobItem, Checked> jobStatusColumn;

	@FXML
	private TableColumn<ModelCheckingJobItem, Integer> indexColumn;

	@FXML
	private TableColumn<ModelCheckingJobItem, String> messageColumn;

	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	
	private final StageManager stageManager;
	private final Injector injector;
	private final ResourceBundle bundle;
	private final Modelchecker checker;
	private final CheckBox selectAll;

	@Inject
	private ModelcheckingView(final CurrentTrace currentTrace,
			final CurrentProject currentProject, final StageManager stageManager, final Injector injector, 
			final ResourceBundle bundle, final Modelchecker checker) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.injector = injector;
		this.bundle = bundle;
		this.checker = checker;
		this.selectAll = new CheckBox();
		stageManager.loadFXML(this, "modelchecking_view.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent("verification", "Model");
		resetView();
		setBindings();
		setListeners();
		setContextMenus();
	}
	
	private Callback<TableColumn.CellDataFeatures<ModelCheckingItem, Boolean>, ObservableValue<Boolean>> makeOptionValueFactory(final ModelCheckingOptions.Options option, boolean negated) {
		return features -> {
			BooleanBinding binding = Bindings.createBooleanBinding(
					() -> features.getValue().getOptions().getPrologOptions().contains(option),
					features.getValue().optionsProperty());
			return negated ? binding.not() : binding;
		};
	}
	
	private void setBindings() {
		injector.getInstance(DisablePropertyController.class).addDisableProperty(addModelCheckButton.disableProperty(), currentTrace.existsProperty().not());
		injector.getInstance(DisablePropertyController.class).addDisableProperty(checkMachineButton.disableProperty(), currentTrace.existsProperty().not());
		cancelButton.disableProperty().bind(checker.currentJobThreadsProperty().emptyProperty());
		statusColumn.setCellFactory(col -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		strategyColumn.setCellValueFactory(features -> Bindings.createStringBinding(() ->
			bundle.getString(SearchStrategy.fromOptions(features.getValue().getOptions()).getName())
		));
		deadlockColumn.setCellFactory(col -> new BooleanCell<>());
		deadlockColumn.setCellValueFactory(makeOptionValueFactory(ModelCheckingOptions.Options.FIND_DEADLOCKS, false));
		invariantsViolationsColumn.setCellFactory(col -> new BooleanCell<>());
		invariantsViolationsColumn.setCellValueFactory(makeOptionValueFactory(ModelCheckingOptions.Options.FIND_INVARIANT_VIOLATIONS, false));
		assertionViolationsColumn.setCellFactory(col -> new BooleanCell<>());
		assertionViolationsColumn.setCellValueFactory(makeOptionValueFactory(ModelCheckingOptions.Options.FIND_ASSERTION_VIOLATIONS, false));
		otherErrorsColumn.setCellFactory(col -> new BooleanCell<>());
		otherErrorsColumn.setCellValueFactory(makeOptionValueFactory(ModelCheckingOptions.Options.FIND_OTHER_ERRORS, true));
		goalsColumn.setCellFactory(col -> new BooleanCell<>());
		goalsColumn.setCellValueFactory(makeOptionValueFactory(ModelCheckingOptions.Options.FIND_GOAL, false));
		stopAtFullCoverageColumn.setCellFactory(col -> new BooleanCell<>());
		stopAtFullCoverageColumn.setCellValueFactory(makeOptionValueFactory(ModelCheckingOptions.Options.STOP_AT_FULL_COVERAGE, false));
		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory(CheckingType.MODELCHECKING, injector, this));
		
		jobStatusColumn.setCellFactory(col -> new CheckedCell<>());
		jobStatusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		indexColumn.setCellValueFactory(new PropertyValueFactory<>("index"));
		messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));

		selectAll.setSelected(true);
		selectAll.selectedProperty().addListener((observable, from, to) -> {
			if(!to) {
				checkMachineButton.disableProperty().unbind();
				checkMachineButton.setDisable(true);
			} else {
				injector.getInstance(DisablePropertyController.class).addDisableProperty(checkMachineButton.disableProperty(), currentProject.getCurrentMachine().modelcheckingItemsProperty().emptyProperty());
			}
		});
		selectAll.setOnAction(e -> {
			for(IExecutableItem item : tvItems.getItems()) {
				item.setSelected(selectAll.isSelected());
				Machine machine = injector.getInstance(CurrentProject.class).getCurrentMachine();
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.MODELCHECKING);
				tvItems.refresh();
			}
		});

		shouldExecuteColumn.setGraphic(selectAll);

		injector.getInstance(DisablePropertyController.class).addDisableProperty(tvItems.disableProperty(), currentTrace.existsProperty().not());
		injector.getInstance(DisablePropertyController.class).addDisableProperty(tvChecks.disableProperty(), currentTrace.existsProperty().not());


		tvItems.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to != null && (from == null || !from.getOptions().recheckExisting(false).equals(to.getOptions().recheckExisting(false)))) {
				tvChecks.itemsProperty().unbind();
				tvChecks.itemsProperty().bind(to.itemsProperty());
				tvChecks.getSelectionModel().selectFirst();
			}
		});
		
		tvChecks.getSelectionModel().selectedItemProperty().addListener((observable, from, to) ->
			Platform.runLater(() -> {
				if(to != null && to.getStats() != null) {
					showStats(to.getStats());
				} else {
					resetView();
				}
			})
		);
	}
	
	private void setListeners() {
		currentProject.currentMachineProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue != null) {
				bindMachine(newValue);
			} else {
				tvItems.getItems().clear();
				tvItems.itemsProperty().unbind();
				tvChecks.getItems().clear();
				tvChecks.itemsProperty().unbind();
			}
		});
		
		currentTrace.existsProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue) {
				checkMachineButton.disableProperty().bind(currentProject.getCurrentMachine().modelcheckingItemsProperty().emptyProperty().or(checker.currentJobThreadsProperty().emptyProperty().not()));
			} else {
				checkMachineButton.disableProperty().bind(currentTrace.existsProperty().not().or(checker.currentJobThreadsProperty().emptyProperty().not()));
			}
		});
		
		currentProject.addListener((observable, from, to) -> {
			if(to != from) {
				this.resetView();
			}
		});
	}
	
	public void bindMachine(Machine machine) {
		machine.getModelcheckingItems().forEach(item -> item.getItems().clear());
		tvChecks.getItems().clear();
		tvChecks.itemsProperty().unbind();
		tvChecks.refresh();
		tvItems.itemsProperty().unbind();
		tvItems.itemsProperty().bind(machine.modelcheckingItemsProperty());
		resetView();
		tvItems.refresh();
	}
	
	private void tvItemsClicked(MouseEvent e) {
		ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
		if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() >= 2) {
			if(item.getItems().isEmpty()) {
				item.setOptions(item.getOptions().recheckExisting(true));
			} else if (item.getItems()
					.stream()
					.filter(job -> job.getChecked() == Checked.SUCCESS)
					.collect(Collectors.toList()).isEmpty()) {
				item.setOptions(item.getOptions().recheckExisting(false));
			} else {
				return;
			}
			checker.checkItem(item, false);
		}
	}
	
	private void setContextMenus() {
		tvItems.setRowFactory(table -> {
			final TableRow<ModelCheckingItem> row = new TableRow<>();
			row.setOnMouseClicked(this::tvItemsClicked);
			
			MenuItem checkItem = new MenuItem(bundle.getString("verifications.modelchecking.modelcheckingView.contextMenu.check"));
			checkItem.setOnAction(e-> {
				ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
				item.setOptions(item.getOptions().recheckExisting(true));
				checker.checkItem(item, false);
			});
			
			BooleanBinding disableCheckProperty = Bindings.createBooleanBinding(
					() -> row.isEmpty() || row.getItem() == null || !row.getItem().getItems().isEmpty(), row.emptyProperty(), row.itemProperty());
			checkItem.disableProperty().bind(disableCheckProperty);
			
			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					checkItem.disableProperty().bind(disableCheckProperty
							.or(checker.currentJobThreadsProperty().emptyProperty().not())
							.or(to.selectedProperty().not()));
				}
			});
			
			MenuItem searchForNewErrorsItem = new MenuItem(bundle.getString("verifications.modelchecking.modelcheckingView.contextMenu.searchForNewErrors"));
			searchForNewErrorsItem.setOnAction(e-> {
				ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
				item.setOptions(item.getOptions().recheckExisting(false));
				checker.checkItem(item, false);
			});
			BooleanBinding disableSearchForNewErrorsProperty = Bindings.createBooleanBinding(
					() -> row.isEmpty() || row.getItem() == null || row.getItem().getItems().isEmpty() || 
					!row.getItem().getItems().stream().filter(item -> item.getChecked() == Checked.SUCCESS).collect(Collectors.toList()).isEmpty(), 
					row.emptyProperty(), row.itemProperty());
			searchForNewErrorsItem.disableProperty().bind(disableSearchForNewErrorsProperty);
			
			MenuItem removeItem = new MenuItem(bundle.getString("verifications.modelchecking.modelcheckingView.contextMenu.remove"));
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

			MenuItem showTraceToErrorItem = new MenuItem(injector.getInstance(ResourceBundle.class).getString("verifications.modelchecking.modelcheckingView.contextMenu.showTraceToError"));
			showTraceToErrorItem.setOnAction(e-> {
				ModelCheckingJobItem item = tvChecks.getSelectionModel().getSelectedItem();
				injector.getInstance(CurrentTrace.class).set(item.getTrace());
			});
			showTraceToErrorItem.disableProperty().bind(Bindings.createBooleanBinding(
					() -> row.isEmpty() || row.getItem() == null || row.getItem().getStats() == null || row.getItem().getTraceDescription() == null,
					row.emptyProperty(), row.itemProperty()));
			
			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
					.then((ContextMenu)null)
					.otherwise(new ContextMenu(showTraceToErrorItem)));
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
		machine.removeModelcheckingItem(item);
		if(tvItems.getItems().isEmpty()) {
			tvChecks.getItems().clear();
		} else {
			tvItems.getSelectionModel().selectFirst();
		}
		injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.MODELCHECKING);
	}
	
	@FXML
	public void checkMachine() {
		currentProject.currentMachineProperty().get().getModelcheckingItems().stream()
			.filter(item -> item.getItems().isEmpty())
			.forEach(item -> {
				item.setOptions(item.getOptions().recheckExisting(true));
				checker.checkItem(item, true);
			});
	}
	
	@FXML
	public void cancelModelcheck() {
		checker.cancelModelcheck();
	}

	public void showStats(ModelCheckStats stats) {
		statsPane.getChildren().setAll(stats);
		AnchorPane.setTopAnchor(stats, 0.0);
		AnchorPane.setRightAnchor(stats, 0.0);
		AnchorPane.setBottomAnchor(stats, 0.0);
		AnchorPane.setLeftAnchor(stats, 0.0);
	}

	public void resetView() {
		showStats(new ModelCheckStats(stageManager, injector));
	}
	
	public void refresh() {
		tvItems.refresh();
		tvChecks.refresh();
	}
	
	public void selectItem(ModelCheckingItem item) {
		tvItems.getSelectionModel().select(item);
	}
	
	public void selectJobItem(ModelCheckingJobItem item) {
		tvChecks.getSelectionModel().select(item);
	}

	public void updateSelectViews() {
		boolean anySelected = false;
		for(ModelCheckingItem item : currentProject.getCurrentMachine().getModelcheckingItems()) {
			if(item.selected()) {
				anySelected = true;
			}
		}
		selectAll.setSelected(anySelected);
	}
}
