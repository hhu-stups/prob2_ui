package de.prob2.ui.verifications.symbolicchecking;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.inject.Inject;
import com.google.inject.Provider;
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
import de.prob2.ui.verifications.CheckingExecutors;
import de.prob2.ui.verifications.ExecutionContext;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;

@FXMLInjected
@Singleton
public final class SymbolicCheckingView extends CheckingViewBase<SymbolicCheckingFormulaItem> {
	private final class Row extends RowBase {
		private Row() {
			executeMenuItem.setText(i18n.translate("symbolic.view.contextMenu.check"));

			MenuItem showMessage = new MenuItem(i18n.translate("symbolic.view.contextMenu.showCheckingMessage"));
			showMessage.setOnAction(e -> this.getItem().getResult().showAlert(stageManager, i18n));
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
					showMessage.disableProperty().bind(to.resultProperty().isNull());
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
	private final Provider<SymbolicCheckingChoosingStage> choosingStageProvider;

	@FXML
	private Button addFormulaButton;
	@FXML
	private HelpButton helpButton;
	@FXML
	private TableColumn<SymbolicCheckingFormulaItem, String> typeColumn;

	@Inject
	public SymbolicCheckingView(final StageManager stageManager, final I18n i18n, final CurrentTrace currentTrace,
	                            final CurrentProject currentProject, final CheckingExecutors checkingExecutors,
	                            final DisablePropertyController disablePropertyController, final Provider<SymbolicCheckingChoosingStage> choosingStageProvider) {
		super(stageManager, i18n, disablePropertyController, currentTrace, currentProject, checkingExecutors);
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.choosingStageProvider = choosingStageProvider;
		stageManager.loadFXML(this, "symbolic_checking_view.fxml");
	}

	@Override
	protected ObservableList<SymbolicCheckingFormulaItem> getItemsProperty(Machine machine) {
		return machine.getSymbolicCheckingFormulas();
	}

	@Override
	public void initialize() {
		super.initialize();

		addFormulaButton.disableProperty().bind(currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B).or(disablePropertyController.disableProperty()));

		itemsTable.setRowFactory(table -> new Row());
		typeColumn.setCellValueFactory(features -> new SimpleStringProperty(features.getValue().getTaskType(i18n)));

		helpButton.setHelpContent("verification", "Symbolic");
	}

	@Override
	protected CompletableFuture<?> executeItemImpl(SymbolicCheckingFormulaItem item, CheckingExecutors executors, ExecutionContext context) {
		return super.executeItemImpl(item, executors, context).thenApply(res -> {
			List<Trace> counterExamples = item.getCounterExamples();
			if (!counterExamples.isEmpty()) {
				currentTrace.set(counterExamples.get(0));
			}
			return res;
		});
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
}
