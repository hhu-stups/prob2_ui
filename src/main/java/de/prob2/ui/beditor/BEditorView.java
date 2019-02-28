package de.prob2.ui.beditor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob.animator.command.GetAllUsedFilenamesCommand;
import de.prob.animator.command.GetInternalRepresentationPrettyPrintCommand;
import de.prob.animator.domainobjects.ErrorItem;
import de.prob.animator.domainobjects.MachineFileInformation;
import de.prob.statespace.StateSpace;
import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.menu.ExternalEditor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FXMLInjected
@Singleton
public class BEditorView extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(BEditorView.class);
	private static final Charset EDITOR_CHARSET = StandardCharsets.UTF_8;

	@FXML private Button saveButton;
	@FXML private Button openExternalButton;
	@FXML private Label warningLabel;
	@FXML private BEditor beditor;
	@FXML private HelpButton helpButton;
	@FXML private ChoiceBox<MachineFileInformation> machineChoice;

	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final CurrentProject currentProject;
	private final CurrentTrace currentTrace;
	private final StopActions stopActions;
	private final Injector injector;

	private final ObjectProperty<Path> path;
	private final StringProperty lastSavedText;
	private final BooleanProperty saved;
	private final BooleanProperty reloaded;
	
	private Thread watchThread;
	private WatchKey key;

	@Inject
	private BEditorView(final StageManager stageManager, final ResourceBundle bundle, final CurrentProject currentProject, final CurrentTrace currentTrace, final StopActions stopActions, final Injector injector) {
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.currentProject = currentProject;
		this.currentTrace = currentTrace;
		this.stopActions = stopActions;
		this.injector = injector;
		this.path = new SimpleObjectProperty<>(this, "path", null);
		this.lastSavedText = new SimpleStringProperty(this, "lastSavedText", null);
		this.saved = new SimpleBooleanProperty(this, "saved", true);
		this.reloaded = new SimpleBooleanProperty(this, "reloaded", true);
		this.watchThread = null;
		this.key = null;
		stageManager.loadFXML(this, "beditorView.fxml");
	}

	@FXML
	private void initialize() {
		// We can't use Bindings.equal here, because beditor.textProperty() is not an ObservableObjectValue.
		saved.bind(Bindings.createBooleanBinding(
			() -> Objects.equals(lastSavedText.get(), beditor.getText()),
			lastSavedText, beditor.textProperty()
		).or(machineChoice.getSelectionModel().selectedItemProperty().isNull()));
		currentTrace.stateSpaceProperty().addListener((o, from, to) -> {
			if(to == null) {
				return;
			}
			updateIncludedMachines();
			reloaded.set(true);
		});
		saveButton.disableProperty().bind(saved);
		openExternalButton.disableProperty().bind(this.pathProperty().isNull());
		warningLabel.textProperty().bind(Bindings.when(saved)
			.then(Bindings.when(reloaded)
				.then("")
				.otherwise(bundle.getString("beditor.reloadWarning"))
			)
			.otherwise(bundle.getString("beditor.unsavedWarning"))
		);
		setHint();
		
		currentProject.currentMachineProperty().addListener((observable, from, to) -> {
			if (to == null) {
				machineChoice.getItems().clear();
				this.setHint();
			} else {
				// The correct list of included machines is available once the machine is fully loaded.
				// Until that happens, display only the main machine.
				machineChoice.getItems().setAll(currentMachineFileInformation());
				machineChoice.getSelectionModel().selectFirst();
			}
		});
		
		machineChoice.getSelectionModel().selectedItemProperty().addListener((observable, from, to) -> {
			if(to == null) {
				return;
			}
			switchMachine(to.getPath());
		});
		
		this.stopActions.add(beditor::stopHighlighting);
		helpButton.setHelpContent(this.getClass());
	}
	
	private void switchMachine(String path) {
		final Path machinePath = currentProject.getLocation().resolve(path);
		resetWatching();
		registerFile(machinePath);
		loadText(machinePath);
		beditor.clearHistory();
	}
	
	private void loadText(Path machinePath) {
		if(currentProject.getCurrentMachine().getType() == Machine.Type.EVENTB) {
			final StateSpace stateSpace = currentTrace.getStateSpace();
			if (stateSpace == null) {
				setHint();
			} else {
				GetInternalRepresentationPrettyPrintCommand cmd = new GetInternalRepresentationPrettyPrintCommand();
				stateSpace.execute(cmd);
				this.setEditorText(cmd.getPrettyPrint(), machinePath);
				beditor.setEditable(false);
			}
		} else {
			setText(machinePath);
		}
	}
	
	private MachineFileInformation currentMachineFileInformation() {
		final Path machinePath = currentProject.getCurrentMachine().getPath();
		final String[] split = machinePath.getFileName().toString().split("\\.", 2);
		assert split.length == 2;
		return new MachineFileInformation(split[0], split[1], machinePath.toString());
	}
	
	private void updateIncludedMachines() {
		GetAllUsedFilenamesCommand cmd = new GetAllUsedFilenamesCommand();
		currentTrace.getStateSpace().execute(cmd);
		if(cmd.getFiles().isEmpty()) {
			machineChoice.getItems().setAll(currentMachineFileInformation());
		} else {
			machineChoice.getItems().setAll(cmd.getFiles());
		}
		machineChoice.getSelectionModel().selectFirst();
	}
	
	private void registerFile(Path path) {
		Path directory = path.getParent();
		WatchService watcher;
		try {
			watcher = directory.getFileSystem().newWatchService();
			directory.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException e) {
			LOGGER.error(String.format("Could not register file: %s", path), e);
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
					WatchEvent.Kind<?> kind = event.kind();
					if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
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
		this.setEditorText(bundle.getString("beditor.hint"), null);
		beditor.setEditable(false);
	}

	private void setEditorText(String text, Path path) {
		this.setPath(path);
		this.lastSavedText.set(text);
		beditor.clear();
		beditor.appendText(text);
		beditor.getStyleClass().add("editor");
		beditor.startHighlighting();
		beditor.setEditable(true);
	}
	
	private void setText(Path path) {
		String text;
		try (final Stream<String> lines = Files.lines(path)) {
			text = lines.collect(Collectors.joining(System.lineSeparator()));
		} catch (IOException | UncheckedIOException e) {
			stageManager.makeExceptionAlert(e, "common.alerts.couldNotReadFile.content", path).showAndWait();
			LOGGER.error(String.format("Could not read file: %s", path), e);
			return;
		}
		this.setEditorText(text, path);
	}

	@FXML
	public void handleSave() {
		resetWatching();
		lastSavedText.set(beditor.getText());
		reloaded.set(false);
		assert this.getPath() != null;
		// Maybe add something for the user, that reloads the machine automatically?
		try {
			Files.write(this.getPath(), beditor.getText().getBytes(EDITOR_CHARSET), StandardOpenOption.TRUNCATE_EXISTING);
			registerFile(this.getPath());
		} catch (IOException e) {
			stageManager.makeExceptionAlert(e, "common.alerts.couldNotSaveFile.content", path).showAndWait();
			LOGGER.error(String.format("Could not save file: %s", path), e);
		}
	}
	
	private void resetWatching() {
		if(watchThread != null) {
			watchThread.interrupt();
		}
		if(key != null) {
			key.reset();
		}
	}

	@FXML
	private void handleOpenExternal() {
		injector.getInstance(ExternalEditor.class).open(this.getPath());
	}
	
	public void highlightErrorLocations(final Collection<ErrorItem.Location> locations) {
		beditor.getErrorLocations().setAll(locations);
	}
}
