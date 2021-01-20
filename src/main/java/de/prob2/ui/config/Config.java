package de.prob2.ui.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.json.JsonManager;
import de.prob.json.JsonMetadata;
import de.prob.json.ObjectWithMetadata;
import de.prob2.ui.MainController;
import de.prob2.ui.internal.ConfigFile;
import de.prob2.ui.internal.PerspectiveKind;
import de.prob2.ui.internal.StopActions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class Config {
	private static final Logger logger = LoggerFactory.getLogger(Config.class);

	private final Path configFilePath;
	private final JsonManager<ConfigData> jsonManager;
	private final RuntimeOptions runtimeOptions;
	
	private ConfigData currentConfigData;
	private final List<ConfigListener> listeners;

	@Inject
	private Config(final @ConfigFile Path configFilePath, final Gson gson, final JsonManager<ConfigData> jsonManager, final RuntimeOptions runtimeOptions, final StopActions stopActions) {
		this.configFilePath = configFilePath;
		this.jsonManager = jsonManager;
		this.jsonManager.initContext(new JsonManager.Context<ConfigData>(gson, ConfigData.class, "Config", 2) {
			private static final String GUI_STATE_FIELD = "guiState";
			private static final String PERSPECTIVE_KIND_FIELD = "perspectiveKind";
			private static final String PERSPECTIVE_FIELD = "perspective";
			private static final String ERROR_LEVEL_FIELD = "errorLevel";
			
			@Override
			public ObjectWithMetadata<JsonObject> convertOldData(final JsonObject oldObject, final JsonMetadata oldMetadata) {
				if (oldMetadata.getFormatVersion() <= 0 && oldObject.has(GUI_STATE_FIELD)) {
					final String guiState = oldObject.remove(GUI_STATE_FIELD).getAsString();
					final PerspectiveKind perspectiveKind;
					final String perspective;
					if (guiState.contains("detached")) {
						perspectiveKind = PerspectiveKind.PRESET;
						perspective = MainController.DEFAULT_PERSPECTIVE;
					} else if (guiState.startsWith("custom ")) {
						perspectiveKind = PerspectiveKind.CUSTOM;
						perspective = guiState.replace("custom ", "");
					} else {
						perspectiveKind = PerspectiveKind.PRESET;
						perspective = guiState;
					}
					oldObject.addProperty(PERSPECTIVE_KIND_FIELD, perspectiveKind.name());
					oldObject.addProperty(PERSPECTIVE_FIELD, perspective);
				}
				if (oldMetadata.getFormatVersion() <= 1) {
					oldObject.addProperty(ERROR_LEVEL_FIELD, "WARNING");
				}
				return new ObjectWithMetadata<>(oldObject, oldMetadata);
			}
		});
		this.runtimeOptions = runtimeOptions;

		this.listeners = new ArrayList<>();

		try {
			Files.createDirectories(this.configFilePath.getParent());
		} catch (IOException e) {
			logger.warn("Failed to create the parent directory for the config file {}", this.configFilePath, e);
		}

		this.load();
		;
		stopActions.add(this::save);
	}

	public void addListener(final ConfigListener listener) {
		this.listeners.add(listener);
		listener.loadConfig(this.currentConfigData);
	}

	public void load() {
		ConfigData configData;
		if (this.runtimeOptions.isLoadConfig()) {
			try {
				configData = this.jsonManager.readFromFile(this.configFilePath).getObject();
				logger.info("Config successfully loaded from {}", this.configFilePath);
				if (configData == null) {
					// Config file is empty, use default config.
					configData = new ConfigData();
				}
			} catch (FileNotFoundException | NoSuchFileException exc) {
				logger.info("Config file not found, loading default settings");
				configData = new ConfigData();
			} catch (IOException exc) {
				logger.warn("Failed to open config file", exc);
				return;
			}
		} else {
			logger.info("Config loading disabled via runtime options, loading default config");
			configData = new ConfigData();
		}
		
		for (final ConfigListener listener : this.listeners) {
			listener.loadConfig(configData);
		}

		this.currentConfigData = configData;
	}

	public void save() {
		if (!this.runtimeOptions.isSaveConfig()) {
			logger.info("Config saving disabled via runtime options, ignoring config save request");
			return;
		}
		
		final ConfigData configData = this.currentConfigData;

		for (final ConfigListener listener : this.listeners) {
			listener.saveConfig(configData);
		}

		try {
			this.jsonManager.writeToFile(this.configFilePath, configData);
		} catch (FileNotFoundException | NoSuchFileException exc) {
			logger.warn("Failed to create config file", exc);
		} catch (IOException exc) {
			logger.warn("Failed to save config file", exc);
		}
		logger.info("Config successfully saved to {}", this.configFilePath);
	}
}
