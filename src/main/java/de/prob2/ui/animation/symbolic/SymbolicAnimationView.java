package de.prob2.ui.animation.symbolic;


import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicView;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckingType;
import de.prob2.ui.verifications.MachineStatusHandler;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.fxml.FXML;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import javafx.scene.control.TableRow;


@Singleton
public class SymbolicAnimationView extends SymbolicView<SymbolicAnimationFormulaItem> {
	
	@Inject
	public SymbolicAnimationView(final StageManager stageManager, final ResourceBundle bundle, final CurrentTrace currentTrace, 
					final CurrentProject currentProject, final SymbolicAnimationFormulaHandler symbolicCheckHandler, 
					final SymbolicAnimationChecker symbolicChecker, final Injector injector) {
		super(stageManager, bundle, currentTrace, currentProject, injector, symbolicChecker, symbolicCheckHandler);
		stageManager.loadFXML(this, "symbolic_animation_view.fxml");
	}
	

	
	protected ListProperty<SymbolicAnimationFormulaItem> formulasProperty(Machine machine) {
		return machine.symbolicAnimationFormulasProperty();
	}
	
	@Override
	protected void removeFormula(Machine machine, SymbolicAnimationFormulaItem item) {
		machine.removeSymbolicAnimationFormula(item);
	}
	
	protected void setContextMenu() {
		tvFormula.setRowFactory(table -> {
			
			final TableRow<SymbolicAnimationFormulaItem> row = new TableRow<>();
			
			MenuItem checkItem = new MenuItem(bundle.getString("verifications.symbolicchecking.view.contextMenu.check"));
			checkItem.setDisable(true);
			checkItem.setOnAction(e-> {
				formulaHandler.handleItem(row.getItem(), false);
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(currentProject.getCurrentMachine(), CheckingType.SYMBOLIC);
			});
			
			MenuItem showMessage = new MenuItem(bundle.getString("verifications.symbolicchecking.view.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> injector.getInstance(SymbolicAnimationResultHandler.class).showResult(row.getItem()));
			
			MenuItem showStateItem = new MenuItem(bundle.getString("verifications.symbolicchecking.view.contextMenu.showFoundState"));
			showStateItem.setDisable(true);
			
			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					checkItem.disableProperty().bind(row.emptyProperty()
							.or(executor.currentJobThreadsProperty().emptyProperty().not())
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

	
	private void openItem(SymbolicAnimationFormulaItem item) {
		SymbolicAnimationFormulaInput formulaInput = injector.getInstance(SymbolicAnimationFormulaInput.class);
		formulaInput.changeFormula(item);
	}
		
}
