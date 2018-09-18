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
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;

import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;


@Singleton
public class SymbolicAnimationView extends SymbolicView<SymbolicAnimationFormulaItem> {
	
	private class SymbolicAnimationCellFactory extends SymbolicCellFactory implements Callback<TableView<SymbolicAnimationFormulaItem>, TableRow<SymbolicAnimationFormulaItem>>{
	
		@Override
		public TableRow<SymbolicAnimationFormulaItem> call(TableView<SymbolicAnimationFormulaItem> param) {
			TableRow<SymbolicAnimationFormulaItem> row = createRow();
			
			MenuItem showStateItem = new MenuItem(bundle.getString("verifications.symbolicchecking.view.contextMenu.showFoundState"));
			showStateItem.setDisable(true);
			
			MenuItem showMessage = new MenuItem(bundle.getString("verifications.symbolicchecking.view.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> injector.getInstance(SymbolicAnimationResultHandler.class).showResult(row.getItem()));
			
			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					showMessage.disableProperty().bind(to.resultItemProperty().isNull()
							.or(Bindings.createBooleanBinding(() -> to.getResultItem() != null && Checked.SUCCESS == to.getResultItem().getChecked(), to.resultItemProperty())));
					showStateItem.disableProperty().bind(row.emptyProperty()
							.or(to.exampleProperty().isNull()));
					if(to.getExample() != null) {
						showStateItem.setOnAction(event-> currentTrace.set(to.getExample()));
					}
				}
			});
			row.getContextMenu().getItems().addAll(showMessage, showStateItem);
			return row;
		}
	}
	
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
	
	@Override
	protected void setContextMenu() {
		tvFormula.setRowFactory(new SymbolicAnimationCellFactory());
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(SymbolicAnimationChoosingStage.class).reset();
		injector.getInstance(SymbolicAnimationChoosingStage.class).showAndWait();
	}

	@Override
	protected void openItem(SymbolicAnimationFormulaItem item) {
		SymbolicAnimationFormulaInput formulaInput = injector.getInstance(SymbolicAnimationFormulaInput.class);
		formulaInput.changeFormula(item);
	}
		
}
