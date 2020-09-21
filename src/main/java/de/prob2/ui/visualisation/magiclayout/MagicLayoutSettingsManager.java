package de.prob2.ui.visualisation.magiclayout;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ResourceBundle;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.inject.Inject;

import de.prob.json.JsonManager;
import de.prob.json.JsonMetadata;
import de.prob.json.ObjectWithMetadata;
import de.prob2.ui.config.FileChooserManager;
import de.prob2.ui.internal.StageManager;
import de.prob2.ui.internal.VersionInfo;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.stage.FileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MagicLayoutSettingsManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(MagicLayoutSettingsManager.class);
	private static final String MAGIC_FILE_EXTENSION = "prob2magic";

	private final JsonManager<MagicLayoutSettings> jsonManager;
	private final VersionInfo versionInfo;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final FileChooserManager fileChooserManager;
	private final ResourceBundle bundle;

	@Inject
	public MagicLayoutSettingsManager(Gson gson, JsonManager<MagicLayoutSettings> jsonManager, VersionInfo versionInfo, CurrentProject currentProject, StageManager stageManager,
									  FileChooserManager fileChooserManager, ResourceBundle bundle) {
		this.jsonManager = jsonManager;
		this.jsonManager.initContext(new JsonManager.Context<MagicLayoutSettings>(gson, MagicLayoutSettings.class, "Magic Layout settings", 1) {
			@Override
			public ObjectWithMetadata<JsonObject> convertOldData(final JsonObject oldObject, final JsonMetadata oldMetadata) {
				if (oldMetadata.getFileType() == null) {
					assert oldMetadata.getFormatVersion() == 0;
					for (final String fieldName : new String[] {"machineName", "nodegroups", "edgegroups"}) {
						if (!oldObject.has(fieldName)) {
							throw new JsonParseException("Not a valid Magic Layout settings file - missing required field " + fieldName);
						}
					}
				}
				return new ObjectWithMetadata<>(oldObject, oldMetadata);
			}
		});
		this.versionInfo = versionInfo;
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
				final JsonMetadata metadata = this.jsonManager.defaultMetadataBuilder()
					.withProBCliVersion(versionInfo.getCliVersion().getShortVersionString())
					.withModelName(layoutSettings.getMachineName())
					.build();
				this.jsonManager.writeToFile(path, layoutSettings, metadata);
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
				return this.jsonManager.readFromFile(path).getObject();
			} catch (IOException e) {
				LOGGER.warn("Failed to read magic layout settings file", e);
				stageManager.makeExceptionAlert(e, "", "common.alerts.couldNotReadFile.content", path);
			}
		}
		return null;
	}
}
