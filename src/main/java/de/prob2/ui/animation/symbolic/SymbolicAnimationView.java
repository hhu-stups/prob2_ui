package de.prob2.ui.animation.symbolic;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;

import com.google.inject.Singleton;

import de.prob2.ui.internal.DisablePropertyController;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

@FXMLInjected
@Singleton
public class SymbolicAnimationView extends SymbolicView<SymbolicAnimationItem> {
	private class SymbolicAnimationCellFactory implements Callback<TableView<SymbolicAnimationItem>, TableRow<SymbolicAnimationItem>> {
		@Override
		public TableRow<SymbolicAnimationItem> call(TableView<SymbolicAnimationItem> param) {
			TableRow<SymbolicAnimationItem> row = new TableRow<>();
			
			row.setOnMouseClicked(e -> {
				final SymbolicAnimationItem item = row.getItem();
				if(e.getClickCount() == 2 && item != null && currentTrace.get() != null) {
					formulaHandler.handleItem(item, false);
				}
			});
			
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
				final SymbolicAnimationItem oldItem = row.getItem();
				final SymbolicAnimationChoosingStage choosingStage = choosingStageProvider.get();
				choosingStage.setMachine(currentTrace.getStateSpace().getLoadedMachine());
				choosingStage.setData(oldItem);
				choosingStage.showAndWait();
				final SymbolicAnimationItem newItem = choosingStage.getResult();
				if (newItem == null) {
					// User cancelled/closed the window
					return;
				}
				final List<SymbolicAnimationItem> items = currentProject.getCurrentMachine().getSymbolicAnimationFormulas();
				final Optional<SymbolicAnimationItem> existingItem = items.stream().filter(newItem::settingsEqual).findAny();
				if (!existingItem.isPresent()) {
					items.set(items.indexOf(oldItem), newItem);
				}
				formulaHandler.handleItem(existingItem.orElse(newItem), false);
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
	private final SymbolicAnimationItemHandler formulaHandler;
	private final Provider<SymbolicAnimationChoosingStage> choosingStageProvider;
	
	@FXML
	private TableColumn<SymbolicAnimationItem, String> typeColumn;
	@FXML
	private TableColumn<SymbolicAnimationItem, String> configurationColumn;
	
	@Inject
	public SymbolicAnimationView(final StageManager stageManager, final I18n i18n, final CurrentTrace currentTrace,
	                             final CurrentProject currentProject, final SymbolicAnimationItemHandler symbolicCheckHandler,
	                             final CliTaskExecutor cliExecutor, final DisablePropertyController disablePropertyController, final Provider<SymbolicAnimationChoosingStage> choosingStageProvider) {
		super(i18n, currentTrace, currentProject, disablePropertyController, cliExecutor);
		this.stageManager = stageManager;
		this.formulaHandler = symbolicCheckHandler;
		this.choosingStageProvider = choosingStageProvider;
		stageManager.loadFXML(this, "symbolic_animation_view.fxml");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		tvFormula.setRowFactory(new SymbolicAnimationCellFactory());
		typeColumn.setCellValueFactory(features -> i18n.translateBinding(features.getValue().getType()));
		configurationColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
		helpButton.setHelpContent("animation", "Symbolic");
	}
	
	@Override
	protected ListProperty<SymbolicAnimationItem> formulasProperty(Machine machine) {
		return machine.symbolicAnimationFormulasProperty();
	}
	
	@FXML
	public void addFormula() {
		final SymbolicAnimationChoosingStage choosingStage = choosingStageProvider.get();
		choosingStage.setMachine(currentTrace.getStateSpace().getLoadedMachine());
		choosingStage.showAndWait();
		final SymbolicAnimationItem newItem = choosingStage.getResult();
		if (newItem == null) {
			// User cancelled/closed the window
			return;
		}
		final List<SymbolicAnimationItem> items = currentProject.getCurrentMachine().getSymbolicAnimationFormulas();
		final Optional<SymbolicAnimationItem> existingItem = items.stream().filter(newItem::settingsEqual).findAny();
		if (!existingItem.isPresent()) {
			items.add(newItem);
		}
		this.formulaHandler.handleItem(existingItem.orElse(newItem), false);
	}
	
	@FXML
	public void checkMachine() {
		currentProject.getCurrentMachine().getSymbolicAnimationFormulas().forEach(item -> formulaHandler.handleItem(item, true));
	}
}
