package de.prob2.ui.project.machines;

import com.google.inject.Inject;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.prob2.ui.internal.StageManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class MachinesItem extends VBox {
	@FXML
	private Label nameLabel;
	@FXML
	private Label locationLabel;

	private Machine machine;

	@Inject
	MachinesItem(Machine machine, final StageManager stageManager) {
		this.machine = machine;
		stageManager.loadFXML(this, "machines_item.fxml");
	}

	@FXML
	public void initialize() {
		nameLabel.setText(machine.getName());
		locationLabel.setText(machine.getPath().toString());
		final FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.PLAY);
		icon.setId("machines-icon");
		nameLabel.setGraphic(icon);
        nameLabel.setText(machine.lastUsed.getName() + " : " + machine.getName());
    }
	
	Machine getMachine() {
		return machine;
	}

	public void refresh() {
		nameLabel.setText(machine.getName());
	}

	public void setRunning(){
		final FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.SPINNER);
		icon.setId("machines-icon-running");
		nameLabel.setGraphic(icon);
		nameLabel.setText(machine.lastUsed.getName() + " : " + machine.getName());
	}

	public void setNotRunning(){
		final FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.PLAY);
		icon.setId("machines-icon");
		nameLabel.setGraphic(icon);
		nameLabel.setText(machine.lastUsed.getName() + " : " + machine.getName());
	}
}
