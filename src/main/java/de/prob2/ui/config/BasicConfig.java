package de.prob2.ui.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Locale;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simplified version of {@link Config} that can only load a subset of all config settings and doesn't support saving.
 * This is needed to read certain config settings before the injector has been set up,
 * for example the locale override.
 */
public final class BasicConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(BasicConfig.class);
	
	private final Gson gson;
	
	public BasicConfig(final Gson gson) {
		super();
		
		this.gson = gson;
	}
	
	/**
	 * Load basic settings from the config file.
	 *
	 * @return basic settings from the config file
	 */
	private BasicConfigData load() {
		try (final Reader reader = Files.newBufferedReader(Config.LOCATION)) {
			final BasicConfigData data = gson.fromJson(reader, BasicConfigData.class);
			if (data == null) {
				// Config file is empty, use defaults instead.
				return new BasicConfigData();
			}
			return data;
		} catch (FileNotFoundException | NoSuchFileException exc) {
			LOGGER.info("Config file not found while loading basic config, loading default settings", exc);
			return new BasicConfigData();
		} catch (IOException exc) {
			LOGGER.warn("Failed to open config file while loading basic config", exc);
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
