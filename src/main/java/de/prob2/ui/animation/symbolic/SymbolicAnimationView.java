package de.prob2.ui.animation.symbolic;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;

import com.google.inject.Singleton;

import de.prob.statespace.FormalismType;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.CheckingViewBase;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;

@FXMLInjected
@Singleton
public class SymbolicAnimationView extends CheckingViewBase<SymbolicAnimationItem> {
	private final class Row extends RowBase {
		private Row() {
			executeMenuItem.setText(i18n.translate("symbolic.view.contextMenu.check"));
			
			MenuItem showMessage = new MenuItem(i18n.translate("symbolic.view.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> this.getItem().getResultItem().showAlert(stageManager, i18n));
			contextMenu.getItems().add(showMessage);

			MenuItem showStateItem = new MenuItem(i18n.translate("animation.symbolic.view.contextMenu.showFoundTrace"));
			showStateItem.setOnAction(e -> currentTrace.set(this.getItem().getExample()));
			contextMenu.getItems().add(showStateItem);

			this.itemProperty().addListener((observable, from, to) -> {
				if(to != null) {
					showMessage.disableProperty().bind(to.resultItemProperty().isNull());
					showStateItem.disableProperty().bind(to.exampleProperty().isNull());
				}
			});
		}
	}
	
	private final StageManager stageManager;
	private final I18n i18n;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final SymbolicAnimationItemHandler formulaHandler;
	private final Provider<SymbolicAnimationChoosingStage> choosingStageProvider;
	
	@FXML
	private Button addFormulaButton;
	@FXML
	private HelpButton helpButton;
	@FXML
	private TableColumn<SymbolicAnimationItem, String> typeColumn;
	
	@Inject
	public SymbolicAnimationView(final StageManager stageManager, final I18n i18n, final CurrentTrace currentTrace,
	                             final CurrentProject currentProject, final SymbolicAnimationItemHandler symbolicCheckHandler,
	                             final DisablePropertyController disablePropertyController, final Provider<SymbolicAnimationChoosingStage> choosingStageProvider) {
		super(i18n, disablePropertyController);
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.formulaHandler = symbolicCheckHandler;
		this.choosingStageProvider = choosingStageProvider;
		stageManager.loadFXML(this, "symbolic_animation_view.fxml");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		itemsTable.setRowFactory(table -> new Row());
		typeColumn.setCellValueFactory(features -> i18n.translateBinding(features.getValue().getType()));
		
		final ChangeListener<Machine> machineChangeListener = (o, from, to) -> {
			this.items.unbind();
			if (to != null) {
				this.items.bind(to.symbolicAnimationFormulasProperty());
			} else {
				this.items.clear();
			}
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());
		
		addFormulaButton.disableProperty().bind(currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B).or(disablePropertyController.disableProperty()));
		helpButton.setHelpContent("animation", "Symbolic");
	}
	
	@Override
	protected String configurationForItem(final SymbolicAnimationItem item) {
		return item.getCode();
	}
	
	@Override
	protected void executeItem(final SymbolicAnimationItem item) {
		formulaHandler.handleItem(item, false);
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
		final Optional<SymbolicAnimationItem> existingItem = items.stream().filter(newItem::settingsEqual).findAny();
		if (!existingItem.isPresent()) {
			items.add(newItem);
		}
		this.formulaHandler.handleItem(existingItem.orElse(newItem), false);
	}
	
	@Override
	protected Optional<SymbolicAnimationItem> editItem(final SymbolicAnimationItem oldItem) {
		final SymbolicAnimationChoosingStage choosingStage = choosingStageProvider.get();
		choosingStage.setMachine(currentTrace.getStateSpace().getLoadedMachine());
		choosingStage.setData(oldItem);
		choosingStage.showAndWait();
		return Optional.ofNullable(choosingStage.getResult());
	}
	
	@FXML
	public void checkMachine() {
		items.forEach(item -> formulaHandler.handleItem(item, true));
	}
}
