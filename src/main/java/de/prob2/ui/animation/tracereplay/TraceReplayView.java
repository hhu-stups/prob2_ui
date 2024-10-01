package de.prob2.ui.animation.tracereplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.google.common.base.Strings;
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
import de.prob2.ui.menu.ExternalEditor;
import de.prob2.ui.menu.RevealInExplorer;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;
import de.prob2.ui.sharedviews.CheckingViewBase;
import de.prob2.ui.sharedviews.DescriptionView;
import de.prob2.ui.verifications.CheckingExecutors;
import de.prob2.ui.verifications.ExecutionContext;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import static de.prob2.ui.sharedviews.DescriptionView.getTraceDescriptionView;

@FXMLInjected
@Singleton
public final class TraceReplayView extends CheckingViewBase<ReplayTrace> {
	private final class Row extends RowBase {
		private Row() {
			// tooltip with path and description
			ObservableValue<Path> location = this.itemProperty().map(ReplayTrace::getLocation);
			ObservableValue<String> description = this.itemProperty()
					.flatMap(ReplayTrace::loadedTraceProperty)
					.map(TraceJsonFile::getDescription);
			StringBinding tooltipText = Bindings.createStringBinding(() -> {
				String s = Objects.toString(location.getValue(), "");
				String d = description.getValue();
				if (!Strings.isNullOrEmpty(d)) {
					s += "\n" + d;
				}

				return s;
			}, location, description);
			Tooltip tt = new Tooltip();
			tt.textProperty().bind(tooltipText);
			tt.setShowDelay(Duration.millis(200));
			this.tooltipProperty().bind(Bindings.when(tooltipText.isEmpty()).then((Tooltip) null).otherwise(tt));

			executeMenuItem.setText(i18n.translate("animation.tracereplay.view.contextMenu.replayTrace"));
			editMenuItem.setText(i18n.translate("animation.tracereplay.view.contextMenu.editId"));
			removeMenuItem.setText(i18n.translate("animation.tracereplay.view.contextMenu.removeTrace"));
			// Will be re-added in a different place later.
			contextMenu.getItems().remove(removeMenuItem);

			final MenuItem addTestsItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.editTrace"));
			addTestsItem.setOnAction(event -> {
				TraceTestView traceTestView = injector.getInstance(TraceTestView.class);
				traceTestView.loadReplayTrace(currentProject.get(), currentProject.getCurrentMachine(), this.getItem());
				traceTestView.show();
			});

			final MenuItem showStatusItem = new MenuItem(i18n.translate("animation.tracereplay.view.contextMenu.showStatus"));
			showStatusItem.setOnAction(event -> {
				ReplayedTraceStatusAlert alert = injector.getInstance(ReplayedTraceStatusAlert.class);
				alert.initReplayTrace(this.getItem());
				alert.showAndWait().ifPresent(buttonType -> {
					if (buttonType.equals(alert.getAcceptButtonType())) {
						currentTrace.set(this.getItem().getTrace());
					}
				});
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
	                        final CurrentTrace currentTrace, final CheckingExecutors checkingExecutors, final TraceChecker traceChecker, final I18n i18n,
	                        final FileChooserManager fileChooserManager, final Injector injector, final TraceFileHandler traceFileHandler) {
		super(stageManager, i18n, disablePropertyController, currentTrace, currentProject, checkingExecutors);
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
		return machine.getTraces();
	}

	@Override
	@FXML
	public void initialize() {
		super.initialize();
		helpButton.setHelpContent("animation", "Trace");

		stepsColumn.setCellValueFactory(features -> {
			ReplayTrace trace = features.getValue();
			return Bindings.createStringBinding(() -> {
				TraceJsonFile traceFile = trace.getLoadedTrace();
				if (traceFile == null) {
					try {
						traceFile = trace.load();
					} catch (IOException ignore) {
						// ignore errors, so the user does not get bombarded with errors on startup
					}
				}

				if (traceFile != null) {
					return String.valueOf(traceFile.getTransitionList().size());
				} else {
					return i18n.translate("common.notAvailable");
				}
			}, trace.loadedTraceProperty());
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
	protected CompletableFuture<?> executeItemImpl(ReplayTrace item, CheckingExecutors executors, ExecutionContext context) {
		return super.executeItemImpl(item, executors, context).thenCompose(res ->
			traceChecker.askKeepReplayedTrace(item)
		).thenApply(trace -> {
			trace.ifPresent(currentTrace::set);
			return null;
		}).exceptionally(exc -> {
			Platform.runLater(() -> traceFileHandler.showLoadError(item, exc));
			// Do not pass on the exception - otherwise the default exception handling in CheckingViewBase will display the same exception again.
			return null;
		});
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
						        .toList();
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
