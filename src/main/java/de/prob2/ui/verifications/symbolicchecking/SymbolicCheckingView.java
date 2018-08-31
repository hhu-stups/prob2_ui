package de.prob2.ui.verifications.symbolicchecking;

import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;


import de.prob.statespace.Trace;

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
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import javafx.scene.control.TableRow;


@Singleton
public class SymbolicCheckingView extends SymbolicView<SymbolicCheckingFormulaItem> {

	@Inject
	public SymbolicCheckingView(final StageManager stageManager, final ResourceBundle bundle, final CurrentTrace currentTrace, 
					final CurrentProject currentProject, final SymbolicCheckingFormulaHandler symbolicCheckHandler, 
					final SymbolicFormulaChecker symbolicChecker, final Injector injector) {
		super(stageManager, bundle, currentTrace, currentProject, injector, symbolicChecker, symbolicCheckHandler);
		stageManager.loadFXML(this, "symbolic_checking_view.fxml");
	}
	
	protected ListProperty<SymbolicCheckingFormulaItem> formulasProperty(Machine machine) {
		return machine.symbolicCheckingFormulasProperty();
	}
	
	@Override
	protected void removeFormula(Machine machine, SymbolicCheckingFormulaItem item) {
		machine.removeSymbolicCheckingFormula(item);
	}
	
	protected void setContextMenu() {
		tvFormula.setRowFactory(table -> {
			
			final TableRow<SymbolicCheckingFormulaItem> row = new TableRow<>();
			
			MenuItem checkItem = new MenuItem(bundle.getString("verifications.symbolicchecking.view.contextMenu.check"));
			checkItem.setDisable(true);
			checkItem.setOnAction(e-> {
				formulaHandler.handleItem(row.getItem(), false);
				injector.getInstance(MachineStatusHandler.class).updateMachineStatus(currentProject.getCurrentMachine(), CheckingType.SYMBOLIC);
			});


			
			Menu showCounterExampleItem = new Menu(bundle.getString("verifications.symbolicchecking.view.contextMenu.showCounterExample"));
			showCounterExampleItem.setDisable(true);
			
			MenuItem showMessage = new MenuItem(bundle.getString("verifications.symbolicchecking.view.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> injector.getInstance(SymbolicCheckingResultHandler.class).showResult(row.getItem()));
			
			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					checkItem.disableProperty().bind(row.emptyProperty()
							.or(executor.currentJobThreadsProperty().emptyProperty().not())
							.or(to.shouldExecuteProperty().not()));
					showMessage.disableProperty().bind(to.resultItemProperty().isNull()
							.or(Bindings.createBooleanBinding(() -> to.getResultItem() != null && Checked.SUCCESS == to.getResultItem().getChecked(), to.resultItemProperty())));
					showCounterExampleItem.disableProperty().bind(row.emptyProperty()
							.or(to.counterExamplesProperty().emptyProperty()));
					showCounterExamples(to, showCounterExampleItem);
				}
			});

			
			MenuItem removeItem = new MenuItem(bundle.getString("verifications.symbolicchecking.view.contextMenu.remove"));
			removeItem.setOnAction(e -> removeFormula());
			removeItem.disableProperty().bind(row.emptyProperty());
			
			MenuItem changeItem = new MenuItem(bundle.getString("verifications.symbolicchecking.view.contextMenu.change"));
			changeItem.setOnAction(e->openItem(row.getItem()));
			
			row.setContextMenu(new ContextMenu(checkItem, changeItem, showCounterExampleItem, showMessage, removeItem));
			return row;
		});
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(SymbolicCheckingChoosingStage.class).reset();
		injector.getInstance(SymbolicCheckingChoosingStage.class).showAndWait();
	}
	
	private void showCounterExamples(SymbolicCheckingFormulaItem item, Menu counterExampleItem) {
		counterExampleItem.getItems().clear();
		List<Trace> counterExamples = item.getCounterExamples();
		for(int i = 0; i < counterExamples.size(); i++) {
			MenuItem traceItem = new MenuItem(String.format(bundle.getString("verifications.symbolicchecking.view.contextMenu.showCounterExample.counterExample"), i + 1));
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
