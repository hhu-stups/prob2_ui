package de.prob2.ui.project.machines;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.ProjectManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EditMachinesDialog extends Dialog<Machine> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EditMachinesDialog.class);

	@FXML
	private TextField nameField;
	@FXML
	private Label nameErrorExplanationLabel;
	@FXML
	private TextField locationField;
	@FXML
	private Label locationErrorExplanationLabel;
	@FXML
	private Button changeLocationButton;
	@FXML
	private TextArea descriptionTextArea;
	@FXML
	private ButtonType okButtonType;

	private final I18n i18n;
	private final CurrentProject currentProject;
	private final ProjectManager projectManager;
	private final FileChooserManager fileChooserManager;

	private Machine machine;

	@Inject
	public EditMachinesDialog(StageManager stageManager, I18n i18n, CurrentProject currentProject, ProjectManager projectManager, FileChooserManager fileChooserManager, FileChooserManager fileChooserManager1) {
		this.i18n = i18n;
		this.currentProject = currentProject;
		this.projectManager = projectManager;
		this.fileChooserManager = fileChooserManager1;

		this.setResultConverter(type -> {
			if (type == null || type.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
				return null;
			} else {
				boolean needsProjectSave = false;
				boolean needsMachineReload = false;

				Path absoluteOld = null;
				Path absoluteNew = null;
				try {
					absoluteOld = this.getOldAbsolutePath();
					absoluteNew = this.getNewAbsolutePath();
					absoluteNew = absoluteNew.toRealPath();
					Path relativeNew = this.currentProject.getLocation().toRealPath().relativize(absoluteNew);
					if (!relativeNew.equals(this.machine.getLocation())) {
						needsProjectSave = true;
						if (this.currentProject.getCurrentMachine() == this.machine) {
							if (!this.currentProject.confirmMachineReplace()) {
								return null;
							}

							needsMachineReload = true;
						}

						LOGGER.info("changing location of machine {} from {} to {}", machine.getName(), absoluteOld, absoluteNew);
						this.machine.setLocation(relativeNew);
					}
				} catch (Exception e) {
					String finalOld = absoluteOld != null ? absoluteOld.toString() : this.machine.getLocation().toString();
					String finalNew = absoluteNew != null ? absoluteNew.toString() : this.locationField.getText();
					LOGGER.error(
							"could not move machine {} from {} to {}",
							this.machine.getName(),
							finalOld,
							finalNew,
							e
					);
					Platform.runLater(() -> stageManager.makeExceptionAlert(
							e,
							"project.machines.editMachinesDialog.locationErrorDialog.content",
							machine.getName(),
							finalOld,
							finalNew
					));
					return null;
				}

				this.machine.setName(nameField.getText());
				this.machine.setDescription(descriptionTextArea.getText());

				if (needsProjectSave) {
					// we need to save the current project, else we might lose a renamed machine!
					this.projectManager.saveCurrentProject();
				}
				if (needsMachineReload) {
					// if we changed the path of the current machine we need to reload it so the editor works again
					// we already asked for confirmation above
					Platform.runLater(() -> this.currentProject.loadMachineWithoutConfirmation(this.machine));
				}

				return this.machine;
			}
		});
		stageManager.loadFXML(this, "edit_machines_dialog.fxml");
	}

	@FXML
	private void initialize() {
		Button okButton = (Button) this.getDialogPane().lookupButton(okButtonType);
		okButton.disableProperty().bind(nameErrorExplanationLabel.textProperty().isNotEmpty().or(locationErrorExplanationLabel.textProperty().isNotEmpty()));

		changeLocationButton.setOnAction(e -> {
			// TODO: change the initial directory to the parent directory of the old machine path
			Path result = fileChooserManager.showOpenMachineChooser(this.getOwner());
			if (result != null) {
				try {
					result = result.toRealPath();
					Path relative = this.currentProject.getLocation().toRealPath().relativize(result.toRealPath());
					this.locationField.setText(relative.toString());
				} catch (Exception ex) {
					LOGGER.warn("error checking new location for machine {}: {}", machine.getName(), result, ex);
				}
			}
		});
	}

	public Optional<Machine> editAndShow(Machine machine) {
		this.setTitle(i18n.translate("project.machines.editMachinesDialog.title", machine.getName()));
		this.machine = machine;

		List<Machine> machinesList = currentProject.getMachines();
		Set<String> machineNamesSet = machinesList.stream().map(Machine::getName).collect(Collectors.toSet());
		machineNamesSet.remove(machine.getName());

		nameField.textProperty().addListener((observable, from, to) -> {
			if (to == null || to.isBlank()) {
				nameErrorExplanationLabel.setText(i18n.translate("project.machines.editMachinesDialog.machineNameEmpty"));
			} else if (machineNamesSet.contains(to)) {
				nameErrorExplanationLabel.setText(i18n.translate("project.machines.editMachinesDialog.machineAlreadyExistsError", to));
			} else {
				nameErrorExplanationLabel.setText("");
			}
		});

		locationField.textProperty().addListener((observable, from, to) -> {
			if (to == null || to.isBlank()) {
				locationErrorExplanationLabel.setText(i18n.translate("project.machines.editMachinesDialog.machineLocationEmpty"));
				return;
			}

			Path newPath = null;
			try {
				newPath = this.getNewAbsolutePath();
				if (!Files.isRegularFile(newPath)) {
					locationErrorExplanationLabel.setText(i18n.translate("project.machines.editMachinesDialog.machineLocationDoesNotExistError", to));
					return;
				}
			} catch (Exception e) {
				LOGGER.warn("error checking new location for machine {}: {}", machine.getName(), newPath != null ? newPath : this.locationField.getText(), e);
				locationErrorExplanationLabel.setText(i18n.translate("project.machines.editMachinesDialog.machineLocationError", to));
				return;
			}

			locationErrorExplanationLabel.setText("");
		});

		nameField.setText(machine.getName());
		locationField.setText(machine.getLocation().toString());
		descriptionTextArea.setText(machine.getDescription());

		return super.showAndWait();
	}

	private Path getOldAbsolutePath() {
		return this.currentProject.get().getAbsoluteMachinePath(this.machine);
	}

	private Path getNewAbsolutePath() {
		return this.currentProject.getLocation().resolve(this.locationField.getText());
	}
}
