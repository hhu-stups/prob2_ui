package de.prob2.ui.beditor;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.be4.classicalb.core.parser.rules.IModel;
import de.prob.animator.command.GetInternalRepresentationCommand;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.FormulaTranslationMode;
import de.prob.model.brules.RulesModel;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.scripting.EventBFactory;
import de.prob.scripting.EventBPackageFactory;
import de.prob.statespace.StateSpace;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.internal.TextAreaState;
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
public class BEditorView extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(BEditorView.class);
	private static final Charset EDITOR_CHARSET = StandardCharsets.UTF_8;

	@FXML
	private Button saveButton;

	@FXML
	private Button openExternalButton;

	@FXML
	private Button searchButton;

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
	private final Injector injector;

	private final ObjectProperty<Path> path;
	private final StringProperty lastSavedText;
	private final BooleanProperty saved;
	private final ObservableList<ErrorItem> errors;

	private Thread watchThread;
	private WatchKey key;
	private boolean saving;

	@Inject
	private BEditorView(final StageManager stageManager, final I18n i18n, final CurrentProject currentProject, final CurrentTrace currentTrace, final Injector injector) {
		this.stageManager = stageManager;
		this.i18n = i18n;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.injector = injector;
		this.path = new SimpleObjectProperty<>(this, "path", null);
		this.lastSavedText = new SimpleStringProperty(this, "lastSavedText", null);
		this.saved = new SimpleBooleanProperty(this, "saved", true);
		this.errors = FXCollections.observableArrayList();
		this.watchThread = null;
		this.key = null;
		stageManager.loadFXML(this, "beditorView.fxml");
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
		warningLabel.textProperty().bind(
			Bindings.when(saved)
				.then("")
				.otherwise(i18n.translate("beditor.unsavedWarning"))
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
				// Until that happens, display only the main machine and the cached selection.
				machineChoice.getItems().setAll(currentProject.get().getAbsoluteMachinePath(currentProject.getCurrentMachine()));
				Path selectedMachine = to.getCachedEditorState().getSelectedMachine();
				if (selectedMachine != null) {
					selectMachine(selectedMachine);
				} else {
					machineChoice.getSelectionModel().selectFirst();
				}
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
			if (to == null) {
				return;
			}
			switchMachine(to);
		});

		cbUnicode.selectedProperty().addListener((observable, from, to) -> showInternalRepresentation(currentTrace.getStateSpace(), path.get()));

		helpButton.setHelpContent("mainView.editor", null);

		Nodes.addInputMap(this, InputMap.consume(EventPattern.keyPressed(KeyCode.F, KeyCombination.SHORTCUT_DOWN), e -> handleSearch()));
	}

	BEditor getEditor() {
		return this.beditor;
	}

	private void updateSaved() {
		boolean newSaved = getPath() == null;
		if (!newSaved) {
			String lastSaved = Objects.requireNonNullElse(this.lastSavedText.get(), "");
			String editor = Objects.requireNonNullElse(this.beditor.getText(), "");
			newSaved = lastSaved.equals(editor);
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
		loadText(machinePath);
		beditor.clearHistory();

		currentProject.getCurrentMachine().getCachedEditorState().setSelectedMachine(machinePath);

		beditor.requestFocus();
		TextAreaState textAreaState = currentProject.getCurrentMachine().getCachedEditorState().getTextAreaState(machinePath);
		if (textAreaState != null) {
			restoreState(textAreaState);
		}
	}

	private void restoreState(TextAreaState textAreaState) {
		beditor.moveTo(Math.max(0, Math.min(beditor.getLength(), textAreaState.caretPosition)));
		beditor.requestFollowCaret();
	}

	private void loadText(Path machinePath) {
		if (currentProject.getCurrentMachine().getModelFactoryClass() == EventBFactory.class || currentProject.getCurrentMachine().getModelFactoryClass() == EventBPackageFactory.class) {
			final StateSpace stateSpace = currentTrace.getStateSpace();
			cbUnicode.setVisible(true);
			cbUnicode.setManaged(true);
			if (stateSpace == null) {
				setHint();
				cbUnicode.setSelected(false);
			} else {
				cbUnicode.setSelected(true);
				showInternalRepresentation(stateSpace, machinePath);
			}
		} else {
			cbUnicode.setVisible(false);
			cbUnicode.setManaged(false);
			cbUnicode.setSelected(false);
			setText(machinePath);
		}
	}

	private void showInternalRepresentation(StateSpace stateSpace, Path machinePath) {
		if (stateSpace == null) {
			return;
		}

		final GetInternalRepresentationCommand cmd = new GetInternalRepresentationCommand();
		cmd.setTranslationMode(this.cbUnicode.isSelected() ? FormulaTranslationMode.UNICODE : FormulaTranslationMode.ASCII);
		cmd.setTypeInfos(GetInternalRepresentationCommand.TypeInfos.NEEDED);
		stateSpace.execute(cmd);
		this.setEditorText(cmd.getPrettyPrint(), machinePath);
		this.beditor.setEditable(false);
	}

	private void updateIncludedMachines() {
		final AbstractModel model = currentTrace.getModel();
		if (model instanceof ClassicalBModel) {
			machineChoice.getItems().setAll(((ClassicalBModel) model).getLoadedMachineFiles());
		} else if (model instanceof RulesModel) {
			// references of main machine are not enough; referenced can reference other rmch again
			machineChoice.getItems().setAll(((RulesModel) model).getRulesProject().getBModels().stream()
				                                .map(IModel::getMachineReferences)
				                                .flatMap(refs -> refs.stream()
					                                                 .map(ref -> Paths.get(ref.getFile().getPath())))
				                                .distinct().toList());
			// main machine is not included in list of referenced rmch
			machineChoice.getItems().add(currentProject.get().getAbsoluteMachinePath(currentProject.getCurrentMachine()));
		} else {
			// TODO: We could extract the refinement hierarchy via model.getMachines() or model.calculateDependencies on the EventBModel.
			// Here, we have the problem that the current project might not include all of them refined machines
			// Still, could we assume that the machine is located in the same folder as the loaded machine? If yes, then it might still be possible to implement this feature
			machineChoice.getItems().setAll(currentProject.get().getAbsoluteMachinePath(currentProject.getCurrentMachine()));
		}

		Path selectedMachine = currentProject.getCurrentMachine().getCachedEditorState().getSelectedMachine();
		if (selectedMachine != null) {
			selectMachine(selectedMachine);
		} else {
			machineChoice.getSelectionModel().selectFirst();
		}
	}

	private void registerFile(Path path) {
		Path directory = Objects.requireNonNull(path, "path").getParent();
		if (!Files.isRegularFile(path) || directory == null) {
			LOGGER.error("Could not register watch service for invalid file path {} with parent {}", path, directory);
			return;
		}

		WatchService watcher;
		try {
			watcher = directory.getFileSystem().newWatchService();
			directory.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException e) {
			LOGGER.error("Could not register file: {}", path, e);
			return;
		}
		watchThread = new Thread(() -> {
			while (true) {
				try {
					key = watcher.take();
				} catch (InterruptedException ignored) {
					Thread.currentThread().interrupt();
					return;
				}
				for (WatchEvent<?> event : key.pollEvents()) {
					if (path.getFileName().equals(event.context())) {
						// Only reload on events for the file itself, not for other files in the directory.
						Platform.runLater(() -> loadText(path));
					}
				}
				key.reset();
			}
		}, "BEditor File Change Watcher");
		injector.getInstance(StopActions.class).add(watchThread::interrupt);
		watchThread.start();
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

	public ReadOnlyBooleanProperty savedProperty() {
		return saved;
	}

	private void setHint() {
		this.setEditorText(i18n.translate("beditor.hint"), null);
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
				// conclusion: either nothing changed or the user changed the editor text
				// => do not discard the changes of the user!
				this.updateSaved();
				return;
			}

			this.setPath(path);
			beditor.replaceText(text);
			beditor.moveTo(0);
			beditor.requestFollowCaret();
			lastSavedText.set(beditor.getText());
			beditor.setEditable(true);
		} finally {
			beditor.setChangingText(false);
			beditor.reloadHighlighting();
		}
	}

	private void setText(Path path) {
		if (!Files.isRegularFile(Objects.requireNonNull(path, "path"))) {
			LOGGER.error("Could not read text of invalid file path {}", path);
			return;
		}

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

			currentProject.reloadCurrentMachine();
		} finally {
			this.saving = false;
		}
	}

	private void resetWatching() {
		if (watchThread != null) {
			watchThread.interrupt();
		}
		if (key != null) {
			key.reset();
		}
	}

	@FXML
	private void handleOpenExternal() {
		injector.getInstance(ExternalEditor.class).open(this.getPath());
	}

	@FXML
	public void handleSearch() {
		injector.getInstance(SearchStage.class).open(this);
	}

	private static boolean fileNameMatchesCurrentPath(String filename, Path currentPath) {
		Objects.requireNonNull(currentPath, "currentPath");
		if (filename == null || filename.isEmpty()) {
			return false;
		}

		try {
			return Files.isSameFile(Paths.get(filename), currentPath);
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
		this.focusAndShowMachine(Paths.get(errorLocation.getFilename()));
		beditor.jumpToErrorSource(errorLocation);
	}

	public void jumpToSearchResult(ErrorItem.Location searchResultLocation) {
		this.focusAndShowMachine(Paths.get(searchResultLocation.getFilename()));
		beditor.jumpToSearchResult(searchResultLocation);
	}
}
