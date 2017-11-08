package de.prob2.ui.verifications.symbolicchecking;

import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.statespace.Trace;

import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.MachineStatusHandler;
import de.prob2.ui.verifications.ShouldExecuteValueFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;

@Singleton
public class SymbolicCheckingView extends AnchorPane {
	
	
	@FXML
	private HelpButton helpButton;
		
	@FXML
	private TableView<SymbolicCheckingFormulaItem> tvFormula;
	
	@FXML
	private TableColumn<SymbolicCheckingFormulaItem, FontAwesomeIconView> formulaStatusColumn;
	
	@FXML
	private TableColumn<SymbolicCheckingFormulaItem, String> formulaNameColumn;
	
	@FXML
	private TableColumn<SymbolicCheckingFormulaItem, String> formulaDescriptionColumn;
	
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
	
	private final SymbolicCheckingFormulaHandler symbolicCheckHandler;
	
	private final SymbolicFormulaChecker symbolicChecker;

	@Inject
	public SymbolicCheckingView(final StageManager stageManager, final ResourceBundle bundle, final CurrentTrace currentTrace, 
					final CurrentProject currentProject, final SymbolicCheckingFormulaHandler symbolicCheckHandler, 
					final SymbolicFormulaChecker symbolicChecker, final Injector injector) {
		this.bundle = bundle;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.symbolicCheckHandler = symbolicCheckHandler;
		this.symbolicChecker = symbolicChecker;
		this.injector = injector;
		stageManager.loadFXML(this, "symbolic_checking_view.fxml");
	}
	
