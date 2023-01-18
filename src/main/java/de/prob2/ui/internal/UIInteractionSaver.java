package de.prob2.ui.internal;

import com.google.inject.Inject;
import de.prob.json.JacksonManager;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.simulation.configuration.SimulationConfiguration;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;

import java.io.IOException;
import java.nio.file.Path;

public class UIInteractionSaver extends ProBFileHandler {

	private final UIInteraction uiInteraction;

	private final RealTimeSimulator realTimeSimulator;

	private final JacksonManager<SimulationConfiguration> jsonManager;

	@Inject
	public UIInteractionSaver(final VersionInfo versionInfo, final CurrentProject currentProject, final StageManager stageManager, final FileChooserManager fileChooserManager, final I18n i18n, final UIInteraction uiInteraction, final RealTimeSimulator realTimeSimulator, final JacksonManager<SimulationConfiguration> jsonManager) {
		super(versionInfo, currentProject, stageManager, fileChooserManager, i18n);
		this.uiInteraction = uiInteraction;
		this.realTimeSimulator = realTimeSimulator;
		this.jsonManager = jsonManager;
	}

	public void saveAsAutomaticSimulation() throws IOException {
		final Path path = openSaveFileChooser("simulation.tracereplay.fileChooser.saveTimedTrace.title", "common.fileChooser.fileTypes.proB2Simulation", FileChooserManager.Kind.SIMULATION, "Automatic_Simulation_with_User_Interaction");
		if (path != null) {
			SimulationConfiguration configuration = uiInteraction.createAutomaticSimulation(realTimeSimulator);
			this.jsonManager.writeToFile(path, configuration);
		}

	}

}
