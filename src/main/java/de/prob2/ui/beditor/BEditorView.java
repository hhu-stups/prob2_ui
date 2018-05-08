package de.prob2.ui.beditor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class BEditorView extends BorderPane {
	private static final Logger LOGGER = LoggerFactory.getLogger(BEditorView.class);
	private static final Charset EDITOR_CHARSET = Charset.forName("UTF-8");

	@FXML private Button saveButton;
	@FXML private BEditor beditor;

	private final StageManager stageManager;
	private final ResourceBundle bundle;
	private final CurrentProject currentProject;
	private final StopActions stopActions;

	private final ObjectProperty<Path> path;

	@Inject
	private BEditorView(final StageManager stageManager, final ResourceBundle bundle, final CurrentProject currentProject, final StopActions stopActions) {
		this.stageManager = stageManager;
		this.bundle = bundle;
		this.currentProject = currentProject;
		this.stopActions = stopActions;
		this.path = new SimpleObjectProperty<>(this, "path", null);
		stageManager.loadFXML(this, "beditorView.fxml");
	}

	@FXML
	private void initialize() {
		this.saveButton.disableProperty().bind(this.pathProperty().isNull());
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
					stageManager.makeAlert(
						Alert.AlertType.ERROR,
						String.format(bundle.getString("project.machines.error.couldNotReadFile"), machinePath, e)
					).showAndWait();
					return;
				}
				this.setEditorText(text, machinePath);
			}
		});
		this.stopActions.add(beditor::stopHighlighting);
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

	public void setHint(){
		this.setPath(null);
		beditor.clear();
		beditor.appendText(bundle.getString("beditor.hint"));
		beditor.getStyleClass().add("editor");
		beditor.startHighlighting();
		beditor.setEditable(false);
	}

	public void setEditorText(String text, Path path) {
		this.setPath(path);
		beditor.clear();
		beditor.appendText(text);
		beditor.getStyleClass().add("editor");
		beditor.startHighlighting();
		beditor.setEditable(true);
	}

	@FXML
	public void handleSave() {
		assert this.getPath() != null;
		// Maybe add something for the user, that reloads the machine automatically?
		try {
			Files.write(this.getPath(), beditor.getText().getBytes(EDITOR_CHARSET), StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			LOGGER.error(bundle.getString("beditor.couldNotSaveFile"), e);
		}
	}
}
