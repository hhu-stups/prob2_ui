package de.prob2.ui.dataimport;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.ProjectManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
public final class JSONDataImportDialog extends DataImportDialog {

	private final ProjectManager projectManager;

	@Inject
	public JSONDataImportDialog(FileChooserManager fileChooserManager, I18n i18n, StageManager stageManager,
	                            ProjectManager projectManager) {
		super(fileChooserManager, i18n, stageManager, ImportType.JSON);
		this.projectManager = projectManager;
		stageManager.loadFXML(this, "data_import_dialog.fxml");
	}

	@FXML
	public void initialize() {
		super.initialize();
		this.version.setText("LibraryJSON");
	}

	@Override
	void importImplementation() {
		try {
			String importMch =
					"MACHINE " + machineName.get() + " SEES LibraryJSON\n" +
					"DEFINITIONS \"LibraryJSON.def\"\n" +
					"CONSTANTS JSON_DATA\n" +
					"PROPERTIES JSON_DATA = READ_JSON(\"" + file.get().toPath().toRealPath() + "\")\n" +
					"END";

			Path generated = directory.get().resolve(machineName.get() + ".mch");
			Files.write(generated, importMch.getBytes());
			Platform.runLater(() -> {
				projectManager.openFile(generated);
				this.close();
			});
		} catch (IOException ex) {
			Platform.runLater(() -> {
				Alert alert = stageManager.makeExceptionAlert(ex, "dataimport.dialog.error.failed", "dataimport.dialog.error.failed.content");
				alert.initOwner(this);
				alert.showAndWait();
			});
		}
	}
}
