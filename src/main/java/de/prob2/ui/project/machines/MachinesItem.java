package de.prob2.ui.project.machines;

import java.util.Objects;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class MachinesItem extends VBox {
	@FXML private Label nameLabel;
	@FXML private FontAwesomeIconView runningIcon;
	@FXML private Label locationLabel;

	private final Machine machine;
	private final CurrentProject currentProject;

	MachinesItem(final Machine machine, final StageManager stageManager, final CurrentProject currentProject) {
		Objects.requireNonNull(machine, "machine");
		this.machine = machine;
		this.currentProject = currentProject;
		stageManager.loadFXML(this, "machines_item.fxml");
	}

	@FXML
	private void initialize() {
		this.refresh();
		locationLabel.setText(machine.getPath().toString());
		currentProject.currentMachineProperty().addListener(o -> this.refresh());
	}
	
	Machine getMachine() {
		return machine;
	}

	void refresh() {
		if (this.machine.equals(currentProject.getCurrentMachine())) {
			if (!runningIcon.getStyleClass().contains("running")) {
				runningIcon.getStyleClass().add("running");
			}
		} else {
			runningIcon.getStyleClass().remove("running");
		}
		nameLabel.setText(machine.getLastUsed().getName() + " : " + machine.getName());
	}
}
