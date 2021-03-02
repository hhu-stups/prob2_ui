package de.prob2.ui.animation.tracereplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import de.prob.check.tracereplay.check.exceptions.PrologTermNotDefinedException;
import de.prob.check.tracereplay.json.TraceManager;
import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.scripting.ModelTranslationError;
import de.prob.statespace.FormalismType;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.config.FileChooserManager.Kind;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.sharedviews.DescriptionView;
import de.prob2.ui.sharedviews.TraceViewHandler;
import de.prob2.ui.verifications.IExecutableItem;
import de.prob2.ui.verifications.ItemSelectedFactory;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static de.prob2.ui.animation.tracereplay.TraceModificationChecker.traceNotReplayAble;

@FXMLInjected
@Singleton
public class TraceReplayView extends ScrollPane {
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
	private TableColumn<IExecutableItem, CheckBox> shouldExecuteColumn;
	@FXML
	private SplitPane splitPane;

	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final TraceChecker traceChecker;
	private final ResourceBundle bundle;
	private final FileChooserManager fileChooserManager;
	private final Injector injector;
	private final TraceViewHandler traceViewHandler;
	private final CheckBox selectAll;
	private boolean showDescription;

	@Inject
	private TraceReplayView(final StageManager stageManager, final CurrentProject currentProject,
			final CurrentTrace currentTrace, final TraceChecker traceChecker, final ResourceBundle bundle,
			final FileChooserManager fileChooserManager, final Injector injector, final TraceViewHandler traceViewHandler) {
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.traceChecker = traceChecker;
		this.bundle = bundle;
		this.fileChooserManager = fileChooserManager;
		this.injector = injector;
		this.traceViewHandler = traceViewHandler;
		this.selectAll = new CheckBox();
		stageManager.loadFXML(this, "trace_replay_view.fxml");
	}

	@FXML
	private void initialize() {
		helpButton.setHelpContent("animation", "Trace");
		traceTableView.setItems(traceViewHandler.getTraces());
		initTableColumns();
		initTableRows();

		final BooleanBinding partOfDisableBinding = currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B);
		loadTraceButton.disableProperty().bind(partOfDisableBinding.or(currentProject.currentMachineProperty().isNull()));
		cancelButton.disableProperty().bind(traceChecker.runningProperty().not());
		checkButton.disableProperty().bind(partOfDisableBinding.or(currentTrace.isNull().or(traceViewHandler.getNoTraces().or(selectAll.selectedProperty().not().or(injector.getInstance(DisablePropertyController.class).disableProperty())))));
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
            final MenuItem showDescriptionItem = traceViewHandler.createShowDescriptionItem();
            final MenuItem showErrorItem = traceViewHandler.createShowErrorItem();
            final MenuItem openInExternalEditorItem = traceViewHandler.createOpenInExternalEditorItem();
			final MenuItem deleteTraceItem = traceViewHandler.createDeleteTraceItem();
			final MenuItem recheckTraceItem = traceViewHandler.createRecheckTraceForChangesItem();

            // Set listeners for menu items
            traceViewHandler.initializeRow(this.getScene(), row, replayTraceItem, showErrorItem, openInExternalEditorItem);
            deleteTraceItem.setOnAction(event -> currentProject.getCurrentMachine().removeTraceFile(row.getItem().getLocation()));
            recheckTraceItem.setOnAction(event -> {
				try {
					TraceModificationChecker traceModificationChecker = new TraceModificationChecker(row.getItem(), currentTrace.getStateSpace(), injector, currentProject, stageManager);
					traceModificationChecker.check();
					List<Path> persistentTraceList = traceModificationChecker.evaluateResults();
					persistentTraceList.remove(row.getItem().getLocation());
					addPathsToProject(persistentTraceList);
				} catch (ModelTranslationError | PrologTermNotDefinedException modelTranslationError) {
					Optional<ButtonType> dialogResult = traceNotReplayAble(bundle).showAndWait();
				}

			});
            showDescriptionItem.setOnAction(event -> showDescription(row.getItem()));

			row.contextMenuProperty().bind(
					Bindings.when(row.emptyProperty())
					.then((ContextMenu) null)
					.otherwise(new ContextMenu(replayTraceItem, showErrorItem, new SeparatorMenuItem(), showDescriptionItem, deleteTraceItem, new SeparatorMenuItem(), openInExternalEditorItem, recheckTraceItem)));

			row.setOnMouseClicked(event -> {
				ReplayTrace item = row.getItem();
				if(item == null) {
					return;
				}
				if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					this.traceChecker.check(item, true);
				} else if(showDescription && event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 1) {
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
	private void loadTraceFromFile() throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("animation.tracereplay.fileChooser.loadTrace.title"));
		fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.proB2Trace", TraceFileHandler.TRACE_FILE_EXTENSION));
		Path traceFile = fileChooserManager.showOpenFileChooser(fileChooser, Kind.TRACES, stageManager.getCurrent());

		if (traceFile != null) {
		//Check if machine file is valid
		TraceManager traceManager = new TraceManager(new ObjectMapper());
		TraceJsonFile traceJsonFile = traceManager.load(traceFile);

		try {
			TraceModificationChecker traceModificationChecker = new TraceModificationChecker(traceJsonFile, traceFile, currentTrace.getStateSpace(), injector, currentProject, stageManager);
			traceModificationChecker.check();
			List<Path> persistentTraceList = traceModificationChecker.evaluateResults();
			addPathsToProject(persistentTraceList);
		} catch (ModelTranslationError | PrologTermNotDefinedException modelTranslationError) {
			modelTranslationError.printStackTrace();
		}
		}
	}

	public void closeDescription() {
		if (showDescription) {
			splitPane.getItems().remove(1);
			showDescription = false;
		}
	}

	private void showDescription(ReplayTrace trace) {
		if(showDescription) {
			closeDescription();
		}
		splitPane.getItems().add(1, new DescriptionView(trace, this::closeDescription, stageManager, injector));
		splitPane.setDividerPositions(0.66);
		showDescription = true;
	}

	private void addPathsToProject(List<Path> persistentTraceList){
		persistentTraceList.forEach(element -> {
			Path relative = currentProject.getLocation().relativize(element);
			currentProject.getCurrentMachine().addTraceFile(relative);
		});
	}

	public void refresh() {
	    this.traceTableView.refresh();
    }
}
