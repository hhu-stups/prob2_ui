package de.prob2.ui.visualisation.magiclayout;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ResourceBundle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;

import de.prob.json.JacksonManager;
import de.prob.json.JsonConversionException;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.stage.FileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MagicLayoutSettingsManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(MagicLayoutSettingsManager.class);
	private static final String MAGIC_FILE_EXTENSION = "prob2magic";

	private final JacksonManager<MagicLayoutSettings> jsonManager;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final ResourceBundle bundle;

	@Inject
	public MagicLayoutSettingsManager(ObjectMapper objectMapper, JacksonManager<MagicLayoutSettings> jsonManager, CurrentProject currentProject, StageManager stageManager,
									  FileChooserManager fileChooserManager, ResourceBundle bundle) {
		this.jsonManager = jsonManager;
		this.jsonManager.initContext(new JacksonManager.Context<MagicLayoutSettings>(objectMapper, MagicLayoutSettings.class, MagicLayoutSettings.FILE_TYPE, MagicLayoutSettings.CURRENT_FORMAT_VERSION) {
			@Override
			public boolean shouldAcceptOldMetadata() {
				return true;
			}

			@Override
			public ObjectNode convertOldData(final ObjectNode oldObject, final int oldVersion) {
				if (oldVersion == 0) {
					for (final String fieldName : new String[] {"machineName", "nodegroups", "edgegroups"}) {
						if (!oldObject.has(fieldName)) {
							throw new JsonConversionException("Not a valid Magic Layout settings file - missing required field " + fieldName);
						}
					}
				}
				return oldObject;
			}
		});
		this.currentProject = currentProject;
		this.stageManager = stageManager;
		this.fileChooserManager = fileChooserManager;
		this.bundle = bundle;
	}

	public void save(MagicLayoutSettings layoutSettings) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(
				bundle.getString("visualisation.magicLayout.settingsManager.fileChooser.saveLayoutSettings.title"));

		final Path magicSettingsFolder = currentProject.getLocation().resolve("magic_graph").resolve("settings");
		if (!magicSettingsFolder.toFile().exists()) {
			magicSettingsFolder.toFile().mkdirs();
		}
		fileChooser.setInitialDirectory(magicSettingsFolder.toFile());

		fileChooser.setInitialFileName(currentProject.getCurrentMachine().getName() + "." + MAGIC_FILE_EXTENSION);
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.proB2MagicSettings", MAGIC_FILE_EXTENSION));
		final Path path = fileChooserManager.showSaveFileChooser(fileChooser, null, stageManager.getCurrent());

		if (path != null) {
			try {
				this.jsonManager.writeToFile(path, layoutSettings);
			} catch (FileNotFoundException exc) {
				LOGGER.warn("Failed to create layout settings file", exc);
				stageManager.makeExceptionAlert(exc,
						"visualisation.magicLayout.settingsManager.alert.failedToCreateLayoutSettingsFile.content")
						.showAndWait();
			} catch (IOException exc) {
				LOGGER.warn("Failed to save layout settings", exc);
				stageManager.makeExceptionAlert(exc,
						"visualisation.magicLayout.settingsManager.alert.failedToCreateLayoutSettingsFile.content")
						.showAndWait();
			}
		}
	}

	public MagicLayoutSettings load() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(
				bundle.getString("visualisation.magicLayout.settingsManager.fileChooser.loadLayoutSettings.title"));

		final Path magicSettingsFolder = currentProject.getLocation().resolve("magic_graph").resolve("settings");
		if (!magicSettingsFolder.toFile().exists()) {
			magicSettingsFolder.toFile().mkdirs();
		}
		fileChooser.setInitialDirectory(magicSettingsFolder.toFile());
		fileChooser.getExtensionFilters().add(fileChooserManager.getExtensionFilter("common.fileChooser.fileTypes.proB2MagicSettings", MAGIC_FILE_EXTENSION));
		final Path path = fileChooserManager.showOpenFileChooser(fileChooser, null, stageManager.getCurrent());
		if (path != null) {
			try {
				return this.jsonManager.readFromFile(path);
			} catch (IOException e) {
				LOGGER.warn("Failed to read magic layout settings file", e);
				stageManager.makeExceptionAlert(e, "", "common.alerts.couldNotReadFile.content", path).showAndWait();
			}
		}
		return null;
	}
}
