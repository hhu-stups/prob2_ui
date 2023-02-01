package de.prob2.ui.animation.tracereplay;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.CheckingViewBase;
import de.prob2.ui.sharedviews.DescriptionView;
import de.prob2.ui.sharedviews.RefactorButton;
import de.prob2.ui.sharedviews.TraceViewHandler;

import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;

@FXMLInjected
@Singleton
public final class TraceReplayView extends CheckingViewBase<ReplayTrace> {
	private final class Row extends RowBase {
		private Row() {
			final MenuItem replayTraceItem = traceViewHandler.createReplayTraceItem();
			final MenuItem addTestsItem = traceViewHandler.createAddTestsItem();
			final MenuItem editIdItem = traceViewHandler.createEditIdItem();
			final MenuItem showDescriptionItem = traceViewHandler.createShowDescriptionItem();
			final MenuItem showStatusItem = traceViewHandler.createShowStatusItem();
			final MenuItem openInExternalEditorItem = traceViewHandler.createOpenInExternalEditorItem();
			final MenuItem deleteTraceItem = traceViewHandler.createDeleteTraceItem();
			final MenuItem revealInExplorerItem = traceViewHandler.createRevealInExplorerItem();
			final MenuItem recheckTraceItem = traceViewHandler.createRecheckTraceForChangesItem();

			// Set listeners for menu items
			traceViewHandler.initializeRow(this.getScene(), this, addTestsItem, replayTraceItem, showStatusItem, openInExternalEditorItem, revealInExplorerItem);
			editIdItem.setOnAction(event -> {
				final ReplayTrace trace = this.getItem();
				final TextInputDialog dialog = new TextInputDialog(trace.getId() == null ? "" : trace.getId());
				stageManager.register(dialog);
				dialog.setTitle(i18n.translate("animation.tracereplay.view.contextMenu.editId"));
				dialog.setHeaderText(i18n.translate("vomanager.validationTaskId"));
				dialog.getEditor().setPromptText(i18n.translate("common.optionalPlaceholder"));
				final Optional<String> res = dialog.showAndWait();
				res.ifPresent(idText -> {
					final String id = idText.trim().isEmpty() ? null : idText;
					final List<ReplayTrace> traces = currentProject.getCurrentMachine().getTraces();
					traces.set(traces.indexOf(trace), trace.withId(id));
				});
			});
			deleteTraceItem.setOnAction(event -> currentProject.getCurrentMachine().getTraces().remove(this.getItem()));
			recheckTraceItem.setOnAction(event -> {
				final Machine currentMachine = currentProject.getCurrentMachine();
				Path currentMachinePath = currentProject.getLocation().resolve(currentMachine.getLocation());
				final TraceJsonFile traceFile;
				try {
					traceFile = this.getItem().load();
				} catch (IOException e) {
					injector.getInstance(TraceFileHandler.class).showLoadError(this.getItem().getAbsoluteLocation(), e);
					return;
				}
				TraceRefactoredSetup traceRefactoredSetup = new TraceRefactoredSetup(traceFile, currentMachinePath, null, this.getItem().getAbsoluteLocation(), currentTrace.getStateSpace(), injector, currentProject, stageManager);
				traceRefactoredSetup.executeCheck(true);
				List<Path> persistentTraceList = traceRefactoredSetup.evaluateResults();
				persistentTraceList.remove(this.getItem().getLocation());
				persistentTraceList.forEach(trace -> traceFileHandler.addTraceFile(currentMachine, trace));
			});
			showDescriptionItem.setOnAction(event -> showDescription(this.getItem()));

			contextMenu.getItems().addAll(replayTraceItem, addTestsItem, editIdItem, showStatusItem, new SeparatorMenuItem(), showDescriptionItem, deleteTraceItem, new SeparatorMenuItem(), openInExternalEditorItem, revealInExplorerItem, recheckTraceItem);

			this.setOnMouseClicked(event -> {
				ReplayTrace item = this.getItem();
				if (item == null) {
					return;
				}
				if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
					traceChecker.check(item, true);
				}
			});
		}
	}

	private final StageManager stageManager;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final TraceChecker traceChecker;
	private final I18n i18n;
	private final FileChooserManager fileChooserManager;
	private final Injector injector;
	private final TraceFileHandler traceFileHandler;
	private final TraceViewHandler traceViewHandler;

	@FXML
	private TableColumn<ReplayTrace, Node> statusProgressColumn;
	@FXML
	private Button loadTraceButton;
	@FXML
	private HelpButton helpButton;
	@FXML
	private RefactorButton refactorButton;
	@FXML
	private SplitPane splitPane;
	private boolean showDescription;

	@Inject
	private TraceReplayView(final StageManager stageManager, final CurrentProject currentProject, final DisablePropertyController disablePropertyController,
							final CurrentTrace currentTrace, final TraceChecker traceChecker, final I18n i18n,
							final FileChooserManager fileChooserManager, final Injector injector, final TraceFileHandler traceFileHandler, final TraceViewHandler traceViewHandler) {
		super(disablePropertyController);
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.traceChecker = traceChecker;
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.injector = injector;
		this.traceFileHandler = traceFileHandler;
		this.traceViewHandler = traceViewHandler;
		stageManager.loadFXML(this, "trace_replay_view.fxml");
	}

	@Override
	@FXML
	public void initialize() {
		super.initialize();
		helpButton.setHelpContent("animation", "Trace");
		items.bind(traceViewHandler.getTraces());
		initTableColumns();
		initTableRows();

		itemsTable.getSelectionModel().selectedItemProperty().addListener((o, from, to) -> {
			if (showDescription) {
				closeDescription();
				if (to != null) {
					showDescription(to);
				}
			}
		});

		final BooleanBinding partOfDisableBinding = currentTrace.modelProperty().formalismTypeProperty().isNotEqualTo(FormalismType.B);
		loadTraceButton.disableProperty().bind(partOfDisableBinding.or(currentProject.currentMachineProperty().isNull()));
	}

	private void initTableColumns() {
		statusProgressColumn.setCellValueFactory(injector.getInstance(TraceViewHandler.class).getTraceStatusFactory());
	}

	private void initTableRows() {
		this.itemsTable.setRowFactory(table -> new Row());
	}

	@Override
	protected String configurationForItem(final ReplayTrace item) {
		return item.getName();
	}

	@FXML
	private void checkMachine() {
		traceChecker.checkAll(items);
	}

	@FXML
	private void loadTraceFromFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("animation.tracereplay.fileChooser.loadTrace.title"));
		fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.proB2Trace", TraceFileHandler.TRACE_FILE_EXTENSION));
		Path traceFile = fileChooserManager.showOpenFileChooser(fileChooser, Kind.TRACES, stageManager.getCurrent());
		if (traceFile != null) {
			final ReplayTrace replayTrace = traceFileHandler.addTraceFile(currentProject.getCurrentMachine(), traceFile);
			traceChecker.check(replayTrace, true);
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
}
