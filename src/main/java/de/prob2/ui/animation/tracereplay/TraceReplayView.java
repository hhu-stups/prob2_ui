package de.prob2.ui.animation.tracereplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.io.MoreFiles;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.check.tracereplay.json.storage.TraceJsonFile;
import de.prob.statespace.FormalismType;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.config.FileChooserManager.Kind;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.DisablePropertyController;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.layout.FontSize;
import de.prob2.ui.menu.ExternalEditor;
import de.prob2.ui.menu.RevealInExplorer;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.CheckingViewBase;
import de.prob2.ui.sharedviews.DescriptionView;
import de.prob2.ui.verifications.CheckedIcon;
import de.prob2.ui.verifications.ExecutionContext;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import static de.prob2.ui.sharedviews.DescriptionView.getTraceDescriptionView;

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
			removeMenuItem.setText(i18n.translate("animation.tracereplay.view.contextMenu.removeTrace"));
			// Will be re-added in a different place later.
			contextMenu.getItems().remove(removeMenuItem);

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

			final MenuItem openInExternalEditorItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.openInExternalEditor"));
			openInExternalEditorItem.setOnAction(event -> injector.getInstance(ExternalEditor.class).open(this.getItem().getAbsoluteLocation()));

			final MenuItem revealInExplorerItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.revealInExplorer"));
			revealInExplorerItem.setOnAction(event -> injector.getInstance(RevealInExplorer.class).revealInExplorer(this.getItem().getAbsoluteLocation()));

			contextMenu.getItems().addAll(addTestsItem, showStatusItem, new SeparatorMenuItem(), showDescriptionItem, removeMenuItem, new SeparatorMenuItem(), openInExternalEditorItem, revealInExplorerItem);
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
	private TableColumn<ReplayTrace, String> stepsColumn;
	@FXML
	private MenuButton loadTraceButton;
	@FXML
	private HelpButton helpButton;
	@FXML
	private SplitPane splitPane;
	private boolean showDescription;

	@Inject
	private TraceReplayView(final StageManager stageManager, final CurrentProject currentProject, final DisablePropertyController disablePropertyController,
	                        final CurrentTrace currentTrace, final CliTaskExecutor cliExecutor, final TraceChecker traceChecker, final I18n i18n,
	                        final FileChooserManager fileChooserManager, final Injector injector, final TraceFileHandler traceFileHandler) {
		super(i18n, disablePropertyController, currentTrace, currentProject, cliExecutor);
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
	protected ObservableList<ReplayTrace> getItemsProperty(Machine machine) {
		return machine.getMachineProperties().getTraces();
	}

	@Override
	@FXML
	public void initialize() {
		super.initialize();
		helpButton.setHelpContent("animation", "Trace");

		statusProgressColumn.setCellValueFactory(features -> {
			final ReplayTrace trace = features.getValue();

			final CheckedIcon statusIcon = new CheckedIcon();
			statusIcon.bindableFontSizeProperty().bind(injector.getInstance(FontSize.class).fontSizeProperty());
			trace.checkedProperty().addListener((o, from, to) -> Platform.runLater(() -> statusIcon.setChecked(to)));
			statusIcon.setChecked(trace.getChecked());

			final ProgressIndicator replayProgress = new ProgressBar();
			replayProgress.progressProperty().bind(trace.progressProperty());

			return Bindings.when(trace.progressProperty().isEqualTo(-1))
				       .<Node>then(statusIcon)
				       .otherwise(replayProgress);
		});

		stepsColumn.setCellValueFactory(features -> {
			ReplayTrace trace = features.getValue();
			TraceJsonFile traceFile = trace.getLoadedTrace();
			if (traceFile == null) {
				try {
					traceFile = trace.load();
				} catch (IOException ignore) {
					// ignore errors, so the user does not get bombarded with errors on startup
				}
			}

			String steps;
			if (traceFile != null) {
				steps = String.valueOf(traceFile.getTransitionList().size());
			} else {
				steps = i18n.translate("common.notAvailable");
			}

			return new SimpleStringProperty(steps);
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
	protected void executeItemSync(final ReplayTrace item, final ExecutionContext context) {
		// FIXME Respect execution context
		traceChecker.check(item);
	}

	@Override
	protected Optional<ReplayTrace> showItemDialog(final ReplayTrace oldItem) {
		if (oldItem == null) {
			// Adding a trace - ask the user to select a trace file.
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(i18n.translate("animation.tracereplay.fileChooser.loadTrace.title"));
			fileChooser.setInitialDirectory(currentProject.getLocation().toFile());
			fileChooser.getExtensionFilters().add(fileChooserManager.getProB2TraceFilter());
			Path traceFile = fileChooserManager.showOpenFileChooser(fileChooser, Kind.TRACES, stageManager.getCurrent());
			if (traceFile == null) {
				return Optional.empty();
			} else {
				return Optional.of(traceFileHandler.createReplayTraceForPath(traceFile));
			}
		} else {
			// This only implements editing the validation task ID.
			// Editing the trace itself is a different menu item (addTestsItem).
			// TODO Make this a proper dialog and perhaps allow viewing/changing the trace file path?
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
		final DescriptionView descriptionView =
			getTraceDescriptionView(trace, this.stageManager, traceFileHandler, this.i18n, this::closeDescription);
		splitPane.getItems().add(1, descriptionView);
		splitPane.setDividerPositions(0.66);
		showDescription = true;
	}

	@FXML
	public void loadTracesDirectory() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(i18n.translate("animation.tracereplay.fileChooser.loadTracesDirectory.title"));
		directoryChooser.setInitialDirectory(currentProject.getLocation().toFile());
		Path directory = fileChooserManager.showDirectoryChooser(directoryChooser, Kind.TRACES, stageManager.getCurrent());
		List<Path> paths = new ArrayList<>();
		if (directory != null) {
			try (Stream<Path> walk = Files.walk(directory)) {
				paths = walk.filter(Files::isRegularFile)
					        .filter(p -> MoreFiles.getFileExtension(p).equals(TraceFileHandler.TRACE_FILE_EXTENSION))
					        .collect(Collectors.toList());
			} catch (IOException e) {
				final Alert alert = stageManager.makeExceptionAlert(e, "animation.tracereplay.alerts.traceDirectoryError.header", "animation.tracereplay.alerts.traceDirectoryError.error");
				alert.initOwner(this.getScene().getWindow());
				alert.show();
			}
			for (Path path : paths) {
				traceFileHandler.addTraceFile(currentProject.getCurrentMachine(), path);
			}
		}
	}

	@Override
	protected void removeItem(ReplayTrace item) {
		super.removeItem(item);
		this.traceFileHandler.deleteTraceFile(item);
	}
}
