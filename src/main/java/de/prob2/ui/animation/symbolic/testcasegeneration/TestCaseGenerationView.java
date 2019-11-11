package de.prob2.ui.animation.symbolic.testcasegeneration;


import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.statespace.FormalismType;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.symbolic.testcasegeneration.TraceInformationItem;
import de.prob2.ui.animation.symbolic.testcasegeneration.TraceInformationStage;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ISelectableCheckingView;
import de.prob2.ui.verifications.ItemSelectedFactory;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import javax.inject.Inject;
import java.util.List;
import java.util.ResourceBundle;

 
@FXMLInjected
@Singleton
public class TestCaseGenerationView extends ScrollPane implements ISelectableCheckingView {
	
	@FXML
	private HelpButton helpButton;
		
	@FXML
	private TableView<TestCaseGenerationItem> tvFormula;
	
	@FXML
	private TableColumn<TestCaseGenerationItem, Checked> statusColumn;
	
	@FXML
	private TableColumn<TestCaseGenerationItem, String> nameColumn;
	
	@FXML
	private TableColumn<TestCaseGenerationItem, String> descriptionColumn;
	
	@FXML
	private TableColumn<IExecutableItem, CheckBox> shouldExecuteColumn;
	
	@FXML
	private Button addFormulaButton;
	
	@FXML
	private Button checkMachineButton;
	
	@FXML
	private Button cancelButton;
	
	private final ResourceBundle bundle;
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;

	private final Injector injector;
	
	private final TestCaseGenerator testCaseGenerator;
	
	private final TestCaseGenerationItemHandler itemHandler;

	private final CheckBox selectAll;
	
	private class TestCaseGenerationCellFactory implements Callback<TableView<TestCaseGenerationItem>, TableRow<TestCaseGenerationItem>>{
		@Override
		public TableRow<TestCaseGenerationItem> call(TableView<TestCaseGenerationItem> param) {
			TableRow<TestCaseGenerationItem> row = new TableRow<>();
			
			MenuItem checkItem = new MenuItem(bundle.getString("symbolic.view.contextMenu.check"));
			checkItem.setDisable(true);
			checkItem.setOnAction(e-> itemHandler.handleItem(row.getItem(), false));

			MenuItem removeItem = new MenuItem(bundle.getString("symbolic.view.contextMenu.removeConfiguration"));
			removeItem.setOnAction(e -> removeFormula());
			
			MenuItem changeItem = new MenuItem(bundle.getString("symbolic.view.contextMenu.changeConfiguration"));
			changeItem.setOnAction(e->openItem(row.getItem()));
			
			MenuItem showDetails = new MenuItem(bundle.getString("symbolic.view.contextMenu.showDetails"));
			showDetails.setDisable(true);
			showDetails.setOnAction(e -> {
				TestCaseGenerationItem item = row.getItem();
				TraceInformationStage stage = injector.getInstance(TraceInformationStage.class);
				@SuppressWarnings("unchecked")
				ObservableList<TraceInformationItem> traces = FXCollections.observableArrayList((List<TraceInformationItem>) item.getAdditionalInformation("traceInformation"));
				stage.setTraces(traces);
				
				@SuppressWarnings("unchecked")
				ObservableList<TraceInformationItem> uncoveredOperations = FXCollections.observableArrayList((List<TraceInformationItem>) item.getAdditionalInformation("uncoveredOperations"));
				stage.setUncoveredOperations(uncoveredOperations);
				
				stage.show();
			});

			Menu showStateItem = new Menu(bundle.getString("animation.symbolic.view.contextMenu.showFoundPaths"));
			showStateItem.setDisable(true);
			
			MenuItem showMessage = new MenuItem(bundle.getString("symbolic.view.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> injector.getInstance(TestCaseGenerationResultHandler.class).showResult(row.getItem()));

			MenuItem saveTraces = new MenuItem(bundle.getString("animation.symbolic.view.contextMenu.savePaths"));
			saveTraces.setOnAction(e -> {
				TestCaseGenerationItem item = row.getItem();
				injector.getInstance(TestCaseGenerationResultHandler.class).saveTraces(item);
			});

			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					showMessage.disableProperty().bind(to.resultItemProperty().isNull()
							.or(Bindings.createBooleanBinding(() -> to.getResultItem() != null && Checked.SUCCESS == to.getResultItem().getChecked(), to.resultItemProperty())));
					showStateItem.disableProperty().bind(to.examplesProperty().emptyProperty());
					showExamples(to, showStateItem);
					checkItem.disableProperty().bind(testCaseGenerator.currentJobThreadsProperty().emptyProperty().not()
							.or(to.selectedProperty().not()));
				}
			});

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
					.then((ContextMenu) null)
					.otherwise(new ContextMenu(checkItem, changeItem, removeItem, showDetails, showMessage, showStateItem, saveTraces)));		

