package de.prob2.ui.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.json.JacksonManager;
import de.prob.json.JsonConversionException;
import de.prob2.ui.internal.ConfigFile;
import de.prob2.ui.internal.StopActions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class Config {
	private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

	private final Path configFilePath;
	private final JacksonManager<ConfigData> jacksonManager;
	private final RuntimeOptions runtimeOptions;
	
	private ConfigData currentConfigData;
	private final List<ConfigListener> listeners;

	@Inject
	private Config(final @ConfigFile Path configFilePath, final ObjectMapper objectMapper, final JacksonManager<ConfigData> jacksonManager, final RuntimeOptions runtimeOptions, final StopActions stopActions) {
		this.configFilePath = configFilePath;
		this.jacksonManager = jacksonManager;
		this.jacksonManager.initContext(new JacksonManager.Context<>(objectMapper, ConfigData.class, ConfigData.FILE_TYPE, ConfigData.CURRENT_FORMAT_VERSION) {
			@Override
			public ObjectNode convertOldData(final ObjectNode oldObject, final int oldVersion) {
				if (oldVersion <= 0) {
					throw new JsonConversionException("Loading config files older than format version 1 is no longer supported (config file uses format version " + oldVersion + ")");
				}

				if (oldVersion <= 1) {
					oldObject.put("errorLevel", "WARNING");
				}

				if (oldVersion <= 2) {
					JsonNode currentMainTabNode = oldObject.get("currentMainTab");
					if (currentMainTabNode != null && "visualisation".equals(currentMainTabNode.textValue())) {
						oldObject.set("currentMainTab", oldObject.nullNode());
						oldObject.put("currentVisualisationTab", "animationFunction");
					} else {
						oldObject.put("currentVisualisationTab", "visb");
					}

					JsonNode bConsoleExpandedNode = oldObject.remove("bConsoleExpanded");
					if (bConsoleExpandedNode.isBoolean() && bConsoleExpandedNode.booleanValue()) {
						JsonNode expandedTitledPanesNode = oldObject.get("expandedTitledPanes");
						if (expandedTitledPanesNode instanceof ArrayNode expandedTitledPanes) {
							expandedTitledPanes.add("bconsole");
						}
					}
				}

				// Version 4 just adds auto reload machine option
				if (oldVersion <= 3) {
					oldObject.put("autoReloadMachine", "true");
				}

				return oldObject;
			}
		});
		this.runtimeOptions = runtimeOptions;

		this.listeners = new ArrayList<>();

		try {
			Files.createDirectories(this.configFilePath.getParent());
		} catch (IOException e) {
			LOGGER.warn("Failed to create the parent directory for the config file {}", this.configFilePath, e);
		}

		this.load();
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
				configData = this.jacksonManager.readFromFile(this.configFilePath);
				LOGGER.info("Config successfully loaded from {}", this.configFilePath);
				if (configData == null) {
					// Config file is empty, use default config.
					configData = new ConfigData();
				}
			} catch (FileNotFoundException | NoSuchFileException exc) {
				LOGGER.info("Config file not found, loading default settings");
				configData = new ConfigData();
			} catch (IOException exc) {
				LOGGER.warn("Failed to load config file", exc);
				throw new UncheckedIOException("Failed to load config file: " + exc, exc);
			}
		} else {
			LOGGER.info("Config loading disabled via runtime options, loading default config");
			configData = new ConfigData();
		}
		
		for (final ConfigListener listener : this.listeners) {
			listener.loadConfig(configData);
		}

		this.currentConfigData = configData;
	}

	public void save() {
		if (!this.runtimeOptions.isSaveConfig()) {
			LOGGER.info("Config saving disabled via runtime options, ignoring config save request");
			return;
		}
		
		final ConfigData configData = this.currentConfigData;
		
		// Replace any existing metadata that was previously loaded from the file.
		configData.setMetadata(ConfigData.metadataBuilder().build());

		for (final ConfigListener listener : this.listeners) {
			listener.saveConfig(configData);
		}

		try {
			this.jacksonManager.writeToFile(this.configFilePath, configData);
		} catch (FileNotFoundException | NoSuchFileException exc) {
			LOGGER.warn("Failed to create config file", exc);
		} catch (IOException exc) {
			LOGGER.warn("Failed to save config file", exc);
		}
		LOGGER.info("Config successfully saved to {}", this.configFilePath);
	}
}
