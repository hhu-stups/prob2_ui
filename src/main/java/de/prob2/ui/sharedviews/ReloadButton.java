package de.prob2.ui.sharedviews;

import com.google.inject.Inject;

import de.prob2.ui.internal.FXMLInjected;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.MachineLoader;

import de.prob2.ui.simulation.simulators.RealTimeSimulator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

@FXMLInjected
public class ReloadButton extends Button {

	private final CurrentProject currentProject;
	private final MachineLoader machineLoader;
	private final RealTimeSimulator realTimeSimulator;

	@Inject
	private ReloadButton(final StageManager stageManager, final CurrentProject currentProject, final MachineLoader machineLoader, final RealTimeSimulator realTimeSimulator) {
		super();
		this.currentProject = currentProject;
		this.machineLoader = machineLoader;
		this.realTimeSimulator = realTimeSimulator;

		stageManager.loadFXML(this, "reload_button.fxml");
	}

	@FXML
	private void initialize() {
		this.disableProperty().bind(currentProject.currentMachineProperty().isNull().or(machineLoader.loadingProperty()).or(realTimeSimulator.runningProperty()));
		this.setOnAction(event -> currentProject.reloadCurrentMachine());
	}
}
