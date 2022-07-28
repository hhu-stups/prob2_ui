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
import de.prob2.ui.internal.AbstractResultHandler;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.verifications.Checked;
import de.prob2.ui.verifications.CheckedCell;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ItemSelectedFactory;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

@FXMLInjected
@Singleton
public class TestCaseGenerationView extends ScrollPane {

	@FXML
	private HelpButton helpButton;

	@FXML
	private TableView<TestCaseGenerationItem> tvTestCases;

	@FXML
	private TableColumn<TestCaseGenerationItem, Checked> statusColumn;

	@FXML
	private TableColumn<TestCaseGenerationItem, String> configurationColumn;

	@FXML
	private TableColumn<TestCaseGenerationItem, String> typeColumn;

	@FXML
	private TableColumn<IExecutableItem, CheckBox> shouldExecuteColumn;

	@FXML
	private Button addTestCaseButton;

	@FXML
	private Button generateButton;

	@FXML
	private Button cancelButton;

	private final StageManager stageManager;

	private final I18n i18n;

	private final CurrentTrace currentTrace;

	private final CurrentProject currentProject;

	private final Injector injector;

	private final TestCaseGenerator testCaseGenerator;

	private final TestCaseGenerationItemHandler itemHandler;

	private final CheckBox selectAll;

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
	                              final CurrentProject currentProject, final TestCaseGenerationItemHandler itemHandler,
	                              final TestCaseGenerator testCaseGenerator, final Injector injector) {
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
		this.itemHandler = itemHandler;
		this.testCaseGenerator = testCaseGenerator;
		this.selectAll = new CheckBox();
		stageManager.loadFXML(this, "test_case_generation_view.fxml");
	}

	@FXML
	public void initialize() {
		helpButton.setHelpContent("animation", "Symbolic");
		setBindings();
		tvTestCases.setRowFactory(new TestCaseGenerationCellFactory());
		final ChangeListener<Machine> machineChangeListener = (observable, oldValue, newValue) -> {
			tvTestCases.itemsProperty().unbind();
			if (newValue != null) {
				tvTestCases.itemsProperty().bind(newValue.testCasesProperty());
			} else {
				tvTestCases.setItems(FXCollections.emptyObservableList());
			}
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());
	}

	private void setBindings() {
		final BooleanBinding partOfDisableBinding = Bindings.createBooleanBinding(() -> !(currentTrace.modelProperty().get() instanceof EventBModel) && !(currentTrace.modelProperty().get() instanceof ClassicalBModel), currentTrace.modelProperty());
		addTestCaseButton.disableProperty().bind(partOfDisableBinding.or(injector.getInstance(DisablePropertyController.class).disableProperty()));
		final BooleanProperty noTestCases = new SimpleBooleanProperty();
		currentProject.currentMachineProperty().addListener((o, from, to) -> {
			noTestCases.unbind();
			if (to != null) {
				noTestCases.bind(to.testCasesProperty().emptyProperty());
			} else {
				noTestCases.set(true);
			}
		});
		generateButton.disableProperty().bind(partOfDisableBinding.or(noTestCases.or(selectAll.selectedProperty().not().or(injector.getInstance(DisablePropertyController.class).disableProperty()))));
		cancelButton.disableProperty().bind(testCaseGenerator.runningProperty().not());
		statusColumn.setCellFactory(col -> new CheckedCell<>());
		statusColumn.setCellValueFactory(new PropertyValueFactory<>("checked"));
		configurationColumn.setCellValueFactory(new PropertyValueFactory<>("configurationDescription"));
		typeColumn.setCellValueFactory(features -> i18n.translateBinding(features.getValue().getType()));
		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory(tvTestCases, selectAll));
		shouldExecuteColumn.setGraphic(selectAll);
		tvTestCases.setOnMouseClicked(e -> {
			TestCaseGenerationItem item = tvTestCases.getSelectionModel().getSelectedItem();
			if (e.getClickCount() == 2 && item != null && currentTrace.get() != null) {
				itemHandler.handleItem(item);
			}
		});
		tvTestCases.disableProperty().bind(partOfDisableBinding.or(currentTrace.isNull().or(injector.getInstance(DisablePropertyController.class).disableProperty())));
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
		if (choosingStage.isCheckRequested()) {
			itemHandler.generateTestCases(existingItem.orElse(newItem));
		}
	}

	private void removeFormula() {
		Machine machine = currentProject.getCurrentMachine();
		TestCaseGenerationItem item = tvTestCases.getSelectionModel().getSelectedItem();
		machine.getTestCases().remove(item);
	}

	private void changeItem(TestCaseGenerationItem item) {
		TestCaseGenerationChoosingStage choosingStage = injector.getInstance(TestCaseGenerationChoosingStage.class);
		choosingStage.setItem(item);
		choosingStage.useChangeButtons();
		choosingStage.showAndWait();
		final TestCaseGenerationItem newItem = choosingStage.getItem();
		if (newItem == null) {
			return;
		}
		if (!itemHandler.replaceItem(item, newItem).isPresent()) {
			if (choosingStage.isCheckRequested()) {
				itemHandler.generateTestCases(newItem);
			}
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

	@FXML
	public void cancel() {
		testCaseGenerator.interrupt();
	}
}
