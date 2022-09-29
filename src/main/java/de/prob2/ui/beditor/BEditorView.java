package de.prob2.ui.beditor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.GetInternalRepresentationCommand;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.FormulaTranslationMode;
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
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;

import org.fxmisc.richtext.model.StyleSpans;
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
	private StyleSpans<Collection<String>> highlighting;
	private boolean changingText;

	@Inject
	private BEditorView(final StageManager stageManager, final I18n i18n, final CurrentProject currentProject, final CurrentTrace currentTrace, final Injector injector) {
		/*
		 *	TODO: remember scroll position.
		 *   Getting scrollbar values does not work. Getting estimated y values of Virtualized Scroll Pane does produce weird values (Code area refreshing too often?)
		 */
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
		this.highlighting = null;
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
		warningLabel.textProperty().bind(Bindings.when(saved)
				                                 .then("")
				                                 .otherwise(i18n.translate("beditor.unsavedWarning"))
		);
		setHint();

		machineChoice.setConverter(new StringConverter<Path>() {
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
				}
			}
		});

		beditor.caretPositionProperty().addListener((observable, from, to) -> {
			if (!changingText) {
				// System.out.println("[" + machineChoice.getSelectionModel().getSelectedItem() + "] caretPosition: " + to);
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
			if (!changingText) {
				// System.out.println("[" + machineChoice.getSelectionModel().getSelectedItem() + "] scrollXPosition: " + to);
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
			if (!changingText) {
				// System.out.println("[" + machineChoice.getSelectionModel().getSelectedItem() + "] scrollYPosition: " + to);
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
			// System.out.println("[" + Thread.currentThread().getName() + "] selectedItem changed: from=" + from + " to=" + to);
			if (to == null) {
				return;
			}
			switchMachine(to);
		});

		cbUnicode.selectedProperty().addListener((observable, from, to) -> showInternalRepresentation(currentTrace.getStateSpace(), path.get()));

		helpButton.setHelpContent("mainView.editor", null);
	}

	private void updateSaved() {
		boolean newSaved = getPath() == null;
		if (!newSaved) {
			String lastSaved = this.lastSavedText.get();
			if (lastSaved == null) {
				lastSaved = "";
			}

			String editor = this.beditor.getText();
			if (editor == null) {
				editor = "";
			}

			newSaved = Arrays.equals(lastSaved.split("(\r)?\n"), editor.split("(\r)?\n"));
		}

		this.saved.set(newSaved);
	}

	private void updateErrors() {
		if (this.savedProperty().get()) {
			this.beditor.getErrors().setAll(this.getErrors().stream()
					                                .filter(error -> error.getLocations().stream()
							                                                 .map(ErrorItem.Location::getFilename)
							                                                 .anyMatch(this::isCurrentEditorFile))
					                                .collect(Collectors.toList()));
		} else {
			this.beditor.getErrors().clear();
		}
	}

	private void switchMachine(final Path machinePath) {
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
		// System.out.println("[" + machineChoice.getSelectionModel().getSelectedItem() + "] restoring: " + textAreaState);
		beditor.moveTo(textAreaState.caretPosition);
		beditor.requestFollowCaret();

		// TODO: scroll to old position
		// beditor.estimatedScrollXProperty().setValue(textAreaState.scrollXPosition);
		// beditor.estimatedScrollYProperty().setValue(textAreaState.scrollYPosition);
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
		cmd.setTranslationMode(cbUnicode.isSelected() ? FormulaTranslationMode.UNICODE : FormulaTranslationMode.ASCII);
		cmd.setTypeInfos(GetInternalRepresentationCommand.TypeInfos.NEEDED);
		stateSpace.execute(cmd);
		this.setEditorText(cmd.getPrettyPrint(), machinePath);
		beditor.setEditable(false);
	}

	private void updateIncludedMachines() {
		final AbstractModel model = currentTrace.getModel();
		if (model instanceof ClassicalBModel) {
			machineChoice.getItems().setAll(((ClassicalBModel) model).getLoadedMachineFiles());
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
		Path directory = path.getParent();
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
		changingText = true;
		this.setPath(path);
		this.lastSavedText.set(text);
		if (!beditor.getText().equals(text)) {
			beditor.replaceText(text);
			// beditor.moveTo(0);
			// beditor.requestFollowCaret();
		}
		beditor.setEditable(true);
		changingText = false;
	}

	private void setText(Path path) {
		String text;
		try (final Stream<String> lines = Files.lines(path)) {
			text = lines.collect(Collectors.joining(System.lineSeparator()));
		} catch (IOException | UncheckedIOException e) {
			LOGGER.error("Could not read file: {}", path, e);
			if (e.getCause() instanceof MalformedInputException) {
				final Alert alert = stageManager.makeExceptionAlert(e, "beditor.encodingError.header", "beditor.encodingError.content", path);
				alert.initOwner(this.getScene().getWindow());
				alert.show();
			} else {
				final Alert alert = stageManager.makeExceptionAlert(e, "common.alerts.couldNotReadFile.content", path);
				alert.initOwner(this.getScene().getWindow());
				alert.show();
			}
			return;
		}
		this.setEditorText(text, path);
	}

	@FXML
	public void handleSave() {
		resetWatching();
		assert this.getPath() != null;
		try {
			Files.write(this.getPath(), beditor.getText().getBytes(EDITOR_CHARSET), StandardOpenOption.TRUNCATE_EXISTING);
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

	private boolean isCurrentEditorFile(final String filename) {
		if (getPath() == null) {
			throw new IllegalStateException("cannot compare file to currently opened file because there is no opened file");
		}

		if (filename == null || filename.isEmpty()) {
			return false;
		}

		try {
			return Files.isSameFile(Paths.get(filename), this.getPath());
		} catch (Exception e) {
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

	public void jumpToSource(Path machinePath, int line, int column) {
		stageManager.getMainStage().toFront();
		injector.getInstance(MainView.class).switchTabPane("beditorTab");

		this.selectMachine(machinePath);

		BEditor bEditor = injector.getInstance(BEditor.class);
		bEditor.requestFocus();
		bEditor.moveTo(line, column);
		bEditor.requestFollowCaret();
	}

	void setHighlighting(StyleSpans<Collection<String>> highlighting) {
		this.highlighting = highlighting;
	}

	StyleSpans<Collection<String>> getHighlighting() {
		return highlighting;
	}
}
