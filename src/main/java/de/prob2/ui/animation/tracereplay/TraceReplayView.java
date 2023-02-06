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
import de.prob2.ui.layout.BindableGlyph;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.menu.ExternalEditor;
import de.prob2.ui.menu.RevealInExplorer;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.CheckingViewBase;
import de.prob2.ui.sharedviews.DescriptionView;
import de.prob2.ui.sharedviews.RefactorButton;
import de.prob2.ui.sharedviews.TraceViewHandler;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;

import org.controlsfx.glyphfont.FontAwesome;

@FXMLInjected
@Singleton
public final class TraceReplayView extends CheckingViewBase<ReplayTrace> {
	private final class Row extends RowBase {
		private Row() {
			itemProperty().addListener((o, from, to) -> {
				if (to != null) {
					setTooltip(new Tooltip(this.getItem().getLocation().toString()));
				}
			});

			executeMenuItem.setText(i18n.translate("animation.tracereplay.view.contextMenu.replayTrace"));
			editMenuItem.setText(i18n.translate("animation.tracereplay.view.contextMenu.editId"));

			final MenuItem addTestsItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.editTrace"));
			addTestsItem.setOnAction(event -> {
				TraceTestView traceTestView = injector.getInstance(TraceTestView.class);
				traceTestView.loadReplayTrace(this.getItem());
				traceTestView.show();
			});

			final MenuItem showStatusItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.showStatus"));
			showStatusItem.setOnAction(event -> {
				ReplayedTraceStatusAlert alert = new ReplayedTraceStatusAlert(injector, this.getItem());
				alert.show();
			});

			final MenuItem showDescriptionItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.showDescription"));
			showDescriptionItem.setOnAction(event -> showDescription(this.getItem()));

			final MenuItem deleteTraceItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.removeTrace"));
			deleteTraceItem.setOnAction(event -> currentProject.getCurrentMachine().getTraces().remove(this.getItem()));

			final MenuItem openInExternalEditorItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.openInExternalEditor"));
			openInExternalEditorItem.setOnAction(event ->
				injector.getInstance(ExternalEditor.class).open(this.getItem().getAbsoluteLocation())
			);

			final MenuItem revealInExplorerItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.revealInExplorer"));
			revealInExplorerItem.setOnAction(event ->
				injector.getInstance(RevealInExplorer.class).revealInExplorer(this.getItem().getAbsoluteLocation())
			);

			final MenuItem recheckTraceItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.refactorTrace"));

			recheckTraceItem.setOnAction(event -> {
				final Machine currentMachine = currentProject.getCurrentMachine();
				Path currentMachinePath = currentProject.getLocation().resolve(currentMachine.getLocation());
				final TraceJsonFile traceFile;
				try {
					traceFile = this.getItem().load();
				} catch (IOException e) {
					traceFileHandler.showLoadError(this.getItem().getAbsoluteLocation(), e);
					return;
				}
				TraceRefactoredSetup traceRefactoredSetup = new TraceRefactoredSetup(traceFile, currentMachinePath, null, this.getItem().getAbsoluteLocation(), currentTrace.getStateSpace(), injector, currentProject, stageManager);
				traceRefactoredSetup.executeCheck(true);
				List<Path> persistentTraceList = traceRefactoredSetup.evaluateResults();
				persistentTraceList.remove(this.getItem().getLocation());
				persistentTraceList.forEach(trace -> traceFileHandler.addTraceFile(currentMachine, trace));
			});

			contextMenu.getItems().addAll(addTestsItem, showStatusItem, new SeparatorMenuItem(), showDescriptionItem, deleteTraceItem, new SeparatorMenuItem(), openInExternalEditorItem, revealInExplorerItem, recheckTraceItem);
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
							final FileChooserManager fileChooserManager, final Injector injector, final TraceFileHandler traceFileHandler) {
		super(disablePropertyController);
		this.stageManager = stageManager;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.traceChecker = traceChecker;
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.injector = injector;
		this.traceFileHandler = traceFileHandler;
		stageManager.loadFXML(this, "trace_replay_view.fxml");
	}

	@Override
	@FXML
	public void initialize() {
		super.initialize();
		helpButton.setHelpContent("animation", "Trace");

		final ChangeListener<Machine> machineChangeListener = (observable, from, to) -> {
			items.unbind();
			if (to != null) {
				items.bind(to.tracesProperty());
			} else {
				items.set(FXCollections.observableArrayList());
			}
		};
		currentProject.currentMachineProperty().addListener(machineChangeListener);
		machineChangeListener.changed(null, null, currentProject.getCurrentMachine());

		statusProgressColumn.setCellValueFactory(features -> {
			final ReplayTrace trace = features.getValue();
			
			final BindableGlyph statusIcon = new BindableGlyph("FontAwesome", FontAwesome.Glyph.QUESTION_CIRCLE);
			statusIcon.getStyleClass().add("status-icon");
			statusIcon.bindableFontSizeProperty().bind(injector.getInstance(FontSize.class).fontSizeProperty());
			trace.checkedProperty().addListener((o, from, to) -> Platform.runLater(() -> TraceViewHandler.updateStatusIcon(statusIcon, to)));
			TraceViewHandler.updateStatusIcon(statusIcon, trace.getChecked());
			
			final ProgressIndicator replayProgress = new ProgressBar();
			replayProgress.progressProperty().bind(trace.progressProperty());
			
			return Bindings.when(trace.progressProperty().isEqualTo(-1)).<Node>then(statusIcon)
				.otherwise(replayProgress);
		});

		itemsTable.setRowFactory(table -> new Row());
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

	@Override
	protected String configurationForItem(final ReplayTrace item) {
		return item.getName();
	}

	@Override
	protected void executeItem(final ReplayTrace item) {
		traceChecker.check(item, true);
	}

	@Override
	protected Optional<ReplayTrace> editItem(final ReplayTrace oldItem) {
		// This only implements editing the validation task ID.
		// Editing the trace itself is a different menu item (addTestsItem).
		final TextInputDialog dialog = new TextInputDialog(oldItem.getId() == null ? "" : oldItem.getId());
		stageManager.register(dialog);
		dialog.setTitle(i18n.translate("animation.tracereplay.view.contextMenu.editId"));
		dialog.setHeaderText(i18n.translate("vomanager.validationTaskId"));
		dialog.getEditor().setPromptText(i18n.translate("common.optionalPlaceholder"));
		return dialog.showAndWait().map(idText -> {
			final String id = idText.trim().isEmpty() ? null : idText;
			return oldItem.withId(id);
		});
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
		splitPane.getItems().add(1, new DescriptionView(trace, this::closeDescription, stageManager, i18n));
		splitPane.setDividerPositions(0.66);
		showDescription = true;
	}
}
