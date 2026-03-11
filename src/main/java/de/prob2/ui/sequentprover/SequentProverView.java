package de.prob2.ui.sequentprover;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob.animator.command.ExportSequentProverProofCommand;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.prob2fx.CurrentTrace;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;

import java.nio.file.Path;

@FXMLInjected
@Singleton
public final class SequentProverView extends AnchorPane {

	@FXML
	private Button btnExportProof;

	private final StageManager stageManager;
	private final I18n i18n;
	private final FileChooserManager fileChooserManager;
	private final CurrentTrace currentTrace;
	private final CurrentProject currentProject;

	@Inject
	public SequentProverView(final StageManager stageManager, final I18n i18n, final FileChooserManager fileChooserManager,
	                         final CurrentTrace currentTrace, final CurrentProject currentProject) {
		this.i18n = i18n;
		this.fileChooserManager = fileChooserManager;
		this.currentTrace = currentTrace;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.stageManager.loadFXML(this, "sequent_prover_view.fxml");
	}

	@FXML
	public void initialize() {
		currentTrace.addListener((o, oldTrace, newTrace) ->
			btnExportProof.setDisable(newTrace == null || newTrace.getCurrentState() == null || !newTrace.getCurrentState().isInitialised()));
	}

	@FXML
	public void exportProof() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("common.fileChooser.save.title"));
		fileChooser.setInitialFileName(currentProject.getCurrentMachine().getName());
		fileChooser.getExtensionFilters().addAll(
				fileChooserManager.getHtmlFilter(),
				fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.bpr", "bpr"),
				fileChooserManager.getPdfFilter(),
				fileChooserManager.getPngFilter(),
				fileChooserManager.getSvgFilter(),
				fileChooserManager.getDotFilter()
		);
		Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.VISUALISATIONS, stageManager.getCurrent());
		if (path != null) {
			ExportSequentProverProofCommand cmd = new ExportSequentProverProofCommand(path, currentTrace.get());
			currentTrace.getStateSpace().execute(cmd);
		}
	}

}
