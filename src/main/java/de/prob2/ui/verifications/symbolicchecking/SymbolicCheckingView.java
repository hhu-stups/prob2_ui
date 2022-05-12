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

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
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
			
			MenuItem removeItem = new MenuItem(bundle.getString("symbolic.view.contextMenu.removeConfiguration"));
			removeItem.setOnAction(e -> removeFormula());
			
			MenuItem changeItem = new MenuItem(bundle.getString("symbolic.view.contextMenu.changeConfiguration"));
			changeItem.setOnAction(e->openItem(row.getItem()));
			
			Menu showCounterExampleItem = new Menu(bundle.getString("verifications.symbolicchecking.view.contextMenu.showCounterExample"));
			showCounterExampleItem.setDisable(true);
			
			MenuItem showMessage = new MenuItem(bundle.getString("symbolic.view.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> injector.getInstance(SymbolicCheckingResultHandler.class).showResult(row.getItem()));
			
			row.itemProperty().addListener((observable, from, to) -> {
				final InvalidationListener updateCounterExamplesListener = o -> showCounterExamples(to, showCounterExampleItem);

				if (from != null) {
					from.counterExamplesProperty().removeListener(updateCounterExamplesListener);
				}

				if(to != null) {
					showMessage.disableProperty().bind(to.resultItemProperty().isNull());
					showCounterExampleItem.disableProperty().bind(to.counterExamplesProperty().emptyProperty());
					to.counterExamplesProperty().addListener(updateCounterExamplesListener);
					updateCounterExamplesListener.invalidated(null);
				}
			});
			
			ContextMenu contextMenu = row.getContextMenu();
			contextMenu.getItems().addAll(changeItem, removeItem, showMessage, showCounterExampleItem);
	
			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
					.then((ContextMenu) null)
					.otherwise(contextMenu));			
			
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
		super(bundle, currentTrace, currentProject, injector, symbolicChecker, symbolicCheckHandler, SymbolicCheckingFormulaItem.class);
		stageManager.loadFXML(this, "symbolic_checking_view.fxml");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		tvFormula.setRowFactory(new SymbolicCheckingCellFactory());
		helpButton.setHelpContent("verification", "Symbolic");
	}
	
	@Override
	protected ListProperty<SymbolicCheckingFormulaItem> formulasProperty(Machine machine) {
		return machine.symbolicCheckingFormulasProperty();
	}
	
	@Override
	protected void removeFormula(Machine machine, SymbolicCheckingFormulaItem item) {
		machine.getSymbolicCheckingFormulas().remove(item);
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(SymbolicCheckingChoosingStage.class).showAndWait();
	}
	
	@Override
	protected void openItem(SymbolicCheckingFormulaItem item) {
		final SymbolicCheckingChoosingStage choosingStage = injector.getInstance(SymbolicCheckingChoosingStage.class);
		choosingStage.changeFormula(item, injector.getInstance(SymbolicCheckingResultHandler.class));
	}
		
}
