package de.prob2.ui.animation.tracereplay;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.statespace.FormalismType;
import de.prob2.ui.animation.tracereplay.refactoring.TraceRefactoredSetup;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.config.FileChooserManager.Kind;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.DescriptionView;
import de.prob2.ui.sharedviews.RefactorButton;
import de.prob2.ui.sharedviews.TraceViewHandler;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ItemSelectedFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;

@FXMLInjected
@Singleton
public class TraceReplayView extends ScrollPane {
	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final TraceChecker traceChecker;
	private final ResourceBundle bundle;
	private final FileChooserManager fileChooserManager;
	private final Injector injector;
	private final TraceFileHandler traceFileHandler;
	private final TraceViewHandler traceViewHandler;
	private final CheckBox selectAll;

	@FXML
	private TableView<ReplayTrace> traceTableView;
	@FXML
	private TableColumn<ReplayTrace, Node> statusColumn;
	@FXML
	private TableColumn<ReplayTrace, String> nameColumn;
	@FXML
	private Button checkButton;
	@FXML
	private Button cancelButton;
	@FXML
	private Button loadTraceButton;
	@FXML
	private HelpButton helpButton;
	@FXML
	private RefactorButton refactorButton;

	@FXML
	private TableColumn<IExecutableItem, CheckBox> shouldExecuteColumn;
	@FXML
	private SplitPane splitPane;
	private boolean showDescription;

	@Inject
	private TraceReplayView(final StageManager stageManager, final CurrentProject currentProject,
							final CurrentTrace currentTrace, final TraceChecker traceChecker, final ResourceBundle bundle,
							final FileChooserManager fileChooserManager, final Injector injector, final TraceFileHandler traceFileHandler, final TraceViewHandler traceViewHandler) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.traceChecker = traceChecker;
		this.bundle = bundle;
		this.fileChooserManager = fileChooserManager;
		this.injector = injector;
		this.traceFileHandler = traceFileHandler;
		this.traceViewHandler = traceViewHandler;
		this.selectAll = new CheckBox();
		stageManager.loadFXML(this, "trace_replay_view.fxml");
	}

	@FXML
	private void initialize() {
		helpButton.setHelpContent("animation", "Trace");
		traceTableView.itemsProperty().bind(traceViewHandler.getTraces());
		initTableColumns();
		initTableRows();

		final BooleanBinding partOfDisableBinding = currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B);
		loadTraceButton.disableProperty().bind(partOfDisableBinding.or(currentProject.currentMachineProperty().isNull()));
		cancelButton.disableProperty().bind(traceChecker.runningProperty().not());
		checkButton.disableProperty().bind(partOfDisableBinding.or(currentTrace.isNull().or(traceViewHandler.getTraces().emptyProperty().or(selectAll.selectedProperty().not().or(injector.getInstance(DisablePropertyController.class).disableProperty())))));
		traceTableView.disableProperty().bind(partOfDisableBinding.or(currentTrace.stateSpaceProperty().isNull()));
	}

	private void initTableColumns() {
		shouldExecuteColumn.setCellValueFactory(new ItemSelectedFactory(traceTableView, selectAll));
		shouldExecuteColumn.setGraphic(selectAll);
		statusColumn.setCellValueFactory(injector.getInstance(TraceViewHandler.class).getTraceStatusFactory());
		nameColumn.setCellValueFactory(
				features -> new SimpleStringProperty(features.getValue().getLocation().getFileName().toString()));
	}

	private void initTableRows() {
		this.traceTableView.setRowFactory(param -> {
			final TableRow<ReplayTrace> row = new TableRow<>();

			final MenuItem replayTraceItem = traceViewHandler.createReplayTraceItem();
			final MenuItem addTestsItem = traceViewHandler.createAddTestsItem();
			final MenuItem showDescriptionItem = traceViewHandler.createShowDescriptionItem();
			final MenuItem showErrorItem = traceViewHandler.createShowErrorItem();
			final MenuItem openInExternalEditorItem = traceViewHandler.createOpenInExternalEditorItem();
			final MenuItem deleteTraceItem = traceViewHandler.createDeleteTraceItem();
			final MenuItem recheckTraceItem = traceViewHandler.createRecheckTraceForChangesItem();

			// Set listeners for menu items
			traceViewHandler.initializeRow(this.getScene(), row, addTestsItem, replayTraceItem, showErrorItem, openInExternalEditorItem);
			deleteTraceItem.setOnAction(event -> currentProject.getCurrentMachine().getTraces().remove(row.getItem()));
			recheckTraceItem.setOnAction(event -> {
				final Machine currentMachine = currentProject.getCurrentMachine();
				Path currentMachinePath = currentProject.getLocation().resolve(currentMachine.getLocation());
				final TraceJsonFile traceFile;
				try {
					traceFile = row.getItem().load();
				} catch (IOException e) {
					injector.getInstance(TraceFileHandler.class).showLoadError(row.getItem().getAbsoluteLocation(), e);
					return;
				}
				TraceRefactoredSetup traceRefactoredSetup = new TraceRefactoredSetup(traceFile, currentMachinePath, null, row.getItem().getAbsoluteLocation(), currentTrace.getStateSpace(), injector, currentProject, stageManager);
				traceRefactoredSetup.executeCheck(true);
				List<Path> persistentTraceList = traceRefactoredSetup.evaluateResults();
				persistentTraceList.remove(row.getItem().getLocation());
				persistentTraceList.forEach(trace -> traceFileHandler.addTraceFile(currentMachine, trace));
			});
			showDescriptionItem.setOnAction(event -> showDescription(row.getItem()));

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
							.then((ContextMenu) null)
							.otherwise(new ContextMenu(replayTraceItem, addTestsItem, showErrorItem, new SeparatorMenuItem(), showDescriptionItem, deleteTraceItem, new SeparatorMenuItem(), openInExternalEditorItem, recheckTraceItem)));

			row.setOnMouseClicked(event -> {
				ReplayTrace item = row.getItem();
				if (item == null) {
					return;
				}
				if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					this.traceChecker.check(item, true);
				} else if (showDescription && event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 1) {
					showDescription(row.getItem());
					row.updateSelected(true);
				}
			});

			return row;
		});
	}

	@FXML
	private void checkMachine() {
		traceChecker.checkAll(traceTableView.getItems());
	}

	@FXML
	public void cancel() {
		traceChecker.cancelReplay();
	}

	@FXML
	private void loadTraceFromFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("animation.tracereplay.fileChooser.loadTrace.title"));
		fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.proB2Trace", TraceFileHandler.TRACE_FILE_EXTENSION));
		Path traceFile = fileChooserManager.showOpenFileChooser(fileChooser, Kind.TRACES, stageManager.getCurrent());
		if (traceFile != null) {
			traceFileHandler.addTraceFile(currentProject.getCurrentMachine(), traceFile);
		}

	}

	public void closeDescription() {
		if (showDescription) {
			splitPane.getItems().remove(1);
			showDescription = false;
		}
	}

	private void showDescription(ReplayTrace trace) {
		if (showDescription) {
			closeDescription();
		}
		splitPane.getItems().add(1, new DescriptionView(trace, this::closeDescription, stageManager, injector));
		splitPane.setDividerPositions(0.66);
		showDescription = true;
	}

	public void refresh() {
		this.traceTableView.refresh();
	}
}
