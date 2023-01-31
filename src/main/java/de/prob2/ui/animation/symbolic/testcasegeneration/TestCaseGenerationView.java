package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.eventb.EventBModel;
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
import de.prob2.ui.sharedviews.InterruptIfRunningButton;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;

@FXMLInjected
@Singleton
public class TestCaseGenerationView extends CheckingViewBase<TestCaseGenerationItem> {

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

	private final TestCaseGenerationItemHandler itemHandler;

	private class TestCaseGenerationCellFactory implements Callback<TableView<TestCaseGenerationItem>, TableRow<TestCaseGenerationItem>> {
		@Override
		public TableRow<TestCaseGenerationItem> call(TableView<TestCaseGenerationItem> param) {
			TableRow<TestCaseGenerationItem> row = new TableRow<>();

			MenuItem checkItem = new MenuItem(i18n.translate("animation.testcase.view.contextMenu.generate"));
			checkItem.setDisable(true);
			checkItem.setOnAction(e -> itemHandler.handleItem(row.getItem()));

			MenuItem removeItem = new MenuItem(i18n.translate("animation.testcase.view.contextMenu.removeConfiguration"));
			removeItem.setOnAction(e -> removeFormula());

			MenuItem changeItem = new MenuItem(i18n.translate("animation.testcase.view.contextMenu.changeConfiguration"));
			changeItem.setOnAction(e -> changeItem(row.getItem()));

			MenuItem showDetails = new MenuItem(i18n.translate("animation.testcase.view.contextMenu.showDetails"));
			showDetails.setDisable(true);
			showDetails.setOnAction(e -> {
				TestCaseGenerationItem item = row.getItem();
				TraceInformationStage stage = injector.getInstance(TraceInformationStage.class);
				stage.setTraces(item.getTraceInformation());
				stage.setUncoveredOperations(item.getUncoveredOperations());
				stage.show();
				stage.toFront();
			});

			Menu showStateItem = new Menu(i18n.translate("animation.testcase.view.contextMenu.showFoundPaths"));
			showStateItem.setDisable(true);

			MenuItem showMessage = new MenuItem(i18n.translate("animation.testcase.view.contextMenu.showGenerationMessage"));
			showMessage.setOnAction(e -> row.getItem().getResultItem().showAlert(stageManager, i18n));

			MenuItem saveTraces = new MenuItem(i18n.translate("animation.testcase.view.contextMenu.savePaths"));
			saveTraces.setOnAction(e -> {
				TestCaseGenerationItem item = row.getItem();
				injector.getInstance(TestCaseGenerationResultHandler.class).saveTraces(item);
			});

			row.itemProperty().addListener((observable, from, to) -> {
				final InvalidationListener updateExamplesListener = o -> showExamples(to, showStateItem);

				if (from != null) {
					from.examplesProperty().removeListener(updateExamplesListener);
				}

				if (to != null) {
					showMessage.disableProperty().bind(to.resultItemProperty().isNull());
					showStateItem.disableProperty().bind(to.examplesProperty().emptyProperty());
					to.examplesProperty().addListener(updateExamplesListener);
					updateExamplesListener.invalidated(null);
					checkItem.disableProperty().bind(testCaseGenerator.runningProperty().or(to.selectedProperty().not()));
					showDetails.disableProperty().bind(to.examplesProperty().emptyProperty());
					saveTraces.disableProperty().bind(to.examplesProperty().emptyProperty());
				}
			});

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
							.then((ContextMenu) null)
							.otherwise(new ContextMenu(checkItem, changeItem, removeItem, showDetails, showMessage, showStateItem, saveTraces)));

			return row;
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

	@Inject
	public TestCaseGenerationView(final StageManager stageManager, final I18n i18n, final CurrentTrace currentTrace,
	                              final CurrentProject currentProject, final DisablePropertyController disablePropertyController, final TestCaseGenerationItemHandler itemHandler,
	                              final TestCaseGenerator testCaseGenerator, final Injector injector) {
		super(disablePropertyController);
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
		this.itemHandler = itemHandler;
		this.testCaseGenerator = testCaseGenerator;
		stageManager.loadFXML(this, "test_case_generation_view.fxml");
	}

	@Override
	@FXML
	public void initialize() {
		super.initialize();
		helpButton.setHelpContent("animation", "testCases");
		setBindings();
		itemsTable.setRowFactory(new TestCaseGenerationCellFactory());
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
		itemsTable.setOnMouseClicked(e -> {
			TestCaseGenerationItem item = itemsTable.getSelectionModel().getSelectedItem();
			if (e.getClickCount() == 2 && item != null && currentTrace.get() != null) {
				itemHandler.handleItem(item);
			}
		});
	}

	@Override
	protected String configurationForItem(final TestCaseGenerationItem item) {
		return item.getConfigurationDescription();
	}

	@FXML
	public void addTestCase() {
		final TestCaseGenerationChoosingStage choosingStage = injector.getInstance(TestCaseGenerationChoosingStage.class);
		choosingStage.showAndWait();
		final TestCaseGenerationItem newItem = choosingStage.getItem();
		if (newItem == null) {
			return;
		}
		final Optional<TestCaseGenerationItem> existingItem = itemHandler.addItem(newItem);
		itemHandler.generateTestCases(existingItem.orElse(newItem));
	}

	private void removeFormula() {
		Machine machine = currentProject.getCurrentMachine();
		TestCaseGenerationItem item = itemsTable.getSelectionModel().getSelectedItem();
		machine.getTestCases().remove(item);
	}

	private void changeItem(TestCaseGenerationItem item) {
		TestCaseGenerationChoosingStage choosingStage = injector.getInstance(TestCaseGenerationChoosingStage.class);
		choosingStage.setItem(item);
		choosingStage.showAndWait();
		final TestCaseGenerationItem newItem = choosingStage.getItem();
		if (newItem == null) {
			return;
		}
		if (!itemHandler.replaceItem(item, newItem).isPresent()) {
			itemHandler.generateTestCases(newItem);
		} else {
			stageManager.makeAlert(Alert.AlertType.INFORMATION, 
				"verifications.abstractResultHandler.alerts.alreadyExists.header",
				"verifications.abstractResultHandler.alerts.alreadyExists.content.configuration").show();
		}
	}

	@FXML
	public void generate() {
		Machine machine = currentProject.getCurrentMachine();
		itemHandler.handleMachine(machine);
	}
}
