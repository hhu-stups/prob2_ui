package de.prob2.ui.verifications.cbc;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.Trace;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.MachineTableView;
import de.prob2.ui.verifications.cbc.CBCFormulaItem.CBCType;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;

import java.util.List;

import javax.inject.Inject;

@Singleton
public class CBCView extends AnchorPane {
	
	@FXML
	private HelpButton helpButton;
	
	@FXML
	private MachineTableView tvMachines;
	
	@FXML
	private TableView<CBCFormulaItem> tvFormula;
	
	@FXML
	private TableColumn<LTLFormulaItem, FontAwesomeIconView> formulaStatusColumn;
	
	@FXML
	private TableColumn<LTLFormulaItem, String> formulaNameColumn;
	
	@FXML
	private TableColumn<LTLFormulaItem, String> formulaDescriptionColumn;
	
	@FXML
	private Button addFormulaButton;
	
	@FXML
	private Button checkSelectedMachineButton;
	
	@FXML
	private Button checkAllOperationsButton;
	
	@FXML
	private Button findDeadlockButton;
	
	@FXML
	private Button findRedundantsButton;
	
	@FXML
	private Button findValidStateButton;
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;

	private final Injector injector;
	
	private final CBCFormulaHandler cbcHandler;
	
	private final AnimationSelector animations;
	
