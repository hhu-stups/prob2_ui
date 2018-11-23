package de.prob2.ui.verifications.symbolicchecking;

import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;


import de.prob.statespace.Trace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicView;
import de.prob2.ui.verifications.Checked;


import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;


@FXMLInjected
@Singleton
public class SymbolicCheckingView extends SymbolicView<SymbolicCheckingFormulaItem> {
	
	private class SymbolicCheckingCellFactory extends SymbolicCellFactory implements Callback<TableView<SymbolicCheckingFormulaItem>, TableRow<SymbolicCheckingFormulaItem>> {
		
		@Override
		public TableRow<SymbolicCheckingFormulaItem> call(TableView<SymbolicCheckingFormulaItem> param) {
			TableRow<SymbolicCheckingFormulaItem> row = createRow();
			
			Menu showCounterExampleItem = new Menu(bundle.getString("verifications.symbolicchecking.view.contextMenu.showCounterExample"));
			showCounterExampleItem.setDisable(true);
			
			MenuItem showMessage = new MenuItem(bundle.getString("symbolic.view.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> injector.getInstance(SymbolicCheckingResultHandler.class).showResult(row.getItem()));
			
			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					showMessage.disableProperty().bind(to.resultItemProperty().isNull()
							.or(Bindings.createBooleanBinding(() -> to.getResultItem() != null && Checked.SUCCESS == to.getResultItem().getChecked(), to.resultItemProperty())));
					showCounterExampleItem.disableProperty().bind(row.emptyProperty()
							.or(to.counterExamplesProperty().emptyProperty()));
					showCounterExamples(to, showCounterExampleItem);
				}
			});
			
			row.getContextMenu().getItems().addAll(showMessage, showCounterExampleItem);
			return row;
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
	}

	@Inject
	public SymbolicCheckingView(final StageManager stageManager, final ResourceBundle bundle, final CurrentTrace currentTrace, 
					final CurrentProject currentProject, final SymbolicCheckingFormulaHandler symbolicCheckHandler, 
					final SymbolicFormulaChecker symbolicChecker, final Injector injector) {
		super(bundle, currentTrace, currentProject, injector, symbolicChecker, symbolicCheckHandler);
		stageManager.loadFXML(this, "symbolic_checking_view.fxml");
	}
	
	protected ListProperty<SymbolicCheckingFormulaItem> formulasProperty(Machine machine) {
		return machine.symbolicCheckingFormulasProperty();
	}
	
	@Override
	protected void removeFormula(Machine machine, SymbolicCheckingFormulaItem item) {
		machine.removeSymbolicCheckingFormula(item);
	}
	
	@Override
	protected void setContextMenu() {
		tvFormula.setRowFactory(new SymbolicCheckingCellFactory());
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(SymbolicCheckingChoosingStage.class).reset();
		injector.getInstance(SymbolicCheckingChoosingStage.class).showAndWait();
	}
	
	@Override
	protected void openItem(SymbolicCheckingFormulaItem item) {
		SymbolicCheckingFormulaInput formulaInput = injector.getInstance(SymbolicCheckingFormulaInput.class);
		formulaInput.changeFormula(item, injector.getInstance(SymbolicCheckingView.class), injector.getInstance(SymbolicCheckingResultHandler.class),
									injector.getInstance(SymbolicCheckingFormulaHandler.class), injector.getInstance(SymbolicCheckingChoosingStage.class));
	}
		
}
