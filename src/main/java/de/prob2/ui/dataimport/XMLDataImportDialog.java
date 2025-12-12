package de.prob2.ui.dataimport;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.project.ProjectManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Singleton
public final class XMLDataImportDialog extends DataImportDialog {

	private static final ObservableList<String> xmlEncodings = FXCollections.observableArrayList(
			"auto", "ISO-8859-1", "ISO-8859-2", "ISO-8859-15", "UTF-8", "UTF-16", "UTF-16LE", "UTF-16BE",
			"UTF-32", "UTF-32LE", "UTF-32BE", "ANSI_X3.4-1968", "windows 1252"
		);

	private ChoiceBox<String> cbEncoding;

	private final ProjectManager projectManager;

	@Inject
	public XMLDataImportDialog(FileChooserManager fileChooserManager, I18n i18n, StageManager stageManager,
	                           ProjectManager projectManager) {
		super(fileChooserManager, i18n, stageManager, ImportType.XML);
		this.projectManager = projectManager;
		stageManager.loadFXML(this, "data_import_dialog.fxml");
	}

	@FXML
	public void initialize() {
		super.initialize();
		this.addAdditionalOptions();

		this.version.setText("LibraryXML " + i18n.translate("dataimport.dialog.options.xml.untyped"));
		this.cbEncoding.getSelectionModel().select("auto");
	}

	private void addAdditionalOptions() {
		Label label = new Label("Encoding: ");
		this.cbEncoding = new ChoiceBox<>(xmlEncodings);
		HBox hBox = new HBox();
		hBox.setSpacing(10);
		hBox.setAlignment(Pos.CENTER_LEFT);
		hBox.getChildren().addAll(label, cbEncoding);

		this.dialogOptions.getChildren().addAll(hBox);
	}

	@Override
	void importImplementation() {
		try {
			String importMch =
					"MACHINE " + machineName.get() + "\n" +
					"DEFINITIONS \"LibraryXML.def\"\n" +
					"CONSTANTS XML_DATA\n" +
					"PROPERTIES XML_DATA = READ_XML(\"" +
							file.get().toPath().toRealPath() + "\",\"" +
							cbEncoding.getSelectionModel().getSelectedItem() + "\")\n" +
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
