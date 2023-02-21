package de.prob2.ui.simulation.interactive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import de.prob.json.JacksonManager;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.ProBFileHandler;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;

import java.io.IOException;
import java.nio.file.Path;

public class UIInteractionSaver extends ProBFileHandler {

	private final UIInteractionHandler uiInteraction;

	private final RealTimeSimulator realTimeSimulator;

	private final JacksonManager<SimulationModelConfiguration> jsonManager;

	@Inject
	public UIInteractionSaver(final VersionInfo versionInfo, final CurrentProject currentProject, final StageManager stageManager,
							  final FileChooserManager fileChooserManager, final I18n i18n, final UIInteractionHandler uiInteraction,
							  final RealTimeSimulator realTimeSimulator, final JacksonManager<SimulationModelConfiguration> jsonManager,
							  final ObjectMapper objectMapper) {
		super(versionInfo, currentProject, stageManager, fileChooserManager, i18n);
		this.uiInteraction = uiInteraction;
		this.realTimeSimulator = realTimeSimulator;
		this.jsonManager = jsonManager;
		jsonManager.initContext(new JacksonManager.Context<>(objectMapper, SimulationModelConfiguration.class, SimulationModelConfiguration.SimulationFileType.INTERACTION_REPLAY.getName(), SimulationModelConfiguration.CURRENT_FORMAT_VERSION));
	}

	public void saveUIInteractions() throws IOException {
		final Path path = openSaveFileChooser("simulation.tracereplay.fileChooser.saveUIReplay.title", "common.fileChooser.fileTypes.proB2Simulation", FileChooserManager.Kind.SIMULATION, "json");
		if (path != null) {
			SimulationModelConfiguration configuration = uiInteraction.createUserInteractionSimulation(realTimeSimulator);
			this.jsonManager.writeToFile(path, configuration);
		}

	}

}
