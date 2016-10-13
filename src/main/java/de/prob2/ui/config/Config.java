package de.prob2.ui.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import de.prob.Main;
import de.prob.model.representation.AbstractElement;

import de.prob2.ui.menu.RecentFiles;
import de.prob2.ui.states.ClassBlacklist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
public final class Config {
	private static final class ConfigData {
		private int maxRecentFiles;
		private List<String> recentFiles;
		private List<String> statesViewHiddenClasses;
	}
	
	private static final File LOCATION = new File(Main.getProBDirectory() + File.separator + "prob2ui" + File.separator + "config.json");
	private static final File DEFAULT;
	static {
		try {
			DEFAULT = new File(Config.class.getResource("default.json").toURI());
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}
	private static final Logger logger = LoggerFactory.getLogger(Config.class);
	
	private final Gson gson;
	private final ClassBlacklist classBlacklist;
	private final RecentFiles recentFiles;
	
	private final ConfigData defaultData;
	
	@Inject
	private Config(final ClassBlacklist classBlacklist, final RecentFiles recentFiles) {
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		this.classBlacklist = classBlacklist;
		this.recentFiles = recentFiles;
		
		try (final Reader defaultReader = new InputStreamReader(new FileInputStream(DEFAULT), "UTF-8")) {
			this.defaultData = gson.fromJson(defaultReader, ConfigData.class);
		} catch (FileNotFoundException exc) {
			throw new IllegalStateException("Default config file not found", exc);
		} catch (IOException exc) {
			throw new IllegalStateException("Failed to open default config file", exc);
		}
		
		if (!LOCATION.getParentFile().exists() && !LOCATION.getParentFile().mkdirs()) {
			logger.warn("Failed to create the parent directory for the config file {}", LOCATION.getAbsolutePath());
		}
		
		this.load();
	}
	
	public void load() {
		ConfigData configData;
		try (final Reader reader = new InputStreamReader(new FileInputStream(LOCATION), "UTF-8")) {
			configData = gson.fromJson(reader, ConfigData.class);
		} catch (FileNotFoundException ignored) { // NOSONAR
			// Config file doesn't exist yet, use the defaults
			configData = this.defaultData;
		} catch (IOException exc) {
			logger.warn("Failed to open config file", exc);
			return;
		}
		
		// If some keys are null (for example when loading a config from a previous version that did not have those keys), replace them with their values from the default config.
		if (configData.recentFiles == null) {
			configData.maxRecentFiles = this.defaultData.maxRecentFiles;
			configData.recentFiles = new ArrayList<>(this.defaultData.recentFiles);
		}
		
		if (configData.statesViewHiddenClasses == null) {
			configData.statesViewHiddenClasses = new ArrayList<>(this.defaultData.statesViewHiddenClasses);
		}
		
		this.recentFiles.setMaximum(configData.maxRecentFiles);
		this.recentFiles.setAll(configData.recentFiles);
		
		for (String name : configData.statesViewHiddenClasses) {
			Class<? extends AbstractElement> clazz;
			try {
				clazz = Class.forName(name).asSubclass(AbstractElement.class);
			} catch (ClassNotFoundException exc) {
				logger.warn("Class not found, cannot add to states view blacklist", exc);
				continue;
			} catch (ClassCastException exc) {
				logger.warn("Class is not a subclass of AbstractElement, cannot add to states view blacklist", exc);
				continue;
			}
			classBlacklist.getKnownClasses().add(clazz);
			classBlacklist.getBlacklist().add(clazz);
		}
	}
	
	public void save() {
		final ConfigData configData = new ConfigData();
		configData.maxRecentFiles = this.recentFiles.getMaximum();
		configData.recentFiles = new ArrayList<>(this.recentFiles);
		configData.statesViewHiddenClasses = new ArrayList<>();
		for (Class<? extends AbstractElement> clazz : classBlacklist.getBlacklist()) {
			configData.statesViewHiddenClasses.add(clazz.getCanonicalName());
		}
		
		try (final Writer writer = new OutputStreamWriter(new FileOutputStream(LOCATION), "UTF-8")) {
			gson.toJson(configData, writer);
		} catch (FileNotFoundException exc) {
			logger.warn("Failed to create config file", exc);
		} catch (IOException exc) {
			logger.warn("Failed to save config file", exc);
		}
	}
}
