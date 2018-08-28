package de.prob2.ui.animation.symbolic;


import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.Project;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicView;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.MachineStatusHandler;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import javafx.scene.control.TableRow;


@Singleton
public class SymbolicAnimationView extends SymbolicView<SymbolicAnimationFormulaItem> {
	
	private final SymbolicAnimationFormulaHandler symbolicCheckHandler;
	
	private final SymbolicAnimationChecker symbolicChecker;

	@Inject
	public SymbolicAnimationView(final StageManager stageManager, final ResourceBundle bundle, final CurrentTrace currentTrace, 
					final CurrentProject currentProject, final SymbolicAnimationFormulaHandler symbolicCheckHandler, 
					final SymbolicAnimationChecker symbolicChecker, final Injector injector) {
		super(stageManager, bundle, currentTrace, currentProject, injector, symbolicChecker);
		this.symbolicCheckHandler = symbolicCheckHandler;
		this.symbolicChecker = symbolicChecker;
		stageManager.loadFXML(this, "symbolic_animation_view.fxml");
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
				checkMachineButton.disableProperty().bind(currentProject.getCurrentMachine().symbolicCheckingFormulasProperty().emptyProperty().or(symbolicChecker.currentJobThreadsProperty().emptyProperty().not()));
			} else {
				checkMachineButton.disableProperty().bind(currentTrace.existsProperty().not().or(symbolicChecker.currentJobThreadsProperty().emptyProperty().not()));
			}
		});
	}
	
	public void bindMachine(Machine machine) {
		tvFormula.itemsProperty().unbind();
		tvFormula.itemsProperty().bind(machine.symbolicAnimationFormulasProperty());
		tvFormula.refresh();
	}
	
	protected void setBindings() {
		super.setBindings();
		tvFormula.setOnMouseClicked(e-> {
			SymbolicAnimationFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
			if(e.getClickCount() == 2 && item != null && currentTrace.exists()) {
				symbolicCheckHandler.handleItem(item, false);
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(currentProject.getCurrentMachine(), CheckingType.SYMBOLIC);
			}
		});
	}
	
	
	protected void setContextMenu() {
		tvFormula.setRowFactory(table -> {
			
			final TableRow<SymbolicAnimationFormulaItem> row = new TableRow<>();
			
			MenuItem checkItem = new MenuItem(bundle.getString("verifications.symbolicchecking.view.contextMenu.check"));
			checkItem.setDisable(true);
			checkItem.setOnAction(e-> {
				symbolicCheckHandler.handleItem(row.getItem(), false);
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(currentProject.getCurrentMachine(), CheckingType.SYMBOLIC);
			});
			
			MenuItem showMessage = new MenuItem(bundle.getString("verifications.symbolicchecking.view.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> injector.getInstance(SymbolicAnimationResultHandler.class).showResult(row.getItem()));
			
			MenuItem showStateItem = new MenuItem(bundle.getString("verifications.symbolicchecking.view.contextMenu.showFoundState"));
			showStateItem.setDisable(true);
			
			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					checkItem.disableProperty().bind(row.emptyProperty()
							.or(symbolicChecker.currentJobThreadsProperty().emptyProperty().not())
							.or(to.shouldExecuteProperty().not()));
					showMessage.disableProperty().bind(to.resultItemProperty().isNull()
							.or(Bindings.createBooleanBinding(() -> to.getResultItem() != null && Checked.SUCCESS == to.getResultItem().getChecked(), to.resultItemProperty())));
					
					showStateItem.disableProperty().bind(row.emptyProperty()
							.or(to.exampleProperty().isNull()));
					if(to.getExample() != null) {
						showStateItem.setOnAction(event-> currentTrace.set(to.getExample()));
					}
				}
			});

			
			MenuItem removeItem = new MenuItem(bundle.getString("verifications.symbolicchecking.view.contextMenu.remove"));
			removeItem.setOnAction(e -> removeFormula());
			removeItem.disableProperty().bind(row.emptyProperty());
			
			MenuItem changeItem = new MenuItem(bundle.getString("verifications.symbolicchecking.view.contextMenu.change"));
			changeItem.setOnAction(e->openItem(row.getItem()));
			
			row.setContextMenu(new ContextMenu(checkItem, changeItem, showMessage, showStateItem, removeItem));
			return row;
		});
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(SymbolicAnimationChoosingStage.class).reset();
		injector.getInstance(SymbolicAnimationChoosingStage.class).showAndWait();
	}
	
	@FXML
	public void checkMachine() {
		Machine machine = currentProject.getCurrentMachine();
		symbolicCheckHandler.handleMachine(machine);
		injector.getInstance(MachineStatusHandler.class).updateMachineStatus(machine, CheckingType.SYMBOLIC);
		refresh();
	}
	
	@FXML
	public void cancel() {
		symbolicChecker.interrupt();
	}
	
	private void removeFormula() {
		Machine machine = currentProject.getCurrentMachine();
		SymbolicAnimationFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
		machine.removeSymbolicAnimationFormula(item);
		updateProject();
	}
	
	
	public void updateProject() {
		currentProject.update(new Project(currentProject.getName(), currentProject.getDescription(), 
				currentProject.getMachines(), currentProject.getPreferences(), currentProject.getLocation()));
	}
	
	public void refresh() {
		tvFormula.refresh();
	}

	
	private void openItem(SymbolicAnimationFormulaItem item) {
		SymbolicAnimationFormulaInput formulaInput = injector.getInstance(SymbolicAnimationFormulaInput.class);
		formulaInput.changeFormula(item);
	}
		
}
