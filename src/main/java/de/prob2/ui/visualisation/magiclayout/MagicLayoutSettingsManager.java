package de.prob2.ui.visualisation.magiclayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

import com.google.inject.Inject;

import de.prob2.ui.internal.StageManager;
import de.prob2.ui.json.JsonManager;
import de.prob2.ui.json.JsonMetadata;
import de.prob2.ui.prob2fx.CurrentProject;

import javafx.scene.control.Alert.AlertType;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MagicLayoutSettingsManager {
	private static final Charset CHARSET = StandardCharsets.UTF_8;
	private static final Logger LOGGER = LoggerFactory.getLogger(MagicLayoutSettingsManager.class);
	private static final String MAGIC_FILE_ENDING = "*.prob2magic";
	private static final String FILE_TYPE = "Magic Layout settings";
	private static final int CURRENT_FORMAT_VERSION = 0;

	private final JsonManager jsonManager;
	private final CurrentProject currentProject;
	private final StageManager stageManager;
	private final ResourceBundle bundle;

	@Inject
	public MagicLayoutSettingsManager(JsonManager jsonManager, CurrentProject currentProject, StageManager stageManager,
			ResourceBundle bundle) {
		this.jsonManager = jsonManager;
		this.currentProject = currentProject;
		this.stageManager = stageManager;
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

		fileChooser.setInitialFileName(currentProject.getCurrentMachine().getName() + MAGIC_FILE_ENDING.substring(1));
		fileChooser.getExtensionFilters()
				.add(new ExtensionFilter(String
						.format(bundle.getString("common.fileChooser.fileTypes.proB2MagicSettings"), MAGIC_FILE_ENDING),
						MAGIC_FILE_ENDING));
		File file = fileChooser.showSaveDialog(stageManager.getCurrent());

		if (file != null) {
			try (final Writer writer = Files.newBufferedWriter(file.toPath(), CHARSET)) {
				final JsonMetadata metadata = this.jsonManager.metadataBuilder(FILE_TYPE, CURRENT_FORMAT_VERSION)
					.withCurrentInfo()
					.withUserCreator()
					.withModelName(layoutSettings.getMachineName())
					.build();
				this.jsonManager.write(writer, layoutSettings, metadata);
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
		fileChooser.getExtensionFilters()
				.add(new ExtensionFilter(String
						.format(bundle.getString("common.fileChooser.fileTypes.proB2MagicSettings"), MAGIC_FILE_ENDING),
						MAGIC_FILE_ENDING));
		File file = fileChooser.showOpenDialog(stageManager.getCurrent());
		if (file != null) {
			try {
				Reader reader = Files.newBufferedReader(file.toPath(), CHARSET);
				MagicLayoutSettings layoutSettings = this.jsonManager.read(reader, MagicLayoutSettings.class, FILE_TYPE, CURRENT_FORMAT_VERSION).getObject();
				if (isValidMagicLayoutSettings(layoutSettings)) {
					return layoutSettings;
				}
				LOGGER.warn(String.format("Could not open file '%s'. The file does not contain valid Magic Layout settings.", file));
				stageManager.makeAlert(AlertType.ERROR, "", "visualisation.magicLayout.settingsManager.alert.noValidLayoutSettings.content", file);
			} catch (IOException e) {
				LOGGER.warn("Failed to read magic layout settings file", e);
				stageManager.makeExceptionAlert(e, "", "common.alerts.couldNotReadFile.content", file);
			}
		}
		return null;
	}

	private boolean isValidMagicLayoutSettings(MagicLayoutSettings layoutSettings) {
		return layoutSettings.getMachineName() != null && layoutSettings.getNodegroups() != null
				&& layoutSettings.getEdgegroups() != null;
	}

}
