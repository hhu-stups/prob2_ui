package de.prob2.ui.menu;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.animator.command.GetInternalRepresentationCommand;
import de.prob.animator.domainobjects.FormulaTranslationMode;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.CSPModel;
import de.prob.statespace.StateSpace;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.ExtendedCodeArea;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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
	private ExtendedCodeArea codeTextArea;
	
	@FXML
	private CheckBox cbUnicode;
	
	@FXML 
	private Button saveAsButton;
	
	private final StageManager stageManager;
	
	private final FileChooserManager fileChooserManager;
	
	private final CurrentTrace currentTrace;

	private final CurrentProject currentProject;
	
	@Inject
	private ViewCodeStage(final StageManager stageManager, final FileChooserManager fileChooserManager, final CurrentProject currentProject, final CurrentTrace currentTrace) {
		super();
		
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		
		this.stageManager.loadFXML(this, "view_code_stage.fxml");
	}
	
	@FXML
	private void initialize() {
		this.cbUnicode.selectedProperty().addListener((observable, from, to) -> updateCode(currentTrace.getStateSpace()));

		this.currentProject.currentMachineProperty().addListener((o, from, to) -> {
			if (to != null) {
				this.setTitle(to.getName());
			}
		});
		Machine currentMachine = currentProject.getCurrentMachine();
		if (currentMachine != null) {
			this.setTitle(currentMachine.getName());
		}
		
		this.currentTrace.stateSpaceProperty().addListener((observable, from, to) -> this.updateCode(to));
		this.updateCode(currentTrace.getStateSpace());
	}
	
	private void updateCode(StateSpace stateSpace) {
		if (stateSpace == null) {
			this.codeTextArea.clear();
			this.saveAsButton.setDisable(true);
			return;
		}
		
		AbstractModel model = stateSpace.getModel();
		saveAsButton.setDisable(!(model instanceof ClassicalBModel || model instanceof CSPModel));
		
		final GetInternalRepresentationCommand cmd = new GetInternalRepresentationCommand();
		cmd.setTranslationMode(cbUnicode.isSelected() ? FormulaTranslationMode.UNICODE : FormulaTranslationMode.ASCII);
		cmd.setTypeInfos(GetInternalRepresentationCommand.TypeInfos.NEEDED);
		stateSpace.execute(cmd);
		this.codeTextArea.replaceText(cmd.getPrettyPrint());
	}
	
	@FXML
	private void saveAs() {
		final FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().setAll(
			fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.classicalB", "mch"),
			fileChooserManager.getAllExtensionsFilter()
		);
		chooser.setInitialFileName(this.getTitle() + ".mch");
		final Path selected = fileChooserManager.showSaveFileChooser(chooser, FileChooserManager.Kind.PROJECTS_AND_MACHINES, this);
		if (selected == null) {
			return;
		}
		
		try (final Writer out = Files.newBufferedWriter(selected)) {
			out.write(this.codeTextArea.getText());
		} catch (IOException e) {
			LOGGER.error("Failed to save value to file", e);
			final Alert alert = stageManager.makeExceptionAlert(e, "common.alerts.couldNotSaveFile.content", selected);
			alert.initOwner(this);
			alert.showAndWait();
		}
	}
}