			return row;
		}

		private void showExamples(TestCaseGenerationItem item, Menu exampleItem) {
			exampleItem.getItems().clear();
			List<Trace> examples = item.getExamples();
			for(int i = 0; i < examples.size(); i++) {
				MenuItem traceItem = new MenuItem(String.format(bundle.getString("animation.symbolic.view.contextMenu.showExample"), i + 1));
				final int index = i;
				traceItem.setOnAction(e-> currentTrace.set((examples.get(index))));
				exampleItem.getItems().add(traceItem);
			}
		}
	}
	
	@Inject
	public TestCaseGenerationView(final StageManager stageManager, final ResourceBundle bundle, final CurrentTrace currentTrace, 
					final CurrentProject currentProject, final TestCaseGenerationItemHandler itemHandler, 
					final TestCaseGenerator testCaseGenerator, final Injector injector) {
		this.bundle = bundle;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
		this.itemHandler = itemHandler;
		this.testCaseGenerator = testCaseGenerator;
		this.selectAll = new CheckBox();
		stageManager.loadFXML(this, "test_case_generation_view.fxml");
	}
	
	@FXML
	public void initialize() {
		helpButton.setHelpContent(this.getClass());
		setBindings();
		setContextMenu();
		currentProject.currentMachineProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue != null) {
				bindMachine(newValue);
			} else {
				tvFormula.getItems().clear();
				tvFormula.itemsProperty().unbind();
			}
		});
		currentTrace.existsProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue) {
				injector.getInstance(DisablePropertyController.class).addDisableProperty(checkMachineButton.disableProperty(), testCasesProperty(currentProject.getCurrentMachine()).emptyProperty().or(testCaseGenerator.currentJobThreadsProperty().emptyProperty().not()));
			} else {
				checkMachineButton.disableProperty().unbind();
				checkMachineButton.setDisable(true);
			}
		});
	}
	
	public void bindMachine(Machine machine) {
		tvFormula.itemsProperty().unbind();
		tvFormula.itemsProperty().bind(testCasesProperty(machine));
		tvFormula.refresh();
	}
	
	protected void setBindings() {
		final BooleanBinding partOfDisableBinding = currentTrace.existsProperty().not()
				.or(Bindings.createBooleanBinding(() -> currentTrace.getModel() == null || currentTrace.getModel().getFormalismType() != FormalismType.B, currentTrace.modelProperty()));
		injector.getInstance(DisablePropertyController.class).addDisableProperty(addFormulaButton.disableProperty(), partOfDisableBinding);
		injector.getInstance(DisablePropertyController.class).addDisableProperty(checkMachineButton.disableProperty(), partOfDisableBinding);
		cancelButton.disableProperty().bind(testCaseGenerator.currentJobThreadsProperty().emptyProperty());
		injector.getInstance(DisablePropertyController.class).addDisableProperty(tvFormula.disableProperty(), partOfDisableBinding);
		statusColumn.setCellFactory(col -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory(CheckingType.TEST_CASE_GENERATION, injector, this));

		selectAll.setSelected(true);
		selectAll.selectedProperty().addListener((observable, from, to) -> {
			if(!to) {
				checkMachineButton.disableProperty().unbind();
				checkMachineButton.setDisable(true);
			} else {
				injector.getInstance(DisablePropertyController.class).addDisableProperty(checkMachineButton.disableProperty(), testCasesProperty(currentProject.getCurrentMachine()).emptyProperty());
			}
		});
		selectAll.setOnAction(e-> {
			for(IExecutableItem item : tvFormula.getItems()) {
				item.setSelected(selectAll.isSelected());
				tvFormula.refresh();
			}
		});

		shouldExecuteColumn.setGraphic(selectAll);
		tvFormula.setOnMouseClicked(e-> {
			TestCaseGenerationItem item = tvFormula.getSelectionModel().getSelectedItem();
			if(e.getClickCount() == 2 && item != null && currentTrace.exists()) {
				itemHandler.handleItem(item, false);
			}
		});

	}

	public ListProperty<TestCaseGenerationItem> testCasesProperty(Machine machine) {
		return machine.testCasesProperty();
	}
	

	private void removeFormula(Machine machine, TestCaseGenerationItem item) {
		machine.removeTestCase(item);
	}
	
	private void setContextMenu() {
		tvFormula.setRowFactory(new TestCaseGenerationCellFactory());
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(TestCaseGenerationChoosingStage.class).reset();
		injector.getInstance(TestCaseGenerationChoosingStage.class).showAndWait();
	}
	
	private void removeFormula() {
		Machine machine = currentProject.getCurrentMachine();
		TestCaseGenerationItem item = tvFormula.getSelectionModel().getSelectedItem();
		removeFormula(machine, item);
	}

	private void openItem(TestCaseGenerationItem item) {
		TestCaseGenerationInput input = injector.getInstance(TestCaseGenerationInput.class);
		input.changeFormula(item, injector.getInstance(TestCaseGenerationView.class),
				injector.getInstance(TestCaseGenerationResultHandler.class), injector.getInstance(TestCaseGenerationChoosingStage.class));
	}
	
	public void refresh() {
		tvFormula.refresh();
	}
	
	@FXML
	public void checkMachine() {
		Machine machine = currentProject.getCurrentMachine();
		itemHandler.handleMachine(machine);
		refresh();
	}
	
	@FXML
	public void cancel() {
		testCaseGenerator.interrupt();
	}

	@Override
	public void updateSelectViews() {
		boolean anySelected = false;
		for(TestCaseGenerationItem item : testCasesProperty(currentProject.getCurrentMachine()).get()) {
			if(item.selected()) {
				anySelected = true;
			}
		}
		selectAll.setSelected(anySelected);
	}
		
}
