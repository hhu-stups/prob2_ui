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
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.CSPModel;
import de.prob.statespace.Language;
import de.prob.statespace.StateSpace;
import de.prob2.ui.beditor.InternalRepresentationCodeArea;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.TranslatableAdapter;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import de.prob2.ui.project.machines.Machine;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@FXMLInjected
public final class ViewCodeStage extends Stage {
	private static final Logger LOGGER = LoggerFactory.getLogger(ViewCodeStage.class);
	
	@FXML 
	private InternalRepresentationCodeArea codeTextArea;

	@FXML
	private ChoiceBox<FormulaTranslationMode> cbFormulaTranslationMode;
	
	@FXML 
	private Button saveAsButton;
	
	private final StageManager stageManager;
	
	private final FileChooserManager fileChooserManager;

	private final I18n i18n;
	
	private final CurrentTrace currentTrace;

	private final CurrentProject currentProject;
	
	@Inject
	private ViewCodeStage(final StageManager stageManager, final FileChooserManager fileChooserManager, final I18n i18n,
	                      final CurrentProject currentProject, final CurrentTrace currentTrace) {
		super();
		
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.i18n = i18n;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		
		this.stageManager.loadFXML(this, "view_code_stage.fxml");
	}
	
	@FXML
	private void initialize() {
		this.cbFormulaTranslationMode.getItems().setAll(FormulaTranslationMode.values());
		this.cbFormulaTranslationMode.setConverter(this.i18n.translateConverter(TranslatableAdapter.adapter(ftm -> switch (ftm) {
			case ASCII -> "menu.viewCodeStage.formulaTranslationMode.ascii";
			case UNICODE -> "menu.viewCodeStage.formulaTranslationMode.unicode";
			case LATEX -> "menu.viewCodeStage.formulaTranslationMode.latex";
			case ATELIERB -> "menu.viewCodeStage.formulaTranslationMode.atelierb";
			case ATELIERB_PP -> "menu.viewCodeStage.formulaTranslationMode.atelierbpp";
		})));
		this.cbFormulaTranslationMode.setValue(FormulaTranslationMode.ASCII);
		this.cbFormulaTranslationMode.valueProperty().addListener((o, f, t)
				-> updateCode(currentTrace.getStateSpace()));

		this.currentProject.currentMachineProperty().addListener((o, from, to) -> {
			this.makeTitle(to);
		});
		this.makeTitle(currentProject.getCurrentMachine());
		
		this.currentTrace.stateSpaceProperty().addListener((observable, from, to) -> this.updateCode(to));
		this.updateCode(currentTrace.getStateSpace());
	}

	private void makeTitle(Machine machine) {
		if (machine != null)
			this.setTitle(machine.getName() + " (" + i18n.translate("menu.viewCodeStage.title") + ")");
	}
	
	private void updateCode(StateSpace stateSpace) {
		if (stateSpace == null) {
			this.codeTextArea.clear();
			this.saveAsButton.setDisable(true);
			return;
		}
		
		AbstractModel model = stateSpace.getModel();
		boolean allowExport = ((model.getLanguage().getTranslatedTo() == Language.CLASSICAL_B || model instanceof EventBModel)
					&& cbFormulaTranslationMode.getValue().equals(FormulaTranslationMode.ATELIERB))
				|| model instanceof ClassicalBModel
				|| model instanceof CSPModel;
		// if translation mode is AtelierB and internal rep is classical B or Event-B -> always enabled
		// else only if classical B or CSP
		saveAsButton.setDisable(!allowExport);
		
		final GetInternalRepresentationCommand cmd = new GetInternalRepresentationCommand();
		cmd.setTranslationMode(cbFormulaTranslationMode.getValue());
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
		chooser.setInitialFileName(currentProject.getCurrentMachine().getName());
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
