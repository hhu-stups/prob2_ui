package de.prob2.ui.project.machines;

import java.util.ResourceBundle;

import com.google.inject.Injector;

import de.prob2.ui.internal.StageManager;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

public class MachineView extends AnchorPane {
	
	@FXML
	private Label titelLabel;
	@FXML
	private Text descriptionText;
	
	private final Machine machine;
	private final Injector injector;
	private final ResourceBundle bundle;
	
	MachineView(final Machine machine, final StageManager stageManager, final Injector injector) {
		this.machine = machine;
		this.injector = injector;
		this.bundle = injector.getInstance(ResourceBundle.class);
		stageManager.loadFXML(this, "machine_view.fxml");
	}

	@FXML
	public void initialize() {
		titelLabel.setText(String.format(bundle.getString("project.machines.descriptionView.title"),machine.getName()));
		descriptionText.setText(machine.getDescription().isEmpty()? bundle.getString("project.machines.descriptionView.placeholder") : machine.getDescription());
	}

	@FXML
	public void closeMachineView() {
		injector.getInstance(MachinesTab.class).closeMachineView();
	}

	Machine getMachine() {
		return this.machine;
	}
}
