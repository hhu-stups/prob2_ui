package de.prob2.ui.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.Main;
import de.prob2.ui.internal.StopActions;
import de.prob2.ui.json.JsonManager;
import de.prob2.ui.json.JsonMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class Config {
	// This Gson instance is for loadBasicConfig only. Everywhere else, JsonManager should be used.
	private static final Gson BASIC_GSON = new GsonBuilder().create();
	private static final Path LOCATION = Paths.get(Main.getProBDirectory(), "prob2ui", "config.json");
	private static final String FILE_TYPE = "Config";
	private static final int CURRENT_FORMAT_VERSION = 0;

	private static final Logger logger = LoggerFactory.getLogger(Config.class);

	private final JsonManager jsonManager;
	private final RuntimeOptions runtimeOptions;
	
	private ConfigData currentConfigData;
	private final List<ConfigListener> listeners;

	@Inject
	private Config(final JsonManager jsonManager, final RuntimeOptions runtimeOptions, final StopActions stopActions) {
		this.jsonManager = jsonManager;
		this.runtimeOptions = runtimeOptions;

		this.listeners = new ArrayList<>();

		try {
			Files.createDirectories(LOCATION.getParent());
		} catch (IOException e) {
			logger.warn("Failed to create the parent directory for the config file {}", LOCATION, e);
		}

		this.load();
		
		stopActions.add(this::save);
	}

	/**
	 * Load basic settings from the config file. This method is static so it can be called before all of {@link Config}'s dependencies are available.
	 *
	 * @return basic settings from the config file
	 */
	private static BasicConfigData loadBasicConfig() {
		try (final Reader reader = Files.newBufferedReader(LOCATION)) {
			final BasicConfigData data = BASIC_GSON.fromJson(reader, BasicConfigData.class);
			if (data == null) {
				// Config file is empty, use defaults instead.
				return new BasicConfigData();
			}
			return data;
		} catch (FileNotFoundException | NoSuchFileException exc) {
			logger.info("Config file not found while loading basic config, loading default settings", exc);
			return new BasicConfigData();
		} catch (IOException exc) {
			logger.warn("Failed to open config file while loading basic config", exc);
			return new BasicConfigData();
		}
	}

	/**
	 * Get the locale override from the config file. This method is static so it can be called before all of {@link Config}'s dependencies are available.
	 *
	 * @return the locale override
	 */
	public static Locale getLocaleOverride() {
		return loadBasicConfig().localeOverride;
	}

	public void addListener(final ConfigListener listener) {
		this.listeners.add(listener);
		listener.loadConfig(this.currentConfigData);
	}

	public void load() {
		ConfigData configData;
		if (this.runtimeOptions.isLoadConfig()) {
			try (final Reader reader = Files.newBufferedReader(LOCATION)) {
				configData = this.jsonManager.read(reader, ConfigData.class, FILE_TYPE, CURRENT_FORMAT_VERSION).getObject();
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

		try (final Writer writer = Files.newBufferedWriter(LOCATION)) {
			final JsonMetadata metadata = this.jsonManager.metadataBuilder(FILE_TYPE, CURRENT_FORMAT_VERSION)
				.withCurrentInfo()
				.withUserCreator()
				.build();
			this.jsonManager.write(writer, configData, metadata);
		} catch (FileNotFoundException | NoSuchFileException exc) {
			logger.warn("Failed to create config file", exc);
		} catch (IOException exc) {
			logger.warn("Failed to save config file", exc);
		}
	}
}
