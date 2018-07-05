package de.prob2.ui.verifications.modelchecking;


import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;


import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.stats.StatsView;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.MachineStatusHandler;
import de.prob2.ui.verifications.ShouldExecuteValueFactory;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
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


@Singleton
public final class ModelcheckingView extends ScrollPane {


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
	
	private final StageManager stageManager;
	private final Injector injector;
	private final ResourceBundle bundle;
	private final Modelchecker checker;

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
		stageManager.loadFXML(this, "modelchecking_view.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent(this.getClass());
		showStats(new ModelCheckStats(stageManager, injector));
		setBindings();
		setListeners();
		setContextMenus();
	}
	
	private void setBindings() {
		addModelCheckButton.disableProperty().bind(currentTrace.existsProperty().not().or(checker.currentJobThreadsProperty().emptyProperty().not()));
		checkMachineButton.disableProperty().bind(currentTrace.existsProperty().not().or(checker.currentJobThreadsProperty().emptyProperty().not()));
		cancelButton.disableProperty().bind(checker.currentJobThreadsProperty().emptyProperty());
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
		
		tvItems.disableProperty().bind(currentTrace.existsProperty().not().or(checker.currentJobThreadsProperty().emptyProperty().not()));
		tvItems.getFocusModel().focusedItemProperty().addListener((observable, from, to) -> {
			ModelCheckingItem item = tvItems.getFocusModel().getFocusedItem();
			Platform.runLater(() -> {
				if (item != null) {
					if (item.getStats() == null) {
						resetView();
					} else {
						checker.setCurrentStats(item.getStats());
						statsPane.getChildren().setAll(item.getStats());
					}
				}	
			});
		});
	}
	
	private void setListeners() {
		currentProject.currentMachineProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue != null) {
				bindMachine(newValue);
			} else {
				tvItems.getItems().clear();
				tvItems.itemsProperty().unbind();
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

	private void tvItemsClicked(MouseEvent e) {
		ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
		if (item != null && e.getButton() == MouseButton.PRIMARY && e.getClickCount() >= 2) {
			checker.checkItem(item, false);
		}
	}
	
	public void bindMachine(Machine machine) {
		tvItems.itemsProperty().unbind();
		tvItems.itemsProperty().bind(machine.modelcheckingItemsProperty());
		resetView();
		tvItems.refresh();
	}
	
	private void setContextMenus() {
		tvItems.setRowFactory(table -> {
			final TableRow<ModelCheckingItem> row = new TableRow<>();
			row.setOnMouseClicked(this::tvItemsClicked);
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
			
			MenuItem checkItem = new MenuItem(bundle.getString("verifications.modelchecking.menu.check"));
			checkItem.setDisable(true);
			checkItem.setOnAction(e-> {
				ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
				item.setOptions(item.getOptions().recheckExisting(true));
				checker.checkItem(item, false);
			});
			
			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					checkItem.disableProperty().bind(row.emptyProperty()
							.or(checker.currentJobThreadsProperty().emptyProperty().not())
							.or(to.shouldExecuteProperty().not()));
				}
			});
			
			MenuItem showDetailsItem = new MenuItem(bundle.getString("verifications.modelchecking.menu.showDetails"));
			showDetailsItem.setOnAction(e-> {
				ModelcheckingItemDetailsStage fullValueStage = injector.getInstance(ModelcheckingItemDetailsStage.class);
				ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
				fullValueStage.setValues(item.getStrategy(), item.getDescription());
				fullValueStage.show();
			});
			showDetailsItem.disableProperty().bind(row.emptyProperty());
			
			MenuItem searchForNewErrorsItem = new MenuItem(bundle.getString("verifications.modelchecking.stage.options.searchForNewErrors"));
			searchForNewErrorsItem.setOnAction(e-> {
				ModelCheckingItem item = tvItems.getSelectionModel().getSelectedItem();
				item.setOptions(item.getOptions().recheckExisting(false));
				checker.checkItem(item, false);
			});
			searchForNewErrorsItem.disableProperty().bind(disableErrorItemsBinding);
			
			MenuItem removeItem = new MenuItem(bundle.getString("verifications.modelchecking.menu.remove"));
			removeItem.setOnAction(e -> removeItem());
			removeItem.disableProperty().bind(row.emptyProperty());
			
			row.contextMenuProperty().bind(
				Bindings.when(row.emptyProperty())
				.then((ContextMenu)null)
				.otherwise(new ContextMenu(showTraceToErrorItem, checkItem, showDetailsItem, searchForNewErrorsItem, removeItem))
			);
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
	}
	
	@FXML
	public void checkMachine() {
		currentProject.currentMachineProperty().get().getModelcheckingItems().forEach(item -> {
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
	}
	
	public void selectLast() {
		tvItems.getSelectionModel().selectLast();
	}
	
}
