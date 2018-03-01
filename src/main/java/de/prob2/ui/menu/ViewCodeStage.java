package de.prob2.ui.menu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ViewCodeStage extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(ViewCodeStage.class);
	
	@FXML private TextArea codeTextArea;
	@FXML private Button saveAsButton;
	
	private final StageManager stageManager;
	private final ResourceBundle bundle;
	
	private final StringProperty code;
	
	@Inject
	private ViewCodeStage(final StageManager stageManager, final ResourceBundle bundle) {
		super();
		
		this.stageManager = stageManager;
		this.bundle = bundle;
		
		this.code = new SimpleStringProperty(this, "code", null);
		
		this.stageManager.loadFXML(this, "view_code_stage.fxml");
	}
	
	@FXML
	private void initialize() {
		this.codeTextArea.textProperty().bind(this.codeProperty());
	}
	
	public StringProperty codeProperty() {
		return this.code;
	}
	
	public String getCode() {
		return this.codeProperty().get();
	}
	
	public void setCode(final String code) {
		this.codeProperty().set(code);
	}
	
	@FXML
	private void saveAs() {
		final FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().setAll(
			new FileChooser.ExtensionFilter(bundle.getString("common.fileChooser.fileTypes.text"), "*.txt"),
			new FileChooser.ExtensionFilter(bundle.getString("common.fileChooser.fileTypes.all"), "*.*")
		);
		chooser.setInitialFileName(this.getTitle() + ".txt");
		final File selected = chooser.showSaveDialog(this);
		if (selected == null) {
			return;
		}
		
		try (
			final OutputStream os = new FileOutputStream(selected);
			final Writer out = new OutputStreamWriter(os, StandardCharsets.UTF_8)
		) {
			out.write(this.getCode());
		} catch (FileNotFoundException e) {
			LOGGER.error("Could not open file for writing", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, String.format(bundle.getString("states.fullValue.error.couldNotWriteFile"), e.getMessage())).showAndWait();
		} catch (IOException e) {
			LOGGER.error("Failed to save value to file", e);
			stageManager.makeAlert(Alert.AlertType.ERROR, String.format(bundle.getString("states.fullValue.error.couldNotSave"), e.getMessage())).showAndWait();
		}
	}
}
