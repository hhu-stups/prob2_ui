package de.prob2.ui.menu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.command.GetInternalRepresentationPrettyPrintCommand;
import de.prob.animator.command.GetInternalRepresentationPrettyPrintUnicodeCommand;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.CSPModel;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@FXMLInjected
public final class ViewCodeStage extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(ViewCodeStage.class);
	
	@FXML 
	private TextArea codeTextArea;
	
	@FXML
	private CheckBox cbUnicode;
	
	@FXML 
	private Button saveAsButton;
	
	private final StageManager stageManager;
	
	private final FileChooserManager fileChooserManager;
	
	private final CurrentTrace currentTrace;

	private final CurrentProject currentProject;
	
	private final StringProperty code;
	
	@Inject
	private ViewCodeStage(final StageManager stageManager, final FileChooserManager fileChooserManager, final CurrentProject currentProject, final CurrentTrace currentTrace) {
		super();
		
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		
		this.code = new SimpleStringProperty(this, "code", null);
		
		this.stageManager.loadFXML(this, "view_code_stage.fxml");
	}
	
	@FXML
	private void initialize() {
		this.codeTextArea.textProperty().bind(this.codeProperty());
		this.cbUnicode.selectedProperty().addListener((observable, from, to) -> setCode());

		this.currentTrace.addListener((observable, from, to) -> {
			if(to != null) {
				AbstractModel model = to.getModel();
				if(model instanceof ClassicalBModel || model instanceof CSPModel) {
					saveAsButton.setDisable(false);
				} else {
					saveAsButton.setDisable(true);
				}
				this.setTitle(currentProject.getCurrentMachine().getName());
				this.setCode();
			}
		});

		this.setOnShown(e -> {
			if(currentTrace.get() != null) {
				this.setTitle(currentProject.getCurrentMachine().getName());
				this.setCode();
			}
		});
	}
	
	public StringProperty codeProperty() {
		return this.code;
	}
	
	public String getCode() {
		return this.codeProperty().get();
	}
	
	public void setCode() {
		if(cbUnicode.isSelected()) {
			final GetInternalRepresentationPrettyPrintUnicodeCommand cmd = new GetInternalRepresentationPrettyPrintUnicodeCommand();
			this.currentTrace.getStateSpace().execute(cmd);
			this.codeProperty().set(cmd.getPrettyPrint());
		} else {
			final GetInternalRepresentationPrettyPrintCommand cmd = new GetInternalRepresentationPrettyPrintCommand();
			this.currentTrace.getStateSpace().execute(cmd);
			this.codeProperty().set(cmd.getPrettyPrint());
		}
	}
	
	@FXML
	private void saveAs() {
		final FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().setAll(
			fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.classicalB", "mch"),
			fileChooserManager.getAllExtensionsFilter()
		);
		chooser.setInitialFileName(this.getTitle() + ".mch");
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
			stageManager.makeExceptionAlert(e, "common.alerts.couldNotWriteFile.content", selected).showAndWait();
		} catch (IOException e) {
			LOGGER.error("Failed to save value to file", e);
			stageManager.makeExceptionAlert(e, "common.alerts.couldNotSaveFile.content", selected).showAndWait();
		}
	}
}
