package de.prob2.ui.verifications.cbc;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.MachineTableView;
import de.prob2.ui.verifications.MachineTableView.CheckingType;
import de.prob2.ui.verifications.ltl.formula.LTLFormulaItem;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;

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
	
	private final CurrentTrace currentTrace;
	
	private final CurrentProject currentProject;

	private final Injector injector;
	
	private final CBCChecker cbcChecker;
	
	@Inject
	public CBCView(final StageManager stageManager, final CurrentTrace currentTrace, 
					final CurrentProject currentProject, final CBCChecker cbcChecker,
					final Injector injector) {
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.cbcChecker = cbcChecker;
		this.injector = injector;
		stageManager.loadFXML(this, "cbc_view.fxml");
	}
	
	@FXML
	public void initialize() {
		helpButton.setHelpContent("HelpMain.html");
		tvMachines.setCheckingType(CheckingType.CBC);
		addFormulaButton.disableProperty().bind(currentTrace.existsProperty().not());
		formulaStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		formulaNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		formulaDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		tvMachines.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to != null) {
				tvFormula.itemsProperty().unbind();
				tvFormula.itemsProperty().bind(to.cbcFormulasProperty());
			}
		});
		currentTrace.existsProperty().addListener((observable, oldValue, newValue) -> {
			if(tvMachines.getSelectionModel().getSelectedIndex() < 0) {
				tvMachines.getSelectionModel().select(0);
			}
		});
		addFormulaButton.disableProperty().bind(currentTrace.existsProperty().not());
		tvFormula.setRowFactory(table -> {
			
			final TableRow<CBCFormulaItem> row = new TableRow<>();
			
			MenuItem check = new MenuItem("Check separately");
			check.setOnAction(e-> {
				CBCFormulaItem item = row.getItem();
				cbcChecker.checkInvariant(item.getName());
			});
			
			MenuItem removeItem = new MenuItem("Remove Formula");
			removeItem.setOnAction(e -> removeFormula());
			removeItem.disableProperty().bind(row.emptyProperty());
			row.setContextMenu(new ContextMenu(check, removeItem));
			return row;
		});
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(CBCChoosingStage.class).showAndWait();
	}
	
	private void removeFormula() {
		Machine machine = tvMachines.getSelectionModel().getSelectedItem();
		CBCFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
		machine.removeCBCFormula(item);
		currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
				tvMachines.getItems(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
				currentProject.getLocation()));
		currentProject.setSaved(false);
	}
	
	public Machine getCurrentMachine() {
		return tvMachines.getSelectionModel().getSelectedItem();
	}
	
	public void updateMachineStatus(Machine machine) {
		for(CBCFormulaItem formula : machine.getCBCFormulas()) {
			if(formula.getChecked() == Checked.FAIL) {
				machine.setCBCCheckedFailed();
				return;
			}
		}
		machine.setCBCCheckedSuccessful();
	}
	
	public void updateProject() {
		currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
				tvMachines.getItems(), currentProject.getPreferences(), currentProject.getRunconfigurations(), 
				currentProject.getLocation()));
	}
	
	public void refresh() {
		tvMachines.refresh();
		tvFormula.refresh();
	}
		
}
