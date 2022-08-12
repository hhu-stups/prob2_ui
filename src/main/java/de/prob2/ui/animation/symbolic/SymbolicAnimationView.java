package de.prob2.ui.animation.symbolic;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.symbolic.SymbolicView;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;

@FXMLInjected
@Singleton
public class SymbolicAnimationView extends SymbolicView<SymbolicAnimationItem> {
	private class SymbolicAnimationCellFactory implements Callback<TableView<SymbolicAnimationItem>, TableRow<SymbolicAnimationItem>> {
		@Override
		public TableRow<SymbolicAnimationItem> call(TableView<SymbolicAnimationItem> param) {
			TableRow<SymbolicAnimationItem> row = new TableRow<>();
			
			MenuItem checkItem = new MenuItem(i18n.translate("symbolic.view.contextMenu.check"));
			checkItem.setDisable(true);
			checkItem.setOnAction(e -> formulaHandler.handleItem(row.getItem(), false));
			
			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					checkItem.disableProperty().bind(cliExecutor.runningProperty().or(to.selectedProperty().not()));
				}
			});

			MenuItem removeItem = new MenuItem(i18n.translate("symbolic.view.contextMenu.removeConfiguration"));
			removeItem.setOnAction(e -> {
				Machine machine = currentProject.getCurrentMachine();
				SymbolicAnimationItem item = tvFormula.getSelectionModel().getSelectedItem();
				machine.getSymbolicAnimationFormulas().remove(item);
			});
			
			MenuItem changeItem = new MenuItem(i18n.translate("symbolic.view.contextMenu.changeConfiguration"));
			changeItem.setOnAction(e -> {
				final SymbolicAnimationChoosingStage choosingStage = injector.getInstance(SymbolicAnimationChoosingStage.class);
				choosingStage.changeFormula(row.getItem());
			});
			

			MenuItem showStateItem = new MenuItem(i18n.translate("animation.symbolic.view.contextMenu.showFoundTrace"));
			showStateItem.setOnAction(e -> currentTrace.set(row.getItem().getExample()));
			
			MenuItem showMessage = new MenuItem(i18n.translate("symbolic.view.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> row.getItem().getResultItem().showAlert(stageManager, i18n));


			row.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					showMessage.disableProperty().bind(to.resultItemProperty().isNull());
					showStateItem.disableProperty().bind(to.exampleProperty().isNull());
				}
			});
			
			ContextMenu contextMenu = new ContextMenu(checkItem, changeItem, removeItem, showMessage, showStateItem);
			
			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
					.then((ContextMenu) null)
					.otherwise(contextMenu));			

			return row;
		}
	}
	
	private final StageManager stageManager;
	
	@Inject
	public SymbolicAnimationView(final StageManager stageManager, final I18n i18n, final CurrentTrace currentTrace,
	                             final CurrentProject currentProject, final SymbolicAnimationItemHandler symbolicCheckHandler,
	                             final CliTaskExecutor cliExecutor, final Injector injector) {
		super(i18n, currentTrace, currentProject, injector, cliExecutor, symbolicCheckHandler);
		this.stageManager = stageManager;
		stageManager.loadFXML(this, "symbolic_animation_view.fxml");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		tvFormula.setRowFactory(new SymbolicAnimationCellFactory());
		helpButton.setHelpContent("animation", "Symbolic");
	}
	
	@Override
	protected ListProperty<SymbolicAnimationItem> formulasProperty(Machine machine) {
		return machine.symbolicAnimationFormulasProperty();
	}
	
	@FXML
	public void addFormula() {
		injector.getInstance(SymbolicAnimationChoosingStage.class).showAndWait();
	}
	
	@FXML
	public void checkMachine() {
		currentProject.getCurrentMachine().getSymbolicAnimationFormulas().forEach(item -> formulaHandler.handleItem(item, true));
	}
}
