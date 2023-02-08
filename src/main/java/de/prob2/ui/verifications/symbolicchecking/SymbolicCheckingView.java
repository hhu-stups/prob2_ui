package de.prob2.ui.verifications.symbolicchecking;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;

import com.google.inject.Singleton;

import de.prob.statespace.FormalismType;
import de.prob.statespace.Trace;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.CheckingViewBase;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;

@FXMLInjected
@Singleton
public class SymbolicCheckingView extends CheckingViewBase<SymbolicCheckingFormulaItem> {
	private final class Row extends RowBase {
		private Row() {
			executeMenuItem.setText(i18n.translate("symbolic.view.contextMenu.check"));
			
			MenuItem showMessage = new MenuItem(i18n.translate("symbolic.view.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> this.getItem().getResultItem().showAlert(stageManager, i18n));
			contextMenu.getItems().add(showMessage);
			
			Menu showCounterExampleItem = new Menu(i18n.translate("verifications.symbolicchecking.view.contextMenu.showCounterExample"));
			showCounterExampleItem.setDisable(true);
			contextMenu.getItems().add(showCounterExampleItem);
			
			this.itemProperty().addListener((observable, from, to) -> {
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
	private final I18n i18n;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final SymbolicCheckingFormulaHandler formulaHandler;
	private final Provider<SymbolicCheckingChoosingStage> choosingStageProvider;

	@FXML
	private Button addFormulaButton;
	@FXML
	private HelpButton helpButton;
	@FXML
	private TableColumn<SymbolicCheckingFormulaItem, String> typeColumn;

	@Inject
	public SymbolicCheckingView(final StageManager stageManager, final I18n i18n, final CurrentTrace currentTrace,
	                            final CurrentProject currentProject, final SymbolicCheckingFormulaHandler symbolicCheckHandler,
	                            final DisablePropertyController disablePropertyController, final Provider<SymbolicCheckingChoosingStage> choosingStageProvider) {
		super(i18n, disablePropertyController);
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.formulaHandler = symbolicCheckHandler;
		this.choosingStageProvider = choosingStageProvider;
		stageManager.loadFXML(this, "symbolic_checking_view.fxml");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		addFormulaButton.disableProperty().bind(currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B).or(disablePropertyController.disableProperty()));
		
		itemsTable.setRowFactory(table -> new Row());
		typeColumn.setCellValueFactory(features -> i18n.translateBinding(features.getValue().getType()));
		
		final ChangeListener<Machine> machineChangeListener = (o, from, to) -> {
			this.items.unbind();
			if (to != null) {
				this.items.bind(to.symbolicCheckingFormulasProperty());
			} else {
				this.items.clear();
			}
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());
		
		helpButton.setHelpContent("verification", "Symbolic");
	}
	
	@Override
	protected String configurationForItem(final SymbolicCheckingFormulaItem item) {
		return item.getCode();
	}
	
	@Override
	protected void executeItem(final SymbolicCheckingFormulaItem item) {
		formulaHandler.handleItem(item, false);
	}
	
	@Override
	protected Optional<SymbolicCheckingFormulaItem> showItemDialog(final SymbolicCheckingFormulaItem oldItem) {
		final SymbolicCheckingChoosingStage choosingStage = choosingStageProvider.get();
		choosingStage.setMachine(currentTrace.getStateSpace().getLoadedMachine());
		if (oldItem != null) {
			choosingStage.setData(oldItem);
		}
		choosingStage.showAndWait();
		return Optional.ofNullable(choosingStage.getResult());
	}
	
	@FXML
	public void checkMachine() {
		items.forEach(item -> formulaHandler.handleItem(item, true));
	}
}