	@FXML
	public void initialize() {
		helpButton.setHelpContent("Verification.md.html");
		setBindings();
		setContextMenu();
		currentProject.currentMachineProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue != null) {
				tvFormula.itemsProperty().bind(newValue.symbolicCheckingFormulasProperty());
				tvFormula.refresh();
			} else {
				tvFormula.getItems().clear();
				tvFormula.itemsProperty().unbind();
			}
		});
		currentTrace.existsProperty().addListener((observable, oldValue, newValue) -> {
			if(newValue) {
				checkMachineButton.disableProperty().bind(currentProject.getCurrentMachine().symbolicCheckingFormulasProperty().emptyProperty().or(symbolicChecker.currentJobThreadsProperty().emptyProperty().not()));
			} else {
				checkMachineButton.disableProperty().bind(currentTrace.existsProperty().not().or(symbolicChecker.currentJobThreadsProperty().emptyProperty().not()));
			}
		});
	}
	
	private void setBindings() {
		addFormulaButton.disableProperty().bind(currentTrace.existsProperty().not().or(symbolicChecker.currentJobThreadsProperty().emptyProperty().not()));
		checkMachineButton.disableProperty().bind(currentTrace.existsProperty().not().or(symbolicChecker.currentJobThreadsProperty().emptyProperty().not()));
		cancelButton.disableProperty().bind(symbolicChecker.currentJobThreadsProperty().emptyProperty());
		tvFormula.disableProperty().bind(currentTrace.existsProperty().not().or(symbolicChecker.currentJobThreadsProperty().emptyProperty().not()));
		formulaStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		formulaNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		formulaDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		shouldExecuteColumn.setCellValueFactory(new ShouldExecuteValueFactory(de.prob2.ui.verifications.Type.SYMBOLIC, injector));
		
		tvFormula.setOnMouseClicked(e-> {
			SymbolicCheckingFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
			if(e.getClickCount() == 2 &&  item != null && currentTrace.exists()) {
				symbolicCheckHandler.handleItem(item);
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(currentProject.getCurrentMachine(), de.prob2.ui.verifications.Type.SYMBOLIC);
			}
		});
	}
	
	
	private void setContextMenu() {
		tvFormula.setRowFactory(table -> {
			
			final TableRow<SymbolicCheckingFormulaItem> row = new TableRow<>();
			
			MenuItem check = new MenuItem(bundle.getString("verifications.symbolic.menu.checkSeparately"));
			check.setOnAction(e-> {
				symbolicCheckHandler.handleItem(row.getItem());
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(currentProject.getCurrentMachine(), de.prob2.ui.verifications.Type.SYMBOLIC);
			});
			check.disableProperty().bind(row.emptyProperty());
			
			Menu showCounterExampleItem = new Menu(bundle.getString("verifications.symbolic.menu.showCounterExample"));
			showCounterExampleItem.setDisable(true);
			
			MenuItem showMessage = new MenuItem(bundle.getString("verifications.showCheckingMessage"));
			showMessage.setOnAction(e -> injector.getInstance(SymbolicCheckingResultHandler.class).showResult(row.getItem()));
			
			MenuItem showStateItem = new MenuItem(bundle.getString("verifications.symbolic.menu.showFoundState"));
			showStateItem.setDisable(true);
			
			MenuItem removeItem = new MenuItem(bundle.getString("verifications.symbolic.menu.remove"));
			removeItem.setOnAction(e -> removeFormula());
			removeItem.disableProperty().bind(row.emptyProperty());
			
			MenuItem changeItem = new MenuItem(bundle.getString("verifications.symbolic.menu.change"));
			changeItem.setOnAction(e->openItem(row.getItem()));
			
			row.setOnMouseClicked(e-> {
				
				if(e.getButton() == MouseButton.SECONDARY) {
					SymbolicCheckingFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
					if(row.emptyProperty().get() || item.getCounterExamples().isEmpty()) {
						showCounterExampleItem.setDisable(true);
					} else {
						showCounterExampleItem.setDisable(false);
						showCounterExamples(showCounterExampleItem);
					}
					
					if(item != null && item.getType() == SymbolicCheckingType.FIND_VALID_STATE) {
						if(row.emptyProperty().get() || item.getExample() == null) {
							showStateItem.setDisable(true);
						} else {
							showStateItem.setDisable(false);
							showStateItem.setOnAction(event-> currentTrace.set((item.getExample())));
						}
					}
					
					if(item.getResultItem() == null || Checked.SUCCESS == item.getResultItem().getChecked()) {
						showMessage.setDisable(true);
					} else {
						showMessage.setDisable(false);
					}
				}
			});
			
			row.setContextMenu(new ContextMenu(check, changeItem, showCounterExampleItem, showMessage, showStateItem, removeItem));
			return row;
		});
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(SymbolicCheckingChoosingStage.class).reset();
		injector.getInstance(SymbolicCheckingChoosingStage.class).showAndWait();
	}
	
	@FXML
	public void checkMachine() {
		Machine machine = currentProject.getCurrentMachine();
		symbolicCheckHandler.handleMachine(machine);
		injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, de.prob2.ui.verifications.Type.SYMBOLIC);
		refresh();
	}
	
	@FXML
	public synchronized void cancel() {
		symbolicChecker.interrupt();
	}
	
	private void removeFormula() {
		Machine machine = currentProject.getCurrentMachine();
		SymbolicCheckingFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
		machine.removeSymbolicCheckingFormula(item);
		updateProject();
	}
	
	
	public void updateProject() {
		currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
				currentProject.getMachines(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
				currentProject.getLocation()));
	}
	
	public void refresh() {
		tvFormula.refresh();
	}
	
	private void showCounterExamples(Menu counterExampleItem) {
		counterExampleItem.getItems().clear();
		SymbolicCheckingFormulaItem currentItem = tvFormula.getSelectionModel().getSelectedItem();
		List<Trace> counterExamples = currentItem.getCounterExamples();
		for(int i = 0; i < counterExamples.size(); i++) {
			MenuItem traceItem = new MenuItem(String.format(bundle.getString("verifications.symbolic.menu.showCounterExample.counterExample"), i + 1));
			final int index = i;
			traceItem.setOnAction(e-> currentTrace.set((counterExamples.get(index))));
			counterExampleItem.getItems().add(traceItem);
		}

	}
	
	private void openItem(SymbolicCheckingFormulaItem item) {
		SymbolicCheckingFormulaInput formulaInput = injector.getInstance(SymbolicCheckingFormulaInput.class);
		formulaInput.changeFormula(item);
	}
		
}
