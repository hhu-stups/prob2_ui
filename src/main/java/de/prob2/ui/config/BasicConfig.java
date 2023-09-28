package de.prob2.ui.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob2.ui.internal.ConfigFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simplified version of {@link Config} that can only load a subset of all config settings and doesn't support saving.
 * This is needed to read certain config settings before the injector has been set up,
 * for example the locale override.
 */
@Singleton
public final class BasicConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(BasicConfig.class);
	
	private final Path configFilePath;
	private final ObjectMapper objectMapper;
	
	@Inject
	private BasicConfig(final @ConfigFile Path configFilePath, final ObjectMapper objectMapper) {
		super();
		
		this.configFilePath = configFilePath;
		this.objectMapper = objectMapper;
	}
	
	/**
	 * Load basic settings from the config file.
	 *
	 * @return basic settings from the config file
	 */
	private BasicConfigData load() {
		try {
			final BasicConfigData data = this.objectMapper.readValue(this.configFilePath.toFile(), BasicConfigData.class);
			// Config file is empty, use defaults instead.
			return Objects.requireNonNullElseGet(data, BasicConfigData::new);
		} catch (FileNotFoundException | NoSuchFileException exc) {
			LOGGER.info("Config file not found while loading basic config, loading default settings", exc);
			return new BasicConfigData();
		} catch (IOException exc) {
			LOGGER.warn("Failed to load basic config", exc);
			return new BasicConfigData();
		}
	}
	
	/**
	 * Get the locale override from the config file.
	 *
	 * @return the locale override
	 */
	public Locale getLocaleOverride() {
		return load().localeOverride;
	}
}
