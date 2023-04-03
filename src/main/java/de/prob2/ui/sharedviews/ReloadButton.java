package de.prob2.ui.sharedviews;

import com.google.inject.Inject;

import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.project.MachineLoader;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class ReloadButton {

	private final CurrentProject currentProject;
	private final MachineLoader machineLoader;
	private final RealTimeSimulator realTimeSimulator;

	@FXML
	private Button button;

	@Inject
	private ReloadButton(final CurrentProject currentProject, final MachineLoader machineLoader, final RealTimeSimulator realTimeSimulator) {
		super();
		this.currentProject = currentProject;
		this.machineLoader = machineLoader;
		this.realTimeSimulator = realTimeSimulator;
	}

	@FXML
	private void initialize() {
		button.disableProperty().bind(currentProject.currentMachineProperty().isNull().or(machineLoader.loadingProperty()).or(realTimeSimulator.runningProperty()));
		button.setOnAction(event -> currentProject.reloadCurrentMachine());
	}
}
