package de.prob2.ui.simulation.interactive;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import de.prob.json.JacksonManager;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.I18n;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;
import de.prob2.ui.simulation.configuration.SimulationModelConfiguration;
import de.prob2.ui.simulation.simulators.RealTimeSimulator;

import javafx.stage.FileChooser;

public final class UIInteractionSaver {
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final I18n i18n;
	private final UIInteractionHandler uiInteraction;
	private final RealTimeSimulator realTimeSimulator;
	private final JacksonManager<SimulationModelConfiguration> jsonManager;

	@Inject
	public UIInteractionSaver(final CurrentProject currentProject, final StageManager stageManager,
							  final FileChooserManager fileChooserManager, final I18n i18n, final UIInteractionHandler uiInteraction,
							  final RealTimeSimulator realTimeSimulator, final JacksonManager<SimulationModelConfiguration> jsonManager,
							  final ObjectMapper objectMapper) {
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.i18n = i18n;
		this.uiInteraction = uiInteraction;
		this.realTimeSimulator = realTimeSimulator;
		this.jsonManager = jsonManager;
		jsonManager.initContext(new JacksonManager.Context<>(objectMapper, SimulationModelConfiguration.class, SimulationModelConfiguration.SimulationFileType.INTERACTION_REPLAY.getName(), SimulationModelConfiguration.CURRENT_FORMAT_VERSION));
	}

	public void saveUIInteractions() throws IOException {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(i18n.translate("simulation.tracereplay.fileChooser.saveUIReplay.title"));
		fileChooser.setInitialFileName(currentProject.getCurrentMachine().getName() + ".json");
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.simulation", "json"));
		final Path path = this.fileChooserManager.showSaveFileChooser(fileChooser, FileChooserManager.Kind.SIMULATION, stageManager.getCurrent());
		if (path != null) {
			SimulationModelConfiguration configuration = uiInteraction.createUserInteractionSimulation(realTimeSimulator);
			this.jsonManager.writeToFile(path, configuration);
		}

	}

}
