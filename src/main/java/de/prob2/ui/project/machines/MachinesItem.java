package de.prob2.ui.project.machines;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class MachinesItem extends VBox {
	@FXML private Label nameLabel;
	@FXML private FontAwesomeIconView runningIcon;
	@FXML private Label locationLabel;

	private final Machine machine;

	MachinesItem(final Machine machine, final StageManager stageManager) {
		this.machine = machine;
		stageManager.loadFXML(this, "machines_item.fxml");
	}

	@FXML
	private void initialize() {
		nameLabel.setText(machine.getName());
		locationLabel.setText(machine.getPath().toString());
		nameLabel.setText(machine.getLastUsed().getName() + " : " + machine.getName());
	}
	
	Machine getMachine() {
		return machine;
	}

	void refresh() {
		nameLabel.setText(machine.getName());
	}

	void setRunning() {
		runningIcon.getStyleClass().add("running");
		nameLabel.setText(machine.getLastUsed().getName() + " : " + machine.getName());
	}

	void setNotRunning() {
		runningIcon.getStyleClass().remove("running");
		nameLabel.setText(machine.getLastUsed().getName() + " : " + machine.getName());
	}
}
