package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.eventb.EventBModel;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.CheckingViewBase;
import de.prob2.ui.sharedviews.InterruptIfRunningButton;
import de.prob2.ui.verifications.AbstractCheckableItem;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;

@FXMLInjected
@Singleton
public class TestCaseGenerationView extends CheckingViewBase<TestCaseGenerationItem> {
	private final class Row extends RowBase {
		private Row() {
			executeMenuItem.setText(i18n.translate("animation.testcase.view.contextMenu.generate"));

			MenuItem showDetails = new MenuItem(i18n.translate("animation.testcase.view.contextMenu.showDetails"));
			showDetails.setDisable(true);
			showDetails.setOnAction(e -> {
				TraceInformationStage stage = injector.getInstance(TraceInformationStage.class);
				stage.setResult(this.getItem().getResult());
				stage.show();
				stage.toFront();
			});
			contextMenu.getItems().add(showDetails);

			MenuItem showMessage = new MenuItem(i18n.translate("animation.testcase.view.contextMenu.showGenerationMessage"));
			showMessage.setOnAction(e -> this.getItem().getResultItem().showAlert(stageManager, i18n));
			contextMenu.getItems().add(showMessage);

			Menu showStateItem = new Menu(i18n.translate("animation.testcase.view.contextMenu.showFoundPaths"));
			showStateItem.setDisable(true);
			contextMenu.getItems().add(showStateItem);

			MenuItem saveTraces = new MenuItem(i18n.translate("animation.testcase.view.contextMenu.savePaths"));
			saveTraces.setOnAction(e -> {
				TestCaseGenerationItem item = this.getItem();
				injector.getInstance(TraceFileHandler.class).save(item, currentProject.getCurrentMachine());
			});
			contextMenu.getItems().add(saveTraces);

			this.itemProperty().addListener((observable, from, to) -> {
				final InvalidationListener updateExamplesListener = o -> showExamples(to, showStateItem);
				
				if (from != null) {
					from.examplesProperty().removeListener(updateExamplesListener);
				}
				
				if (to != null) {
					showMessage.disableProperty().bind(to.resultItemProperty().isNull());
					showStateItem.disableProperty().bind(to.examplesProperty().emptyProperty());
					to.examplesProperty().addListener(updateExamplesListener);
					updateExamplesListener.invalidated(null);
					showDetails.disableProperty().bind(to.resultProperty().isNull());
					saveTraces.disableProperty().bind(to.examplesProperty().emptyProperty());
				}
			});
		}

		private void showExamples(TestCaseGenerationItem item, Menu exampleItem) {
			exampleItem.getItems().clear();
			List<Trace> examples = item.getExamples();
			for (int i = 0; i < examples.size(); i++) {
				MenuItem traceItem = new MenuItem(i18n.translate("animation.testcase.view.contextMenu.showExample", i + 1));
				final int index = i;
				traceItem.setOnAction(e -> currentTrace.set((examples.get(index))));
				exampleItem.getItems().add(traceItem);
			}
		}
	}

	@FXML
	private HelpButton helpButton;

	@FXML
	private TableColumn<TestCaseGenerationItem, String> typeColumn;

	@FXML
	private Button addTestCaseButton;

	@FXML
	private InterruptIfRunningButton cancelButton;

	private final StageManager stageManager;

	private final I18n i18n;

	private final CurrentTrace currentTrace;

	private final CurrentProject currentProject;

	private final Injector injector;

	private final TestCaseGenerator testCaseGenerator;

	@Inject
	public TestCaseGenerationView(final StageManager stageManager, final I18n i18n, final CurrentTrace currentTrace,
	                              final CurrentProject currentProject, final DisablePropertyController disablePropertyController,
	                              final TestCaseGenerator testCaseGenerator, final Injector injector) {
		super(i18n, disablePropertyController);
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
		this.testCaseGenerator = testCaseGenerator;
		stageManager.loadFXML(this, "test_case_generation_view.fxml");
	}

	@Override
	@FXML
	public void initialize() {
		super.initialize();
		helpButton.setHelpContent("animation", "testCases");
		setBindings();
		itemsTable.setRowFactory(table -> new Row());
		final ChangeListener<Machine> machineChangeListener = (observable, oldValue, newValue) -> {
			items.unbind();
			if (newValue != null) {
				items.bind(newValue.testCasesProperty());
			} else {
				items.set(FXCollections.emptyObservableList());
			}
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());
	}

	private void setBindings() {
		final BooleanBinding partOfDisableBinding = Bindings.createBooleanBinding(() -> !(currentTrace.modelProperty().get() instanceof EventBModel) && !(currentTrace.modelProperty().get() instanceof ClassicalBModel), currentTrace.modelProperty());
		addTestCaseButton.disableProperty().bind(partOfDisableBinding.or(disablePropertyController.disableProperty()));
		cancelButton.runningProperty().bind(testCaseGenerator.runningProperty());
		cancelButton.getInterruptButton().setOnAction(e -> testCaseGenerator.interrupt());
		typeColumn.setCellValueFactory(features -> i18n.translateBinding(features.getValue().getType()));
	}

	@Override
	protected String configurationForItem(final TestCaseGenerationItem item) {
		return item.getConfigurationDescription();
	}

	@Override
	protected BooleanExpression disableItemBinding(final TestCaseGenerationItem item) {
		return testCaseGenerator.runningProperty();
	}

	@Override
	protected void executeItem(final TestCaseGenerationItem item) {
		testCaseGenerator.generateTestCases(item, currentTrace.getStateSpace());
	}

	@Override
	protected Optional<TestCaseGenerationItem> showItemDialog(final TestCaseGenerationItem oldItem) {
		TestCaseGenerationChoosingStage choosingStage = injector.getInstance(TestCaseGenerationChoosingStage.class);
		if (oldItem != null) {
			choosingStage.setItem(oldItem);
		}
		choosingStage.showAndWait();
		return Optional.ofNullable(choosingStage.getItem());
	}

	@FXML
	public void generate() {
		final StateSpace stateSpace = currentTrace.getStateSpace();
		items.stream()
			.filter(AbstractCheckableItem::selected)
			.forEach(item -> testCaseGenerator.generateTestCases(item, stateSpace));
	}
}
