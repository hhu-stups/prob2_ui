package de.prob2.ui.animation.symbolic;


import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.statespace.Trace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicView;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;

import javax.inject.Inject;
import java.util.List;
import java.util.ResourceBundle;


@FXMLInjected
@Singleton
public class SymbolicAnimationView extends SymbolicView<SymbolicAnimationItem> {
	
	private class SymbolicAnimationCellFactory extends SymbolicCellFactory implements Callback<TableView<SymbolicAnimationItem>, TableRow<SymbolicAnimationItem>>{
	
		@Override
		public TableRow<SymbolicAnimationItem> call(TableView<SymbolicAnimationItem> param) {
			TableRow<SymbolicAnimationItem> row = createRow();

			MenuItem removeItem = new MenuItem(bundle.getString("symbolic.view.contextMenu.removeConfiguration"));
			removeItem.setOnAction(e -> removeFormula());
			
			MenuItem changeItem = new MenuItem(bundle.getString("symbolic.view.contextMenu.changeConfiguration"));
			changeItem.setOnAction(e->openItem(row.getItem()));
			

			Menu showStateItem = new Menu(bundle.getString("animation.symbolic.view.contextMenu.showFoundPaths"));
			showStateItem.setDisable(true);
			
			MenuItem showMessage = new MenuItem(bundle.getString("symbolic.view.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> injector.getInstance(SymbolicAnimationResultHandler.class).showResult(row.getItem()));


			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					showMessage.disableProperty().bind(to.resultItemProperty().isNull());
					showStateItem.disableProperty().bind(to.examplesProperty().emptyProperty());
					showExamples(to, showStateItem);
				}
			});
			
			ContextMenu contextMenu = row.getContextMenu();
			contextMenu.getItems().addAll(changeItem, removeItem, showMessage, showStateItem);
			
			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
					.then((ContextMenu) null)
					.otherwise(contextMenu));			

			return row;
		}

		private void showExamples(SymbolicAnimationItem item, Menu exampleItem) {
			exampleItem.getItems().clear();
			List<Trace> examples = item.getExamples();
			for(int i = 0; i < examples.size(); i++) {
				MenuItem traceItem = new MenuItem(String.format(bundle.getString("animation.symbolic.view.contextMenu.showExample"), i + 1));
				final int index = i;
				traceItem.setOnAction(e-> currentTrace.set((examples.get(index))));
				exampleItem.getItems().add(traceItem);
			}
		}
	}
	
	@Inject
	public SymbolicAnimationView(final StageManager stageManager, final ResourceBundle bundle, final CurrentTrace currentTrace, 
					final CurrentProject currentProject, final SymbolicAnimationItemHandler symbolicCheckHandler, 
					final SymbolicAnimationChecker symbolicChecker, final Injector injector) {
		super(bundle, currentTrace, currentProject, injector, symbolicChecker, symbolicCheckHandler, SymbolicAnimationItem.class);
		stageManager.loadFXML(this, "symbolic_animation_view.fxml");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		helpButton.setHelpContent("animation", "Symbolic");
	}
	
	protected ListProperty<SymbolicAnimationItem> formulasProperty(Machine machine) {
		return machine.symbolicAnimationFormulasProperty();
	}
	
	@Override
	protected void removeFormula(Machine machine, SymbolicAnimationItem item) {
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
	protected void openItem(SymbolicAnimationItem item) {
		SymbolicAnimationInput formulaInput = injector.getInstance(SymbolicAnimationInput.class);
		formulaInput.changeFormula(item, injector.getInstance(SymbolicAnimationView.class),
				injector.getInstance(SymbolicAnimationResultHandler.class), injector.getInstance(SymbolicAnimationChoosingStage.class));
	}
		
}
