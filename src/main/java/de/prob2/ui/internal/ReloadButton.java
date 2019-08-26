package de.prob2.ui.internal;

import com.google.inject.Inject;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.MachineLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

@FXMLInjected
public class ReloadButton extends Button {

	private final CurrentProject currentProject;
	private final MachineLoader machineLoader;

	@Inject
	private ReloadButton(final StageManager stageManager, final CurrentProject currentProject, final MachineLoader machineLoader) {
		super();
		this.currentProject = currentProject;
		this.machineLoader = machineLoader;

		stageManager.loadFXML(this, "reload_button.fxml");
	}

	@FXML
	private void initialize() {
		this.setOnAction(event -> currentProject.reloadCurrentMachine());
		this.disableProperty().bind(currentProject.currentMachineProperty().isNull().or(machineLoader.loadingProperty()));
	}
}
