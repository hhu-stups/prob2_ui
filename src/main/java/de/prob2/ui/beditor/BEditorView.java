package de.prob2.ui.beditor;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.GetInternalRepresentationCommand;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.FormulaTranslationMode;
import de.prob.scripting.EventBFactory;
import de.prob.scripting.EventBPackageFactory;
import de.prob.statespace.StateSpace;
import de.prob2.ui.config.Config;
import de.prob2.ui.config.ConfigData;
import de.prob2.ui.config.ConfigListener;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.TextAreaState;
import de.prob2.ui.internal.executor.CliTaskExecutor;
import de.prob2.ui.internal.executor.FxThreadExecutor;
import de.prob2.ui.menu.ExternalEditor;
import de.prob2.ui.menu.MainView;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public final class BEditorView extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(BEditorView.class);
	private static final Charset EDITOR_CHARSET = StandardCharsets.UTF_8;

	@FXML
	private Button saveButton;

	@FXML
	private Button openExternalButton;

	@FXML
	private ToggleButton searchButton;

	@FXML
	private SearchPane searchPane;

	@FXML
	private Label warningLabel;

	@FXML
	private BEditor beditor;

	@FXML
	private HelpButton helpButton;

	@FXML
	private ChoiceBox<Path> machineChoice;

	@FXML
	private CheckBox cbUnicode;

	private final StageManager stageManager;
	private final I18n i18n;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final CliTaskExecutor cliExecutor;
	private final FxThreadExecutor fxExecutor;
	private final Injector injector;

	private final ObjectProperty<Path> path;
	private final StringProperty lastSavedText;
	private final StringProperty lastLoadedText;
	private final BooleanProperty autoReloadMachine;
	private final BooleanProperty saved;
	private final BooleanProperty fileContentChanged;
	private final ObservableList<ErrorItem> errors;

	private Thread watchThread;
	private boolean saving;
	private boolean ignoreMachineChoiceSelectionChange;

	@Inject
	private BEditorView(
		StageManager stageManager,
		I18n i18n,
		CurrentProject currentProject,
		CurrentTrace currentTrace,
		CliTaskExecutor cliExecutor,
		FxThreadExecutor fxExecutor,
		Config config,
		Injector injector
	) {
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.cliExecutor = cliExecutor;
		this.fxExecutor = fxExecutor;
		this.injector = injector;
		this.path = new SimpleObjectProperty<>(this, "path", null);
		this.lastSavedText = new SimpleStringProperty(this, "lastSavedText", null);
		this.lastLoadedText = new SimpleStringProperty(this, "lastLoadedText", null);
		this.autoReloadMachine = new SimpleBooleanProperty(this, "autoReloadMachine", true);
		this.saved = new SimpleBooleanProperty(this, "saved");
		this.fileContentChanged = new SimpleBooleanProperty(this, "fileContentChanged", false);
		this.errors = FXCollections.observableArrayList();
		this.watchThread = null;
		stageManager.loadFXML(this, "beditorView.fxml");

		config.addListener(new ConfigListener() {
			@Override
			public void loadConfig(final ConfigData configData) {
				autoReloadMachine.set(configData.autoReloadMachine);
			}

			@Override
			public void saveConfig(final ConfigData configData) {
				configData.autoReloadMachine = autoReloadMachine.get();
			}
		});
	}

	@FXML
	private void initialize() {
		// The saved property is updated manually using listeners instead of using bindings,
		// because RichTextFX invalidates its observable values too often.
		// For example, the text property is invalidated even if only the formatting changed and not the text itself,
		// which leads to infinite recursion if the saved property triggers a change in formatting.
		lastSavedText.addListener(o -> this.updateSaved());
		beditor.textProperty().addListener(o -> this.updateSaved());
		path.addListener(o -> this.updateSaved());
		saved.addListener(o -> this.updateErrors());
		errors.addListener((InvalidationListener) o -> this.updateErrors());
		currentTrace.stateSpaceProperty().addListener((o, from, to) -> {
			if (to == null) {
				return;
			}
			updateIncludedMachines();
		});
		saveButton.disableProperty().bind(saved);
		openExternalButton.disableProperty().bind(this.pathProperty().isNull());
		searchButton.disableProperty().bind(this.pathProperty().isNull());
		searchButton.selectedProperty().addListener((obs, ov, nv) -> {
			if (nv) {
				searchPane.show(this);
			} else {
				searchPane.hide();
			}
		});
		searchPane.visibleProperty().bind(searchButton.selectedProperty());
		searchPane.managedProperty().bind(searchButton.selectedProperty());
		warningLabel.textProperty().bind(
			Bindings.when(saved)
				.then(Bindings.when(fileContentChanged)
						.then(i18n.translate("beditor.warnings.fileContentChanged"))
						.otherwise(""))
				.otherwise(i18n.translate("beditor.warnings.unsaved"))
		);
		Platform.runLater(this::setHint);

		machineChoice.setConverter(new StringConverter<>() {
			@Override
			public String toString(final Path object) {
				return object == null ? "" : object.getFileName().toString();
			}

			@Override
			public Path fromString(final String string) {
				throw new AssertionError("Should never be called");
			}
		});

		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			if (to == null) {
				machineChoice.getItems().clear();
				this.setHint();
			} else {
				// The correct list of included machines is available once the machine is fully loaded.
				// Until that happens, display only the cached last opened or the main machine.
				Path toShow = to.getCachedEditorState().getSelectedMachine();
				if (toShow == null) {
					toShow = currentProject.get().getAbsoluteMachinePath(to);
				}

				// clearing selection so that later the "confirm replace"-dialog is not shown again
				machineChoice.getSelectionModel().clearSelection();
				machineChoice.getItems().setAll(toShow);
				machineChoice.getSelectionModel().selectFirst();
			}
		});

		beditor.caretPositionProperty().addListener((observable, from, to) -> {
			if (!beditor.isChangingText()) {
				Machine m = currentProject.getCurrentMachine();
				if (m != null) {
					Path machine = machineChoice.getSelectionModel().getSelectedItem();
					if (machine != null) {
						m.getCachedEditorState().setCaretPosition(machine, to);
					}
				}
			}
		});
		beditor.estimatedScrollXProperty().addListener((observable, from, to) -> {
			if (!beditor.isChangingText()) {
				Machine m = currentProject.getCurrentMachine();
				if (m != null) {
					Path machine = machineChoice.getSelectionModel().getSelectedItem();
					if (machine != null) {
						m.getCachedEditorState().setScrollXPosition(machine, to);
					}
				}
			}
		});
		beditor.estimatedScrollYProperty().addListener((observable, from, to) -> {
			if (!beditor.isChangingText()) {
				Machine m = currentProject.getCurrentMachine();
				if (m != null) {
					Path machine = machineChoice.getSelectionModel().getSelectedItem();
					if (machine != null) {
						m.getCachedEditorState().setScrollYPosition(machine, to);
					}
				}
			}
		});

		currentProject.addListener((observable, from, to) -> resetWatching());

		machineChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if (this.ignoreMachineChoiceSelectionChange || to == null) {
				return;
			}

			if (from != null && !this.currentProject.confirmMachineReplace()) {
				Platform.runLater(() -> {
					this.ignoreMachineChoiceSelectionChange = true;
					try {
						this.machineChoice.getSelectionModel().select(from);
					} finally {
						this.ignoreMachineChoiceSelectionChange = false;
					}
				});
				return;
			}

			switchMachine(to);
		});

		cbUnicode.selectedProperty().addListener((observable, from, to) ->
			showInternalRepresentation(currentTrace.getStateSpace(), path.get()).exceptionally(exc -> {
				stageManager.showUnhandledExceptionAlert(exc, this.getScene().getWindow());
				return null;
			})
		);

		helpButton.setHelpContent("mainView.editor", null);

		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.F, KeyCombination.SHORTCUT_DOWN), e -> {
			if (searchButton.isSelected()) {
				searchPane.startSearch();
			} else {
				searchButton.fire();
			}
		}));
		Nodes.addInputMap(this, InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.ESCAPE),
				() -> searchButton.isSelected(), e -> searchButton.fire()));
		Nodes.addInputMap(this, InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.G, KeyCombination.SHORTCUT_DOWN),
				() -> searchButton.isSelected(), e -> searchPane.handleGotoNext(true)));
		Nodes.addInputMap(this, InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.G, KeyCombination.SHIFT_DOWN, KeyCombination.SHORTCUT_DOWN),
				() -> searchButton.isSelected(), e -> searchPane.handleGotoPrevious(true)));
	}

	BEditor getEditor() {
		return this.beditor;
	}

	private void updateSaved() {
		boolean newSaved;
		if (this.getPath() != null) {
			String lastSaved = Objects.requireNonNullElse(this.lastSavedText.get(), "");
			String editor = Objects.requireNonNullElse(this.beditor.getText(), "");
			newSaved = lastSaved.equals(editor);
		} else {
			// do not show unsaved warning when there is no file open right now
			newSaved = true;
		}

		this.saved.set(newSaved);
	}

	private void updateErrors() {
		Path currentPath = this.getPath();
		if (currentPath != null && this.savedProperty().get()) {
			this.beditor.getErrors().setAll(
				this.getErrors().stream()
					.filter(error -> error.getLocations().stream()
						                 .map(ErrorItem.Location::getFilename)
						                 .anyMatch(filename -> fileNameMatchesCurrentPath(filename, currentPath)))
					.collect(Collectors.toList())
			);
		} else {
			this.beditor.getErrors().clear();
		}
	}

	private void switchMachine(final Path machinePath) {
		Objects.requireNonNull(machinePath, "machinePath");

		resetWatching();
		registerFile(machinePath);
		loadText(machinePath).whenComplete((res, exc) -> {
			if (exc == null) {
				Platform.runLater(() -> {
					currentProject.getCurrentMachine().getCachedEditorState().setSelectedMachine(machinePath);

					beditor.requestFocus();
					TextAreaState textAreaState = currentProject.getCurrentMachine().getCachedEditorState().getTextAreaState(machinePath);
					if (textAreaState != null) {
						restoreState(textAreaState);
					}
				});
			} else {
				stageManager.showUnhandledExceptionAlert(exc, this.getScene().getWindow());
			}
		});
		if (searchButton.isSelected())
			searchButton.fire(); // hide search
	}

	private void restoreState(TextAreaState textAreaState) {
		beditor.moveTo(Math.max(0, Math.min(beditor.getLength(), textAreaState.caretPosition)));
		beditor.requestFollowCaret();
	}

	private CompletableFuture<Void> loadText(Path machinePath) {
		if (currentProject.getCurrentMachine().getModelFactoryClass() == EventBFactory.class || currentProject.getCurrentMachine().getModelFactoryClass() == EventBPackageFactory.class) {
			final StateSpace stateSpace = currentTrace.getStateSpace();
			cbUnicode.setVisible(true);
			cbUnicode.setManaged(true);
			if (stateSpace == null) {
				this.machineChoice.getSelectionModel().clearSelection();
				this.setWaitingForInternalRepresentationHint();
				return CompletableFuture.completedFuture(null);
			} else {
				return showInternalRepresentation(stateSpace, machinePath);
			}
		} else {
			cbUnicode.setVisible(false);
			cbUnicode.setManaged(false);
			setText(machinePath);
			return CompletableFuture.completedFuture(null);
		}
	}

	private CompletableFuture<Void> showInternalRepresentation(StateSpace stateSpace, Path machinePath) {
		this.beditor.setEditable(false);
		if (stateSpace == null || machinePath == null) {
			this.machineChoice.getSelectionModel().clearSelection();
			this.setWaitingForInternalRepresentationHint();
			return CompletableFuture.completedFuture(null);
		}

		return cliExecutor.submit(() -> {
			GetInternalRepresentationCommand cmd = new GetInternalRepresentationCommand();
			cmd.setTranslationMode(this.cbUnicode.isSelected() ? FormulaTranslationMode.UNICODE : FormulaTranslationMode.ASCII);
			cmd.setTypeInfos(GetInternalRepresentationCommand.TypeInfos.NEEDED);
			stateSpace.execute(cmd);
			return cmd.getPrettyPrint();
		}).thenApplyAsync(text -> {
			this.setEditorText(text, machinePath);
			return null;
		}, fxExecutor);
	}

	private void updateIncludedMachines() {
		this.ignoreMachineChoiceSelectionChange = true;
		try {
			Path prevSelected = this.machineChoice.getSelectionModel().getSelectedItem();
			machineChoice.getItems().setAll(currentTrace.getModel().getAllFiles());

			if (prevSelected != null && this.machineChoice.getItems().contains(prevSelected)) {
				this.selectMachine(prevSelected);
				return;
			}
		} finally {
			this.ignoreMachineChoiceSelectionChange = false;
		}

		Path cachedSelected = currentProject.getCurrentMachine().getCachedEditorState().getSelectedMachine();
		if (cachedSelected != null && this.machineChoice.getItems().contains(cachedSelected)) {
			this.selectMachine(cachedSelected);
		} else {
			this.machineChoice.getSelectionModel().selectFirst();
		}
	}

	private void registerFile(Path path) {
		this.resetWatching();
		Path directory = Objects.requireNonNull(path, "path").getParent();
		if (directory == null || !Files.isRegularFile(path)) {
			LOGGER.error("Could not register watch service for invalid file path {} with parent {}", path, directory);
			return;
		}

		WatchService watcher;
		try {
			watcher = directory.getFileSystem().newWatchService();
			directory.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException e) {
			LOGGER.error("Could not register file: {}", path, e);
			return;
		}

		this.watchThread = new Thread(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				WatchKey key;
				try {
					key = watcher.take();
				} catch (InterruptedException ignored) {
					return;
				}
				for (WatchEvent<?> event : key.pollEvents()) {
					if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
						continue;
					}

					if (path.getFileName().equals(event.context())) {
						// Only reload on events for the file itself, not for other files in the directory.
						Platform.runLater(() -> {
							loadText(path).exceptionally(exc -> {
								stageManager.showUnhandledExceptionAlert(exc, this.getScene().getWindow());
								return null;
							});
							this.updateFileContentChanged();
						});
						// if our file changed we do not care about other changes, let's just queue the reload and wait for the next change
						break;
					}
				}
				if (!key.reset()) {
					return;
				}
			}
		}, "BEditor File Change Watcher for " + path.getFileName());
		this.watchThread.setDaemon(true);
		this.watchThread.start();
	}

	private void updateFileContentChanged() {
		boolean newFileContentChanged;
		if (this.getPath() != null) {
			String lastLoaded = Objects.requireNonNullElse(this.lastLoadedText.get(), "");
			String editor = Objects.requireNonNullElse(this.beditor.getText(), "");
			newFileContentChanged = !lastLoaded.equals(editor);
		} else {
			// do not show file content changed warning when there is no file open right now
			newFileContentChanged = false;
		}
		this.fileContentChanged.set(newFileContentChanged);
	}

	public ObjectProperty<Path> pathProperty() {
		return this.path;
	}

	public Path getPath() {
		return this.pathProperty().get();
	}

	public void setPath(final Path path) {
		this.pathProperty().set(path);
	}

	public BooleanProperty autoReloadMachineProperty() {
		return this.autoReloadMachine;
	}

	public ReadOnlyBooleanProperty savedProperty() {
		return saved;
	}

	private void setHint() {
		this.setEditorText(i18n.translate("beditor.hint"), null);
		beditor.setEditable(false);
	}

	private void setWaitingForInternalRepresentationHint() {
		this.setEditorText(i18n.translate("beditor.hint.waitingForInternalRepresentation"), null);
		beditor.setEditable(false);
	}

	private void setEditorText(String text, Path path) {
		if (this.saving) {
			return;
		}

		beditor.setChangingText(true);
		try {
			if (
				this.getPath() != null && this.getPath().equals(path)
					&& this.lastSavedText.get() != null && this.lastSavedText.get().equals(text)
			) {
				// fact 1: we are loading the same file path that we have open right now
				// fact 2: the file contents equal the last saved text
				// conclusion: either nothing changed or the user changed the text in the editor
				// => do not discard the changes of the user!
				this.updateSaved();
				return;
			}

			Stopwatch sw = Stopwatch.createStarted();
			// reset state
			beditor.clearHistory();
			beditor.setSearchResults(null);
			beditor.cancelHighlighting();
			// load new text
			this.setPath(path);
			beditor.replaceText(""); // doing an extra step here to force a reload of the highlighting
			beditor.replaceText(text);
			beditor.moveTo(0);
			beditor.requestFollowCaret();
			lastSavedText.set(beditor.getText());
			beditor.clearHistory(); // clear history again to forget setting of text, might fix hhu-stups/prob-issues#363
			beditor.setEditable(true);
			LOGGER.debug("Setting editor text took {}", sw.stop());
		} finally {
			beditor.setChangingText(false);
			// trigger a garbage collection whenever the text changes to remove old style information ASAP
			System.gc();
		}
	}

	private void setText(Path path) {
		if (!Files.isRegularFile(Objects.requireNonNull(path, "path"))) {
			LOGGER.error("Could not read text of invalid file path {}", path);
			return;
		}

		Stopwatch sw = Stopwatch.createStarted();
		String text;
		try {
			text = Files.readString(path, EDITOR_CHARSET);
		} catch (IOException e) {
			LOGGER.error("Could not read file: {}", path, e);
			final Alert alert;
			if (e instanceof CharacterCodingException) {
				alert = stageManager.makeExceptionAlert(e, "beditor.encodingError.header", "beditor.encodingError.content", path);
			} else {
				alert = stageManager.makeExceptionAlert(e, "common.alerts.couldNotOpenFile.content", path);
			}

			alert.initOwner(this.getScene().getWindow());
			alert.show();
			return;
		}
		LOGGER.debug("Loading file '{}' took {}", path, sw.stop());
		this.setEditorText(text, path);
	}

	@FXML
	public void handleSave() {
		if (this.saving) {
			return;
		}

		this.saving = true;
		try {
			resetWatching();
			assert this.getPath() != null;
			try {
				Files.writeString(this.getPath(), beditor.getText(), EDITOR_CHARSET, StandardOpenOption.TRUNCATE_EXISTING);
			} catch (IOException e) {
				final Alert alert = stageManager.makeExceptionAlert(e, "common.alerts.couldNotSaveFile.content", path);
				alert.initOwner(this.getScene().getWindow());
				alert.showAndWait();
				LOGGER.error("Could not save file: {}", path, e);
				return;
			}
			lastSavedText.set(beditor.getText());
			registerFile(this.getPath());

			if (autoReloadMachine.get()) {
				currentProject.reloadCurrentMachine();
			} else {
				this.updateFileContentChanged();
			}
		} finally {
			this.saving = false;
		}
	}

	public void updateLoadedText() {
		lastLoadedText.set(beditor.getText());
		fileContentChanged.set(false);
	}

	private void resetWatching() {
		if (this.watchThread != null) {
			this.watchThread.interrupt();
			this.watchThread = null;
		}
	}

	@FXML
	private void handleOpenExternal() {
		injector.getInstance(ExternalEditor.class).open(this.getPath());
	}

	private static boolean fileNameMatchesCurrentPath(String filename, Path currentPath) {
		Objects.requireNonNull(currentPath, "currentPath");
		if (filename == null || filename.isEmpty()) {
			return false;
		}

		try {
			return Files.isSameFile(Path.of(filename), currentPath);
		} catch (IOException | InvalidPathException e) {
			LOGGER.warn("Failed to check if file is identical to editor file", e);
			return false;
		}
	}

	public ObservableList<ErrorItem> getErrors() {
		return this.errors;
	}

	public void selectMachine(Path path) {
		machineChoice.getSelectionModel().select(path);
	}

	private void focusAndShowMachine(Path machinePath) {
		stageManager.getMainStage().toFront();
		injector.getInstance(MainView.class).switchTabPane("beditorTab");
		this.selectMachine(machinePath);
		beditor.requestFocus();
	}

	public void jumpToPosition(int paragraphIndex, int columnIndex) {
		beditor.moveTo(paragraphIndex, columnIndex);
		beditor.requestFollowCaret();
	}

	public void jumpToPosition(int position) {
		beditor.moveTo(position);
		beditor.requestFollowCaret();
	}

	public void jumpToSource(Path machinePath, int paragraphIndex, int columnIndex) {
		this.focusAndShowMachine(machinePath);
		this.jumpToPosition(paragraphIndex, columnIndex);
	}

	public void jumpToErrorSource(ErrorItem.Location errorLocation) {
		this.focusAndShowMachine(Path.of(errorLocation.getFilename()));
		beditor.jumpToErrorSource(errorLocation);
	}

	public void jumpToSearchResult(ErrorItem.Location searchResultLocation) {
		this.focusAndShowMachine(Path.of(searchResultLocation.getFilename()));
		beditor.jumpToSearchResult(searchResultLocation);
	}
}
