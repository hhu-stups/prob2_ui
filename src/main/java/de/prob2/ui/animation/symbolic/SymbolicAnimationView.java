package de.prob2.ui.animation.symbolic;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.inject.Inject;
import com.google.inject.Provider;
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
import de.prob2.ui.verifications.CheckingExecutors;
import de.prob2.ui.verifications.ExecutionContext;
import de.prob2.ui.verifications.ICheckingResult;
import de.prob2.ui.verifications.TraceResult;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;

@FXMLInjected
@Singleton
public final class SymbolicAnimationView extends CheckingViewBase<SymbolicAnimationItem> {
	private final class Row extends RowBase {
		private Row() {
			executeMenuItem.setText(i18n.translate("symbolic.view.contextMenu.check"));
			
			MenuItem showMessage = new MenuItem(i18n.translate("symbolic.view.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> this.getItem().getResult().showAlert(stageManager, i18n));
			contextMenu.getItems().add(showMessage);

			MenuItem showStateItem = new MenuItem(i18n.translate("animation.symbolic.view.contextMenu.showFoundTrace"));
			showStateItem.setOnAction(e -> {
				SymbolicAnimationItem task = itemsTable.getSelectionModel().getSelectedItem();
				currentTrace.set(((TraceResult)task.getResult()).getTrace());
			});
			contextMenu.getItems().add(showStateItem);

			ChangeListener<ICheckingResult> resultListener = (o, from, to) -> {
				showMessage.setDisable(to == null);
				showStateItem.setDisable(!(to instanceof TraceResult traceResult) || traceResult.getTraces().isEmpty());
			};

			this.itemProperty().addListener((observable, from, to) -> {
				if (from != null) {
					from.resultProperty().removeListener(resultListener);
				}
				if (to != null) {
					to.resultProperty().addListener(resultListener);
					resultListener.changed(null, null, to.getResult());
				}
			});
		}
	}
	
	private final StageManager stageManager;
	private final I18n i18n;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;
	private final Provider<SymbolicAnimationChoosingStage> choosingStageProvider;
	
	@FXML
	private Button addFormulaButton;
	@FXML
	private HelpButton helpButton;
	@FXML
	private TableColumn<SymbolicAnimationItem, String> typeColumn;
	
	@Inject
	public SymbolicAnimationView(final StageManager stageManager, final I18n i18n, final CurrentTrace currentTrace,
	                             final CurrentProject currentProject, final CheckingExecutors checkingExecutors,
	                             final DisablePropertyController disablePropertyController, final Provider<SymbolicAnimationChoosingStage> choosingStageProvider) {
		super(stageManager, i18n, disablePropertyController, currentTrace, currentProject, checkingExecutors);
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.choosingStageProvider = choosingStageProvider;
		stageManager.loadFXML(this, "symbolic_animation_view.fxml");
	}

	@Override
	protected ObservableList<SymbolicAnimationItem> getItemsProperty(Machine machine) {
		return machine.getSymbolicAnimationFormulas();
	}

	@Override
	public void initialize() {
		super.initialize();
		itemsTable.setRowFactory(table -> new Row());
		typeColumn.setCellValueFactory(features -> new SimpleStringProperty(features.getValue().getTaskType(i18n)));
		
		addFormulaButton.disableProperty().bind(currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B).or(disablePropertyController.disableProperty()));
		helpButton.setHelpContent("animation", "Symbolic");
	}
	
	@Override
	protected CompletableFuture<?> executeItemImpl(SymbolicAnimationItem item, CheckingExecutors executors, ExecutionContext context) {
		return super.executeItemImpl(item, executors, context).thenApply(res -> {
			if (item.getResult() instanceof TraceResult traceResult && !traceResult.getTraces().isEmpty()) {
				currentTrace.set(traceResult.getTrace());
			}
			return res;
		});
	}
	
	@Override
	protected Optional<SymbolicAnimationItem> showItemDialog(final SymbolicAnimationItem oldItem) {
		final SymbolicAnimationChoosingStage choosingStage = choosingStageProvider.get();
		choosingStage.setMachine(currentTrace.getStateSpace().getLoadedMachine());
		if (oldItem != null) {
			choosingStage.setData(oldItem);
		}
		choosingStage.showAndWait();
		return Optional.ofNullable(choosingStage.getResult());
	}
}
