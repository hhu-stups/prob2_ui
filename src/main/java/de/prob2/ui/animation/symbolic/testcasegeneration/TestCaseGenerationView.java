package de.prob2.ui.animation.symbolic.testcasegeneration;


import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.statespace.FormalismType;
import de.prob.statespace.Trace;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ItemSelectedFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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

 
@FXMLInjected
@Singleton
public class TestCaseGenerationView extends ScrollPane {
	
	@FXML
	private HelpButton helpButton;
		
	@FXML
	private TableView<TestCaseGenerationItem> tvTestCases;
	
	@FXML
	private TableColumn<TestCaseGenerationItem, Checked> statusColumn;
	
	@FXML
	private TableColumn<TestCaseGenerationItem, String> nameColumn;
	
	@FXML
	private TableColumn<TestCaseGenerationItem, String> descriptionColumn;
	
	@FXML
	private TableColumn<IExecutableItem, CheckBox> shouldExecuteColumn;
	
	@FXML
	private Button addTestCaseButton;
	
	@FXML
	private Button generateButton;
	
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
			
			MenuItem checkItem = new MenuItem(bundle.getString("animation.testcase.view.contextMenu.generate"));
			checkItem.setDisable(true);
			checkItem.setOnAction(e-> itemHandler.handleItem(row.getItem(), false));

			MenuItem removeItem = new MenuItem(bundle.getString("animation.testcase.view.contextMenu.removeConfiguration"));
			removeItem.setOnAction(e -> removeFormula());
			
			MenuItem changeItem = new MenuItem(bundle.getString("animation.testcase.view.contextMenu.changeConfiguration"));
			changeItem.setOnAction(e->openItem(row.getItem()));
			
			MenuItem showDetails = new MenuItem(bundle.getString("animation.testcase.view.contextMenu.showDetails"));
			showDetails.setDisable(true);
			showDetails.setOnAction(e -> {
				TestCaseGenerationItem item = row.getItem();
				TraceInformationStage stage = injector.getInstance(TraceInformationStage.class);
				stage.setTraces(item.getTraceInformation());
				stage.setUncoveredOperations(item.getUncoveredOperations());
				stage.show();
			});

			Menu showStateItem = new Menu(bundle.getString("animation.testcase.view.contextMenu.showFoundPaths"));
			showStateItem.setDisable(true);
			
			MenuItem showMessage = new MenuItem(bundle.getString("animation.testcase.view.contextMenu.showGenerationMessage"));
			showMessage.setOnAction(e -> injector.getInstance(TestCaseGenerationResultHandler.class).showResult(row.getItem()));

			MenuItem saveTraces = new MenuItem(bundle.getString("animation.testcase.view.contextMenu.savePaths"));
			saveTraces.setOnAction(e -> {
				TestCaseGenerationItem item = row.getItem();
				injector.getInstance(TestCaseGenerationResultHandler.class).saveTraces(item);
			});

			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					showMessage.disableProperty().bind(to.resultItemProperty().isNull());
					showStateItem.disableProperty().bind(to.examplesProperty().emptyProperty());
					showExamples(to, showStateItem);
					checkItem.disableProperty().bind(testCaseGenerator.currentJobThreadsProperty().emptyProperty().not()
							.or(to.selectedProperty().not()));
					showDetails.disableProperty().bind(to.examplesProperty().emptyProperty());
					saveTraces.disableProperty().bind(to.examplesProperty().emptyProperty());
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
				MenuItem traceItem = new MenuItem(String.format(bundle.getString("animation.testcase.view.contextMenu.showExample"), i + 1));
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
		helpButton.setHelpContent("animation", "Symbolic");
		setBindings();
		tvTestCases.setRowFactory(new TestCaseGenerationCellFactory());
		currentProject.currentMachineProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue != null) {
				bindMachine(newValue);
			} else {
				tvTestCases.getItems().clear();
				tvTestCases.itemsProperty().unbind();
			}
		});
	}
	
	public void bindMachine(Machine machine) {
		tvTestCases.itemsProperty().unbind();
		tvTestCases.itemsProperty().bind(machine.testCasesProperty());
		tvTestCases.refresh();
	}
	
	private void setBindings() {
		final BooleanBinding partOfDisableBinding = currentTrace.existsProperty().not()
				.or(Bindings.createBooleanBinding(() -> currentTrace.getModel() == null || currentTrace.getModel().getFormalismType() != FormalismType.B, currentTrace.modelProperty()));
		addTestCaseButton.disableProperty().bind(partOfDisableBinding.or(injector.getInstance(DisablePropertyController.class).disableProperty()));
		final BooleanProperty noTestCases = new SimpleBooleanProperty();
		currentProject.currentMachineProperty().addListener((o, from, to) -> {
			if (to != null) {
				noTestCases.bind(to.testCasesProperty().emptyProperty());
			} else {
				noTestCases.unbind();
				noTestCases.set(true);
			}
		});
		generateButton.disableProperty().bind(partOfDisableBinding.or(noTestCases.or(selectAll.selectedProperty().not().or(injector.getInstance(DisablePropertyController.class).disableProperty()))));
		cancelButton.disableProperty().bind(testCaseGenerator.currentJobThreadsProperty().emptyProperty());
		tvTestCases.disableProperty().bind(partOfDisableBinding.or(injector.getInstance(DisablePropertyController.class).disableProperty()));
		statusColumn.setCellFactory(col -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory(tvTestCases,  selectAll));
		shouldExecuteColumn.setGraphic(selectAll);
		tvTestCases.setOnMouseClicked(e-> {
			TestCaseGenerationItem item = tvTestCases.getSelectionModel().getSelectedItem();
			if(e.getClickCount() == 2 && item != null && currentTrace.exists()) {
				itemHandler.handleItem(item, false);
			}
		});
		tvTestCases.disableProperty().bind(currentTrace.existsProperty().not().or(injector.getInstance(DisablePropertyController.class).disableProperty()));
	}
	
	@FXML
	public void addTestCase() {
		injector.getInstance(TestCaseGenerationChoosingStage.class).reset();
		injector.getInstance(TestCaseGenerationChoosingStage.class).showAndWait();
	}
	
	private void removeFormula() {
		Machine machine = currentProject.getCurrentMachine();
		TestCaseGenerationItem item = tvTestCases.getSelectionModel().getSelectedItem();
		machine.removeTestCase(item);
	}

	private void openItem(TestCaseGenerationItem item) {
		TestCaseGenerationInput input = injector.getInstance(TestCaseGenerationInput.class);
		input.changeItem(item, injector.getInstance(TestCaseGenerationView.class),
				injector.getInstance(TestCaseGenerationResultHandler.class), injector.getInstance(TestCaseGenerationChoosingStage.class));
	}
	
	public void refresh() {
		tvTestCases.refresh();
	}
	
	@FXML
	public void generate() {
		Machine machine = currentProject.getCurrentMachine();
		itemHandler.handleMachine(machine);
		refresh();
	}
	
	@FXML
	public void cancel() {
		testCaseGenerator.interrupt();
	}
}
