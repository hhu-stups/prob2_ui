package de.prob2.ui.beditor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import de.prob2.ui.helpsystem.HelpButton;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.menu.ExternalEditor;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

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
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class BEditorView extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(BEditorView.class);
	private static final Charset EDITOR_CHARSET = Charset.forName("UTF-8");

	@FXML private Button saveButton;
	@FXML private Button openExternalButton;
	@FXML private Label warningLabel;
	@FXML private BEditor beditor;
	@FXML private HelpButton helpButton;

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
		stageManager.loadFXML(this, "beditorView.fxml");
	}

	@FXML
	private void initialize() {
		// We can't use Bindings.equal here, because beditor.textProperty() is not an ObservableObjectValue.
		saved.bind(Bindings.createBooleanBinding(
			() -> Objects.equals(lastSavedText.get(), beditor.getText()),
			lastSavedText, beditor.textProperty()
		));
		currentTrace.stateSpaceProperty().addListener((o, from, to) -> reloaded.set(true));
		saveButton.disableProperty().bind(this.pathProperty().isNull().or(saved));
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
				this.setHint();
			} else {
				final Path machinePath = currentProject.getLocation().resolve(to.getPath());
				final String text;
				try (final Stream<String> lines = Files.lines(machinePath)) {
					text = lines.collect(Collectors.joining(System.lineSeparator()));
				} catch (IOException | UncheckedIOException e) {
					stageManager.makeExceptionAlert(e, "common.alerts.couldNotReadFile.content", machinePath).showAndWait();
					LOGGER.error(String.format("Could not read file: %s", machinePath), e);
					return;
				}
				this.setEditorText(text, machinePath);
			}
		});
		this.stopActions.add(beditor::stopHighlighting);
		helpButton.setHelpContent(this.getClass());
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

	@FXML
	public void handleSave() {
		lastSavedText.set(beditor.getText());
		reloaded.set(false);
		assert this.getPath() != null;
		// Maybe add something for the user, that reloads the machine automatically?
		try {
			Path path = this.getPath();
			Files.write(path, beditor.getText().getBytes(EDITOR_CHARSET), StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			stageManager.makeExceptionAlert(e, "common.alerts.couldNotSaveFile.content", path).showAndWait();
			LOGGER.error(String.format("Could not save file: %s", path), e);
		}
	}

	@FXML
	private void handleOpenExternal() {
		injector.getInstance(ExternalEditor.class).open(this.getPath());
	}
}
