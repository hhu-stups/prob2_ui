package de.prob2.ui.states;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob.animator.domainobjects.StateError;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.StageManager;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StateErrorDetailsStage extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(StateErrorDetailsStage.class);
	
	@FXML private TextArea descriptionTextArea;
	
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final ResourceBundle bundle;
	
	private final ObjectProperty<StateError> stateError;
	
	@Inject
	private StateErrorDetailsStage(final StageManager stageManager, final FileChooserManager fileChooserManager, final ResourceBundle bundle) {
		super();
		
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.bundle = bundle;
		
		this.stateError = new SimpleObjectProperty<>(this, "stateError", null);
		
		stageManager.loadFXML(this, "state_error_details_stage.fxml");
	}
	
	@FXML
	private void initialize() {
		this.stateErrorProperty().addListener((o, from, to) -> {
			if (to == null) {
				this.descriptionTextArea.setText(null);
			} else {
				this.descriptionTextArea.setText(to.getLongDescription());
			}
		});
	}
	
	public ObjectProperty<StateError> stateErrorProperty() {
		return this.stateError;
	}
	
	public StateError getStateError() {
		return this.stateError.get();
	}
	
	public void setStateError(final StateError stateError) {
		this.stateError.set(stateError);
	}
	
	@FXML
	private void saveAs() {
		final FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().setAll(
			fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.text", "txt"),
			fileChooserManager.getAllExtensionsFilter()
		);
		final String defaultFileName = bundle.getString("states.stateErrorDetailsStage.saveAs.defaultFileName");
		chooser.setInitialFileName(defaultFileName + ".txt");
		final Path selected = fileChooserManager.showSaveFileChooser(chooser, null, this);
		if (selected == null) {
			return;
		}
		
		try (final Writer out = Files.newBufferedWriter(selected)) {
			out.write(this.getStateError().getLongDescription());
		} catch (final IOException e) {
			LOGGER.error("Failed to save state error to file", e);
			stageManager.makeExceptionAlert(e, "common.alerts.couldNotSaveFile.content", selected).showAndWait();
		}
	}
}
