package de.prob2.ui.dataimport;

import com.google.common.io.MoreFiles;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.executor.BackgroundUpdater;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class DataImportDialog extends Stage {
	public enum ImportType {
		CSV("CSV"),
		JSON("JSON"),
		XML("XML");

		private final String type;

		ImportType(String type) {
			this.type = type;
		}

		String getType() {
			return type;
		}
	}

	@FXML
	VBox dialogOptions;
	@FXML
	Label dialogTitle, fileType, version;
	@FXML
	private TextField fileLocationField, directoryField, machineNameField;
	@FXML
	private Tooltip fileLocationTooltip, directoryTooltip;
	@FXML
	private CheckBox cbMachineName;
	@FXML
	private HBox nameBox;
	@FXML
	private ProgressIndicator progressIndicator;
	@FXML
	private Button btCancel, btImportAndOpen;

	final SimpleObjectProperty<File> file = new SimpleObjectProperty<>();
	final SimpleObjectProperty<Path> directory = new SimpleObjectProperty<>();
	final SimpleStringProperty machineName = new SimpleStringProperty();
	private final ImportType importType;

	private final BackgroundUpdater updater;
	final FileChooserManager fileChooserManager;
	final I18n i18n;
	final StageManager stageManager;

	public DataImportDialog(FileChooserManager fileChooserManager, I18n i18n, StageManager stageManager,
	                        ImportType importType) {
		super();
		this.updater = new BackgroundUpdater("Data importer");
		this.fileChooserManager = fileChooserManager;
		this.i18n = i18n;
		this.stageManager = stageManager;
		this.importType = importType;
	}

	@FXML
	public void initialize() {
		initModality(Modality.APPLICATION_MODAL);

		String title = importType.getType() + " " + i18n.translate("menu.advanced.items.dataImport");
		this.setTitle(title);
		this.dialogTitle.setText(title);
		this.fileType.setText(importType.getType() + " " +  i18n.translate("dataimport.dialog.options.file"));

		this.file.addListener((obs, ov, nv) -> {
			if (nv != null) {
				fileLocationField.setText(nv.toPath().normalize().toString());
				fileLocationField.end();
			}
		});
		this.fileLocationTooltip.textProperty().bind(fileLocationField.textProperty());

		this.directory.addListener((obs, ov, nv) -> {
			if (nv != null) {
				directoryField.setText(nv.normalize().toString());
				directoryField.end();
			}
		});
		this.directoryTooltip.textProperty().bind(directoryField.textProperty());

		this.dialogOptions.disableProperty().bind(updater.runningProperty());
		this.btCancel.visibleProperty().bind(updater.runningProperty());
		this.progressIndicator.visibleProperty().bind(updater.runningProperty());
		this.btImportAndOpen.disableProperty().bind(updater.runningProperty());
		this.btImportAndOpen.setOnAction(e -> runImport());

		this.nameBox.disableProperty().bind(cbMachineName.selectedProperty().not());
		this.cbMachineName.selectedProperty().addListener((obs, ov, nv) -> checkMachineNameField(nv, machineNameField.getText()));
		this.machineNameField.textProperty().addListener((obs, ov, nv) -> checkMachineNameField(cbMachineName.isSelected(), nv));
	}

	private void runImport() {
		if (!checkFiles())
			return;

		updater.execute(this::importImplementation);
	}

	void importImplementation() {
		// do nothing
	}

	@FXML
	public void selectFile() {
		String ext = importType.getType().toLowerCase();
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("dataimport.dialog.fileChooser.title"));
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes." + ext, ext));
		Path path = fileChooserManager.showOpenFileChooser(fileChooser, FileChooserManager.Kind.DATA_IMPORT, stageManager.getCurrent());
		if (path != null) {
			file.set(path.toFile());
		}
	}

	@FXML
	public void selectDirectory() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(i18n.translate("project.newProjectStage.directoryChooser.selectLocation.title"));
		Path path = fileChooserManager.showDirectoryChooser(directoryChooser, FileChooserManager.Kind.DATA_IMPORT, stageManager.getCurrent());
		if (path != null) {
			directory.set(path.toAbsolutePath());
		}
	}

	@FXML
	public void selectDefaultDirectory() {
		if (file.get() != null && file.get().getParentFile() != null) {
			directory.set(file.get().getParentFile().toPath());
		}
	}

	@FXML
	public void selectDefaultName() {
		if (file.get() != null) {
			machineNameField.setText(MoreFiles.getNameWithoutExtension(file.get().toPath()));
			machineNameField.end();
		}
	}

	@FXML
	public void cancel() {
		updater.cancel(true);
	}

	private boolean checkFiles() {
		if (file.get() == null || !Files.exists(file.get().toPath())) {
			Alert alert = this.stageManager.makeAlert(Alert.AlertType.ERROR, "dataimport.dialog.error.failed", "dataimport.dialog.error.fileNotExists", file.get());
			alert.initOwner(this);
			alert.show();
			return false;
		}

		if (directory.get() == null || !Files.isDirectory(directory.get())) {
			Alert alert = this.stageManager.makeAlert(Alert.AlertType.ERROR, "dataimport.dialog.error.failed", "dataimport.dialog.error.invalidDirectory", directory.get());
			alert.initOwner(this);
			alert.show();
			return false;
		}

		if (cbMachineName.isSelected()) {
			machineName.set(this.machineNameField.getText());
		} else {
			machineName.set(MoreFiles.getNameWithoutExtension(file.get().toPath()));
		}
		if (invalidMachineName(machineName.get())) {
			Alert alert = this.stageManager.makeAlert(Alert.AlertType.ERROR, "dataimport.dialog.error.failed", "dataimport.dialog.error.invalidName", machineNameField.getText());
			alert.initOwner(this);
			alert.show();
			return false;
		}

		Path machinePath = directory.get().resolve(machineName.get() + ".mch");
		if (confirmMachineReplace() && Files.exists(machinePath) && !confirmReplace(machinePath.toString())) {
			this.toFront();
			return false;
		}
		return additionalFileChecks();
	}

	boolean confirmMachineReplace() {
		return true;
	}

	boolean additionalFileChecks() {
		return true;
	}

	private void checkMachineNameField(boolean isSelected, String machineNameField) {
		if (isSelected && invalidMachineName(machineNameField)) {
			this.machineNameField.setStyle(
					"-fx-border-color: red; " +
					"-fx-border-width: 2px; " +
					"-fx-text-fill: red;"
			);
		} else {
			this.machineNameField.setStyle("");
		}
	}

	private boolean invalidMachineName(String machineName) {
		return machineName == null || machineName.isBlank() || machineName.contains(".");
	}

	boolean confirmReplace(String file) {
		final Alert alert = this.stageManager.makeAlert(Alert.AlertType.CONFIRMATION,
				"dataimport.dialog.error.fileExists.title",
				"dataimport.dialog.error.fileExists.content",
				file);
		Optional<ButtonType> result = alert.showAndWait();
		return result.isPresent() && ButtonType.OK.equals(result.get());
	}
}
