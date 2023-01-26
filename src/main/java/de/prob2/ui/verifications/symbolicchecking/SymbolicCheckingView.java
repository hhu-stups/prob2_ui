package de.prob2.ui.verifications.symbolicchecking;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.statespace.Trace;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.CliTaskExecutor;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

@FXMLInjected
@Singleton
public class SymbolicCheckingView extends SymbolicView<SymbolicCheckingFormulaItem> {
	private class SymbolicCheckingCellFactory implements Callback<TableView<SymbolicCheckingFormulaItem>, TableRow<SymbolicCheckingFormulaItem>> {
		@Override
		public TableRow<SymbolicCheckingFormulaItem> call(TableView<SymbolicCheckingFormulaItem> param) {
			TableRow<SymbolicCheckingFormulaItem> row = new TableRow<>();
			
			row.setOnMouseClicked(e -> {
				final SymbolicCheckingFormulaItem item = row.getItem();
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
				SymbolicCheckingFormulaItem item = tvFormula.getSelectionModel().getSelectedItem();
				machine.getSymbolicCheckingFormulas().remove(item);
			});
			
			MenuItem changeItem = new MenuItem(i18n.translate("symbolic.view.contextMenu.changeConfiguration"));
			changeItem.setOnAction(e -> {
				final SymbolicCheckingFormulaItem oldItem = row.getItem();
				final SymbolicCheckingChoosingStage choosingStage = injector.getInstance(SymbolicCheckingChoosingStage.class);
				choosingStage.setMachine(currentTrace.getStateSpace().getLoadedMachine());
				choosingStage.setData(oldItem);
				choosingStage.showAndWait();
				final SymbolicCheckingFormulaItem newItem = choosingStage.getResult();
				if (newItem == null) {
					// User cancelled/closed the window
					return;
				}
				final List<SymbolicCheckingFormulaItem> items = currentProject.getCurrentMachine().getSymbolicCheckingFormulas();
				final Optional<SymbolicCheckingFormulaItem> existingItem = items.stream().filter(newItem::settingsEqual).findAny();
				if (!existingItem.isPresent()) {
					items.set(items.indexOf(oldItem), newItem);
				}
				formulaHandler.handleItem(existingItem.orElse(newItem), false);
			});
			
			Menu showCounterExampleItem = new Menu(i18n.translate("verifications.symbolicchecking.view.contextMenu.showCounterExample"));
			showCounterExampleItem.setDisable(true);
			
			MenuItem showMessage = new MenuItem(i18n.translate("symbolic.view.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> row.getItem().getResultItem().showAlert(stageManager, i18n));
			
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
			
			ContextMenu contextMenu = new ContextMenu(checkItem, changeItem, removeItem, showMessage, showCounterExampleItem);
			
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
				MenuItem traceItem = new MenuItem(i18n.translate("verifications.symbolicchecking.view.contextMenu.showCounterExample.counterExample", i + 1));
				final int index = i;
				traceItem.setOnAction(e-> currentTrace.set((counterExamples.get(index))));
				counterExampleItem.getItems().add(traceItem);
			}
		}
	}

	private final StageManager stageManager;
	private final SymbolicCheckingFormulaHandler formulaHandler;

	@FXML
	private TableColumn<SymbolicCheckingFormulaItem, String> idColumn;
	@FXML
	private TableColumn<SymbolicCheckingFormulaItem, String> typeColumn;

	@Inject
	public SymbolicCheckingView(final StageManager stageManager, final I18n i18n, final CurrentTrace currentTrace,
	                            final CurrentProject currentProject, final SymbolicCheckingFormulaHandler symbolicCheckHandler,
	                            final CliTaskExecutor cliExecutor, final Injector injector) {
		super(i18n, currentTrace, currentProject, injector, cliExecutor);
		this.stageManager = stageManager;
		this.formulaHandler = symbolicCheckHandler;
		stageManager.loadFXML(this, "symbolic_checking_view.fxml");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		tvFormula.setRowFactory(new SymbolicCheckingCellFactory());
		idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
		typeColumn.setCellValueFactory(features -> i18n.translateBinding(features.getValue().getType()));
		helpButton.setHelpContent("verification", "Symbolic");
	}
	
	@Override
	protected ListProperty<SymbolicCheckingFormulaItem> formulasProperty(Machine machine) {
		return machine.symbolicCheckingFormulasProperty();
	}
	
	@FXML
	public void addFormula() {
		final SymbolicCheckingChoosingStage choosingStage = injector.getInstance(SymbolicCheckingChoosingStage.class);
		choosingStage.setMachine(currentTrace.getStateSpace().getLoadedMachine());
		choosingStage.showAndWait();
		final SymbolicCheckingFormulaItem newItem = choosingStage.getResult();
		if (newItem == null) {
			// User cancelled/closed the window
			return;
		}
		final List<SymbolicCheckingFormulaItem> items = currentProject.getCurrentMachine().getSymbolicCheckingFormulas();
		final Optional<SymbolicCheckingFormulaItem> existingItem = items.stream().filter(newItem::settingsEqual).findAny();
		if (!existingItem.isPresent()) {
			items.add(newItem);
		}
		this.formulaHandler.handleItem(existingItem.orElse(newItem), false);
	}
	
	@FXML
	public void checkMachine() {
		currentProject.getCurrentMachine().getSymbolicCheckingFormulas().forEach(item -> formulaHandler.handleItem(item, true));
	}
}
