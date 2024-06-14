package de.prob2.ui.animation.symbolic.testcasegeneration;

import java.util.List;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.analysis.testcasegeneration.TestCaseGeneratorResult;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.eventb.EventBModel;
import de.prob.statespace.Trace;
import de.prob2.ui.animation.tracereplay.TraceFileHandler;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.CheckingViewBase;
import de.prob2.ui.verifications.ExecutionContext;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.input.MouseButton;

@FXMLInjected
@Singleton
public final class TestCaseGenerationView extends CheckingViewBase<TestCaseGenerationItem> {
	private final class Row extends RowBase {
		private Row() {
			executeMenuItem.setText(i18n.translate("animation.testcase.view.contextMenu.generate"));

			MenuItem showDetails = new MenuItem(i18n.translate("animation.testcase.view.contextMenu.showDetails"));
			showDetails.setDisable(true);
			showDetails.setOnAction(e -> showDetails(this.getItem().getGeneratorResult()));
			this.setOnMouseClicked(e -> {
				if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
					if (this.getItem().getGeneratorResult() != null && !this.getItem().getGeneratorResult().getTestTraces().isEmpty()) {
						showDetails(this.getItem().getGeneratorResult());
					} else {
						executeItem(this.getItem());
					}
				}
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
					showDetails.disableProperty().bind(to.generatorResultProperty().isNull());
					saveTraces.disableProperty().bind(to.examplesProperty().emptyProperty());
				}
			});

		}
		private void showDetails(TestCaseGeneratorResult result) {
			TraceInformationStage stage = injector.getInstance(TraceInformationStage.class);
			stage.setResult(this.getItem().getGeneratorResult());
			stage.show();
			stage.toFront();
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

	private final StageManager stageManager;

	private final I18n i18n;

	private final CurrentTrace currentTrace;

	private final CurrentProject currentProject;

	private final Injector injector;

	@Inject
	public TestCaseGenerationView(final StageManager stageManager, final I18n i18n, final CurrentTrace currentTrace,
	                              final CurrentProject currentProject, final DisablePropertyController disablePropertyController,
	                              final CliTaskExecutor cliExecutor, final Injector injector) {
		super(i18n, disablePropertyController, currentTrace, currentProject, cliExecutor);
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.injector = injector;
		stageManager.loadFXML(this, "test_case_generation_view.fxml");
	}

	@Override
	protected ObservableList<TestCaseGenerationItem> getItemsProperty(Machine machine) {
		return machine.getTestCases();
	}

	@Override
	@FXML
	public void initialize() {
		super.initialize();
		helpButton.setHelpContent("animation", "testCases");
		setBindings();
		itemsTable.setRowFactory(table -> new Row());
	}

	private void setBindings() {
		final BooleanBinding partOfDisableBinding = Bindings.createBooleanBinding(() -> !(currentTrace.modelProperty().get() instanceof EventBModel) && !(currentTrace.modelProperty().get() instanceof ClassicalBModel), currentTrace.modelProperty());
		addTestCaseButton.disableProperty().bind(partOfDisableBinding.or(disablePropertyController.disableProperty()));
		typeColumn.setCellValueFactory(features -> new SimpleStringProperty(features.getValue().getTaskType(i18n)));
	}

	@Override
	protected void executeItemSync(final TestCaseGenerationItem item, final ExecutionContext context) {
		item.execute(context);
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
}
