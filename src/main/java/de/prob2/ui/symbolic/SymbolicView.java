package de.prob2.ui.symbolic;

import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Injector;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.MachineStatusHandler;
import de.prob2.ui.verifications.ShouldExecuteValueFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public abstract class SymbolicView<T extends SymbolicFormulaItem> extends ScrollPane {

	@FXML
	protected HelpButton helpButton;
		
	@FXML
	protected TableView<T> tvFormula;
	
	@FXML
	protected TableColumn<T, FontAwesomeIconView> formulaStatusColumn;
	
	@FXML
	protected TableColumn<T, String> formulaNameColumn;
	
	@FXML
	protected TableColumn<T, String> formulaDescriptionColumn;
	
	@FXML
	protected TableColumn<IExecutableItem, CheckBox> shouldExecuteColumn;
	
	@FXML
	protected Button addFormulaButton;
	
	@FXML
	protected Button checkMachineButton;
	
	@FXML
	protected Button cancelButton;
					
	protected final ResourceBundle bundle;
	
	protected final CurrentTrace currentTrace;
	
	protected final CurrentProject currentProject;

	protected final Injector injector;
	
	protected final SymbolicExecutor executor;
	
	@Inject
	public SymbolicView(final StageManager stageManager, final ResourceBundle bundle, final CurrentTrace currentTrace, 
					final CurrentProject currentProject, final Injector injector, final SymbolicExecutor executor) {
		this.bundle = bundle;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
		this.executor = executor;
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
				checkMachineButton.disableProperty().bind(currentProject.getCurrentMachine().symbolicCheckingFormulasProperty().emptyProperty().or(executor.currentJobThreadsProperty().emptyProperty().not()));
			} else {
				checkMachineButton.disableProperty().bind(currentTrace.existsProperty().not().or(executor.currentJobThreadsProperty().emptyProperty().not()));
			}
		});
	}
	
	protected void setBindings() {
		addFormulaButton.disableProperty().bind(currentTrace.existsProperty().not().or(executor.currentJobThreadsProperty().emptyProperty().not()));
		checkMachineButton.disableProperty().bind(currentTrace.existsProperty().not().or(executor.currentJobThreadsProperty().emptyProperty().not()));
		cancelButton.disableProperty().bind(executor.currentJobThreadsProperty().emptyProperty());
		tvFormula.disableProperty().bind(currentTrace.existsProperty().not().or(executor.currentJobThreadsProperty().emptyProperty().not()));
		formulaStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
		formulaNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		formulaDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
		shouldExecuteColumn.setCellValueFactory(new ShouldExecuteValueFactory(CheckingType.SYMBOLIC, injector));
		CheckBox selectAll = new CheckBox();
		selectAll.setSelected(true);
		selectAll.selectedProperty().addListener((observable, from, to) -> {
			for(IExecutableItem item : tvFormula.getItems()) {
				item.setShouldExecute(to);
				Machine machine = injector.getInstance(CurrentProject.class).getCurrentMachine();
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.SYMBOLIC);
				tvFormula.refresh();
			}
		});
		shouldExecuteColumn.setGraphic(selectAll);

	}
	
	protected abstract void bindMachine(Machine machine);
	
	protected abstract void setContextMenu();
	
}