	@Inject
	public CBCView(final StageManager stageManager, final CurrentTrace currentTrace, 
					final CurrentProject currentProject, final CBCFormulaHandler cbcHandler,
					final Injector injector, final AnimationSelector animations) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.cbcHandler = cbcHandler;
		this.injector = injector;
		this.animations = animations;
		stageManager.loadFXML(this, "cbc_view.fxml");
	}
	
	@FXML
	public void initialize() {
		helpButton.setHelpContent("HelpMain.html");
		tvMachines.setCheckingType(de.prob2.ui.verifications.CheckingType.CBC);
		setBindings();
		setContextMenu();
		currentTrace.existsProperty().addListener((observable, oldValue, newValue) -> {
			if(tvMachines.getSelectionModel().getSelectedIndex() < 0) {
				tvMachines.getSelectionModel().select(0);
			}
		});
	}
	
	private void setBindings() {
		addFormulaButton.disableProperty().bind(currentTrace.existsProperty().not());
		checkSelectedMachineButton.disableProperty().bind(currentTrace.existsProperty().not());
		checkAllOperationsButton.disableProperty().bind(currentTrace.existsProperty().not());
		findDeadlockButton.disableProperty().bind(currentTrace.existsProperty().not());
		findRedundantsButton.disableProperty().bind(currentTrace.existsProperty().not());
		findValidStateButton.disableProperty().bind(currentTrace.existsProperty().not());
		formulaStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		formulaNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		formulaDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		tvMachines.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to != null) {
				tvFormula.itemsProperty().unbind();
				tvFormula.itemsProperty().bind(to.cbcFormulasProperty());
			}
		});
		addFormulaButton.disableProperty().bind(currentTrace.existsProperty().not());
	}
	
	private void setContextMenu() {
		tvFormula.setRowFactory(table -> {
			
			final TableRow<CBCFormulaItem> row = new TableRow<>();
			
			MenuItem check = new MenuItem("Check separately");
			check.setOnAction(e-> {
				cbcHandler.checkItem(row.getItem());
				cbcHandler.updateMachineStatus(getCurrentMachine());
			});
			check.disableProperty().bind(row.emptyProperty());
			
			Menu showCounterExampleItem = new Menu("Show Counter Example");
			showCounterExampleItem.setDisable(true);
			
			MenuItem showStateItem = new MenuItem("Show found State");
			showStateItem.setDisable(true);
			
			MenuItem removeItem = new MenuItem("Remove Formula");
			removeItem.setOnAction(e -> removeFormula());
			removeItem.disableProperty().bind(row.emptyProperty());
			
			MenuItem changeItem = new MenuItem("Change Formula");
			changeItem.setOnAction(e->openItem(row.getItem()));
			changeItem.disableProperty().bind(row.emptyProperty());
			
			row.setOnMouseClicked(e-> {
				if(e.getButton() == MouseButton.SECONDARY) {
					CBCFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
					if(row.emptyProperty().get() || item.getCounterExamples().isEmpty()) {
						showCounterExampleItem.setDisable(true);
					} else {
						showCounterExampleItem.setDisable(false);
						showCounterExamples(showCounterExampleItem);
					}
					
					if(item instanceof CBCFormulaFindStateItem) {
						if(((CBCFormulaFindStateItem) item).getExample() != null) {
							showStateItem.setDisable(false);
							showStateItem.setOnAction(event-> {
								showTrace(((CBCFormulaFindStateItem) item).getExample());
							});
						} else {
							showStateItem.setDisable(true);
						}
					}
				}
			});
			
			row.setContextMenu(new ContextMenu(check, changeItem, showCounterExampleItem, showStateItem, removeItem));
			return row;
		});
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(CBCChoosingStage.class).showAndWait();
	}
	
	@FXML
	public void checkSelectedMachine() {
		Machine machine = tvMachines.getSelectionModel().getSelectedItem();
		cbcHandler.checkMachine(machine);
		cbcHandler.updateMachineStatus(machine);
		refresh();
	}
	
	@FXML
	public void checkAllOperations() {
		List<String> events = injector.getInstance(CBCInvariants.class).getEvents();
		for(String event : events) {
			cbcHandler.addFormula(event, event, CBCFormulaItem.CBCType.INVARIANT, true);
			cbcHandler.checkInvariant(event);
		}
	}
	
	@FXML
	public void findDeadlock() {
		cbcHandler.addFormula("FIND DEADLOCK", "FIND DEADLOCK", CBCFormulaItem.CBCType.DEADLOCK, true);
		cbcHandler.findDeadlock();
	}
	
	@FXML
	public void findRedundants() {
		cbcHandler.addFormula("FIND REDUNDANT INVARIANTS", "FIND REDUNDANT INVARIANTS", CBCFormulaItem.CBCType.INVARIANT, true);
		cbcHandler.findRedundantInvariants();
	}
	
	@FXML
	public void findValidState() {
		injector.getInstance(CBCFindValidState.class).showAndWait();
	}
	
	
	private void removeFormula() {
		Machine machine = tvMachines.getSelectionModel().getSelectedItem();
		CBCFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
		machine.removeCBCFormula(item);
		updateProject();
	}
	
	public Machine getCurrentMachine() {
		return tvMachines.getSelectionModel().getSelectedItem();
	}
	
	public void updateProject() {
		currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
				tvMachines.getItems(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
				currentProject.getLocation()));
		currentProject.setSaved(false);
	}
	
	public void refresh() {
		tvMachines.refresh();
		tvFormula.refresh();
	}
	
	private void showCounterExamples(Menu counterExampleItem) {
		counterExampleItem.getItems().clear();
		CBCFormulaItem currentItem = tvFormula.getSelectionModel().getSelectedItem();
		List<Trace> counterExamples = currentItem.getCounterExamples();
		for(int i = 0; i < counterExamples.size(); i++) {
			MenuItem traceItem = new MenuItem("Counter Example " + Integer.toString(i + 1));
			final int index = i;
			traceItem.setOnAction(e-> showTrace(counterExamples.get(index)));
			counterExampleItem.getItems().add(traceItem);
		}

	}
	
	private void showTrace(Trace trace) {
		if (currentTrace.exists()) {
			this.animations.removeTrace(currentTrace.get());
		}
		animations.addNewAnimation(trace);
	}
	
	private void openItem(CBCFormulaItem item) {
		if(item.getType() == CBCType.INVARIANT) {
			CBCInvariants cbcInvariants = injector.getInstance(CBCInvariants.class);
			cbcInvariants.changeFormula(item);
		} else if(item.getType() == CBCType.SEQUENCE) {
			CBCSequence cbcSequence = injector.getInstance(CBCSequence.class);
			cbcSequence.changeFormula(item);
		} else if(item.getType() == CBCType.DEADLOCK){
			CBCDeadlock cbcDeadlock = injector.getInstance(CBCDeadlock.class);
			cbcDeadlock.changeFormula(item);
		} else {
			CBCFindValidState cbcFindValidState = injector.getInstance(CBCFindValidState.class);
			cbcFindValidState.changeFormula(item);
		}
	}
		
}
