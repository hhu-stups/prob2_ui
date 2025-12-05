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
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

@Singleton
public final class CSVDataImportDialog extends DataImportDialog {

	private CheckBox cbSequence, cbSkipFirstRow, cbAllowAdditionalColumns;
	private Spinner<Integer> nrColumns;

	private final ProjectManager projectManager;

	@Inject
	public CSVDataImportDialog(FileChooserManager fileChooserManager, I18n i18n, StageManager stageManager,
	                           ProjectManager projectManager) {
		super(fileChooserManager, i18n, stageManager, ImportType.CSV);
		this.projectManager = projectManager;
		stageManager.loadFXML(this, "data_import_dialog.fxml");
	}

	@FXML
	public void initialize() {
		super.initialize();
		this.addAdditionalOptions();

		this.version.setText("LibraryCSV");

		this.cbSequence.setSelected(true);
		this.cbSkipFirstRow.setSelected(false);
		this.cbAllowAdditionalColumns.setSelected(true);

		this.file.addListener((obs, ov, nv) -> {
			try (CSVParser parser = CSVParser.parse(nv, StandardCharsets.UTF_8, CSVFormat.DEFAULT)) {
				int maxNrColumns = 0;
				for (CSVRecord record : parser) {
					if (record.size() >= maxNrColumns) {
						maxNrColumns = record.size();
					}
					if (!record.isConsistent()) {
						this.cbAllowAdditionalColumns.setSelected(true);
					}
				}
				this.nrColumns.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxNrColumns, maxNrColumns));
			} catch (IOException ignore) {}
		});
	}

	private void addAdditionalOptions() {
		this.cbSequence = new CheckBox(i18n.translate("dataimport.dialog.options.csv.asSequence"));
		this.cbSkipFirstRow = new CheckBox(i18n.translate("dataimport.dialog.options.csv.skipFirstRow"));
		this.cbAllowAdditionalColumns = new CheckBox(i18n.translate("dataimport.dialog.options.csv.allowAdditionalColumns"));
		this.nrColumns = new Spinner<>(1, Integer.MAX_VALUE, 1);

		Label typeLabel = new Label(i18n.translate("dataimport.dialog.options.csv.nrColumns"));
		this.dialogOptions.getChildren().addAll(cbSequence, cbSkipFirstRow, cbAllowAdditionalColumns, typeLabel, nrColumns);
	}

	@Override
	void importImplementation() {
		try {
			String importMch =
					"MACHINE " + machineName.get() + "\n" +
					"DEFINITIONS \"LibraryCSV.def\"\n" +
					"CONSTANTS CSV_DATA\n" +
					"PROPERTIES\n" +
							"    CSV_DATA : " + (cbSequence.isSelected() ? "seq(" : "POW(") + String.join("*", Collections.nCopies(nrColumns.getValue(), "STRING")) + ") & // specify custom column types\n" +
							"    CSV_DATA = READ_CSV" + (cbSequence.isSelected() ? "_SEQUENCE" : "") + "(\"" +
							file.get().toPath().toRealPath() + "\"," +
							Boolean.toString(cbSkipFirstRow.isSelected()).toUpperCase() + "," +
							Boolean.toString(cbAllowAdditionalColumns.isSelected()).toUpperCase() + ")\n" +
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
